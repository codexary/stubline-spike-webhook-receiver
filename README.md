English | [简体中文](README.zh-CN.md)

# stubline-spike-webhook-receiver

A spike: how we should receive webhooks from the payment provider. The receiver checks that each notification really comes from the provider, rejects old ones replayed later, and processes each event only once even when the provider sends it again. It is not meant for production.

How it decides:

- The provider signs `timestamp.body` (the ASCII timestamp, a period, then the exact raw request body) with HMAC-SHA256 and a shared secret. A wrong signature gets `401`.
- A timestamp more than five minutes away from the receiver's clock gets `401`, so a captured request cannot be replayed later.
- Each event's JSON `id` is remembered. A valid duplicate gets `204` without being processed again, so the provider stops retrying it.

## Build and test

You need JDK 21. The Maven wrapper downloads the right Maven version by itself.

```bash
./mvnw -q verify
```

## Run

Both programs read the shared secret from `WEBHOOK_SECRET`; set the same demo value in both terminals.

Terminal 1, the receiver:

```bash
export WEBHOOK_SECRET=demo-secret
./mvnw -q exec:java
```

It logs `Webhook receiver listening on 8081`.

Terminal 2, a sender that plays the provider and sends a valid event, the same event again, a forged one and a stale one:

```bash
export WEBHOOK_SECRET=demo-secret
./mvnw -q exec:java -Dexec.mainClass=com.elsworne.stubline.spike.webhook.WebhookSender
```

Expected output:

```text
Valid: 204
Duplicate: 204
Forged: 401
Stale: 401
```

The remembered event ids live in memory and are lost on restart. A production receiver would store them and decide how long to keep them.

The receiver binds to loopback because the shared secret and sender are local demo values; it is reachable only from this machine.

## License

This repository is licensed under the [MIT License](LICENSE).

## About this repository

This standalone spike records an experiment for Stubline. Its conclusion is recorded in the main Stubline repository ADR; the observed result is described above.

The code and documents in this repository were written with the help of AI tools; every recorded output comes from actually running them.

## Findings

The experiment results and their effect on the backend decision are recorded in [FINDINGS.md](FINDINGS.md).
