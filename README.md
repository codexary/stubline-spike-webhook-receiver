English | [简体中文](README.zh-CN.md)

# stubline-spike-webhook-receiver

A spike: how we should receive webhooks from the payment provider. The receiver checks that each notification really comes from the provider, rejects old ones replayed later, and processes each event only once even when the provider sends it again. It is not meant for production.

How it decides:

- The provider signs `timestamp.body` (the ASCII timestamp, a period, then the exact raw request body) with HMAC-SHA256 and a shared secret. A wrong signature gets `401`.
- A timestamp more than five minutes away from the receiver's clock gets `401`, so a captured request cannot be replayed later.
- Each event's JSON `id` is remembered. A valid duplicate gets `204` without being processed again, so the provider stops retrying it.

## License

This repository is licensed under the [MIT License](LICENSE).

## About this repository

This standalone spike records an experiment for Stubline. Its conclusion is recorded in the main Stubline repository ADR; the observed result is described above.

The code and documents in this repository were written with the help of AI tools; every recorded output comes from actually running them.
