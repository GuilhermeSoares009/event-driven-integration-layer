<?php

namespace App\IntegrationLayer\Outbox\Jobs;

use App\IntegrationLayer\Outbox\Models\OutboxMessage;
use App\IntegrationLayer\Outbox\Publishers\PublisherRouter;
use Illuminate\Bus\Queueable;
use Illuminate\Contracts\Queue\ShouldQueue;
use Illuminate\Foundation\Bus\Dispatchable;
use Illuminate\Queue\InteractsWithQueue;
use Illuminate\Queue\SerializesModels;
use Illuminate\Support\Facades\Log;

class PublishOutboxMessage implements ShouldQueue
{
    use Dispatchable;
    use InteractsWithQueue;
    use Queueable;
    use SerializesModels;

    public int $tries = 5;

    public function __construct(public int $outboxMessageId)
    {
        $this->onQueue('outbox-publish');
    }

    public function handle(PublisherRouter $router): void
    {
        $message = OutboxMessage::query()->find($this->outboxMessageId);

        if ($message === null) {
            return;
        }

        $claimed = OutboxMessage::query()
            ->where('id', $message->id)
            ->where('status', OutboxMessage::STATUS_PENDING)
            ->update([
                'status' => OutboxMessage::STATUS_SENDING,
            ]);

        if ($claimed === 0) {
            return;
        }

        $message->refresh();

        try {
            $router->resolve($message)->publish($message);

            $message->forceFill([
                'status' => OutboxMessage::STATUS_SENT,
                'sent_at' => now(),
            ])->save();
        } catch (\Throwable $exception) {
            $attempts = $message->attempts + 1;
            $shouldDeadLetter = $attempts >= $this->tries;

            $message->forceFill([
                'status' => OutboxMessage::STATUS_FAILED,
                'attempts' => $attempts,
                'last_error' => $exception->getMessage(),
                'next_retry_at' => $shouldDeadLetter ? null : now()->addSeconds($this->nextRetryDelay($attempts)),
            ])->save();

            if ($shouldDeadLetter) {
                $message->forceFill([
                    'status' => OutboxMessage::STATUS_DEAD,
                ])->save();

                Log::warning('Outbox message moved to DLQ', [
                    'outbox_message_id' => $message->id,
                    'type' => $message->type,
                    'correlation_id' => $message->correlation_id,
                ]);

                return;
            }

            Log::warning('Outbox message publish failed', [
                'outbox_message_id' => $message->id,
                'type' => $message->type,
                'correlation_id' => $message->correlation_id,
                'error' => $exception->getMessage(),
            ]);

            throw $exception;
        }
    }

    public function backoff(): array
    {
        return [30, 120, 300, 900, 1800];
    }

    public function retryUntil(): \DateTimeInterface
    {
        return now()->addHours(6);
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
