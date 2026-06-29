package com.elsworne.stubline.spike.webhook;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Logger;

public final class WebhookMain {
  private static final Logger LOG = Logger.getLogger(WebhookMain.class.getName());

  private WebhookMain() {}

  public static void main(String[] args) throws Exception {
    String secret = System.getenv("WEBHOOK_SECRET");
    if (secret == null || secret.isBlank())
      throw new IllegalArgumentException("Set WEBHOOK_SECRET");
    int port = args.length == 0 ? 8081 : Integer.parseInt(args[0]);
    var server =
        new WebhookReceiver(port, secret.getBytes(StandardCharsets.UTF_8), Clock.systemUTC());
    server.start();
    LOG.info("Webhook receiver listening on " + server.port());
    Runtime.getRuntime().addShutdownHook(new Thread(server::close));
    new CountDownLatch(1).await();
  }
}
