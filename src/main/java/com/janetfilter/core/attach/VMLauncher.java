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

package com.janetfilter.core.attach;

import com.janetfilter.core.commons.Logger;

import java.io.File;
import java.io.IOException;
import java.lang.module.ModuleFinder;
import java.nio.file.Path;
import java.util.Set;

/**
 * Launch target JVM via attach mechanism with support for different JDKs.
 */
public final class VMLauncher {
    private static final org.slf4j.Logger LOG = Logger.getLogger(VMLauncher.class);

    private VMLauncher() {
    }

    public static void launch(File javaHome, File pidFile, String agentPath) throws IOException {
        String javaBin = findJavaBinary(javaHome);
        if (null == javaBin) {
            throw new IOException("Java binary not found in: " + javaHome);
        }

        ProcessBuilder builder = new ProcessBuilder(javaBin, "-jar", agentPath);
        builder.inheritIO();
        LOG.info("Launching VM: {}", javaBin);
        Process process = builder.start();

        try {
            int exitCode = process.waitFor();
            if (0 != exitCode) {
                LOG.warn("VM process exited with code: {}", exitCode);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOG.error("VM launch interrupted", e);
        } finally {
            process.destroy();
        }
    }

    private static String findJavaBinary(File javaHome) {
        if (isJPMSCompatible(javaHome)) {
            LOG.debug("Using JPMS-compatible Java in: {}", javaHome);
        }

        File javaBin = new File(javaHome, "bin/java");
        if (javaBin.exists() && javaBin.canExecute()) {
            return javaBin.getAbsolutePath();
        }

        File javaExe = new File(javaHome, "bin/java.exe");
        if (javaExe.exists() && javaExe.canExecute()) {
            return javaExe.getAbsolutePath();
        }

        File javawBin = new File(javaHome, "bin/javaw");
        if (javawBin.exists() && javawBin.canExecute()) {
            return javawBin.getAbsolutePath();
        }

        return null;
    }

    private static boolean isJPMSCompatible(File javaHome) {
        try {
            Path libDir = javaHome.toPath().resolve("lib");
            Path jrtFs = libDir.resolve("jrt-fs.jar");
            if (!jrtFs.toFile().exists()) {
                return false;
            }

            ModuleFinder systemFinder = ModuleFinder.of(javaHome.toPath());
            Set<String> modules = systemFinder.findAll().stream()
                    .map(m -> m.descriptor().name())
                    .collect(java.util.stream.Collectors.toSet());

            LOG.debug("Found {} JPMS modules", modules.size());
            return !modules.isEmpty();
        } catch (Exception e) {
            LOG.debug("JPMS detection failed", e);
            return false;
        }
    }
}