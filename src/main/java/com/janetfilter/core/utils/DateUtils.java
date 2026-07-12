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

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Date and time formatting utilities.
 */
public class DateUtils {
    /**
     * Full date-time format.
     */
    public static final DateFormat FULL_DF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    /**
     * Full date-time format with microseconds.
     */
    public static final DateFormat FULL_MICRO_DF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    /**
     * Date-only format.
     */
    public static final DateFormat DATE_DF = new SimpleDateFormat("yyyy-MM-dd");
    /**
     * Time-only format.
     */
    public static final DateFormat TIME_DF = new SimpleDateFormat("HH:mm:ss");

    /**
     * Format a date and time.
     *
     * @param date the date to format
     * @return formatted date string
     */
    public static String formatDateTime(Date date) {
        return FULL_DF.format(date);
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
     * Format a date and time with microseconds.
     *
     * @param date the date to format
     * @return formatted date string
     */
    public static String formatDateTimeMicro(Date date) {
        return FULL_MICRO_DF.format(date);
    }

    /**
     * Format the current date and time with microseconds.
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
        return DATE_DF.format(date);
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
        return TIME_DF.format(date);
    }

    /**
     * Parse a time string.
     *
     * @param timeStr the time string to parse
     * @return parsed date
     * @throws ParseException if parsing fails
     */
    public static Date parseTime(String timeStr) throws ParseException {
        return TIME_DF.parse(timeStr);
    }

    /**
     * Parse a date string.
     *
     * @param dateStr the date string to parse
     * @return parsed date
     * @throws ParseException if parsing fails
     */
    public static Date parseDate(String dateStr) throws ParseException {
        return DATE_DF.parse(dateStr);
    }

    /**
     * Parse a date-time string.
     *
     * @param dateTimeStr the date-time string to parse
     * @return parsed date
     * @throws ParseException if parsing fails
     */
    public static Date parseDateTime(String dateTimeStr) throws ParseException {
        return FULL_DF.parse(dateTimeStr);
    }
}
