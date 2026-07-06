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

package com.janetfilter.core.plugin;

import com.janetfilter.core.commons.Logger;

import java.lang.instrument.ClassFileTransformer;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry of built-in transformers with the ability to disable them.
 */
public final class BuiltinTransformerRegistry {
    private static final org.slf4j.Logger LOG = Logger.getLogger(BuiltinTransformerRegistry.class);

    private static final Map<String, ClassFileTransformer> BUILTIN_TRANSFORMERS = new ConcurrentHashMap<>();
    private static final Set<String> DISABLED_TRANSFORMERS = ConcurrentHashMap.newKeySet();

    private BuiltinTransformerRegistry() {
    }

    /**
     * Register a builtin transformer.
     *
     * @param name the transformer name
     * @param transformer the transformer instance
     */
    public static void register(String name, ClassFileTransformer transformer) {
        BUILTIN_TRANSFORMERS.put(name, transformer);
        LOG.info("Registered builtin transformer: {}", name);
    }

    /**
     * Disable a builtin transformer.
     *
     * @param name the transformer name
     */
    public static void disable(String name) {
        DISABLED_TRANSFORMERS.add(name);
        LOG.info("Disabled builtin transformer: {}", name);
    }

    /**
     * Enable a builtin transformer.
     *
     * @param name the transformer name
     */
    public static void enable(String name) {
        DISABLED_TRANSFORMERS.remove(name);
        LOG.info("Enabled builtin transformer: {}", name);
    }

    /**
     * Check if a transformer is enabled.
     *
     * @param name the transformer name
     * @return true if enabled
     */
    public static boolean isEnabled(String name) {
        return BUILTIN_TRANSFORMERS.containsKey(name) && !DISABLED_TRANSFORMERS.contains(name);
    }

    /**
     * Get all active transformers.
     *
     * @return collection of active transformers
     */
    public static Collection<ClassFileTransformer> getActiveTransformers() {
        List<ClassFileTransformer> active = new ArrayList<>();
        for (Map.Entry<String, ClassFileTransformer> entry : BUILTIN_TRANSFORMERS.entrySet()) {
            if (!DISABLED_TRANSFORMERS.contains(entry.getKey())) {
                active.add(entry.getValue());
            }
        }
        return active;
    }

    /**
     * Get all registered transformer names.
     *
     * @return collection of transformer names
     */
    public static Collection<String> getAllTransformerNames() {
        return new ArrayList<>(BUILTIN_TRANSFORMERS.keySet());
    }
}
