# ADR-003 — Handlers idempotentes

## Status
Accepted

## Contexto

Falhas e retries sao inevitaveis. Processamento pode ser executado mais de uma vez.

## Decisao

Handlers devem ser idempotentes e seguros para reexecucao.

## Consequencias

- Reduz risco de efeitos duplicados
- Pode exigir checagens adicionais no dominio
