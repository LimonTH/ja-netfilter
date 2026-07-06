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

public class FilterRule {
    private static final Map<String, RuleType> SUPPORTED_TYPE_MAP;

    static {
        SUPPORTED_TYPE_MAP = new HashMap<>();

        for (RuleType ruleType : RuleType.values()) {
            SUPPORTED_TYPE_MAP.put(ruleType.name(), ruleType);
        }
    }

    private RuleType type;
    private String rule;

    public FilterRule(RuleType type, String rule) {
        this.type = type;
        this.rule = rule;
    }

    public static FilterRule of(String typeStr, String content) {
        RuleType type = SUPPORTED_TYPE_MAP.get(typeStr.toUpperCase());
        if (null == type) {
            return null;
        }

        return new FilterRule(type, content);
    }

    public RuleType getType() {
        return type;
    }

    public void setType(RuleType type) {
        this.type = type;
    }

    public String getRule() {
        return rule;
    }

    public void setRule(String rule) {
        this.rule = rule;
    }

    public boolean test(String content) {
        return type.getRuler().test(this.rule, content);
    }

    @Override
    public String toString() {
        return "{type=" + type + ", rule=" + rule + "}";
    }
}
