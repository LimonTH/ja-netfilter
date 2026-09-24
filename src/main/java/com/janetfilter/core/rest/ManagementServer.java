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

package com.janetfilter.core.rest;

import com.janetfilter.core.BuildVersion;
import com.janetfilter.core.Dispatcher;
import com.janetfilter.core.commons.DebugInfo;
import com.janetfilter.core.plugin.PluginManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Lightweight HTTP management server for controlling the agent at runtime.
 * <p>
 * Implemented on a raw {@link ServerSocket} using only {@code java.base} APIs. This is
 * deliberate: the agent JAR is appended to the bootstrap class loader search, and the
 * bootstrap loader cannot resolve classes from the optional {@code jdk.httpserver} module.
 * A {@code java.net} implementation keeps the whole agent in a single class loader world.
 * </p>
 *
 * <h3>Binding</h3>
 * <p>
 * The server binds to the loopback interface by default. Set {@code janf.management.host} or
 * {@code JANF_MANAGEMENT_HOST} (e.g. {@code 0.0.0.0}) to bind elsewhere, for example inside a
 * container.
 * </p>
 *
 * <h3>Authentication</h3>
 * <p>
 * When {@code janf.management.token} / {@code JANF_MANAGEMENT_TOKEN} is set, every request must
 * carry it in the {@code X-JANF-Token} header or as an {@code Authorization: Bearer <token>}
 * header. Without a token only requests from loopback addresses are accepted.
 * </p>
 *
 * <h3>Available Endpoints</h3>
 * <ul>
 *   <li>{@code GET /status} — Returns agent status as JSON</li>
 *   <li>{@code POST /reload} — Reloads all plugins</li>
 * </ul>
 */
public final class ManagementServer {
    /**
     * Header carrying the management token.
     */
    private static final String TOKEN_HEADER = "X-JANF-Token";

    /**
     * Header carrying a bearer token.
     */
    private static final String AUTHORIZATION_HEADER = "Authorization";

    /**
     * Read timeout for client connections, in milliseconds.
     */
    private static final int SOCKET_TIMEOUT_MILLIS = 10_000;

    /**
     * Maximum size of a request head (request line + headers) accepted from a client.
     */
    private static final int MAX_REQUEST_HEAD_BYTES = 8192;

    private final ServerSocket serverSocket;
    private final Dispatcher dispatcher;
    private final PluginManager pluginManager;
    private final String token;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private volatile ExecutorService acceptExecutor;
    private volatile Thread acceptThread;

    /**
     * Create a management HTTP server.
     *
     * @param port          the port to listen on (0 for random available port)
     * @param dispatcher    the class dispatcher for querying hooked classes
     * @param pluginManager the plugin manager for reloading plugins
     * @throws IOException if the server socket cannot be created
     */
    public ManagementServer(int port, Dispatcher dispatcher, PluginManager pluginManager) throws IOException {
        this.dispatcher = dispatcher;
        this.pluginManager = pluginManager;
        this.token = resolveToken();
        this.serverSocket = new ServerSocket();
        serverSocket.bind(new InetSocketAddress(resolveBindAddress(), port));
    }

    private static String resolveToken() {
        String value = System.getProperty("janf.management.token");
        if (null == value || value.isEmpty()) {
            value = System.getenv("JANF_MANAGEMENT_TOKEN");
        }

        return null == value || value.isEmpty() ? null : value;
    }

    private static InetAddress resolveBindAddress() throws UnknownHostException {
        String host = System.getProperty("janf.management.host");
        if (null == host || host.isEmpty()) {
            host = System.getenv("JANF_MANAGEMENT_HOST");
        }

        // Loopback by default: the endpoints are not authenticated unless a token is configured.
        return null == host || host.isEmpty() ? InetAddress.getLoopbackAddress() : InetAddress.getByName(host);
    }

    /**
     * Start the management server in a background thread.
     * Once started, the server will accept incoming HTTP requests.
     */
    public void start() {
        if (!running.compareAndSet(false, true)) {
            return;
        }

        acceptExecutor = Executors.newFixedThreadPool(2, daemonThreadFactory("janf-management"));
        acceptThread = new Thread(this::acceptLoop, "janf-management-accept");
        acceptThread.setDaemon(true);
        acceptThread.start();

        DebugInfo.info("Management server started on port: " + getPort());
    }

