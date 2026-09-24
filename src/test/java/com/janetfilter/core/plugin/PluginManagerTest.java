/*
 * Copyright (C) 2026 LimonTH
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

import com.janetfilter.core.Dispatcher;
import com.janetfilter.core.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for PluginManager.
 */
public class PluginManagerTest {
    private Path base;
    private Path plugins;
    private Environment environment;
    private Dispatcher dispatcher;
    private PluginManager manager;
    private final List<Class<?>> retransformed = new CopyOnWriteArrayList<>();

    @BeforeEach
    public void setUp() throws IOException {
        base = Files.createTempDirectory("janf-plugin-manager");
        plugins = base.resolve("plugins");
        retransformed.clear();
        environment = new Environment(instrumentation(), base.resolve("ja-netfilter.jar").toFile(), false);
        dispatcher = new Dispatcher(environment);
        manager = new PluginManager(dispatcher, environment);
    }

    /**
     * Create a minimal {@link Instrumentation} test double that records retransformations.
     *
     * @return the instrumentation stub
     */
    private Instrumentation instrumentation() {
        InvocationHandler handler = (proxy, method, args) -> {
            return switch (method.getName()) {
                case "getAllLoadedClasses" -> new Class<?>[]{PluginManagerTest.class};
                case "retransformClasses" -> {
                    retransformed.addAll(Arrays.asList((Class<?>[]) args[0]));
                    yield null;
                }
                case "toString" -> "InstrumentationStub";
                case "hashCode" -> System.identityHashCode(proxy);
                case "equals" -> proxy == args[0];
                default -> null;
            };
        };

        return (Instrumentation) Proxy.newProxyInstance(
                PluginManagerTest.class.getClassLoader(), new Class<?>[]{Instrumentation.class}, handler);
    }

    @Test
    public void testLoadPluginsWithMissingDirectoryShouldNotThrow() {
        assertDoesNotThrow(manager::loadPlugins);

        assertTrue(manager.getLoadedPlugins().isEmpty());
        assertTrue(dispatcher.getHookClassNames().isEmpty());
    }

    @Test
    public void testLoadPluginsWithEmptyDirectoryShouldNotThrow() throws IOException {
        // Regression: an existing but empty plugins directory used to reach
        // Executors.newFixedThreadPool(0) and abort the whole agent initialization.
        Files.createDirectory(plugins);

        assertDoesNotThrow(manager::loadPlugins);

        assertTrue(manager.getLoadedPlugins().isEmpty());
        assertTrue(dispatcher.getHookClassNames().isEmpty());
    }

    @Test
    public void testLoadPluginsShouldSurviveBrokenPluginJar() throws IOException {
        Files.createDirectory(plugins);
        Files.write(plugins.resolve("broken.jar"), new byte[]{1, 2, 3, 4});

        assertDoesNotThrow(manager::loadPlugins);

        assertTrue(manager.getLoadedPlugins().isEmpty());
    }

    @Test
    public void testLoadPluginsShouldIgnoreDisabledPlugin() throws IOException {
        Files.createDirectory(plugins);
        Files.write(plugins.resolve("plugin.disabled.jar"), new byte[]{1, 2, 3, 4});

        assertDoesNotThrow(manager::loadPlugins);

        assertTrue(manager.getLoadedPlugins().isEmpty());
    }

    @Test
    public void testLoadPluginsShouldIgnorePluginWithoutEntryAttribute() throws IOException {
        Files.createDirectory(plugins);
        createJar(plugins.resolve("no-entry.jar"), null);

        assertDoesNotThrow(manager::loadPlugins);

        assertTrue(manager.getLoadedPlugins().isEmpty());
        assertTrue(dispatcher.getHookClassNames().isEmpty());
    }

    @Test
    public void testLoadPluginsShouldIgnorePluginWithMissingEntryClass() throws IOException {
        Files.createDirectory(plugins);
        createJar(plugins.resolve("missing-class.jar"), "com.example.NotInJar");

        assertDoesNotThrow(manager::loadPlugins);

        assertTrue(manager.getLoadedPlugins().isEmpty());
    }

    @Test
    public void testLoadPluginsShouldBeIgnoredWhenCalledTwice() throws IOException {
        Files.createDirectory(plugins);
        manager.loadPlugins();

        assertDoesNotThrow(manager::loadPlugins);
    }

    @Test
    public void testReloadPluginsBeforeInitialLoadShouldPerformInitialLoad() throws IOException {
        Files.createDirectory(plugins);

        assertDoesNotThrow(manager::reloadPlugins);

        assertTrue(manager.getLoadedPlugins().isEmpty());
    }

    @Test
    public void testReloadPluginsShouldNotThrowWithEmptyDirectory() throws IOException {
        Files.createDirectory(plugins);
        manager.loadPlugins();

        assertDoesNotThrow(manager::reloadPlugins);

        assertTrue(manager.getLoadedPlugins().isEmpty());
        assertTrue(dispatcher.getHookClassNames().isEmpty());
    }

    @Test
    public void testReloadPluginsShouldDropPreviouslyRegisteredHooks() throws IOException {
        Files.createDirectory(plugins);
        manager.loadPlugins();
        dispatcher.addTransformer(new StubTransformer());
        assertFalse(dispatcher.getHookClassNames().isEmpty());

        manager.reloadPlugins();

        assertTrue(dispatcher.getHookClassNames().isEmpty());
    }

    @Test
    public void testReloadPluginsShouldRetransformPreviouslyHookedLoadedClasses() throws IOException {
        Files.createDirectory(plugins);
        manager.loadPlugins();
        dispatcher.addTransformer(new StubTransformer());

        manager.reloadPlugins();

        // Retransformation restarts from the original class file bytes, which is how the hooks of
        // a removed plugin are reverted for classes that are already loaded.
        assertTrue(retransformed.contains(PluginManagerTest.class), "already loaded hooked classes must be retransformed");
    }

    @Test
    public void testGetLoadedPluginsShouldReturnACopy() throws IOException {
        Files.createDirectory(plugins);
        manager.loadPlugins();

        List<PluginEntry> first = manager.getLoadedPlugins();
        List<PluginEntry> second = manager.getLoadedPlugins();

        // A fresh list every time, so callers can never mutate the internal state.
        assertNotSame(first, second);
        assertTrue(first.isEmpty());
    }

    private void createJar(Path jarPath, String entryClass) throws IOException {
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        if (null != entryClass) {
            manifest.getMainAttributes().putValue("JANF-Plugin-Entry", entryClass);
        }

        try (JarOutputStream ignored = new JarOutputStream(Files.newOutputStream(jarPath), manifest)) {
            // A manifest-only jar is enough for the loader to inspect it.
        }
    }

    /**
     * Transformer that hooks this test class, so that the retransformation performed during a
     * reload can be observed.
     */
    private static class StubTransformer implements MyTransformer {
        @Override
        public String getHookClassName() {
            return "com/janetfilter/core/plugin/PluginManagerTest";
        }
    }
}
