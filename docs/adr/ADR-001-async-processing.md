# ADR-001 — Processamento Assincrono (202 Accepted)

## Status
Accepted

## Contexto

Webhooks sao at-least-once e podem levar tempo para processamento. Processamento sincrono aumenta risco de timeout e retries do provider.

## Decisao

Responder o webhook rapidamente (202) e processar em background via fila.

## Consequencias

- Menor latencia no request
- Necessita observabilidade e retries confiaveis
- Requer design idempotente
