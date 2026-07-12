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

package com.janetfilter.core.rulers;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for all ruler implementations.
 */
public class RulerTest {

    // ========== PrefixRuler ==========

    @Test
    public void testPrefixRulerShouldMatchWhenContentStartsWithRule() {
        PrefixRuler ruler = new PrefixRuler();
        assertTrue(ruler.test("com.example.", "com.example.TestClass"));
    }

    @Test
    public void testPrefixRulerShouldNotMatchWhenContentDoesNotStartWithRule() {
        PrefixRuler ruler = new PrefixRuler();
        assertFalse(ruler.test("com.example.", "org.example.TestClass"));
    }

    @Test
    public void testPrefixRulerShouldBeCaseSensitive() {
        PrefixRuler ruler = new PrefixRuler();
        assertFalse(ruler.test("com.example.", "COM.EXAMPLE.TestClass"));
    }

    @Test
    public void testPrefixRulerShouldMatchEmptyRule() {
        PrefixRuler ruler = new PrefixRuler();
        assertTrue(ruler.test("", "anything"));
    }

    // ========== PrefixICRuler ==========

    @Test
    public void testPrefixICRulerShouldMatchCaseInsensitive() {
        PrefixICRuler ruler = new PrefixICRuler();
        assertTrue(ruler.test("COM.EXAMPLE.", "com.example.TestClass"));
    }

    @Test
    public void testPrefixICRulerShouldNotMatchWhenPrefixDiffers() {
        PrefixICRuler ruler = new PrefixICRuler();
        assertFalse(ruler.test("com.example.", "org.example.TestClass"));
    }

    // ========== SuffixRuler ==========

    @Test
    public void testSuffixRulerShouldMatchWhenContentEndsWithRule() {
        SuffixRuler ruler = new SuffixRuler();
        assertTrue(ruler.test(".class", "TestClass.class"));
    }

    @Test
    public void testSuffixRulerShouldNotMatchWhenContentDoesNotEndWithRule() {
        SuffixRuler ruler = new SuffixRuler();
        assertFalse(ruler.test(".class", "TestClass.java"));
    }

    @Test
    public void testSuffixRulerShouldBeCaseSensitive() {
        SuffixRuler ruler = new SuffixRuler();
        assertFalse(ruler.test(".class", "TestClass.CLASS"));
    }

    // ========== SuffixICRuler ==========

    @Test
    public void testSuffixICRulerShouldMatchCaseInsensitive() {
        SuffixICRuler ruler = new SuffixICRuler();
        assertTrue(ruler.test(".CLASS", "TestClass.class"));
    }

    @Test
    public void testSuffixICRulerShouldNotMatchWhenSuffixDiffers() {
        SuffixICRuler ruler = new SuffixICRuler();
        assertFalse(ruler.test(".class", "TestClass.java"));
    }

    // ========== KeywordRuler ==========

    @Test
    public void testKeywordRulerShouldMatchWhenContentContainsKeyword() {
        KeywordRuler ruler = new KeywordRuler();
        assertTrue(ruler.test("important", "This is important content"));
    }

    @Test
    public void testKeywordRulerShouldNotMatchWhenKeywordAbsent() {
        KeywordRuler ruler = new KeywordRuler();
        assertFalse(ruler.test("important", "This is trivial content"));
    }

    @Test
    public void testKeywordRulerShouldBeCaseSensitive() {
        KeywordRuler ruler = new KeywordRuler();
        assertFalse(ruler.test("Important", "this is important"));
    }

    // ========== KeywordICRuler ==========

    @Test
    public void testKeywordICRulerShouldMatchCaseInsensitive() {
        KeywordICRuler ruler = new KeywordICRuler();
        assertTrue(ruler.test("IMPORTANT", "This is important content"));
    }

    @Test
    public void testKeywordICRulerShouldNotMatchWhenKeywordAbsent() {
        KeywordICRuler ruler = new KeywordICRuler();
        assertFalse(ruler.test("important", "nothing here"));
    }

    // ========== EqualRuler ==========

    @Test
    public void testEqualRulerShouldMatchExactContent() {
        EqualRuler ruler = new EqualRuler();
        assertTrue(ruler.test("com.example.MyClass", "com.example.MyClass"));
    }

    @Test
    public void testEqualRulerShouldNotMatchDifferentContent() {
        EqualRuler ruler = new EqualRuler();
        assertFalse(ruler.test("com.example.MyClass", "com.example.OtherClass"));
    }

    @Test
    public void testEqualRulerShouldBeCaseSensitive() {
        EqualRuler ruler = new EqualRuler();
        assertFalse(ruler.test("com.example.MyClass", "COM.EXAMPLE.MYCLASS"));
    }

    // ========== EqualICRuler ==========

    @Test
    public void testEqualICRulerShouldMatchCaseInsensitive() {
        EqualICRuler ruler = new EqualICRuler();
        assertTrue(ruler.test("COM.EXAMPLE.MYCLASS", "com.example.MyClass"));
    }

    @Test
    public void testEqualICRulerShouldNotMatchDifferentContent() {
        EqualICRuler ruler = new EqualICRuler();
        assertFalse(ruler.test("com.example.MyClass", "com.example.OtherClass"));
    }

    // ========== RegExpRuler ==========

    @Test
    public void testRegExpRulerShouldMatchPattern() {
        RegExpRuler ruler = new RegExpRuler();
        assertTrue(ruler.test("^com\\.example\\..*", "com.example.TestClass"));
    }

    @Test
    public void testRegExpRulerShouldNotMatchNonMatchingPattern() {
        RegExpRuler ruler = new RegExpRuler();
        assertFalse(ruler.test("^com\\.example\\..*", "org.example.TestClass"));
    }

    @Test
    public void testRegExpRulerShouldMatchExactPattern() {
        RegExpRuler ruler = new RegExpRuler();
        assertTrue(ruler.test("com\\.example\\.MyClass", "com.example.MyClass"));
    }

    @Test
    public void testRegExpRulerShouldHandleSpecialRegexChars() {
        RegExpRuler ruler = new RegExpRuler();
        assertTrue(ruler.test(".*\\$.*", "com.example.MyClass$Inner"));
    }
}