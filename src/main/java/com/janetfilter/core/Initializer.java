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

package com.janetfilter.core;

import com.janetfilter.core.commons.DebugInfo;
import com.janetfilter.core.plugin.PluginHotReloader;
import com.janetfilter.core.plugin.PluginManager;
import com.janetfilter.core.rest.ManagementServer;

import java.lang.instrument.Instrumentation;
import java.util.Set;

/**
 * Initializes the agent by loading plugins and setting up transformers.
 */
public class Initializer {
    /**
     * Initialize the agent.
     *
     * @param environment the environment context
     */
    public static void init(Environment environment) {
        DebugInfo.useFile(environment.getLogsDir());
        DebugInfo.info(environment.toString());

        Dispatcher dispatcher = new Dispatcher(environment);
        PluginManager pluginManager = new PluginManager(dispatcher, environment);
        pluginManager.loadPlugins();

        Instrumentation inst = environment.getInstrumentation();

        // Start hot reload watcher for plugins
        PluginHotReloader hotReloader = new PluginHotReloader(pluginManager, inst);
        hotReloader.start(environment.getPluginsDir());

        // Start management server if configured
        startManagementServer(dispatcher, pluginManager);

        inst.addTransformer(dispatcher, true);
        inst.setNativeMethodPrefix(dispatcher, environment.getNativePrefix());

        Set<String> classSet = dispatcher.getHookClassNames();
        for (Class<?> c : inst.getAllLoadedClasses()) {
            String name = c.getName();
            if (!classSet.contains(name)) {
                continue;
            }

            try {
                Object ignore = c.getGenericSuperclass();
                inst.retransformClasses(c);
            } catch (Throwable e) {
                DebugInfo.error("Retransform class failed: " + name, e);
            }
        }
    }

    /**
     * Start the management HTTP server if configured via system property or environment variable.
     * <p>
     * The server port is read from the system property {@code janf.management.port}
     * or the environment variable {@code JANF_MANAGEMENT_PORT}.
     * If the port is set to 0, a random available port will be used.
     * If neither is set, the server is not started.
     * </p>
     *
     * @param dispatcher    the class dispatcher for querying hooked classes
     * @param pluginManager the plugin manager for reloading plugins
     */
    private static void startManagementServer(Dispatcher dispatcher, PluginManager pluginManager) {
        String portStr = System.getProperty("janf.management.port");
        if (null == portStr || portStr.isEmpty()) {
            portStr = System.getenv("JANF_MANAGEMENT_PORT");
        }
        if (null == portStr || portStr.isEmpty()) {
            return; // Management server not configured
        }

        int port;
        try {
            port = Integer.parseInt(portStr);
        } catch (NumberFormatException e) {
            DebugInfo.warn("Invalid management port: " + portStr);
            return;
        }

        try {
            ManagementServer mgmtServer = new ManagementServer(port, dispatcher, pluginManager);
            mgmtServer.start();
        } catch (Exception e) {
            DebugInfo.error("Failed to start management server", e);
        }
    }
}