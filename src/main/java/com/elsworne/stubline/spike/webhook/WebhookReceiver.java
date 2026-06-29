package com.elsworne.stubline.spike.webhook;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

public final class WebhookReceiver implements AutoCloseable {
    private static final Logger LOG = Logger.getLogger(WebhookReceiver.class.getName());
    private static final Duration TOLERANCE = Duration.ofMinutes(5);
    private final byte[] secret;
    private final Clock clock;
    private final Set<String> receivedIds = ConcurrentHashMap.newKeySet();
    private final AtomicInteger accepted = new AtomicInteger();
    private final ObjectMapper json = new ObjectMapper();
    private final HttpServer server;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public WebhookReceiver(int port, byte[] secret, Clock clock) throws IOException {
        this.secret = secret.clone(); this.clock = clock;
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), port), 0);
        server.setExecutor(executor);
        server.createContext("/webhooks/payments", this::handle);
    }

    public int port() { return server.getAddress().getPort(); }
    public int acceptedCount() { return accepted.get(); }
    public void start() { server.start(); }

    private void handle(HttpExchange exchange) throws IOException {
        try (exchange) {
            if (!exchange.getRequestMethod().equals("POST")) { respond(exchange, 405, "Method Not Allowed"); return; }
            byte[] body = exchange.getRequestBody().readNBytes(1_048_577);
            if (body.length > 1_048_576) { respond(exchange, 413, "Payload Too Large"); return; }
            String rawTimestamp = exchange.getRequestHeaders().getFirst("X-Webhook-Timestamp");
            long timestamp;
            try { timestamp = Long.parseLong(rawTimestamp); }
            catch (NumberFormatException | NullPointerException e) { respond(exchange, 401, "Invalid signature"); return; }
            Instant eventTime;
            try { eventTime = Instant.ofEpochSecond(timestamp); }
            catch (java.time.DateTimeException e) { respond(exchange, 401, "Invalid signature"); return; }
            if (Duration.between(eventTime, clock.instant()).abs().compareTo(TOLERANCE) > 0
                    || !WebhookSignature.verify(secret, timestamp, body, exchange.getRequestHeaders().getFirst("X-Webhook-Signature"))) {
                respond(exchange, 401, "Invalid signature"); return;
            }
            JsonNode event;
            try { event = json.readTree(body); }
            catch (IOException e) { respond(exchange, 400, "Invalid JSON"); return; }
            if (event == null || !event.path("id").isTextual() || event.path("id").asText().isBlank()) {
                respond(exchange, 400, "Missing event id"); return;
            }
            String id = event.path("id").asText();
            if (receivedIds.add(id)) { accepted.incrementAndGet(); LOG.info("Accepted webhook " + id); }
            else LOG.info("Dropped duplicate webhook " + id);
            exchange.sendResponseHeaders(204, -1);
        }
    }

    private static void respond(HttpExchange exchange, int status, String message) throws IOException {
        byte[] bytes = message.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
    }

    @Override public void close() { server.stop(0); executor.shutdownNow(); }
}
