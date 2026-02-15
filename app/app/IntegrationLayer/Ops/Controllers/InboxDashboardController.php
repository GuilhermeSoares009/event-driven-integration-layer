<?php

namespace App\IntegrationLayer\Ops\Controllers;

use App\IntegrationLayer\Inbox\Models\InboxEvent;
use Illuminate\Http\Request;
use Illuminate\View\View;

class InboxDashboardController
{
    public function index(Request $request): View
    {
        $query = InboxEvent::query()->orderByDesc('received_at');

        $provider = $request->query('provider');
        if (is_string($provider) && $provider !== '') {
            $query->where('provider', $provider);
        }

        $topic = $request->query('topic');
        if (is_string($topic) && $topic !== '') {
            $query->where('topic', $topic);
        }

        $status = $request->query('status');
        if (is_string($status) && $status !== '') {
            $query->where('status', strtoupper($status));
        }

        $events = $query->paginate(25)->withQueryString();

        return view('inbox.index', [
            'events' => $events,
            'filters' => [
                'provider' => $provider,
                'topic' => $topic,
                'status' => $status,
            ],
        ]);
    }

    public function show(int $id): View
    {
        $event = InboxEvent::query()->findOrFail($id);

        return view('inbox.show', [
            'event' => $event,
        ]);
    }
}
