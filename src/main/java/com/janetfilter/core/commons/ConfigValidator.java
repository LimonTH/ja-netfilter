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

package com.janetfilter.core.commons;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Basic config validation.
 */
public final class ConfigValidator {
    private ConfigValidator() {
    }

    public static List<String> validate(File file) {
        List<String> errors = new ArrayList<>();

        if (!file.exists()) {
            errors.add("Config file not found: " + file);
            return errors;
        }

        return errors;
    }
}
