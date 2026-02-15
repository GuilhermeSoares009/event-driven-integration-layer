# Event-Driven Integration Layer

Event-driven integration layer focused on webhook ingestion, idempotent processing, and reliable external effects via outbox.

## Overview

- Pattern: webhooks -> inbox -> queue -> processing -> outbox
- Reliability: idempotency, dedupe, retries, DLQ, replay
- Observability: structured logs + basic metrics
- Stack target: Laravel (PHP), Redis queues, PostgreSQL

## Architecture (High Level)

1. `POST /webhooks/{provider}/{topic}`
2. Validate signature + normalize payload
3. Insert `inbox_events` (status `RECEIVED`)
4. Enqueue `ProcessInboxEvent(inbox_event_id)`
5. Worker handles processing and creates outbox in same tx
6. Outbox worker publishes and marks `SENT`

## Key Features

- Inbox dedupe by provider/external ID
- Idempotent handlers
- Retry/backoff + DLQ
- Replay with guardrails
- Minimal dashboard and audit trail

## Project Structure (Planned)

```text
app/IntegrationLayer/
├── Ingestion/
├── Inbox/
├── Handlers/
├── Queue/
├── Outbox/
└── Ops/

docs/adr/
speckit/
skills/
```

## Getting Started (Expected)

Laravel project lives in `app/`.

1. Install dependencies: `cd app && composer install`
2. Configure `.env` (DB + Redis)
3. Run migrations: `cd app && php artisan migrate`
4. Start server: `cd app && php artisan serve`
5. Start worker: `cd app && php artisan queue:work`

Adjust commands to your local setup if scripts differ.

## Docker (PostgreSQL + Redis)

To spin up local dependencies:

1. `docker compose up -d`
2. Update `app/.env` with:
   - `DB_CONNECTION=pgsql`
   - `DB_HOST=127.0.0.1`
   - `DB_PORT=5432`
   - `DB_DATABASE=integration_layer`
   - `DB_USERNAME=postgres`
   - `DB_PASSWORD=postgres`
   - `REDIS_HOST=127.0.0.1`
   - `REDIS_PORT=6379`

Create a test database `integration_layer_test` for `php artisan test`.

## Spec-Driven Development

- Spec Kit assets live under `speckit/`.
- Create features under `specs/<id-feature>/` using templates from `speckit/templates`.
- Keep specs and plans updated as the source of truth.

## Manual Scenarios

- Send the same webhook 5x -> only one effect applied
- Kill worker mid-processing -> resumes without duplicates
- Force handler failure -> retries with backoff
- Exceed attempts -> moves to DLQ (DEAD)
- Replay from DLQ -> processes to PROCESSED
- Outbox send failure -> retries then SENT

## Observability

Structured logs (JSON-like context):
- `webhook.received` (provider, topic, correlation_id, inbox_event_id)
- `webhook.invalid_signature` (provider, topic, correlation_id)
- `inbox.processed` (provider, topic, correlation_id, inbox_event_id)
- `outbox.sent` (type, correlation_id, outbox_message_id)

Metrics logged via `metric` entries:
- `inbox_received_total`
- `inbox_processed_total`
- `inbox_failed_total`
- `inbox_dead_total`
- `outbox_pending_total`
- `outbox_sent_total`
- `outbox_failed_total`
- `outbox_dead_total`
- `webhook_rate_limited_total`

## Rate Limiting

Rate limiting is applied per provider and IP.

Configure limits in `app/config/integration-layer.php`:

- `rate_limits.default_per_minute`
- `rate_limits.<provider>`
