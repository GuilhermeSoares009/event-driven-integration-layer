# Skill: Code Review (Integration Layer)

## Objetivo

Revisar mudancas com foco em confiabilidade de eventos, idempotencia e operabilidade.

## Checklist especifico

- **Idempotencia**: handlers e efeitos externos sao idempotentes
- **Deduplicacao**: chave correta (provider + external_event_id)
- **Transacoes**: outbox criado na mesma transacao do dominio
- **Locks/Claims**: evita concorrencia duplicada no inbox
- **Retry/Backoff**: limites e jitter configurados
- **DLQ/Replay**: criterios claros e replays seguros
- **Correlacao**: correlation_id propagado ponta a ponta
- **Validacao**: assinatura e schema do webhook verificados
- **Observabilidade**: logs estruturados e metricas basicas
- **Rate limit**: backpressure por provider/tenant quando aplicavel

## Perguntas que devem ser respondidas

- O que garante que o mesmo evento nao gera efeitos duplicados?
- O que acontece quando o handler falha repetidamente?
- Onde fica o audit trail do evento?
- Como reproduzir o fluxo em ambiente local?
