# Tasks: Webhook Inbox

## Phase 1 - Schema and routing

- [ ] Create migration for `inbox_events` table
- [ ] Add route `POST /webhooks/{provider}/{topic}`

## Phase 2 - Ingestion pipeline

- [ ] Implement provider signature validator interface
- [ ] Implement payload normalizer and `payload_hash`
- [ ] Persist `InboxEvent` with `status=RECEIVED`
- [ ] Add dedupe by `(provider, external_event_id)` or `payload_hash`

## Phase 3 - Response behavior

- [ ] Return success for duplicate events without new persistence
- [ ] Reject invalid signatures without persistence
- [ ] Ensure response is fast (no heavy processing)

## Phase 4 - Tests and docs

- [ ] Add tests for valid ingestion and dedupe
- [ ] Add tests for invalid signature
- [ ] Update README or ADRs if needed
