package com.epam.gym.workload;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;

/**
 * A stand-in for the Trainer Workload service: records every request and answers with a configurable status
 * after a configurable delay.
 */
final class StubWorkloadServer {

    record ReceivedRequest(String method, String path, Map<String, List<String>> headers, String body) {
        String header(String name) {
            return headers.entrySet().stream()
                    .filter(entry -> entry.getKey().equalsIgnoreCase(name))
                    .map(entry -> entry.getValue().get(0))
                    .findFirst()
                    .orElse(null);
        }
    }

    private final HttpServer server;
    private final List<ReceivedRequest> requests = new CopyOnWriteArrayList<>();
    private volatile int status = 200;
    private volatile Duration delay = Duration.ZERO;

    private StubWorkloadServer(HttpServer server) {
        this.server = server;
    }

    static StubWorkloadServer start() {
        try {
            HttpServer httpServer = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            StubWorkloadServer stub = new StubWorkloadServer(httpServer);
            httpServer.createContext("/", exchange -> {
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                stub.requests.add(new ReceivedRequest(exchange.getRequestMethod(), exchange.getRequestURI().getPath(),
                        Map.copyOf(exchange.getRequestHeaders()), body));
                stub.sleep();
                exchange.sendResponseHeaders(stub.status, -1);
                exchange.close();
            });
            httpServer.setExecutor(Executors.newCachedThreadPool());
            httpServer.start();
            return stub;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    String uri() {
        return "http://localhost:" + server.getAddress().getPort();
    }

    void respondWith(int status, Duration delay) {
        this.status = status;
        this.delay = delay;
    }

    void reset() {
        requests.clear();
        respondWith(200, Duration.ZERO);
    }

    List<ReceivedRequest> requests() {
        return List.copyOf(requests);
    }

    void stop() {
        server.stop(0);
    }

    private void sleep() {
        try {
            Thread.sleep(delay.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
