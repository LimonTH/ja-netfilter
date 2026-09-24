/*
 * Copyright (C) 2026 LimonTH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://gnu.org>.
 */

package com.janetfilter.core.utils;

import java.text.ParseException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;

/**
 * Date and time formatting utilities.
 * <p>
 * All formatters are immutable and therefore safe to share between threads, which matters
 * because log messages are written from several background threads.
 * </p>
 */
public class DateUtils {
    /**
     * Full date-time format.
     */
    public static final DateTimeFormatter FULL_DF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    /**
     * Full date-time format with millisecond precision.
     */
    public static final DateTimeFormatter FULL_MICRO_DF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    /**
     * Date-only format.
     */
    public static final DateTimeFormatter DATE_DF = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    /**
     * Time-only format.
     */
    public static final DateTimeFormatter TIME_DF = DateTimeFormatter.ofPattern("HH:mm:ss");

    /**
     * Format a date and time.
     *
     * @param date the date to format
     * @return formatted date string
     */
    public static String formatDateTime(Date date) {
        return FULL_DF.format(toLocalDateTime(date));
    }

    /**
     * Format the current date and time.
     *
     * @return formatted date string
     */
    public static String formatDateTime() {
        return formatDateTime(new Date());
    }

    /**
     * Format a date and time with millisecond precision.
     *
     * @param date the date to format
     * @return formatted date string
     */
    public static String formatDateTimeMicro(Date date) {
        return FULL_MICRO_DF.format(toLocalDateTime(date));
    }

    /**
     * Format the current date and time with millisecond precision.
     *
     * @return formatted date string
     */
    public static String formatDateTimeMicro() {
        return formatDateTimeMicro(new Date());
    }

    /**
     * Format a date.
     *
     * @param date the date to format
     * @return formatted date string
     */
    public static String formatDate(Date date) {
        return DATE_DF.format(toLocalDateTime(date).toLocalDate());
    }

    /**
     * Format the current date.
     *
     * @return formatted date string
     */
    public static String formatDate() {
        return formatDate(new Date());
    }

    /**
     * Format a time.
     *
     * @param date the date to format
     * @return formatted time string
     */
    public static String formatTime(Date date) {
        return TIME_DF.format(toLocalDateTime(date).toLocalTime());
    }

    /**
     * Parse a time string.
     *
     * @param timeStr the time string to parse
     * @return parsed date
     * @throws ParseException if parsing fails
     */
    public static Date parseTime(String timeStr) throws ParseException {
        try {
            return toDate(LocalTime.parse(timeStr, TIME_DF));
        } catch (DateTimeParseException e) {
            throw toParseException(timeStr, e);
        }
    }

    /**
     * Parse a date string.
     *
     * @param dateStr the date string to parse
     * @return parsed date
     * @throws ParseException if parsing fails
     */
    public static Date parseDate(String dateStr) throws ParseException {
        try {
            return toDate(LocalDate.parse(dateStr, DATE_DF));
        } catch (DateTimeParseException e) {
            throw toParseException(dateStr, e);
        }
    }

    /**
     * Parse a date-time string.
     *
     * @param dateTimeStr the date-time string to parse
     * @return parsed date
     * @throws ParseException if parsing fails
     */
    public static Date parseDateTime(String dateTimeStr) throws ParseException {
        try {
            return toDate(LocalDateTime.parse(dateTimeStr, FULL_DF));
        } catch (DateTimeParseException e) {
            throw toParseException(dateTimeStr, e);
        }
    }

    private static LocalDateTime toLocalDateTime(Date date) {
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    private static Date toDate(LocalDateTime dateTime) {
        return Date.from(dateTime.atZone(ZoneId.systemDefault()).toInstant());
    }

    private static Date toDate(LocalDate date) {
        return Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    private static Date toDate(LocalTime time) {
        return Date.from(time.atDate(LocalDate.now()).atZone(ZoneId.systemDefault()).toInstant());
    }

    private static ParseException toParseException(String value, DateTimeParseException cause) {
        ParseException exception = new ParseException("Unparseable date: \"" + value + "\"", cause.getErrorIndex());
        exception.initCause(cause);

        return exception;
    }
}
