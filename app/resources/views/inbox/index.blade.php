<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="utf-8">
    <title>Inbox Events</title>
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <style>
        body { font-family: system-ui, -apple-system, Segoe UI, sans-serif; margin: 24px; color: #111; }
        h1 { font-size: 22px; margin-bottom: 16px; }
        form { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 12px; margin-bottom: 16px; }
        label { display: block; font-size: 12px; color: #555; margin-bottom: 4px; }
        input { width: 100%; padding: 8px; border: 1px solid #ddd; border-radius: 6px; }
        button { padding: 8px 12px; border: 1px solid #111; background: #111; color: #fff; border-radius: 6px; cursor: pointer; }
        table { width: 100%; border-collapse: collapse; }
        th, td { text-align: left; padding: 8px; border-bottom: 1px solid #eee; font-size: 14px; }
        th { font-size: 12px; text-transform: uppercase; letter-spacing: 0.04em; color: #555; }
        .muted { color: #777; font-size: 12px; }
        .row { display: flex; align-items: center; gap: 8px; }
        .status { padding: 2px 6px; border-radius: 999px; font-size: 12px; background: #f2f2f2; }
        .actions { display: flex; gap: 8px; }
        a { color: #0f62fe; text-decoration: none; }
        .pagination { margin-top: 16px; font-size: 14px; }
        @media (max-width: 720px) {
            form { grid-template-columns: 1fr 1fr; }
            table { display: block; overflow-x: auto; }
        }
    </style>
</head>
<body>
    <h1>Inbox Events</h1>

    <form method="get" action="{{ url('/inbox') }}">
        <div>
            <label for="provider">Provider</label>
            <input id="provider" name="provider" value="{{ $filters['provider'] ?? '' }}" placeholder="stripe">
        </div>
        <div>
            <label for="topic">Topic</label>
            <input id="topic" name="topic" value="{{ $filters['topic'] ?? '' }}" placeholder="orders">
        </div>
        <div>
            <label for="status">Status</label>
            <input id="status" name="status" value="{{ $filters['status'] ?? '' }}" placeholder="RECEIVED">
        </div>
        <div class="actions">
            <div style="align-self: end;">
                <button type="submit">Filter</button>
            </div>
            <div style="align-self: end;">
                <a href="{{ url('/inbox') }}" class="muted">Reset</a>
            </div>
        </div>
    </form>

    <table>
        <thead>
            <tr>
                <th>ID</th>
                <th>Provider</th>
                <th>Topic</th>
                <th>Status</th>
                <th>Attempts</th>
                <th>Received</th>
                <th></th>
            </tr>
        </thead>
        <tbody>
            @forelse ($events as $event)
                <tr>
                    <td>{{ $event->id }}</td>
                    <td>{{ $event->provider }}</td>
                    <td>{{ $event->topic }}</td>
                    <td><span class="status">{{ $event->status }}</span></td>
                    <td>{{ $event->attempts }}</td>
                    <td class="muted">{{ optional($event->received_at)->toDateTimeString() }}</td>
                    <td><a href="{{ url('/inbox/' . $event->id) }}">Details</a></td>
                </tr>
            @empty
                <tr>
                    <td colspan="7" class="muted">No events found.</td>
                </tr>
            @endforelse
        </tbody>
    </table>

    <div class="pagination">
        {{ $events->links() }}
    </div>
</body>
</html>
