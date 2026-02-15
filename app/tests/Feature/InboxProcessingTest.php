<?php

namespace Tests\Feature;

use App\IntegrationLayer\Inbox\Jobs\ProcessInboxEvent;
use App\IntegrationLayer\Inbox\Models\InboxEvent;
use App\IntegrationLayer\Handlers\HandlerInterface;
use App\IntegrationLayer\Handlers\HandlerRouter;
use App\IntegrationLayer\Outbox\Models\OutboxMessage;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Tests\TestCase;

class InboxProcessingTest extends TestCase
{
    use RefreshDatabase;

    private bool $shouldThrow = false;

    public function test_it_marks_event_as_processed(): void
    {
        $this->app->bind(HandlerRouter::class, function () {
            return new class extends HandlerRouter {
                public function resolve(InboxEvent $event): HandlerInterface
                {
                    return new class implements HandlerInterface {
                        public function handle(InboxEvent $event): void
                        {
                        }
                    };
                }
            };
        });

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
        $this->assertDatabaseCount('outbox_messages', 1);
    }

    public function test_it_marks_event_as_dead_after_max_attempts(): void
    {
        $this->app->bind(HandlerRouter::class, function () {
            return new class extends HandlerRouter {
                public function resolve(InboxEvent $event): HandlerInterface
                {
                    return new class implements HandlerInterface {
                        public function handle(InboxEvent $event): void
                        {
                            throw new \RuntimeException('boom');
                        }
                    };
                }
            };
        });

        $event = InboxEvent::query()->create([
            'provider' => 'test',
            'topic' => 'orders',
            'external_event_id' => 'evt_11',
            'payload_hash' => hash('sha256', 'payload'),
            'payload_json' => ['foo' => 'bar'],
            'status' => InboxEvent::STATUS_RECEIVED,
            'attempts' => 4,
            'correlation_id' => 'corr-2',
        ]);

        $job = new ProcessInboxEvent($event->id);
        $job->handle(
            $this->app->make(HandlerRouter::class),
            $this->app->make(\App\IntegrationLayer\Ops\Metrics\MetricsLogger::class),
            $this->app->make(\App\IntegrationLayer\Ops\Tracing\TraceLogger::class)
        );

        $event->refresh();

        $this->assertSame(InboxEvent::STATUS_DEAD, $event->status);
        $this->assertSame(5, $event->attempts);
    }
}
