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

package com.janetfilter.core.models;

import com.janetfilter.core.enums.RuleType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for FilterRule model.
 */
public class FilterRuleTest {

    @Test
    public void testOfShouldCreateRuleWithValidType() {
        FilterRule rule = FilterRule.of("PREFIX", "com.example.");
        assertNotNull(rule);
        assertEquals(RuleType.PREFIX, rule.getType());
        assertEquals("com.example.", rule.getRule());
    }

    @Test
    public void testOfShouldReturnNullForInvalidType() {
        assertNull(FilterRule.of("INVALID", "content"));
    }

    @Test
    public void testOfShouldBeCaseInsensitiveForType() {
        FilterRule rule = FilterRule.of("prefix", "com.example.");
        assertNotNull(rule);
        assertEquals(RuleType.PREFIX, rule.getType());
    }

    @Test
    public void testTestShouldMatchContent() {
        FilterRule rule = FilterRule.of("PREFIX", "com.example.");
        assertNotNull(rule);
        assertTrue(rule.test("com.example.TestClass"));
        assertFalse(rule.test("org.example.TestClass"));
    }

    @Test
    public void testSettersAndGetters() {
        FilterRule rule = new FilterRule(RuleType.EQUAL, "test");
        assertEquals(RuleType.EQUAL, rule.getType());
        assertEquals("test", rule.getRule());

        rule.setType(RuleType.KEYWORD);
        rule.setRule("newRule");
        assertEquals(RuleType.KEYWORD, rule.getType());
        assertEquals("newRule", rule.getRule());
    }

    @Test
    public void testToString() {
        FilterRule rule = new FilterRule(RuleType.PREFIX, "com.example.");
        String str = rule.toString();
        assertTrue(str.contains("PREFIX"));
        assertTrue(str.contains("com.example."));
    }
}