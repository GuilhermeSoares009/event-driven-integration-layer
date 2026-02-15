<?php

namespace Tests\Feature;

use App\IntegrationLayer\Outbox\Jobs\PublishOutboxMessage;
use App\IntegrationLayer\Outbox\Models\OutboxMessage;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Tests\TestCase;

class OutboxPublishingTest extends TestCase
{
    use RefreshDatabase;

    public function test_it_marks_message_as_sent(): void
    {
        $message = OutboxMessage::query()->create([
            'type' => 'orders',
            'payload_json' => ['foo' => 'bar'],
            'status' => OutboxMessage::STATUS_PENDING,
            'attempts' => 0,
            'correlation_id' => 'corr-1',
        ]);

        PublishOutboxMessage::dispatch($message->id);

        $message->refresh();

        $this->assertSame(OutboxMessage::STATUS_SENT, $message->status);
        $this->assertNotNull($message->sent_at);
    }
}
