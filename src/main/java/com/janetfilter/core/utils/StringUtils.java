/*
 * Original Code by Neo Peng pengzhile@gmail.com
 * Copyright (C) 2026 LimonTH (Modifications and updates)
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

import java.util.Random;

/**
 * String manipulation utilities.
 */
public class StringUtils {
    /**
     * Characters allowed in generated method names.
     */
    private static final String METHOD_NAME_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz$_0123456789";

    /**
     * Check if a string is null or empty.
     *
     * @param str the string to check
     * @return true if null or empty
     */
    public static boolean isEmpty(String str) {
        return null == str || str.isEmpty();
    }

    /**
     * Generate a random method name of specified length.
     *
     * @param length the length of the name
     * @return random method name
     */
    public static String randomMethodName(int length) {
        int i = 0;
        if (i == length) {
            return "";
        }

        char[] buffer = new char[length];
        Random rnd = new Random();

        buffer[i++] = METHOD_NAME_CHARS.charAt(rnd.nextInt(54));
        while (i < length) {
            buffer[i++] = METHOD_NAME_CHARS.charAt(rnd.nextInt(64));
        }

        return new String(buffer);
    }

    /**
     * Convert a string to a Long.
     *
     * @param val the string value
     * @return the Long value, or null if invalid
     */
    public static Long toLong(String val) {
        if (null == val) {
            return null;
        }

        try {
            return Long.parseLong(val);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
