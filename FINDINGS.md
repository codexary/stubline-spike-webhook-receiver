English | [简体中文](FINDINGS.zh-CN.md)

# Findings

## What we observed

The receiver verifies HMAC-SHA256 over the timestamp and raw body, rejects requests outside a five-minute window, and remembers event IDs. The demo returns 204 for a valid event and its duplicate, and 401 for a forged or stale event. Remembered IDs live only in memory and disappear on restart.

## Backend decision

Signed delivery can repeat; a callback receiver must authenticate, bound replay, and deduplicate durably. This does not make a payment charge request idempotent. The backend uses `Idempotency-Key` for charge requests and has not integrated provider webhooks. See [backend ADR 0008 — Keyed payment invocations](https://github.com/codexary/stubline-backend/blob/main/docs/adr/0008-keyed-payment-invocations.md).
