# Implementation Plan: Event-Driven Integration Layer

**Date**: 2026-02-14
**Spec**: TBD (use Spec Kit templates under `speckit/templates`)

## Summary

Build an event-driven integration layer (webhooks -> inbox -> queue -> idempotent processing -> outbox) with auditability, replay, and basic observability. The plan focuses on a Laravel-based stack and production-realistic reliability patterns.

## Technical Context

- **Language/Framework**: PHP 8.x + Laravel
- **Queue**: Redis (Laravel queues)
- **Storage**: PostgreSQL
- **Observability**: Structured logs + basic metrics
- **Target**: Server backend
- **Constraints**: Avoid heavy work on request; prefer async processing
- **Scale/Scope**: Small to mid workloads, at-least-once delivery

## Spec-Driven Artifacts (Spec Kit)

When creating features, use the Spec Kit templates from `speckit/templates` and keep specs in `specs/<id-feature>/`:

```text
specs/<id-feature>/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
└── tasks.md
```

## Scope

### MVP
- HTTP webhook ingestion with provider validation
- Inbox persistence with dedupe
- Async processing via queue workers
- Event status + attempts + backoff
- Handlers by provider/topic
- Minimal dashboard (list events and status)

### V1
- Outbox for reliable external effects
- DLQ (dead-letter) + manual replay
- End-to-end correlation ID
- Rate limiting/backpressure per provider/tenant
- Data retention (TTL)

### V2
- Circuit breaker per provider
- Optional tracing (OpenTelemetry)
- Partial ordering by key where required
- Batch replay with guardrails
- Reusable integration adapters/templates

## Architecture (High Level)

### Happy Path
1. `POST /webhooks/{provider}/{topic}`
2. Validate signature + normalize payload
3. Insert `inbox_events` with status `RECEIVED`
4. Enqueue `ProcessInboxEvent(inbox_event_id)`
5. Worker:
   - claim/lock
   - run handler
   - persist effects + create outbox in same tx
6. Outbox worker sends/publishes and marks `SENT`

### Failure Path
- Transient failure -> `FAILED` + `next_retry_at` + backoff
- Max attempts exceeded -> `DEAD` (DLQ)
- Replay -> re-enqueue with guardrails

## Data Model (Minimum)

### inbox_events
- id (pk)
- provider (string)
- topic (string)
- external_event_id (string, nullable)
- payload_hash (string)
- payload_json (json/text)
- status: RECEIVED | PROCESSING | PROCESSED | FAILED | DEAD
- attempts (int)
- next_retry_at (datetime, nullable)
- received_at, processed_at
- last_error_code, last_error_message
- correlation_id (string)

Indexes:
- UNIQUE(provider, external_event_id) when present
- INDEX(status, received_at)
- INDEX(provider, topic, received_at)

### outbox_messages
- id
- type (string)
- payload_json (json/text)
- status: PENDING | SENDING | SENT | FAILED | DEAD
- attempts (int)
- next_retry_at (datetime, nullable)
- last_error (text)
- created_at, sent_at
- correlation_id

Indexes:
- INDEX(status, created_at)
- INDEX(type, created_at)

### Optional: event_processing_logs
- inbox_event_id
- attempt
- started_at, finished_at
- result (ok/fail)
- error_summary

## Reliability Contracts

### Ingestion
- Always validate signatures
- Generate/propagate `correlation_id`
- Persist original payload (or hash + ref)
- Never do heavy processing on request

### Processing
- Logical claim/lock per `inbox_event_id`
- Exponential backoff with jitter
- Limits: `timeout`, `max_attempts`, `retry_until`
- Separate queues: `inbox-processing`, `outbox-publish`, `dlq`

### Replay
- Replay only for `FAILED` or `DEAD`
- New attempt with history preserved
- Batch replay with filters and limits

## Observability

### Structured Logs (JSON)
Include:
- correlation_id
- provider, topic
- inbox_event_id
- attempt
- status transitions
- handler latency
- error summary + code

### Metrics (Initial)
- inbox_received_total
- inbox_processed_total
- inbox_failed_total
- inbox_dead_total
- queue_lag_seconds
- outbox_pending_total
- outbox_sent_total

### Dashboard (Minimal)
- List events with filters (provider, status, time)
- Event detail (payload, history, error)
- Actions: replay / mark ignored (optional)

## Security

- Signature verification per provider
- Rate limiting per IP/provider
- Payload size limits
- Schema validation and sanitization
- Store secrets in env/secret manager

## Implementation Phases

### Phase 0 - Base
- Docker compose (app + db + redis)
- Queue config + worker
- Structured logging

### Phase 1 - Inbox + Ingestion
- Migration `inbox_events`
- Webhook controller (provider/topic)
- Signature validator + normalizer
- Persist with dedupe
- Enqueue `ProcessInboxEvent`

### Phase 2 - Idempotent Processing
- Claim/lock + status transitions
- Handler router (provider/topic -> handler)
- Backoff + attempts + retryUntil
- Simple list/status page

### Phase 3 - Outbox
- Migration `outbox_messages`
- Create outbox within processing transaction
- Worker `PublishOutboxMessage`
- Retry/backoff + statuses

### Phase 4 - DLQ + Replay
- Define DEAD rules
- Commands: `inbox:replay {id}` and `inbox:replay-batch`
- UI/endpoint for replay with guardrails

### Phase 5 - Hardening
- Rate limiting/backpressure per provider
- TTL retention policy
- Document ADRs and manual test scenarios

## Manual Test Scenarios

- Send same webhook 5x -> only 1 effect applied
- Kill worker mid-processing -> resume without duplicates
- Force handler failure -> retries with backoff
- Exceed attempts -> moves to DLQ (DEAD)
- Replay from DLQ -> processes to PROCESSED
- Outbox send failure -> retries then SENT

## Deliverables

- README with overview, architecture, and local run guide
- ADRs in `docs/adr/`
- Scripts to reproduce manual scenarios
- Example providers (2 adapters)

## Done Criteria

- Idempotency demonstrated end-to-end
- Inbox + Outbox implemented
- DLQ + replay available
- Minimal observability (logs + metrics)
- Manual test scripts reproducible

## Complexity Tracking

No known constitution violations at this stage.
