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

import com.janetfilter.core.Environment;

import java.util.List;

/**
 * Plugin entry point interface.
 */
public interface PluginEntry {
    /**
     * Initialize the plugin.
     *
     * @param environment the environment context
     * @param config the plugin configuration
     */
    default void init(Environment environment, PluginConfig config) {
        // get plugin config
    }

    /**
     * Get the plugin name.
     *
     * @return the plugin name
     */
    String getName();

    /**
     * Get the plugin author.
     *
     * @return the author name
     */
    String getAuthor();

    /**
     * Get the plugin version.
     *
     * @return the version string
     */
    default String getVersion() {
        return "v1.0.0";
    }

    /**
     * Get the plugin description.
     *
     * @return the description
     */
    default String getDescription() {
        return "A ja-netfilter plugin.";
    }

    /**
     * Get the list of transformers provided by this plugin.
     *
     * @return list of transformers
     */
    List<MyTransformer> getTransformers();
}
