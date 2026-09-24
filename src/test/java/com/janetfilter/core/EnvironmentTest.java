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

package com.janetfilter.core;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Environment.
 */
public class EnvironmentTest {

    @Test
    public void testBaseDirShouldFallBackToWorkingDirectoryForRelativeAgentPath() {
        // Regression: File#getParentFile() returns null for a relative path, which used to
        // produce a null base directory and cascade into NPEs for the config/plugins/logs dirs.
        Environment environment = new Environment(null, new File("ja-netfilter.jar"), false);
        File workingDir = new File(System.getProperty("user.dir"));

        assertEquals(workingDir, environment.getBaseDir());
        assertEquals(new File(workingDir, "config"), environment.getConfigDir());
        assertEquals(new File(workingDir, "plugins"), environment.getPluginsDir());
        assertEquals(new File(workingDir, "logs"), environment.getLogsDir());
    }

    @Test
    public void testDirectoriesShouldBeRelativeToTheAgentJar() throws IOException {
        File base = Files.createTempDirectory("janf-environment").toFile();
        Environment environment = new Environment(null, new File(base, "ja-netfilter.jar"), false);

        assertEquals(base, environment.getBaseDir());
        assertEquals(new File(base, "config"), environment.getConfigDir());
        assertEquals(new File(base, "plugins"), environment.getPluginsDir());
        assertEquals(new File(base, "logs"), environment.getLogsDir());
    }

    @Test
    public void testAppSpecificDirectoriesShouldBeSuffixed() throws IOException {
        File base = Files.createTempDirectory("janf-environment").toFile();
        Environment environment = new Environment(null, new File(base, "ja-netfilter.jar"), "MyApp", false);

        assertEquals("myapp", environment.getAppName());
        assertEquals(new File(base, "config-myapp"), environment.getConfigDir());
        assertEquals(new File(base, "plugins-myapp"), environment.getPluginsDir());
        assertEquals(new File(base, "logs-myapp"), environment.getLogsDir());
    }

    @Test
    public void testBlankAppNameShouldNotAddSuffixes() throws IOException {
        File base = Files.createTempDirectory("janf-environment").toFile();
        Environment environment = new Environment(null, new File(base, "ja-netfilter.jar"), "  ", false);

        assertEquals("", environment.getAppName());
        assertEquals(new File(base, "config"), environment.getConfigDir());
    }

    @Test
    public void testModesShouldBeMutuallyExclusive() {
        Environment attach = new Environment(null, new File("ja-netfilter.jar"), true);
        assertTrue(attach.isAttachMode());
        assertFalse(attach.isJavaagentMode());

        Environment javaagent = new Environment(null, new File("ja-netfilter.jar"), false);
        assertFalse(javaagent.isAttachMode());
        assertTrue(javaagent.isJavaagentMode());
    }

    @Test
    public void testNativePrefixShouldBeAValidIdentifierPrefix() {
        Environment environment = new Environment(null, new File("ja-netfilter.jar"), false);
        String prefix = environment.getNativePrefix();

        assertNotNull(prefix);
        assertTrue(prefix.endsWith("_"));
        assertTrue(Character.isJavaIdentifierStart(prefix.charAt(0)));
        assertEquals(16, prefix.length());
    }

    @Test
    public void testAgentFileShouldBeExposed() {
        File agentFile = new File("ja-netfilter.jar");

        assertEquals(agentFile, new Environment(null, agentFile, false).getAgentFile());
    }
}
