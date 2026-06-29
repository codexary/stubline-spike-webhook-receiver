package com.elsworne.stubline.spike.webhook;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class WebhookReceiverTest {
  @Test
  void acceptsValidDropsDuplicateAndRejectsForgeryAndStaleReplay() throws Exception {
    byte[] secret = "test-secret".getBytes(StandardCharsets.UTF_8);
    Clock clock = Clock.fixed(Instant.parse("2026-10-29T00:00:00Z"), ZoneOffset.UTC);
    byte[] body = "{\"id\":\"evt-1\"}".getBytes(StandardCharsets.UTF_8);
    long now = clock.instant().getEpochSecond();
    try (var server = new WebhookReceiver(0, secret, clock);
        var client = HttpClient.newHttpClient()) {
      server.start();
      URI uri = URI.create("http://localhost:" + server.port() + "/webhooks/payments");
      String valid = WebhookSignature.sign(secret, now, body);
      assertEquals(204, send(client, uri, body, now, valid));
      assertEquals(204, send(client, uri, body, now, valid));
      assertEquals(1, server.acceptedCount());
      assertEquals(
          401,
          send(
              client,
              uri,
              body,
              now,
              WebhookSignature.sign("wrong".getBytes(StandardCharsets.UTF_8), now, body)));
      long stale = now - 301;
      assertEquals(401, send(client, uri, body, stale, WebhookSignature.sign(secret, stale, body)));
      assertEquals(1, server.acceptedCount());
    }
  }

  @Test
  void acceptsBothToleranceEdgesAndRejectsOneSecondOutside() throws Exception {
    byte[] secret = "test-secret".getBytes(StandardCharsets.UTF_8);
    Clock clock = Clock.fixed(Instant.parse("2026-10-29T00:00:00Z"), ZoneOffset.UTC);
    try (var server = new WebhookReceiver(0, secret, clock);
        var client = HttpClient.newHttpClient()) {
      server.start();
      URI uri = URI.create("http://localhost:" + server.port() + "/webhooks/payments");
      for (int offset : new int[] {-301, -300, 300, 301}) {
        byte[] body = ("{\"id\":\"edge-" + offset + "\"}").getBytes(StandardCharsets.UTF_8);
        long timestamp = clock.instant().getEpochSecond() + offset;
        assertEquals(
            Math.abs(offset) == 300 ? 204 : 401,
            send(client, uri, body, timestamp, WebhookSignature.sign(secret, timestamp, body)));
      }
      assertEquals(2, server.acceptedCount());
    }
  }

  private static int send(HttpClient client, URI uri, byte[] body, long timestamp, String signature)
      throws Exception {
    var request =
        HttpRequest.newBuilder(uri)
            .header("X-Webhook-Timestamp", Long.toString(timestamp))
            .header("X-Webhook-Signature", signature)
            .POST(HttpRequest.BodyPublishers.ofByteArray(body))
            .build();
    return client.send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
  }
}
