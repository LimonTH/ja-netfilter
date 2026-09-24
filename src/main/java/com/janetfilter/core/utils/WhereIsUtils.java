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

package com.janetfilter.core.utils;

import com.janetfilter.core.Launcher;

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.security.CodeSource;
import java.security.ProtectionDomain;

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
     * Marker resource that is always packaged inside the agent JAR.
     * <p>
     * It exists solely to allow locating the agent JAR when it was loaded by a custom
     * system class loader (for example IntelliJ's
     * {@code com.intellij.util.lang.PathClassLoader}) which defines classes without a
     * {@link CodeSource}.
     * </p>
     */
    private static final String JAR_MARKER_RESOURCE = "/6c81ec87e55d331c267262e892427a3d93d76683.txt";

    /**
     * Get the URI of the agent JAR.
     * <p>
     * The preferred way is to read the {@link CodeSource} of {@link Launcher}. However,
     * some custom system class loaders (e.g. IntelliJ's {@code PathClassLoader}) define
     * classes without a {@code CodeSource} or without a {@code location}, in which case
     * we fall back to the location of the marker resource packaged inside the agent JAR.
     * </p>
     *
     * @return the agent JAR URI
     * @throws Exception if unable to locate the agent JAR
     */
    public static URI getJarURI() throws Exception {
        ProtectionDomain domain = Launcher.class.getProtectionDomain();
        CodeSource codeSource = null == domain ? null : domain.getCodeSource();
        URL location = null == codeSource ? null : codeSource.getLocation();
        if (null != location) {
            return location.toURI();
        }

        URL resource = Launcher.class.getResource(JAR_MARKER_RESOURCE);
        if (null == resource) {
            throw new IOException("Can not locate marker resource: " + JAR_MARKER_RESOURCE);
        }

        URLConnection connection = resource.openConnection();
        if (connection instanceof JarURLConnection) {
            return ((JarURLConnection) connection).getJarFileURL().toURI();
        }

        String path = resource.getPath();
        if (!path.endsWith("!" + JAR_MARKER_RESOURCE)) {
            throw new IOException("Invalid marker resource path: " + path);
        }

        return new URI(path.substring(0, path.length() - JAR_MARKER_RESOURCE.length() - 1));
    }

    /**
     * Get the agent JAR as a {@link File}.
     * <p>
     * Unlike {@link URI#getPath()}, the returned file correctly decodes percent-encoded
     * characters (e.g. spaces or non-ASCII characters in the path).
     * </p>
     *
     * @return the agent JAR file
     * @throws Exception if unable to locate the agent JAR
     */
    public static File getJarFile() throws Exception {
        URI uri = getJarURI();
        try {
            return new File(uri);
        } catch (IllegalArgumentException e) {
            return new File(uri.getSchemeSpecificPart());
        }
    }

    private static File getCanonicalFile(File file) {
        try {
            return file.getCanonicalFile();
        } catch (IOException e) {
            return null;
        }
    }
}
