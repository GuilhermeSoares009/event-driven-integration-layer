# Event-Driven Integration Layer

Event-driven integration layer focused on webhook ingestion, idempotent processing, and reliable external effects through the outbox pattern.

## Current Status

- Primary backend target: Java 21 + Spring Boot 3
-  / implementation remains in `app/` during transition
- Local platform is fully dockerized for onboarding and reproducibility
- release , M2, M3, and M4 are complete (bootstrap + ingestion + async workers + ops controls)

## Target Architecture

1. `POST /webhooks/{provider}/{topic}` receives provider events
2. Signature is validated and payload is normalized
3. Event is persisted in `inbox_events` with deduplication guarantees
4. Async processing updates state and creates `outbox_messages`
5. Outbox publisher performs reliable external publishing with retries and DLQ behavior

Current implementation coverage:

- Health endpoints (`/api/v1/health`, `/actuator/health`)
- Webhook ingestion endpoint (`/api/v1/webhooks/{provider}/{topic}` and `/webhooks/{provider}/{topic}`)
- Signature validation by provider secret
- Deduplication by `provider + external_event_id` or `provider + payload_hash`
- Inbox persistence with correlation ID
- Scheduled inbox processing with retry/backoff and DLQ transition
- Outbox publishing to Kafka with retry/backoff and DLQ transition
- Provider/IP rate limiting and provider circuit breaker
- Ops endpoints for replay and retention pruning

## Tech Stack

- Backend: Java 21, Spring Boot 3, Flyway
- Data: PostgreSQL, Redis
- Messaging: Kafka
- Edge/Gateway: Nginx
- Observability baseline: Spring Actuator + Prometheus endpoint
- Containers: Docker Compose

## Repository Structure

```text
service/        Spring Boot service (new implementation)
app/              service (transition reference)
docs/adr/       Architecture decision records
ops/nginx/      Nginx reverse-proxy configuration
docker-compose.yml
```

## Quick Start (Docker)

Prerequisites:

- Docker Desktop (or Docker Engine + Compose plugin)

Run the full local platform:

```bash
docker compose up -d --build
```

Check service health:

```bash
curl http://localhost:8080/api/v1/health
curl http://localhost:8080/actuator/health
```

Stop everything:

```bash
docker compose down
```

## Local Endpoints

- API Gateway (Nginx): `http://localhost:8080`
- Spring health: `GET /api/v1/health`
- Actuator health: `GET /actuator/health`
- Prometheus metrics: `GET /actuator/prometheus`
- PostgreSQL: `localhost:5432`
- Redis: `localhost:6379`
- Kafka: `localhost:9092`

## Webhook Example

```bash
curl -X POST "http://localhost:8080/api/v1/webhooks/test/orders" \
  -H "Content-Type: application/json" \
  -H "X-Signature: <hmac_sha256_hex>" \
  -H "X-Correlation-Id: corr-123" \
  -d '{"external_event_id":"evt_1","foo":"bar"}'
```

## Async Workers

- Inbox worker transitions events through `RECEIVED -> PROCESSING -> PROCESSED`.
- Inbox failures move to `FAILED` with retry backoff and then `DEAD` after max attempts.
- Outbox worker transitions messages through `PENDING -> SENDING -> SENT`.
- Outbox publish failures move to `FAILED` with retry backoff and then `DEAD` after max attempts.
- Retry schedule defaults to `30, 120, 300, 900, 1800` seconds plus jitter.

## Ops Endpoints

- `POST /api/v1/ops/inbox/{id}/replay`
- `POST /api/v1/ops/inbox/replay-batch?provider=&topic=&limit=`
- `DELETE /api/v1/ops/inbox/prune?days=`
- `DELETE /api/v1/ops/outbox/prune?days=`

## Service Configuration

- `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`
- `REDIS_HOST`, `REDIS_PORT`
- `KAFKA_BOOTSTRAP_SERVERS`
- `SPRING_PROFILES_ACTIVE`
- `WEBHOOK_PROVIDER_<PROVIDER>_SECRET`
- `integration.rate-limits.*`
- `integration.circuit-breaker.*`
- `integration.retention.*`
- `integration.processing.inbox.*` and `integration.processing.outbox.*`
- `integration.outbox.kafka.topic-prefix`

Defaults are provided for local Docker usage in `docker-compose.yml`.

For local testing, the compose stack sets `WEBHOOK_PROVIDER_TEST_SECRET=test-secret` by default.

You can also copy `service/.env.example` and adapt values for local execution.

## transition Notes

- The Java service is the target runtime.
- The  app remains available as functional reference while transition releases are completed.
- release publishing follows clean, standardized commits in English.
