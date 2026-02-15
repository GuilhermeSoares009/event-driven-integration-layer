<?php

namespace App\IntegrationLayer\Inbox\Jobs;

use App\IntegrationLayer\Handlers\HandlerRouter;
use App\IntegrationLayer\Inbox\Models\InboxEvent;
use App\IntegrationLayer\Outbox\Jobs\PublishOutboxMessage;
use App\IntegrationLayer\Outbox\Models\OutboxMessage;
use Illuminate\Bus\Queueable;
use Illuminate\Contracts\Queue\ShouldQueue;
use Illuminate\Foundation\Bus\Dispatchable;
use Illuminate\Queue\InteractsWithQueue;
use Illuminate\Queue\SerializesModels;
use Illuminate\Support\Facades\Log;
use Illuminate\Support\Facades\DB;

class ProcessInboxEvent implements ShouldQueue
{
    use Dispatchable;
    use InteractsWithQueue;
    use Queueable;
    use SerializesModels;

    public function __construct(public int $inboxEventId)
    {
        $this->onQueue('inbox-processing');
    }

    public int $tries = 5;

    public function backoff(): array
    {
        return [30, 120, 300, 900, 1800];
    }

    public function retryUntil(): \DateTimeInterface
    {
        return now()->addHours(6);
    }

    public function handle(HandlerRouter $router): void
    {
        $event = InboxEvent::query()->find($this->inboxEventId);

        if ($event === null) {
            return;
        }

        $claimed = InboxEvent::query()
            ->where('id', $event->id)
            ->where('status', InboxEvent::STATUS_RECEIVED)
            ->update([
                'status' => InboxEvent::STATUS_PROCESSING,
            ]);

        if ($claimed === 0) {
            return;
        }

        $event->refresh();

        try {
            DB::transaction(function () use ($event, $router) {
                $router->resolve($event)->handle($event);

                $outbox = OutboxMessage::query()->create([
                    'type' => $event->topic,
                    'payload_json' => $event->payload_json,
                    'status' => OutboxMessage::STATUS_PENDING,
                    'attempts' => 0,
                    'correlation_id' => $event->correlation_id,
                ]);

                $event->forceFill([
                    'status' => InboxEvent::STATUS_PROCESSED,
                    'processed_at' => now(),
                ])->save();

                PublishOutboxMessage::dispatch($outbox->id);
            });
        } catch (\Throwable $exception) {
            $attempts = $event->attempts + 1;
            $shouldDeadLetter = $attempts >= $this->tries;

            $event->forceFill([
                'status' => InboxEvent::STATUS_FAILED,
                'attempts' => $attempts,
                'last_error_code' => get_class($exception),
                'last_error_message' => $exception->getMessage(),
                'next_retry_at' => $shouldDeadLetter ? null : now()->addSeconds($this->nextRetryDelay($attempts)),
            ])->save();

            if ($shouldDeadLetter) {
                $event->forceFill([
                    'status' => InboxEvent::STATUS_DEAD,
                ])->save();

                Log::warning('Inbox event moved to DLQ', [
                    'inbox_event_id' => $event->id,
                    'provider' => $event->provider,
                    'topic' => $event->topic,
                    'correlation_id' => $event->correlation_id,
                ]);

                return;
            }

            Log::warning('Inbox event processing failed', [
                'inbox_event_id' => $event->id,
                'provider' => $event->provider,
                'topic' => $event->topic,
                'correlation_id' => $event->correlation_id,
                'error' => $exception->getMessage(),
            ]);

            throw $exception;
        }
    }

    private function nextRetryDelay(int $attempt): int
    {
        $delays = $this->backoff();
        $index = min($attempt - 1, count($delays) - 1);
        $base = $delays[$index];

        $jitter = random_int(0, (int) round($base * 0.2));

        return $base + $jitter;
    }
}
