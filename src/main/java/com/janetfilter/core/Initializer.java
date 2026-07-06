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
import com.janetfilter.core.plugin.PluginManager;

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
        new PluginManager(dispatcher, environment).loadPlugins();

        Instrumentation inst = environment.getInstrumentation();
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
}