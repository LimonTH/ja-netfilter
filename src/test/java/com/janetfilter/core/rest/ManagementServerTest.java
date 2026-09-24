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

import com.janetfilter.core.Dispatcher;
import com.janetfilter.core.Environment;
import com.janetfilter.core.plugin.PluginManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ManagementServer.
 */
public class ManagementServerTest {
    private static final String TOKEN_PROPERTY = "janf.management.token";
    private static final String TOKEN = "s3cr3t";

    private Path base;
    private ManagementServer server;
    private HttpClient client;

    @BeforeEach
    public void setUp() throws IOException {
        base = Files.createTempDirectory("janf-management");
        System.clearProperty(TOKEN_PROPERTY);
        client = HttpClient.newHttpClient();
    }

    @AfterEach
    public void tearDown() {
        System.clearProperty(TOKEN_PROPERTY);
        if (null != server) {
            server.stop();
            server = null;
        }
    }

    @Test
    public void testServerShouldBindToLoopbackByDefault() throws IOException {
        start();

        assertTrue(server.getBindAddress().isLoopbackAddress());
        assertTrue(server.isRunning());
    }

    @Test
    public void testStatusShouldReturnAgentState() throws Exception {
        start();

        HttpResponse<String> response = send("GET", "/status", null);

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("\"status\":\"running\""));
        assertTrue(response.body().contains("\"hookedClasses\":0"));
        assertTrue(response.body().contains("\"pluginsLoaded\":0"));
    }

    @Test
    public void testStatusShouldRejectUnsupportedMethod() throws Exception {
        start();

        assertEquals(405, send("POST", "/status", null).statusCode());
    }

    @Test
    public void testReloadShouldReloadPlugins() throws Exception {
        start();

        HttpResponse<String> response = send("POST", "/reload", null);

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("\"status\":\"ok\""));
    }

    @Test
    public void testReloadShouldRejectUnsupportedMethod() throws Exception {
        start();

        assertEquals(405, send("GET", "/reload", null).statusCode());
    }

    @Test
    public void testRequestsShouldRequireTokenWhenConfigured() throws Exception {
        System.setProperty(TOKEN_PROPERTY, TOKEN);
        start();

        assertEquals(401, send("GET", "/status", null).statusCode());
        assertEquals(401, send("GET", "/status", "wrong").statusCode());
        assertEquals(200, send("GET", "/status", TOKEN).statusCode());
    }

    @Test
    public void testBearerTokenShouldBeAccepted() throws Exception {
        System.setProperty(TOKEN_PROPERTY, TOKEN);
        start();

        assertEquals(200, sendWithAuthorization("GET", "/status", "Bearer " + TOKEN).statusCode());
        assertEquals(401, sendWithAuthorization("GET", "/status", "Bearer wrong").statusCode());
    }

    @Test
    public void testReloadShouldRequireTokenWhenConfigured() throws Exception {
        System.setProperty(TOKEN_PROPERTY, TOKEN);
        start();

        assertEquals(401, send("POST", "/reload", null).statusCode());
        assertEquals(200, send("POST", "/reload", TOKEN).statusCode());
    }

    @Test
    public void testStopShouldStopAcceptingRequests() throws Exception {
        start();
        assertTrue(server.isRunning());

        server.stop();

        assertFalse(server.isRunning());
        assertThrows(IOException.class, () -> send("GET", "/status", null));
    }

    /**
     * Start a management server on an ephemeral loopback port.
     */
    private void start() throws IOException {
        Environment environment = new Environment(null, base.resolve("ja-netfilter.jar").toFile(), false);
        Dispatcher dispatcher = new Dispatcher(environment);
        PluginManager pluginManager = new PluginManager(dispatcher, environment);
        pluginManager.loadPlugins();

        server = new ManagementServer(0, dispatcher, pluginManager);
        server.start();
    }

    private HttpResponse<String> send(String method, String path, String token) throws Exception {
        HttpRequest.Builder builder = request(method, path);
        if (null != token) {
            builder.header("X-JANF-Token", token);
        }

        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> sendWithAuthorization(String method, String path, String authorization) throws Exception {
        HttpRequest.Builder builder = request(method, path);
        builder.header("Authorization", authorization);

        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpRequest.Builder request(String method, String path) {
        return HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + server.getPort() + path))
                .method(method, HttpRequest.BodyPublishers.noBody());
    }
}
