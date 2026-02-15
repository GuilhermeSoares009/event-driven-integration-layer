# Quickstart: Webhook Inbox

## Goal

Validate that webhook ingestion creates inbox records and deduplicates correctly.

## Steps

1. Configure a provider secret in `.env`.
2. Start the app and queue worker.
3. Send a signed webhook to `POST /webhooks/{provider}/{topic}`.
4. Confirm one `inbox_events` row with `status=RECEIVED`.
5. Send the same webhook again.
6. Confirm no new row was created and the response is success.

## Expected Results

- Valid signature -> inbox record created
- Invalid signature -> request rejected and no record created
- Duplicate external_event_id -> no new record
