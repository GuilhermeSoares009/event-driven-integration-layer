# Implementation Plan: Webhook Inbox

**Branch**: `001-webhook-inbox` | **Date**: 2026-02-14 | **Spec**: `specs/001-webhook-inbox/spec.md`
**Input**: Feature specification from `specs/001-webhook-inbox/spec.md`

## Summary

Implement webhook ingestion with signature validation, inbox persistence, and deduplication based on `(provider, external_event_id)` or `payload_hash` when the external ID is missing. The endpoint must be fast and avoid heavy processing.

## Technical Context

**Language/Version**: PHP 8.x (Laravel)
**Primary Dependencies**: Laravel framework, Redis queue driver
**Storage**: PostgreSQL
**Testing**: phpunit via `php artisan test`
**Target Platform**: Server backend
**Project Type**: web
**Performance Goals**: accept webhook requests quickly (async processing)
**Constraints**: no heavy work in request; validate signature per provider
**Scale/Scope**: small to mid workloads, at-least-once delivery

## Constitution Check

No constitution file in this repo. Proceed with project rules in `AGENTS.md` and Spec Kit templates.

## Project Structure

### Documentation (this feature)

```text
specs/001-webhook-inbox/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
└── contracts/
    └── webhook-ingestion.md
```

### Source Code (repository root)

```text
app/IntegrationLayer/
├── Ingestion/
│   ├── Controllers/
│   ├── Validators/
│   └── Normalizers/
└── Inbox/
    ├── Models/
    └── Repositories/
```

**Structure Decision**: Use a single Laravel app with `app/IntegrationLayer` modules to keep changes scoped and discoverable.

## Phases

### Phase 0 - Data and routing
- Define `inbox_events` schema (migration spec only)
- Define webhook route `POST /webhooks/{provider}/{topic}`

### Phase 1 - Validation and persistence
- Provider signature validation strategy and interface
- Normalize payload for hashing
- Persist `InboxEvent` with `status=RECEIVED`
- Dedupe logic by `(provider, external_event_id)` or `payload_hash`

### Phase 2 - Response semantics
- Return success response for duplicate events
- Return error for invalid signatures without persistence
- Always return fast response (202 or 200 based on contract)

## Complexity Tracking

No violations tracked.
