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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Tests for PluginClassLoader.
 */
public class PluginClassLoaderTest {

    @Test
    public void testLoadClassShouldLoadClassFromPluginJar(@TempDir Path dir) throws Exception {
        try (JarFile jarFile = new JarFile(buildPluginJar(dir).toFile())) {
            PluginClassLoader loader = new PluginClassLoader(jarFile);

            Class<?> klass = loader.loadClass("hello.Hello");

            assertEquals("hello.Hello", klass.getName());
            assertSame(loader, klass.getClassLoader());

            Object instance = klass.getDeclaredConstructor().newInstance();
            assertEquals("hello", klass.getMethod("greet").invoke(instance));
        }
    }

    @Test
    public void testLoadClassShouldThrowForClassMissingFromPluginJar(@TempDir Path dir) throws Exception {
        try (JarFile jarFile = new JarFile(buildPluginJar(dir).toFile())) {
            PluginClassLoader loader = new PluginClassLoader(jarFile);

            assertThrows(ClassNotFoundException.class, () -> loader.loadClass("hello.Missing"));
        }
    }

    @Test
    public void testLoadClassShouldReturnTheSameClassOnRepeatedCalls(@TempDir Path dir) throws Exception {
        try (JarFile jarFile = new JarFile(buildPluginJar(dir).toFile())) {
            PluginClassLoader loader = new PluginClassLoader(jarFile);

            assertSame(loader.loadClass("hello.Hello"), loader.loadClass("hello.Hello"));
        }
    }

    /**
     * Compile a small class and package it into a plugin JAR.
     * <p>
     * The class is deliberately not part of the test class path, so that
     * {@code PluginClassLoader.findClass} has to be used.
     * </p>
     *
     * @param dir temporary directory to build in
     * @return the created plugin JAR
     * @throws Exception if compiling or packaging fails
     */
    private static Path buildPluginJar(Path dir) throws Exception {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assumeTrue(null != compiler, "A JDK with the system Java compiler is required");

        Path sourceDir = Files.createDirectories(dir.resolve("src").resolve("hello"));
        Path classesDir = Files.createDirectories(dir.resolve("classes"));
        Path sourceFile = sourceDir.resolve("Hello.java");
        Files.writeString(sourceFile, "package hello;\n\npublic class Hello {\n    public String greet() {\n        return \"hello\";\n    }\n}\n");

        assertEquals(0, compiler.run(null, null, null, "-d", classesDir.toString(), sourceFile.toString()), "javac failed");

        Path classFile = classesDir.resolve("hello").resolve("Hello.class");
        Path jarPath = dir.resolve("plugin.jar");
        try (JarOutputStream jarOutput = new JarOutputStream(Files.newOutputStream(jarPath))) {
            jarOutput.putNextEntry(new JarEntry("hello/Hello.class"));
            jarOutput.write(Files.readAllBytes(classFile));
            jarOutput.closeEntry();
        }

        return jarPath;
    }
}
