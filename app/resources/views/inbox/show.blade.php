<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="utf-8">
    <title>Inbox Event {{ $event->id }}</title>
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <style>
        body { font-family: system-ui, -apple-system, Segoe UI, sans-serif; margin: 24px; color: #111; }
        h1 { font-size: 22px; margin-bottom: 16px; }
        .meta { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 12px; margin-bottom: 16px; }
        .card { border: 1px solid #eee; border-radius: 8px; padding: 12px; }
        .label { font-size: 12px; color: #555; margin-bottom: 4px; }
        .value { font-size: 14px; }
        pre { background: #f8f8f8; padding: 12px; border-radius: 8px; overflow: auto; }
        a { color: #0f62fe; text-decoration: none; }
        @media (max-width: 720px) {
            .meta { grid-template-columns: 1fr; }
        }
    </style>
</head>
<body>
    <a href="{{ url('/inbox') }}">Back to inbox</a>
    <h1>Inbox Event #{{ $event->id }}</h1>

    <div class="meta">
        <div class="card">
            <div class="label">Provider</div>
            <div class="value">{{ $event->provider }}</div>
        </div>
        <div class="card">
            <div class="label">Topic</div>
            <div class="value">{{ $event->topic }}</div>
        </div>
        <div class="card">
            <div class="label">Status</div>
            <div class="value">{{ $event->status }}</div>
        </div>
        <div class="card">
            <div class="label">Attempts</div>
            <div class="value">{{ $event->attempts }}</div>
        </div>
        <div class="card">
            <div class="label">Correlation ID</div>
            <div class="value">{{ $event->correlation_id }}</div>
        </div>
        <div class="card">
            <div class="label">Received At</div>
            <div class="value">{{ optional($event->received_at)->toDateTimeString() }}</div>
        </div>
        <div class="card">
            <div class="label">Processed At</div>
            <div class="value">{{ optional($event->processed_at)->toDateTimeString() }}</div>
        </div>
        <div class="card">
            <div class="label">Last Error</div>
            <div class="value">{{ $event->last_error_message ?? '-' }}</div>
        </div>
    </div>

    <div class="card">
        <div class="label">Payload</div>
        <pre>{{ json_encode($event->payload_json, JSON_PRETTY_PRINT | JSON_UNESCAPED_SLASHES) }}</pre>
    </div>
</body>
</html>
