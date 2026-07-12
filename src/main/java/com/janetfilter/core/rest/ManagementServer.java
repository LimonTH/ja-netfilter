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

package com.janetfilter.core.rest;

import com.janetfilter.core.BuildVersion;
import com.janetfilter.core.Dispatcher;
import com.janetfilter.core.commons.DebugInfo;
import com.janetfilter.core.plugin.PluginManager;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;

/**
 * Lightweight HTTP management server for controlling the agent at runtime.
 * <p>
 * Provides REST API endpoints for querying status and reloading plugins.
 * The server is started automatically if the system property
 * {@code janf.management.port} or environment variable {@code JANF_MANAGEMENT_PORT}
 * is set to a valid port number.
 * </p>
 *
 * <h3>Available Endpoints</h3>
 * <ul>
 *   <li>{@code GET /status} — Returns agent status as JSON</li>
 *   <li>{@code POST /reload} — Reloads all plugins</li>
 * </ul>
 */
public final class ManagementServer {
    private final HttpServer server;
    private final Dispatcher dispatcher;
    private final PluginManager pluginManager;
    private volatile boolean running = false;

    /**
     * Create a management HTTP server.
     *
     * @param port          the port to listen on (0 for random available port)
     * @param dispatcher    the class dispatcher for querying hooked classes
     * @param pluginManager the plugin manager for reloading plugins
     * @throws IOException if the server cannot be created
     */
    public ManagementServer(int port, Dispatcher dispatcher, PluginManager pluginManager) throws IOException {
        this.dispatcher = dispatcher;
        this.pluginManager = pluginManager;
        this.server = HttpServer.create(new InetSocketAddress(port), 0);
        setupRoutes();
    }

    /**
     * Register HTTP handlers for all API endpoints.
     */
    private void setupRoutes() {
        server.createContext("/status", this::handleStatus);
        server.createContext("/reload", this::handleReload);
        server.setExecutor(Executors.newFixedThreadPool(2));
    }

    /**
     * Start the management server in a background thread.
     * Once started, the server will accept incoming HTTP requests.
     */
    public void start() {
        server.start();
        running = true;
        DebugInfo.info("Management server started on port: " + server.getAddress().getPort());
    }

    /**
     * Stop the management server gracefully.
     * Waits up to 1 second for active requests to complete.
     */
    public void stop() {
        if (running) {
            server.stop(1);
            running = false;
            DebugInfo.info("Management server stopped");
        }
    }

    /**
     * Get the port the server is listening on.
     *
     * @return the port number
     */
    public int getPort() {
        return server.getAddress().getPort();
    }

    /**
     * Check if the server is currently running.
     *
     * @return true if the server is running, false otherwise
     */
    public boolean isRunning() {
        return running;
    }

    /**
     * Handle GET /status requests.
     * Returns a JSON object with agent version, application name,
     * number of hooked classes, and number of loaded plugins.
     *
     * @param exchange the HTTP exchange
     * @throws IOException if an I/O error occurs
     */
    private void handleStatus(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "Method not allowed");
            return;
        }

        String json = String.format(
                "{\"status\":\"running\",\"version\":\"%s\",\"appName\":\"%s\",\"hookedClasses\":%d,\"pluginsLoaded\":%d}",
                BuildVersion.getVersion(),
                BuildVersion.getAppName(),
                dispatcher.getHookClassNames().size(),
                pluginManager.getLoadedPlugins().size()
        );
        sendJsonResponse(exchange, json);
    }

    /**
     * Handle POST /reload requests.
     * Triggers a reload of all plugins from the plugins directory.
     *
     * @param exchange the HTTP exchange
     * @throws IOException if an I/O error occurs
     */
    private void handleReload(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "Method not allowed");
            return;
        }

        try {
            pluginManager.loadPlugins();
            String json = "{\"status\":\"ok\",\"message\":\"Plugins reloaded\"}";
            sendJsonResponse(exchange, json);
        } catch (Exception e) {
            DebugInfo.error("Plugin reload failed", e);
            String json = "{\"status\":\"error\",\"message\":\"" + e.getMessage() + "\"}";
            sendJsonResponse(exchange, 500, json);
        }
    }

    /**
     * Send a 200 OK JSON response.
     *
     * @param exchange the HTTP exchange
     * @param json     the JSON response body
     * @throws IOException if an I/O error occurs
     */
    private void sendJsonResponse(HttpExchange exchange, String json) throws IOException {
        sendJsonResponse(exchange, 200, json);
    }

    /**
     * Send a JSON response with the specified status code.
     *
     * @param exchange   the HTTP exchange
     * @param statusCode the HTTP status code
     * @param json       the JSON response body
     * @throws IOException if an I/O error occurs
     */
    private void sendJsonResponse(HttpExchange exchange, int statusCode, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    /**
     * Send a plain text response with the specified status code.
     *
     * @param exchange   the HTTP exchange
     * @param statusCode the HTTP status code
     * @param message    the response body
     * @throws IOException if an I/O error occurs
     */
    private void sendResponse(HttpExchange exchange, int statusCode, String message) throws IOException {
        byte[] bytes = message.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}