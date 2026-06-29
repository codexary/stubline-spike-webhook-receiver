package com.elsworne.stubline.spike.webhook;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

public final class WebhookSender {
    private WebhookSender() { }
    public static void main(String[] args) throws Exception {
        String secret = System.getenv("WEBHOOK_SECRET");
        if (secret == null || secret.isBlank()) throw new IllegalArgumentException("Set WEBHOOK_SECRET");
        URI uri = URI.create(args.length == 0 ? "http://localhost:8081/webhooks/payments" : args[0]);
        byte[] body = "{\"id\":\"evt-1\",\"type\":\"payment.succeeded\"}".getBytes(StandardCharsets.UTF_8);
        long now = Instant.now().getEpochSecond();
        try (var client = HttpClient.newHttpClient()) {
            System.out.println("Valid: " + send(client, uri, body, now, WebhookSignature.sign(secret.getBytes(StandardCharsets.UTF_8), now, body)));
            System.out.println("Duplicate: " + send(client, uri, body, now, WebhookSignature.sign(secret.getBytes(StandardCharsets.UTF_8), now, body)));
            System.out.println("Forged: " + send(client, uri, body, now, WebhookSignature.sign("wrong-secret".getBytes(StandardCharsets.UTF_8), now, body)));
            long stale = now - 600;
            System.out.println("Stale: " + send(client, uri, body, stale, WebhookSignature.sign(secret.getBytes(StandardCharsets.UTF_8), stale, body)));
        }
    }

    private static int send(HttpClient client, URI uri, byte[] body, long timestamp, String signature) throws Exception {
        var request = HttpRequest.newBuilder(uri).header("X-Webhook-Timestamp", Long.toString(timestamp))
                .header("X-Webhook-Signature", signature).header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofByteArray(body)).build();
        return client.send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
    }
}
