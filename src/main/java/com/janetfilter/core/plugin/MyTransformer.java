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

package com.janetfilter.core.plugin;

import java.security.ProtectionDomain;

/**
 * Interface for class file transformers.
 */
public interface MyTransformer {
    /**
     * Get the class name to hook, or null for global transformer.
     *
     * @return class name in format: package/to/className, or null for global transformer
     */
    String getHookClassName();

    /**
     * Check if this transformer should load in attach mode.
     *
     * @return true to load in attach mode
     */
    default boolean attachMode() {
        return true;
    }

    /**
     * Check if this transformer should load in -javaagent mode.
     *
     * @return true to load in -javaagent mode
     */
    default boolean javaagentMode() {
        return true;
    }

    /**
     * Check if this is a management transformer (for global transformers only).
     *
     * @return true to handle transformation of all classes
     */
    default boolean isManager() {
        return false;
    }

    /**
     * Called before transformation (for global transformers only).
     */
    default void before(ClassLoader loader, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, String className, byte[] classBytes) throws Exception {
        before(className, classBytes);
    }

    /**
     * Called before transformation (for global transformers only).
     */
    default void before(String className, byte[] classBytes) throws Exception {

    }

    /**
     * Pre-transformation hook (for global transformers only).
     */
    default byte[] preTransform(ClassLoader loader, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, String className, byte[] classBytes, int order) throws Exception {
        return preTransform(className, classBytes, order);
    }

    /**
     * Pre-transformation hook (for global transformers only).
     */
    default byte[] preTransform(String className, byte[] classBytes, int order) throws Exception {
        return transform(className, classBytes, order);     // for old version
    }

    /**
     * Transform a class (for normal transformers only).
     */
    default byte[] transform(ClassLoader loader, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, String className, byte[] classBytes, int order) throws Exception {
        return transform(className, classBytes, order);
    }

    /**
     * Transform a class (for normal transformers only).
     */
    default byte[] transform(String className, byte[] classBytes, int order) throws Exception {
        return classBytes;
    }

    /**
     * Post-transformation hook (for global transformers only).
     */
    default byte[] postTransform(ClassLoader loader, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, String className, byte[] classBytes, int order) throws Exception {
        return postTransform(className, classBytes, order);
    }

    /**
     * Post-transformation hook (for global transformers only).
     */
    default byte[] postTransform(String className, byte[] classBytes, int order) throws Exception {
        return classBytes;
    }

    /**
     * Called after transformation (for global transformers only).
     */
    default void after(ClassLoader loader, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, String className, byte[] classBytes) throws Exception {
        after(className, classBytes);
    }

    /**
     * Called after transformation (for global transformers only).
     */
    default void after(String className, byte[] classBytes) throws Exception {

    }
}
