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