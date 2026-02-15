# ADR-002 — Deduplicacao por provider + external_event_id

## Status
Accepted

## Contexto

Eventos duplicados sao comuns em webhooks. Providers geralmente enviam um ID externo para dedupe.

## Decisao

Aplicar dedupe por `(provider, external_event_id)` quando disponivel. Quando ausente, usar `payload_hash`.

## Consequencias

- Evita efeitos duplicados
- Requer hashing/normalizacao de payload quando ID externo nao existe
