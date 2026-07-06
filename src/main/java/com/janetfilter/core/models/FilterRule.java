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

package com.janetfilter.core.models;

import com.janetfilter.core.enums.RuleType;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a filter rule with type and content.
 */
public class FilterRule {
    /**
     * Map of supported rule types.
     */
    private static final Map<String, RuleType> SUPPORTED_TYPE_MAP;

    static {
        SUPPORTED_TYPE_MAP = new HashMap<>();

        for (RuleType ruleType : RuleType.values()) {
            SUPPORTED_TYPE_MAP.put(ruleType.name(), ruleType);
        }
    }

    private RuleType type;
    private String rule;

    /**
     * Create a new filter rule.
     *
     * @param type the rule type
     * @param rule the rule content
     */
    public FilterRule(RuleType type, String rule) {
        this.type = type;
        this.rule = rule;
    }

    /**
     * Create a filter rule from a type string and content.
     *
     * @param typeStr the rule type string
     * @param content the rule content
     * @return the filter rule, or null if type is invalid
     */
    public static FilterRule of(String typeStr, String content) {
        RuleType type = SUPPORTED_TYPE_MAP.get(typeStr.toUpperCase());
        if (null == type) {
            return null;
        }

        return new FilterRule(type, content);
    }

    /**
     * Get the rule type.
     *
     * @return the rule type
     */
    public RuleType getType() {
        return type;
    }

    /**
     * Set the rule type.
     *
     * @param type the rule type
     */
    public void setType(RuleType type) {
        this.type = type;
    }

    /**
     * Get the rule content.
     *
     * @return the rule content
     */
    public String getRule() {
        return rule;
    }

    /**
     * Set the rule content.
     *
     * @param rule the rule content
     */
    public void setRule(String rule) {
        this.rule = rule;
    }

    /**
     * Test if the content matches the rule.
     *
     * @param content the content to test
     * @return true if the rule matches
     */
    public boolean test(String content) {
        return type.getRuler().test(this.rule, content);
    }

    @Override
    public String toString() {
        return "{type=" + type + ", rule=" + rule + "}";
    }
}
