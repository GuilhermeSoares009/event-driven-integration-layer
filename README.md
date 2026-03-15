# Event-Driven Integration Layer

Event-driven integration layer focused on webhook ingestion, idempotent processing, and reliable external effects through the outbox pattern.

## Current Status

- Primary backend target: Java 21 + Spring Boot 3
-  / implementation remains in `app/` during transition
- Local platform is fully dockerized for onboarding and reproducibility
- release  delivers infrastructure and health endpoints; webhook flow transition is next

## Target Architecture

1. `POST /webhooks/{provider}/{topic}` receives provider events
2. Signature is validated and payload is normalized
3. Event is persisted in `inbox_events` with deduplication guarantees
4. Async processing updates state and creates `outbox_messages`
5. Outbox publisher performs reliable external publishing with retries and DLQ behavior

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

## Service Environment Variables

- `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`
- `REDIS_HOST`, `REDIS_PORT`
- `KAFKA_BOOTSTRAP_SERVERS`
- `SPRING_PROFILES_ACTIVE`

Defaults are provided for local Docker usage in `docker-compose.yml`.

You can also copy `service/.env.example` and adapt values for local execution.

## transition Notes

- The Java service is the target runtime.
- The  app remains available as functional reference while transition releases are completed.
- release publishing follows clean, standardized commits in English.
