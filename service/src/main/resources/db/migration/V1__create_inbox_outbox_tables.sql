CREATE TABLE IF NOT EXISTS inbox_events (
    id BIGSERIAL PRIMARY KEY,
    provider VARCHAR(100) NOT NULL,
    topic VARCHAR(100) NOT NULL,
    external_event_id VARCHAR(200),
    payload_hash VARCHAR(64) NOT NULL,
    payload_json JSONB NOT NULL,
    status VARCHAR(32) NOT NULL,
    attempts INTEGER NOT NULL DEFAULT 0,
    next_retry_at TIMESTAMPTZ,
    received_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    processed_at TIMESTAMPTZ,
    last_error_code VARCHAR(255),
    last_error_message TEXT,
    correlation_id VARCHAR(64) NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS inbox_events_provider_external_event_id_ux
    ON inbox_events (provider, external_event_id)
    WHERE external_event_id IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS inbox_events_provider_payload_hash_ux
    ON inbox_events (provider, payload_hash)
    WHERE external_event_id IS NULL;

CREATE INDEX IF NOT EXISTS inbox_events_status_received_at_idx
    ON inbox_events (status, received_at);

CREATE INDEX IF NOT EXISTS inbox_events_provider_topic_received_at_idx
    ON inbox_events (provider, topic, received_at);

CREATE TABLE IF NOT EXISTS outbox_messages (
    id BIGSERIAL PRIMARY KEY,
    type VARCHAR(100) NOT NULL,
    payload_json JSONB NOT NULL,
    status VARCHAR(32) NOT NULL,
    attempts INTEGER NOT NULL DEFAULT 0,
    next_retry_at TIMESTAMPTZ,
    last_error TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    sent_at TIMESTAMPTZ,
    correlation_id VARCHAR(64) NOT NULL
);

CREATE INDEX IF NOT EXISTS outbox_messages_status_created_at_idx
    ON outbox_messages (status, created_at);

CREATE INDEX IF NOT EXISTS outbox_messages_type_created_at_idx
    ON outbox_messages (type, created_at);
