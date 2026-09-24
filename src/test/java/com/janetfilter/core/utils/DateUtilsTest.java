/*
 *
 *  * Original Code by Neo Peng pengzhile@gmail.com
 *  * Copyright (C) 2026 LimonTH (Modifications and updates)
 *  *
 *  * This program is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program.  If not, see <https://gnu.org>.
 *
 */

package com.janetfilter.core.utils;

import org.junit.jupiter.api.Test;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for DateUtils.
 */
public class DateUtilsTest {

    @Test
    public void testFormatShouldReturnExpectedValues() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(2026, Calendar.JANUARY, 2, 3, 4, 5);
        calendar.set(Calendar.MILLISECOND, 0);
        Date date = calendar.getTime();

        assertEquals("2026-01-02", DateUtils.formatDate(date));
        assertEquals("03:04:05", DateUtils.formatTime(date));
        assertEquals("2026-01-02 03:04:05", DateUtils.formatDateTime(date));
        assertEquals("2026-01-02 03:04:05.000", DateUtils.formatDateTimeMicro(date));
    }

    @Test
    public void testParseDateShouldRoundTrip() throws ParseException {
        assertEquals("2026-01-02", DateUtils.formatDate(DateUtils.parseDate("2026-01-02")));
        assertEquals("2026-01-02 03:04:05", DateUtils.formatDateTime(DateUtils.parseDateTime("2026-01-02 03:04:05")));
    }

    @Test
    public void testParseShouldRejectInvalidValues() {
        assertThrows(ParseException.class, () -> DateUtils.parseDate("not-a-date"));
        assertThrows(ParseException.class, () -> DateUtils.parseDateTime("2026-01-02"));
        assertThrows(ParseException.class, () -> DateUtils.parseTime("25:61:61"));
    }

    @Test
    public void testParseExceptionShouldKeepTheCause() {
        ParseException exception = assertThrows(ParseException.class, () -> DateUtils.parseDate("nope"));

        assertNotNull(exception.getCause());
        assertTrue(exception.getMessage().contains("nope"));
    }

    @Test
    public void testFormatDateTimeMicroShouldKeepMilliseconds() {
        Date date = new Date(1_700_000_000_123L);

        assertTrue(DateUtils.formatDateTimeMicro(date).endsWith(".123"));
    }

    @Test
    public void testFormatShouldBeThreadSafe() throws Exception {
        // Regression: shared SimpleDateFormat instances are not thread safe and used to
        // produce corrupted timestamps when several logging threads formatted at once.
        int threadCount = 8;
        int iterations = 500;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        List<Future<?>> futures = new ArrayList<>();

        try {
            for (int i = 0; i < threadCount; i++) {
                futures.add(executor.submit(() -> {
                    try {
                        start.await();
                        for (int j = 0; j < iterations; j++) {
                            Date date = new Date(1_700_000_000_000L + j * 1000L);
                            String formatted = DateUtils.formatDateTime(date);
                            assertEquals(formatted, DateUtils.formatDateTime(DateUtils.parseDateTime(formatted)));
                            DateUtils.formatDate(date);
                            DateUtils.formatTime(date);
                            DateUtils.formatDateTimeMicro(date);
                        }
                    } catch (Throwable e) {
                        failure.compareAndSet(null, e);
                    }

                    return null;
                }));
            }

            start.countDown();
            for (Future<?> future : futures) {
                future.get(30L, TimeUnit.SECONDS);
            }
        } finally {
            executor.shutdownNow();
        }

        Throwable thrown = failure.get();
        assertNull(thrown, () -> "Concurrent date formatting failed: " + thrown);
    }
}
