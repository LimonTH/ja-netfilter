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

import com.janetfilter.core.Dispatcher;
import com.janetfilter.core.Environment;
import com.janetfilter.core.commons.ConfigParser;
import org.slf4j.LoggerFactory;
import com.janetfilter.core.utils.StringUtils;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.stream.Stream;

/**
 * Manages plugin loading and lifecycle.
 */
public final class PluginManager {
    /**
     * Plugin entry point attribute name in manifest.
     */
    private static final String ENTRY_NAME = "JANF-Plugin-Entry";
    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(PluginManager.class);

    private final Instrumentation inst;
    private final Dispatcher dispatcher;
    private final Environment environment;
    private final List<PluginEntry> loadedPlugins = new ArrayList<>();
    private volatile boolean loaded = false;

    /**
     * Create a new plugin manager.
     *
     * @param dispatcher the class dispatcher
     * @param environment the environment context
     */
    public PluginManager(Dispatcher dispatcher, Environment environment) {
        this.inst = environment.getInstrumentation();
        this.dispatcher = dispatcher;
        this.environment = environment;
    }

    /**
     * Load all plugins from the plugins directory.
     */
    public synchronized void loadPlugins() {
        if (loaded) {
            LOG.warn("Plugins already loaded");
            return;
        }

        long startTime = System.currentTimeMillis();

        File pluginsDirectory = environment.getPluginsDir();
        if (!pluginsDirectory.exists() || !pluginsDirectory.isDirectory()) {
            LOG.warn("Plugins directory not found: {}", pluginsDirectory);
            loaded = true;
            return;
        }

        File[] pluginFiles = pluginsDirectory.listFiles((ignore, n) -> n.endsWith(".jar"));
        if (null == pluginFiles) {
            LOG.warn("No plugin files found in: {}", pluginsDirectory);
            loaded = true;
            return;
        }

        int threadCount = Math.min(pluginFiles.length, 4);
        try (ExecutorService executorService = Executors.newFixedThreadPool(threadCount)) {
            List<Future<?>> futures = new ArrayList<>();
            for (File pluginFile : pluginFiles) {
                futures.add(executorService.submit(new PluginLoadTask(pluginFile)));
            }

            executorService.shutdown();
            if (!executorService.awaitTermination(30L, TimeUnit.SECONDS)) {
                for (Future<?> future : futures) {
                    future.cancel(true);
                }
                throw new PluginLoadException("Load plugin timeout");
            }

            double elapsed = (System.currentTimeMillis() - startTime) / 1000D;
            LOG.info("============ All plugins loaded, {}s elapsed ============", String.format("%.2f", elapsed));
        } catch (PluginLoadException e) {
            LOG.error("Load plugin failed", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOG.error("Plugin loading interrupted", e);
        } finally {
            loaded = true;
        }
    }

    /**
     * Get the list of loaded plugins.
     *
     * @return list of loaded plugin entries
     */
    public synchronized List<PluginEntry> getLoadedPlugins() {
        return new ArrayList<>(loadedPlugins);
    }

    /**
     * Task for loading a single plugin.
     */
    private class PluginLoadTask implements Runnable {
        /**
         * Plugin JAR file to load.
         */
        private final File pluginFile;

        /**
         * Create a plugin load task.
         *
         * @param pluginFile the plugin JAR file
         */
        public PluginLoadTask(File pluginFile) {
            this.pluginFile = pluginFile;
        }

        @Override
        public void run() {
            try {
                if (pluginFile.getName().endsWith(environment.getDisabledPluginSuffix())) {
                    LOG.info("Disabled plugin: {}, ignored.", pluginFile);
                    return;
                }

                JarFile jarFile = new JarFile(pluginFile);
                Manifest manifest = jarFile.getManifest();
                String entryClass = manifest.getMainAttributes().getValue(ENTRY_NAME);
                if (StringUtils.isEmpty(entryClass)) {
                    return;
                }

                PluginClassLoader classLoader = new PluginClassLoader(jarFile);
                Class<?> klass = Class.forName(entryClass, false, classLoader);
                boolean implementsPluginEntry = Stream.of(klass.getInterfaces())
                        .anyMatch(iface -> iface == PluginEntry.class);
                if (!implementsPluginEntry) {
                    return;
                }

                synchronized (inst) {
                    inst.appendToBootstrapClassLoaderSearch(jarFile);
                }

                PluginEntry pluginEntry = klass.asSubclass(PluginEntry.class)
                        .getDeclaredConstructor().newInstance();

                File configFile = new File(environment.getConfigDir(), pluginEntry.getName().toLowerCase() + ".conf");
                PluginConfig pluginConfig = new PluginConfig(configFile, ConfigParser.parse(configFile));
                pluginEntry.init(environment, pluginConfig);

                dispatcher.addTransformers(pluginEntry.getTransformers());
                loadedPlugins.add(pluginEntry);

                LOG.info("Plugin loaded: name={}, version={}, author={}", pluginEntry.getName(), pluginEntry.getVersion(), pluginEntry.getAuthor());
            } catch (Throwable e) {
                LOG.error("Parse plugin info failed", e);
            }
        }
    }
}