    /**
     * Stop the management server gracefully.
     * Waits up to 1 second for the accept loop to terminate.
     */
    public void stop() {
        if (!running.compareAndSet(true, false)) {
            return;
        }

        try {
            serverSocket.close();
        } catch (IOException e) {
            DebugInfo.debug("Can not close management server socket", e);
        }

        Thread thread = acceptThread;
        if (null != thread) {
            thread.interrupt();
        }

        ExecutorService executor = acceptExecutor;
        if (null != executor) {
            executor.shutdownNow();
        }

        DebugInfo.info("Management server stopped");
    }

    /**
     * Get the address the server is bound to.
     *
     * @return the bound address
     */
    public InetAddress getBindAddress() {
        return serverSocket.getInetAddress();
    }

    /**
     * Get the port the server is listening on.
     *
     * @return the port number
     */
    public int getPort() {
        return serverSocket.getLocalPort();
    }

    /**
     * Check if the server is currently running.
     *
     * @return true if the server is running, false otherwise
     */
    public boolean isRunning() {
        return running.get();
    }

    private void acceptLoop() {
        while (running.get()) {
            try {
                Socket socket = serverSocket.accept();
                ExecutorService executor = acceptExecutor;
                if (null != executor && !executor.isShutdown()) {
                    executor.execute(() -> handleConnection(socket));
                } else {
                    closeQuietly(socket);
                }
            } catch (SocketException e) {
                if (running.get()) {
                    DebugInfo.warn("Management server socket error: " + e.getMessage());
                }
                return;
            } catch (IOException e) {
                if (running.get()) {
                    DebugInfo.error("Management server accept failed", e);
                }
            }
        }
    }

    private void handleConnection(Socket socket) {
        try (socket) {
            socket.setSoTimeout(SOCKET_TIMEOUT_MILLIS);

            Request request = readRequest(socket);
            if (null == request) {
                sendResponse(socket, 400, "text/plain; charset=UTF-8", "Bad request");
                return;
            }

            if (!isAuthorized(socket, request)) {
                DebugInfo.warn("Rejected management request from " + socket.getRemoteSocketAddress());
                sendResponse(socket, 401, "text/plain; charset=UTF-8", "Unauthorized");
                return;
            }

            route(request, socket);
        } catch (SocketTimeoutException e) {
            DebugInfo.debug("Management request timed out");
        } catch (Throwable e) {
            DebugInfo.debug("Management request failed: " + e.getMessage());
        }
    }

    /**
     * Read and parse the request head (request line and headers).
     *
     * @param socket the client socket
     * @return the parsed request, or {@code null} if the head is malformed
     * @throws IOException if reading fails
     */
    private Request readRequest(Socket socket) throws IOException {
        BufferedReader reader = new BufferedReader(new java.io.InputStreamReader(socket.getInputStream(), StandardCharsets.US_ASCII), MAX_REQUEST_HEAD_BYTES);

        String requestLine = reader.readLine();
        if (null == requestLine || requestLine.isEmpty()) {
            return null;
        }

        String[] parts = requestLine.split(" ");
        if (3 != parts.length) {
            return null;
        }

        Request request = new Request(parts[0], parts[1]);
        int headBytes = requestLine.length() + 2;
        String line;
        while (null != (line = reader.readLine()) && !line.isEmpty()) {
            headBytes += line.length() + 2;
            if (headBytes > MAX_REQUEST_HEAD_BYTES) {
                return null;
            }

            int colon = line.indexOf(':');
            if (colon > 0) {
                request.headers.put(line.substring(0, colon).trim(), line.substring(colon + 1).trim());
            }
        }

        return request;
    }

    /**
     * Check whether a request may access the management API.
     * <p>
     * A token (if configured) takes precedence; without a token only loopback clients are
     * allowed, matching the security model of the agent.
     * </p>
     *
     * @param socket  the client socket, used to resolve the remote address
     * @param request the parsed request
     * @return true if the caller is allowed
     */
    private boolean isAuthorized(Socket socket, Request request) {
        if (null != token) {
            String provided = request.header(TOKEN_HEADER);
            if (null == provided) {
                String authorization = request.header(AUTHORIZATION_HEADER);
                if (null != authorization && authorization.startsWith("Bearer ")) {
                    provided = authorization.substring("Bearer ".length()).trim();
                }
            }

            return token.equals(provided);
        }

        InetAddress remote = socket.getInetAddress();

        return null != remote && remote.isLoopbackAddress();
    }

