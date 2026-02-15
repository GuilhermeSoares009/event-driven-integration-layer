# ADR-004 — Outbox para efeitos externos

## Status
Accepted

## Contexto

Efeitos externos devem ser enviados de forma confiavel sem perder consistencia entre estado interno e envio externo.

## Decisao

Persistir mensagens de outbox na mesma transacao do processamento e publicar via worker dedicado.

## Consequencias

- Evita inconsistencias entre estado e publicacao
- Requer worker e retries para outbox
