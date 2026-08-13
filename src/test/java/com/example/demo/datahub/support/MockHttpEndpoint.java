package com.example.demo.datahub.support;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

/** A tiny local HTTP server so DataHub adapter tests exercise real HTTP calls without an external service. */
public class MockHttpEndpoint implements AutoCloseable {

    private final HttpServer server;

    public MockHttpEndpoint() {
        try {
            server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        server.setExecutor(null);
        server.start();
    }

    public String baseUrl() {
        return "http://localhost:" + server.getAddress().getPort();
    }

    public void respondJson(String path, int status, String jsonBody) {
        server.createContext(path, exchange -> {
            byte[] bytes = jsonBody.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
    }

    public void handle(String path, HttpHandler handler) {
        server.createContext(path, handler);
    }

    @Override
    public void close() {
        server.stop(0);
    }
}
