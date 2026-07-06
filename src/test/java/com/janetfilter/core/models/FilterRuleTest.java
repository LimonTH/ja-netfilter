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