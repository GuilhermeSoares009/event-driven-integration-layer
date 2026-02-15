# ADR-005 — DLQ + Replay com guardrails

## Status
Accepted

## Contexto

Falhas persistentes precisam de isolamento e reprocessamento controlado.

## Decisao

Eventos que excedem tentativas vao para DEAD (DLQ) e podem ser reprocessados manualmente com comandos.

## Consequencias

- Operacao controlada de falhas
- Necessita comandos e politicas de replay
