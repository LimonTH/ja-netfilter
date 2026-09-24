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

import java.io.IOException;
import java.io.InputStream;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

/**
 * Class loader for loading plugin classes from JAR files.
 */
public final class PluginClassLoader extends ClassLoader {
    /**
     * Plugin JAR file.
     */
    private final JarFile jarFile;

    /**
     * Create a plugin class loader.
     *
     * @param jarFile the plugin JAR file
     */
    public PluginClassLoader(JarFile jarFile) {
        this.jarFile = jarFile;
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        // No local cache: ClassLoader#loadClass serializes calls for the same name and the JVM
        // caches defined classes itself, so an extra map would only leak alongside the loader.
        byte[] bytes = loadClassFromFile(name);

        return defineClass(name, bytes, 0, bytes.length);
    }

    private byte[] loadClassFromFile(String fileName) throws ClassNotFoundException {
        String classFile = fileName.replace('.', '/') + ".class";
        ZipEntry entry = jarFile.getEntry(classFile);
        if (null == entry) {
            throw new ClassNotFoundException("Class not found: " + fileName);
        }

        try (InputStream is = jarFile.getInputStream(entry)) {
            return is.readAllBytes();
        } catch (IOException e) {
            throw new ClassNotFoundException("Can't access class: " + fileName, e);
        }
    }
}
