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

package com.janetfilter.core.enums;

import com.janetfilter.core.rulers.*;

/**
 * Types of filter rules available.
 */
public enum RuleType {
    /**
     * Prefix matching rule.
     */
    PREFIX(new PrefixRuler()),
    PREFIX_IC(new PrefixICRuler()),
    SUFFIX(new SuffixRuler()),
    SUFFIX_IC(new SuffixICRuler()),
    KEYWORD(new KeywordRuler()),
    KEYWORD_IC(new KeywordICRuler()),
    EQUAL(new EqualRuler()),
    EQUAL_IC(new EqualICRuler()),
    REGEXP(new RegExpRuler());

    private final Ruler ruler;

    /**
     * Create a rule type with its ruler implementation.
     *
     * @param ruler the ruler implementation
     */
    RuleType(Ruler ruler) {
        this.ruler = ruler;
    }

    /**
     * Get the ruler implementation for this rule type.
     *
     * @return the ruler
     */
    public Ruler getRuler() {
        return ruler;
    }
}
