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

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for StringUtils.
 */
public class StringUtilsTest {

    @Test
    public void testIsEmptyShouldReturnTrueForNull() {
        assertTrue(StringUtils.isEmpty(null));
    }

    @Test
    public void testIsEmptyShouldReturnTrueForEmptyString() {
        assertTrue(StringUtils.isEmpty(""));
    }

    @Test
    public void testIsEmptyShouldReturnFalseForNonEmptyString() {
        assertFalse(StringUtils.isEmpty("test"));
    }

    @Test
    public void testRandomMethodNameShouldGenerateCorrectLength() {
        String name = StringUtils.randomMethodName(15);
        assertNotNull(name);
        assertEquals(15, name.length());
    }

    @Test
    public void testRandomMethodNameShouldReturnEmptyForZeroLength() {
        assertEquals("", StringUtils.randomMethodName(0));
    }

    @Test
    public void testRandomMethodNameShouldStartWithValidJavaIdentifierStart() {
        String name = StringUtils.randomMethodName(10);
        assertNotNull(name);
        char first = name.charAt(0);
        assertTrue(Character.isJavaIdentifierStart(first) || first == '$' || first == '_');
    }

    @Test
    public void testToLongShouldReturnNullForNull() {
        assertNull(StringUtils.toLong(null));
    }

    @Test
    public void testToLongShouldReturnNullForInvalidNumber() {
        assertNull(StringUtils.toLong("not-a-number"));
    }

    @Test
    public void testToLongShouldParseValidNumber() {
        assertEquals(123L, StringUtils.toLong("123"));
    }

    @Test
    public void testToLongShouldParseNegativeNumber() {
        assertEquals(-42L, StringUtils.toLong("-42"));
    }
}