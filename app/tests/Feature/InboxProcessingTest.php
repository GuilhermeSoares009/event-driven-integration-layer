<?php

namespace Tests\Feature;

use App\IntegrationLayer\Inbox\Jobs\ProcessInboxEvent;
use App\IntegrationLayer\Inbox\Models\InboxEvent;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Tests\TestCase;

class InboxProcessingTest extends TestCase
{
    use RefreshDatabase;

    public function test_it_marks_event_as_processed(): void
    {
        $event = InboxEvent::query()->create([
            'provider' => 'test',
            'topic' => 'orders',
            'external_event_id' => 'evt_10',
            'payload_hash' => hash('sha256', 'payload'),
            'payload_json' => ['foo' => 'bar'],
            'status' => InboxEvent::STATUS_RECEIVED,
            'attempts' => 0,
            'correlation_id' => 'corr-1',
        ]);

        ProcessInboxEvent::dispatch($event->id);

        $event->refresh();

        $this->assertSame(InboxEvent::STATUS_PROCESSED, $event->status);
        $this->assertNotNull($event->processed_at);
    }
}
