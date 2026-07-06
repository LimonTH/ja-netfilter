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

import org.slf4j.LoggerFactory;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

/**
 * Transformer wrapper that logs transformations without applying them.
 */
public final class DryRunTransformer implements ClassFileTransformer {
    /**
     * Logger instance.
     */
    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(DryRunTransformer.class);

    private final ClassFileTransformer delegate;

    /**
     * Create a dry run transformer wrapper.
     *
     * @param delegate the actual transformer to delegate to
     */
    public DryRunTransformer(ClassFileTransformer delegate) {
        this.delegate = delegate;
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain, byte[] classFileBuffer) throws IllegalClassFormatException {
        LOG.info("[DRY-RUN] Would transform class: {}", className);
        try {
            byte[] result = delegate.transform(loader, className, classBeingRedefined, protectionDomain, classFileBuffer);
            if (null != result) {
                LOG.info("[DRY-RUN] Would modify bytes for class: {} ({} bytes)", className, result.length);
            }
            return null; // Changes not applied
        } catch (IllegalClassFormatException e) {
            LOG.error("[DRY-RUN] Delegate failed for class: {}", className, e);
            throw e;
        } catch (Throwable e) {
            LOG.error("[DRY-RUN] Delegate failed for class: {}", className, e);
            throw new IllegalClassFormatException("Delegate transformer failed: " + e.getMessage());
        }
    }
}