# Research: Webhook Inbox

## Goals

- Confirm signature validation approach per provider
- Define dedupe strategy when `external_event_id` is missing
- Keep ingestion fast and safe

## Findings

- Signature verification should be provider-specific; default to HMAC (shared secret) if provider does not define a scheme.
- Dedupe primary key uses `(provider, external_event_id)` when present.
- When `external_event_id` is missing, compute `payload_hash` over a normalized JSON payload.
- Payload normalization should remove fields that are non-deterministic (timestamps, request ids) if known per provider.

## Open Questions

- Which providers are in scope for MVP and their signature schemes?
- Response code preference: 200 or 202 for accepted webhooks?
- Payload size limit for ingestion?
