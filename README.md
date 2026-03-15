# Event-Driven Integration Layer

Event-driven integration service for webhook ingestion, idempotent processing, and reliable message publishing using the outbox pattern.

## Features

- Webhook ingestion with HMAC signature validation
- Deduplication by `provider + external_event_id` or `provider + payload_hash`
- Async inbox processing with retry/backoff and dead-letter handling
- Outbox publishing to Kafka with retry/backoff and dead-letter handling
- Operational endpoints for replay, rate limiting, circuit breaker, and pruning
- Built-in observability with Spring Actuator, Prometheus, and Grafana

## Tech Stack

- Java 21 + Spring Boot 3
- PostgreSQL, Redis, Kafka
- Nginx (API gateway)
- Docker Compose
- Prometheus + Grafana

## Quick Start

```bash
docker compose up -d --build
```

Health checks:

```bash
curl http://localhost:8080/api/v1/health
curl http://localhost:8080/actuator/health
```

Stop local stack:

```bash
docker compose down
```

## Main Endpoints

- API gateway: `http://localhost:8080`
- Webhook ingestion: `POST /api/v1/webhooks/{provider}/{topic}`
- Actuator metrics: `GET /actuator/prometheus`
- Prometheus UI: `http://localhost:9090`
- Grafana UI: `http://localhost:3000` (`admin`/`admin`)

## Webhook Example

```bash
curl -X POST "http://localhost:8080/api/v1/webhooks/test/orders" \
  -H "Content-Type: application/json" \
  -H "X-Signature: <hmac_sha256_hex>" \
  -H "X-Correlation-Id: corr-123" \
  -d '{"external_event_id":"evt_1","foo":"bar"}'
```

## Local Configuration

- Defaults are configured in `docker-compose.yml`
- Example env file: `service/.env.example`
- Default test secret: `WEBHOOK_PROVIDER_TEST_SECRET=test-secret`
