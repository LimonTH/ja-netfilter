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

package com.janetfilter.core.commons;

import com.janetfilter.core.models.FilterRule;
import com.janetfilter.core.utils.StringUtils;

import java.io.BufferedReader;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Parser for configuration files.
 */
public class ConfigParser {
    /**
     * Parse a configuration file.
     *
     * @param file the configuration file to parse
     * @return map of section names to filter rules
     * @throws Exception if parsing fails
     */
    public static Map<String, List<FilterRule>> parse(File file) throws Exception {
        Map<String, List<FilterRule>> map = new HashMap<>();

        if (null == file || !file.exists() || !file.isFile() || !file.canRead()) {
            return map;
        }

        // Read file content
        byte[] content = Files.readAllBytes(Paths.get(file.toURI()));
        String contentStr = new String(content, StandardCharsets.UTF_8).trim();

        // Check if the config file is encrypted
        byte[] key = ConfigCipher.getKey();
        if (null != key && contentStr.startsWith("ENC:")) {
            String encryptedData = contentStr.substring(4).trim();
            try {
                contentStr = ConfigCipher.decrypt(encryptedData, key);
                DebugInfo.debug("Decrypted config file: " + file);
            } catch (Exception e) {
                DebugInfo.error("Failed to decrypt config file: " + file, e);
                return map;
            }
        }

        try (BufferedReader reader = new BufferedReader(new java.io.StringReader(contentStr))) {
            int lineNumber = 0;
            String line, lastSection = null;

            while (null != (line = reader.readLine())) {
                lineNumber++;
                line = line.trim();
                if (StringUtils.isEmpty(line)) {
                    continue;
                }

                int len = line.length();
                switch (line.charAt(0)) {
                    case '[':
                        if (']' != line.charAt(len - 1)) {
                            throw new Exception("Invalid section in " + file.getName() + "! Line: " + lineNumber);
                        }

                        String section = line.substring(1, len - 1);
                        if (StringUtils.isEmpty(section)) {
                            throw new Exception("Empty section name in " + file.getName() + "! Line: " + lineNumber);
                        }

                        map.computeIfAbsent(lastSection = section, k -> new ArrayList<>());
                        break;
                    case '#':
                    case ';':
                        break;  // comment
                    case '/':
                        if (len > 1 && '/' == line.charAt(1)) {
                            break;  // comment
                        }
                        throw new Exception("Invalid character in " + file.getName() + "! Line: " + lineNumber);
                    default:
                        if (null == lastSection) {
                            break;  // ignore rules without section
                        }

                        String[] parts = line.split(",", 2);
                        if (2 != parts.length) {
                            throw new Exception("Invalid rule in " + file.getName() + "! Line: " + lineNumber);
                        }

                        String type = parts[0].trim();
                        String ruleContent = parts[1].trim();
                        if (StringUtils.isEmpty(type) || StringUtils.isEmpty(ruleContent)) {
                            throw new Exception("Invalid rule in " + file.getName() + "! Line: " + lineNumber);
                        }

                        if (!Character.isAlphabetic(type.charAt(0))) {
                            throw new Exception("Invalid rule in " + file.getName() + "! Line: " + lineNumber);
                        }

                        FilterRule rule = FilterRule.of(type, ruleContent);
                        if (null == rule) {
                            throw new Exception("Invalid rule type in " + file.getName() + "! Line: " + lineNumber);
                        }

                        map.get(lastSection).add(rule);
                        DebugInfo.debug("Add section: " + lastSection + ", rule: " + rule);
                        break;
                }
            }
        }

        DebugInfo.debug("Config file loaded: " + file);
        return map;
    }
}