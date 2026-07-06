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
import com.janetfilter.core.commons.DebugInfo;
import com.janetfilter.core.utils.StringUtils;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

public final class PluginManager {
    private static final String ENTRY_NAME = "JANF-Plugin-Entry";

    private final Instrumentation inst;
    private final Dispatcher dispatcher;
    private final Environment environment;

    public PluginManager(Dispatcher dispatcher, Environment environment) {
        this.inst = environment.getInstrumentation();
        this.dispatcher = dispatcher;
        this.environment = environment;
    }

    public void loadPlugins() {
        long startTime = System.currentTimeMillis();

        File pluginsDirectory = environment.getPluginsDir();
        if (!pluginsDirectory.exists() || !pluginsDirectory.isDirectory()) {
            return;
        }

        File[] pluginFiles = pluginsDirectory.listFiles((ignore, n) -> n.endsWith(".jar"));
        if (null == pluginFiles) {
            return;
        }

        try (ExecutorService executorService = Executors.newCachedThreadPool()) {
            for (File pluginFile : pluginFiles) {
                executorService.submit(new PluginLoadTask(pluginFile));
            }

            if (!executorService.awaitTermination(30L, TimeUnit.SECONDS)) {
                throw new RuntimeException("Load plugin timeout");
            }

            DebugInfo.debug(String.format("============ All plugins loaded, %.2fs elapsed ============", (System.currentTimeMillis() - startTime) / 1000D));
        } catch (Throwable e) {
            DebugInfo.error("Load plugin failed", e);
        }
    }

    private class PluginLoadTask implements Runnable {
        private final File pluginFile;

        public PluginLoadTask(File pluginFile) {
            this.pluginFile = pluginFile;
        }

        @Override
        public void run() {
            try {
                if (pluginFile.getName().endsWith(environment.getDisabledPluginSuffix())) {
                    DebugInfo.debug("Disabled plugin: " + pluginFile + ", ignored.");
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
                if (!Arrays.asList(klass.getInterfaces()).contains(PluginEntry.class)) {
                    return;
                }

                synchronized (inst) {
                    inst.appendToBootstrapClassLoaderSearch(jarFile);
                }

                PluginEntry pluginEntry = (PluginEntry) Class.forName(entryClass).getDeclaredConstructor().newInstance();

                File configFile = new File(environment.getConfigDir(), pluginEntry.getName().toLowerCase() + ".conf");
                PluginConfig pluginConfig = new PluginConfig(configFile, ConfigParser.parse(configFile));
                pluginEntry.init(environment, pluginConfig);

                dispatcher.addTransformers(pluginEntry.getTransformers());

                DebugInfo.debug("Plugin loaded: {name=" + pluginEntry.getName() + ", version=" + pluginEntry.getVersion() + ", author=" + pluginEntry.getAuthor() + "}");
            } catch (Throwable e) {
                DebugInfo.error("Parse plugin info failed", e);
            }
        }
    }
}
