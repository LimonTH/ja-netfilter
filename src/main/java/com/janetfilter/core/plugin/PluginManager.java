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
import com.janetfilter.core.commons.ConfigParser;
import com.janetfilter.core.commons.DebugInfo;
import com.janetfilter.core.utils.StringUtils;

import java.io.File;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
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

    /**
     * Maximum number of plugin loading threads.
     */
    private static final int MAX_LOAD_THREADS = 4;

    /**
     * Maximum time to wait for all plugins to load, in seconds.
     */
    private static final long LOAD_TIMEOUT_SECONDS = 30L;

    private final Instrumentation inst;
    private final Dispatcher dispatcher;
    private final Environment environment;
    private final List<PluginEntry> loadedPlugins = new CopyOnWriteArrayList<>();

    /**
     * Canonical paths of plugin JARs that were appended to the bootstrap class loader search.
     * <p>
     * {@code AppendToBootstrapClassLoaderSearch} cannot be undone, so the same file must not be
     * appended twice: every append would retain an additional open {@link JarFile} for the
     * lifetime of the JVM.
     * </p>
     */
    private final Set<String> bootstrapJars = ConcurrentHashMap.newKeySet();

    /**
     * Signature ({@code length:lastModified}) of every loaded plugin JAR, keyed by entry class.
     * Used to detect plugins that were modified on disk and therefore need a JVM restart.
     */
    private final Map<String, String> loadedSignatures = new ConcurrentHashMap<>();

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
     * <p>
     * Only the first call has an effect; use {@link #reloadPlugins()} to apply changes made to
     * the plugins directory afterwards.
     * </p>
     */
    public synchronized void loadPlugins() {
        if (loaded) {
            DebugInfo.warn("Plugins already loaded, use reloadPlugins() to reload them");
            return;
        }

        doLoadPlugins();
    }

    /**
     * Reload all plugins from the plugins directory.
     * <p>
     * Registered transformers are dropped and the directory is scanned again, so plugins that
     * were added become active and plugins that were removed stop hooking classes, without a JVM
     * restart. Already loaded classes are retransformed so the change is applied to them.
     * </p>
     * <p>
     * The class <em>code</em> of a plugin that was already loaded cannot be swapped: its JAR is
     * part of the bootstrap class loader search, which is append-only. Such plugins are detected
     * and a warning is logged; a JVM restart is required to pick the new version up.
     * </p>
     */
    public synchronized void reloadPlugins() {
        if (!loaded) {
            DebugInfo.warn("Plugins are not loaded yet, performing the initial load");
            doLoadPlugins();
            return;
        }

        Set<String> previouslyHooked = new HashSet<>(dispatcher.getHookClassNames());

        dispatcher.reset();
        loadedPlugins.clear();
        loaded = false;

        doLoadPlugins();

        retransformHookedClasses(previouslyHooked);
    }

    /**
     * Get a snapshot of the loaded plugins.
     *
     * @return list of loaded plugin entries
     */
    public List<PluginEntry> getLoadedPlugins() {
        return new ArrayList<>(loadedPlugins);
    }

    /**
     * Retransform every already loaded class that is hooked by the current dispatcher.
     *
     * @param extraClassNames additional class names to retransform, e.g. classes that were
     *                        hooked before a reload; may be {@code null}
     */
    public void retransformHookedClasses(Set<String> extraClassNames) {
        if (null == inst) {
            DebugInfo.debug("Instrumentation is not available, skipping retransformation of hooked classes");
            return;
        }

        Set<String> classSet = new HashSet<>(dispatcher.getHookClassNames());
        if (null != extraClassNames) {
            classSet.addAll(extraClassNames);
        }

        if (classSet.isEmpty()) {
            return;
        }

        for (Class<?> klass : inst.getAllLoadedClasses()) {
            if (!classSet.contains(klass.getName())) {
                continue;
            }

            try {
                klass.getGenericSuperclass();
                inst.retransformClasses(klass);
            } catch (Throwable e) {
                DebugInfo.error("Retransform class failed: " + klass.getName(), e);
            }
        }
    }

    /**
     * Scan the plugins directory and load every plugin found in it.
     */
    private void doLoadPlugins() {
        loaded = true;

        long startTime = System.currentTimeMillis();

        File pluginsDirectory = environment.getPluginsDir();
        if (!pluginsDirectory.exists() || !pluginsDirectory.isDirectory()) {
            DebugInfo.warn("Plugins directory not found: " + pluginsDirectory);
            return;
        }

        File[] pluginFiles = pluginsDirectory.listFiles((ignore, n) -> n.endsWith(".jar"));
        if (null == pluginFiles || 0 == pluginFiles.length) {
            DebugInfo.info("No plugin files found in: " + pluginsDirectory);
            return;
        }

        int threadCount = Math.min(pluginFiles.length, MAX_LOAD_THREADS);
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount, daemonThreadFactory("janf-plugin-loader"));
        try {
            List<Future<?>> futures = new ArrayList<>(pluginFiles.length);
            for (File pluginFile : pluginFiles) {
                futures.add(executorService.submit(new PluginLoadTask(pluginFile)));
            }

            executorService.shutdown();
            if (!executorService.awaitTermination(LOAD_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                for (Future<?> future : futures) {
                    future.cancel(true);
                }
                throw new PluginLoadException("Load plugin timeout");
            }

            double elapsed = (System.currentTimeMillis() - startTime) / 1000D;
            DebugInfo.info("============ " + loadedPlugins.size() + " plugin(s) loaded, " + String.format("%.2f", elapsed) + "s elapsed ============");
        } catch (PluginLoadException e) {
            DebugInfo.error("Load plugin failed", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            DebugInfo.error("Plugin loading interrupted", e);
        } finally {
            executorService.shutdownNow();
        }
    }

    private static ThreadFactory daemonThreadFactory(String name) {
        return runnable -> {
            Thread thread = new Thread(runnable, name);
            thread.setDaemon(true);
            return thread;
        };
    }

    /**
     * Add a plugin JAR to the bootstrap class loader search, at most once per file.
     *
     * @param jarFile       the plugin JAR file
     * @param canonicalPath the canonical path of the plugin JAR
     * @return {@code true} if the given {@link JarFile} is now retained by the JVM and must stay
     * open, {@code false} if it is not needed any more and can be closed
     * @throws Exception if the JAR cannot be added to the bootstrap search
     */
    private boolean appendToBootstrapSearch(JarFile jarFile, String canonicalPath) throws Exception {
        if (bootstrapJars.contains(canonicalPath)) {
            DebugInfo.debug("Plugin jar already visible to the bootstrap class loader: " + canonicalPath);
            return false;
        }

        synchronized (inst) {
            inst.appendToBootstrapClassLoaderSearch(jarFile);
        }
        bootstrapJars.add(canonicalPath);

        return true;
    }

    private static void closeQuietly(JarFile jarFile) {
        try {
            jarFile.close();
        } catch (IOException e) {
            DebugInfo.debug("Can not close plugin jar: " + jarFile.getName(), e);
        }
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
            if (pluginFile.getName().endsWith(environment.getDisabledPluginSuffix())) {
                DebugInfo.info("Disabled plugin: " + pluginFile + ", ignored.");
                return;
            }

            String canonicalPath = canonicalPath(pluginFile);
            JarFile jarFile = null;
            boolean keepOpen = false;

            try {
                jarFile = new JarFile(pluginFile);
                Manifest manifest = jarFile.getManifest();
                String entryClass = null == manifest ? null : manifest.getMainAttributes().getValue(ENTRY_NAME);
                if (StringUtils.isEmpty(entryClass)) {
                    DebugInfo.debug("No `" + ENTRY_NAME + "` manifest attribute, ignored: " + pluginFile);
                    return;
                }

                keepOpen = appendToBootstrapSearch(jarFile, canonicalPath);

                PluginClassLoader classLoader = new PluginClassLoader(jarFile);
                Class<?> klass = Class.forName(entryClass, false, classLoader);
                boolean implementsPluginEntry = Stream.of(klass.getInterfaces())
                        .anyMatch(iface -> iface == PluginEntry.class);
                if (!implementsPluginEntry) {
                    DebugInfo.warn("Entry class " + entryClass + " does not implement PluginEntry, ignored: " + pluginFile);
                    return;
                }

                warnIfModified(entryClass, pluginFile);

                PluginEntry pluginEntry = instantiate(entryClass);

                File configFile = new File(environment.getConfigDir(), pluginEntry.getName().toLowerCase() + ".conf");
                PluginConfig pluginConfig = new PluginConfig(configFile, ConfigParser.parse(configFile));
                pluginEntry.init(environment, pluginConfig);

                dispatcher.addTransformers(pluginEntry.getTransformers());
                loadedPlugins.add(pluginEntry);

                DebugInfo.info("Plugin loaded: {name=" + pluginEntry.getName() + ", version=" + pluginEntry.getVersion() + ", author=" + pluginEntry.getAuthor() + "}");
            } catch (Throwable e) {
                DebugInfo.error("Parse plugin info failed: " + pluginFile, e);
            } finally {
                if (null != jarFile && !keepOpen) {
                    closeQuietly(jarFile);
                }
            }
        }

        /**
         * Instantiate a plugin entry class.
         * <p>
         * Uses an explicitly accessible constructor so that plugins relying on the
         * {@code Class.newInstance()} behaviour of the old plugin API, which accepted
         * non-public constructors, keep working.
         * </p>
         *
         * @param entryClass the plugin entry class name
         * @return the plugin entry instance
         * @throws Exception if the instance can not be created
         */
        private PluginEntry instantiate(String entryClass) throws Exception {
            Class<?> klass = Class.forName(entryClass);
            try {
                return (PluginEntry) klass.getDeclaredConstructor().newInstance();
            } catch (IllegalAccessException | NoSuchMethodException e) {
                Constructor<?> constructor = klass.getDeclaredConstructor();
                constructor.setAccessible(true);

                return (PluginEntry) constructor.newInstance();
            }
        }

        /**
         * Warn when a plugin that is already loaded was modified on disk.
         * <p>
         * Plugin classes live in the bootstrap class loader search, which is append-only, so a
         * new version of such a plugin can not be activated without a JVM restart.
         * </p>
         *
         * @param entryClass the plugin entry class name
         * @param pluginFile the plugin JAR file
         */
        private void warnIfModified(String entryClass, File pluginFile) {
            String signature = pluginFile.length() + ":" + pluginFile.lastModified();
            String previous = loadedSignatures.put(entryClass, signature);

            if (null != previous && !previous.equals(signature)) {
                DebugInfo.warn("Plugin `" + entryClass + "` changed on disk, but its classes are already loaded and "
                        + "can not be replaced at runtime. Restart the JVM to apply the new version.");
            }
        }

        private String canonicalPath(File file) {
            try {
                return file.getCanonicalPath();
            } catch (IOException e) {
                DebugInfo.debug("Can not resolve canonical path: " + file, e);
                return file.getAbsolutePath();
            }
        }
    }
}