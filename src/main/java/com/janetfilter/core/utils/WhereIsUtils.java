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

package com.janetfilter.core.utils;

import com.janetfilter.core.Launcher;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;

/**
 * Utilities for locating Java-related executables.
 */
public class WhereIsUtils {
    /**
     * Java home directory path.
     */
    private static final String JAVA_HOME = System.getProperty("java.home");

    /**
     * Find the JPS executable.
     *
     * @return the JPS executable file, or null if not found
     */
    public static File findJPS() {
        String[] paths = new String[]{"bin/jps", "bin/jps.exe", "../bin/jps", "../bin/jps.exe"};

        for (String path : paths) {
            File file = new File(JAVA_HOME, path);
            if (file.exists() && file.isFile() && file.canExecute()) {
                return getCanonicalFile(file);
            }
        }

        return null;
    }

    /**
     * Find the Java executable.
     *
     * @return the Java executable file, or null if not found
     */
    public static File findJava() {
        String[] paths = new String[]{"bin/java", "bin/java.exe", "../bin/java", "../bin/java.exe"};

        for (String path : paths) {
            File file = new File(JAVA_HOME, path);
            if (file.exists() && file.isFile() && file.canExecute()) {
                return getCanonicalFile(file);
            }
        }

        return null;
    }

    /**
     * Find the tools.jar file.
     *
     * @return the tools.jar file, or null if not found
     */
    public static File findToolsJar() {
        String[] paths = new String[]{"lib/tools.jar", "../lib/tools.jar", "../../lib/tools.jar"};

        for (String path : paths) {
            File file = new File(JAVA_HOME, path);
            if (file.exists() && file.isFile()) {
                return getCanonicalFile(file);
            }
        }

        return null;
    }

    /**
     * Get the URI of the agent JAR.
     *
     * @return the agent JAR URI
     * @throws Exception if unable to locate the agent JAR
     */
    public static URI getJarURI() throws Exception {
        URL url = Launcher.class.getProtectionDomain().getCodeSource().getLocation();
        if (null != url) {
            return url.toURI();
        }

        String resourcePath = "/6c81ec87e55d331c267262e892427a3d93d76683.txt";
        url = Launcher.class.getResource(resourcePath);
        if (null == url) {
            throw new Exception("Can not locate resource file.");
        }

        String path = url.getPath();
        if (!path.endsWith("!" + resourcePath)) {
            throw new Exception("Invalid resource path.");
        }

        path = path.substring(0, path.length() - resourcePath.length() - 1);

        return new URI(path);

    }

    private static File getCanonicalFile(File file) {
        try {
            return file.getCanonicalFile();
        } catch (IOException e) {
            return null;
        }
    }
}
