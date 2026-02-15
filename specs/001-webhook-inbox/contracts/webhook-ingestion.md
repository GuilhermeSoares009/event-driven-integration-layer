# Contract: Webhook Ingestion

## Endpoint

`POST /webhooks/{provider}/{topic}`

## Headers

- `X-Signature`: provider signature (required)
- `X-Correlation-Id`: optional; if missing, generate one

## Request Body

JSON payload as provided by the webhook sender.

## Responses

- `200 OK` or `202 Accepted` on successful ingestion
- `401 Unauthorized` on invalid signature
- `400 Bad Request` on malformed payload

## Behavior

- Validate signature before persistence
- Normalize payload and compute `payload_hash`
- Persist `InboxEvent` with `status=RECEIVED`
- Deduplicate by `(provider, external_event_id)` when present
