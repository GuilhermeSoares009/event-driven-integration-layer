# Data Model: Webhook Inbox

## Entities

### InboxEvent

- id (pk)
- provider (string)
- topic (string)
- external_event_id (string, nullable)
- payload_hash (string)
- payload_json (json/text)
- status: RECEIVED | PROCESSING | PROCESSED | FAILED | DEAD
- attempts (int)
- next_retry_at (datetime, nullable)
- received_at, processed_at
- last_error_code, last_error_message
- correlation_id (string)

## Indexes

- UNIQUE(provider, external_event_id) when present
- INDEX(status, received_at)
- INDEX(provider, topic, received_at)
