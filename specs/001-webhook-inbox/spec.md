# Feature Specification: Webhook Inbox

**Feature Branch**: `[001-webhook-inbox]`  
**Created**: 2026-02-14  
**Status**: Draft  
**Input**: User description: "Webhook inbox (ingestao + dedupe + inbox base)"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Receber webhook e registrar inbox (Priority: P1)

Como operador do sistema, quero receber webhooks de providers e registrar eventos na inbox com deduplicacao, para garantir processamento confiavel e auditavel.

**Why this priority**: Sem a inbox, nao existe base para idempotencia, retries e audit trail.

**Independent Test**: Enviar um webhook valido e verificar que um registro foi criado na inbox com status `RECEIVED`.

**Acceptance Scenarios**:

1. **Given** um webhook valido com assinatura correta, **When** envio para `POST /webhooks/{provider}/{topic}`, **Then** o sistema cria um registro em `inbox_events` com `status=RECEIVED` e `correlation_id`.
2. **Given** um webhook valido com `external_event_id`, **When** envio o mesmo webhook novamente, **Then** o sistema nao cria um novo registro e retorna sucesso sem duplicar.

---

### User Story 2 - Rejeitar webhook invalido (Priority: P2)

Como operador, quero que webhooks com assinatura invalida sejam rejeitados para proteger a integridade do sistema.

**Why this priority**: Validacao de assinatura e pre-requisito de seguranca.

**Independent Test**: Enviar webhook com assinatura invalida e verificar resposta de erro.

**Acceptance Scenarios**:

1. **Given** um webhook com assinatura invalida, **When** envio para o endpoint, **Then** o sistema responde com erro e nao cria registro na inbox.

---

### User Story 3 - Normalizar payload e registrar hash (Priority: P3)

Como operador, quero que o payload seja normalizado e registrado com hash para suportar dedupe mesmo sem `external_event_id`.

**Why this priority**: Dedupe por hash reduz duplicidade quando providers nao enviam IDs externos.

**Independent Test**: Enviar webhook sem `external_event_id` e verificar que o hash foi persistido.

**Acceptance Scenarios**:

1. **Given** um webhook sem `external_event_id`, **When** envio para o endpoint, **Then** o sistema calcula `payload_hash` e o persiste junto ao payload.

---

### Edge Cases

- O que acontece quando o payload excede o limite de tamanho?
- Como o sistema reage a `provider` ou `topic` desconhecidos?
- Dedupe por hash deve considerar normalizacao de campos nao deterministas?

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: O sistema MUST expor `POST /webhooks/{provider}/{topic}`.
- **FR-002**: O sistema MUST validar assinatura do webhook por provider.
- **FR-003**: O sistema MUST persistir eventos na `inbox_events` com `status=RECEIVED`.
- **FR-004**: O sistema MUST deduplicar por `(provider, external_event_id)` quando presente.
- **FR-005**: O sistema MUST calcular `payload_hash` quando `external_event_id` estiver ausente.
- **FR-006**: O sistema MUST retornar resposta de sucesso sem duplicar quando o evento ja existir.
- **FR-007**: O sistema MUST rejeitar assinatura invalida sem persistir evento.
- **FR-008**: O sistema MUST gerar ou propagar `correlation_id`.

### Key Entities *(include if feature involves data)*

- **InboxEvent**: Evento recebido via webhook, com provider, topic, payload, hash, status, attempts e correlacao.
- **Provider**: Origem do webhook, com regras de assinatura e normalizacao.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% dos webhooks validos sao registrados na inbox com `status=RECEIVED`.
- **SC-002**: Eventos duplicados por `external_event_id` nao geram novos registros.
- **SC-003**: Webhooks com assinatura invalida sao rejeitados sem persistencia.