    private void route(Request request, Socket socket) throws IOException {
        if ("/status".equals(request.path)) {
            if (!"GET".equalsIgnoreCase(request.method)) {
                sendResponse(socket, 405, "text/plain; charset=UTF-8", "Method not allowed");
                return;
            }

            handleStatus(socket);
            return;
        }

        if ("/reload".equals(request.path)) {
            if (!"POST".equalsIgnoreCase(request.method)) {
                sendResponse(socket, 405, "text/plain; charset=UTF-8", "Method not allowed");
                return;
            }

            handleReload(socket);
            return;
        }

        sendResponse(socket, 404, "text/plain; charset=UTF-8", "Not found");
    }

    /**
     * Handle GET /status requests.
     * Returns a JSON object with agent version, application name,
     * number of hooked classes, and number of loaded plugins.
     *
     * @param socket the client socket
     * @throws IOException if an I/O error occurs
     */
    private void handleStatus(Socket socket) throws IOException {
        String json = String.format(
                "{\"status\":\"running\",\"version\":\"%s\",\"appName\":\"%s\",\"hookedClasses\":%d,\"pluginsLoaded\":%d}",
                BuildVersion.getVersion(),
                BuildVersion.getAppName(),
                dispatcher.getHookClassNames().size(),
                pluginManager.getLoadedPlugins().size()
        );
        sendResponse(socket, 200, "application/json; charset=UTF-8", json);
    }

    /**
     * Handle POST /reload requests.
     * Triggers a reload of all plugins from the plugins directory.
     *
     * @param socket the client socket
     * @throws IOException if an I/O error occurs
     */
    private void handleReload(Socket socket) throws IOException {
        try {
            pluginManager.reloadPlugins();
            sendResponse(socket, 200, "application/json; charset=UTF-8", "{\"status\":\"ok\",\"message\":\"Plugins reloaded\"}");
        } catch (Exception e) {
            DebugInfo.error("Plugin reload failed", e);
            sendResponse(socket, 500, "application/json; charset=UTF-8",
                    "{\"status\":\"error\",\"message\":\"" + escapeJson(e.getMessage()) + "\"}");
        }
    }

    /**
     * Send an HTTP/1.1 response and close the connection.
     *
     * @param socket     the client socket
     * @param statusCode the HTTP status code
     * @param contentType the Content-Type header value
     * @param body       the response body
     * @throws IOException if an I/O error occurs
     */
    private static void sendResponse(Socket socket, int statusCode, String contentType, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        String reason = switch (statusCode) {
            case 200 -> "OK";
            case 400 -> "Bad Request";
            case 401 -> "Unauthorized";
            case 404 -> "Not Found";
            case 405 -> "Method Not Allowed";
            case 500 -> "Internal Server Error";
            default -> "Unknown";
        };

        String head = "HTTP/1.1 " + statusCode + " " + reason + "\r\n"
                + "Content-Type: " + contentType + "\r\n"
                + "Content-Length: " + bytes.length + "\r\n"
                + "Connection: close\r\n"
                + "\r\n";

        OutputStream os = socket.getOutputStream();
        os.write(head.getBytes(StandardCharsets.US_ASCII));
        os.write(bytes);
        os.flush();
    }

    /**
     * Escape a value so it can be embedded into a JSON string literal.
     *
     * @param value the raw value, may be {@code null}
     * @return the escaped value, never {@code null}
     */
    private static String escapeJson(String value) {
        if (null == value) {
            return "";
        }

        StringBuilder builder = new StringBuilder(value.length() + 16);
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"':
                    builder.append("\\\"");
                    break;
                case '\\':
                    builder.append("\\\\");
                    break;
                case '\n':
                    builder.append("\\n");
                    break;
                case '\r':
                    builder.append("\\r");
                    break;
                case '\t':
                    builder.append("\\t");
                    break;
                default:
                    if (c < 0x20) {
                        builder.append(String.format("\\u%04x", (int) c));
                    } else {
                        builder.append(c);
                    }
                    break;
            }
        }

        return builder.toString();
    }

    private static void closeQuietly(Socket socket) {
        try {
            socket.close();
        } catch (IOException e) {
            DebugInfo.debug("Can not close management connection: " + e.getMessage());
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
     * A parsed HTTP request head.
     */
    private static final class Request {
        private final String method;
        private final String path;
        private final java.util.Map<String, String> headers = new java.util.HashMap<>();

        Request(String method, String path) {
            this.method = method;
            this.path = path;
        }

        String header(String name) {
            return headers.get(name);
        }
    }
}
