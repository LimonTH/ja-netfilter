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

import com.janetfilter.core.utils.WhereIsUtils;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

/**
 * Launch target JVM via attach mechanism with support for different JDKs.
 */
public final class VMLauncher {
    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(VMLauncher.class);

    private VMLauncher() {
    }

    /**
     * Launch the agent to attach to a target JVM process.
     *
     * @param agentJar  the agent JAR file to deploy
     * @param targetPid the target process ID to attach to
     * @param agentArgs arguments to pass to the agent
     * @throws IOException if the Java process cannot be started
     */
    public static void launch(File agentJar, String targetPid, String agentArgs) throws IOException {
        File javaBin = WhereIsUtils.findJava();
        if (null == javaBin) {
            throw new IOException("Java binary not found");
        }

        ProcessBuilder builder = new ProcessBuilder(
                javaBin.getAbsolutePath(),
                "-jar", agentJar.getAbsolutePath(),
                targetPid
        );

        if (null != agentArgs && !agentArgs.isEmpty()) {
            builder.command().add(agentArgs);
        }

        builder.inheritIO();
        LOG.info("Launching agent for PID {}: {}", targetPid, javaBin);
        Process process = builder.start();

        try {
            int exitCode = process.waitFor();
            if (0 != exitCode) {
                LOG.warn("Agent process exited with code: {}", exitCode);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOG.error("Agent launch interrupted", e);
        } finally {
            process.destroy();
        }
    }
}