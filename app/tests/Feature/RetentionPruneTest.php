<?php

namespace Tests\Feature;

use App\IntegrationLayer\Inbox\Models\InboxEvent;
use App\IntegrationLayer\Outbox\Models\OutboxMessage;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Tests\TestCase;

class RetentionPruneTest extends TestCase
{
    use RefreshDatabase;

    public function test_it_prunes_inbox_events(): void
    {
        $old = InboxEvent::query()->create([
            'provider' => 'test',
            'topic' => 'orders',
            'external_event_id' => 'evt_old',
            'payload_hash' => hash('sha256', 'payload'),
            'payload_json' => ['foo' => 'bar'],
            'status' => InboxEvent::STATUS_RECEIVED,
            'attempts' => 0,
            'correlation_id' => 'corr-old',
            'received_at' => now()->subDays(31),
        ]);

        InboxEvent::query()->create([
            'provider' => 'test',
            'topic' => 'orders',
            'external_event_id' => 'evt_new',
            'payload_hash' => hash('sha256', 'payload2'),
            'payload_json' => ['foo' => 'bar'],
            'status' => InboxEvent::STATUS_RECEIVED,
            'attempts' => 0,
            'correlation_id' => 'corr-new',
            'received_at' => now()->subDays(5),
        ]);

        $this->artisan('inbox:prune --days=30')->assertExitCode(0);

        $this->assertDatabaseMissing('inbox_events', ['id' => $old->id]);
        $this->assertDatabaseCount('inbox_events', 1);
    }

    public function test_it_prunes_outbox_messages(): void
    {
        $old = OutboxMessage::query()->create([
            'type' => 'orders',
            'payload_json' => ['foo' => 'bar'],
            'status' => OutboxMessage::STATUS_PENDING,
            'attempts' => 0,
            'correlation_id' => 'corr-old',
            'created_at' => now()->subDays(31),
        ]);

        OutboxMessage::query()->create([
            'type' => 'orders',
            'payload_json' => ['foo' => 'bar'],
            'status' => OutboxMessage::STATUS_PENDING,
            'attempts' => 0,
            'correlation_id' => 'corr-new',
            'created_at' => now()->subDays(2),
        ]);

        $this->artisan('outbox:prune --days=30')->assertExitCode(0);

        $this->assertDatabaseMissing('outbox_messages', ['id' => $old->id]);
        $this->assertDatabaseCount('outbox_messages', 1);
    }
}
