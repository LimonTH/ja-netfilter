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

import com.janetfilter.core.commons.DebugInfo;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Watches for plugin file changes and reloads plugins automatically.
 */
public final class PluginHotReloader {
    private final PluginManager pluginManager;
    private final Instrumentation instrumentation;
    private final Map<WatchKey, Path> keys = new HashMap<>();
    private volatile boolean running = false;

    /**
     * Create a plugin hot reloader.
     *
     * @param pluginManager the plugin manager
     * @param instrumentation the instrumentation instance
     */
    public PluginHotReloader(PluginManager pluginManager, Instrumentation instrumentation) {
        this.pluginManager = pluginManager;
        this.instrumentation = instrumentation;
    }

    /**
     * Start watching the plugins directory for changes.
     *
     * @param pluginsDir the plugins directory to watch
     */
    public void start(File pluginsDir) {
        if (!pluginsDir.exists() || !pluginsDir.isDirectory()) {
            DebugInfo.warn("Plugins directory not found for hot reload: " + pluginsDir);
            return;
        }

        running = true;
        Thread reloadThread = new Thread(() -> {
            try (WatchService watchService = FileSystems.getDefault().newWatchService()) {
                Path pluginsPath = pluginsDir.toPath();
                WatchKey key = pluginsPath.register(watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY);
                keys.put(key, pluginsPath);

                DebugInfo.info("Hot reload watcher started for: " + pluginsDir);

                while (running) {
                    WatchKey watchedKey = watchService.take();
                    Path dir = keys.get(watchedKey);

                    for (WatchEvent<?> event : watchedKey.pollEvents()) {
                        WatchEvent.Kind<?> kind = event.kind();
                        Path filename = (Path) event.context();

                        if (null == filename || !filename.toString().endsWith(".jar")) {
                            continue;
                        }

                        DebugInfo.info("Plugin file changed: " + filename + " (" + kind + ")");
                        reloadPlugins();
                    }

                    boolean valid = watchedKey.reset();
                    if (!valid) {
                        DebugInfo.warn("Watch key no longer valid");
                        break;
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                DebugInfo.info("Hot reload watcher interrupted");
            } catch (Exception e) {
                DebugInfo.error("Hot reload watcher error", e);
            }
        }, "PluginHotReloader");

        reloadThread.setDaemon(true);
        reloadThread.start();
    }

    /**
     * Stop the hot reload watcher.
     */
    public void stop() {
        running = false;
        DebugInfo.info("Hot reload watcher stopped");
    }

    /**
     * Reload all plugins.
     */
    private synchronized void reloadPlugins() {
        try {
            pluginManager.loadPlugins();
            DebugInfo.info("Plugins reloaded successfully");
        } catch (Exception e) {
            DebugInfo.error("Failed to reload plugins", e);
        }
    }
}