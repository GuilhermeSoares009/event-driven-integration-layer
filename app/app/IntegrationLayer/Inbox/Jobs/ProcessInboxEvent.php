<?php

namespace App\IntegrationLayer\Inbox\Jobs;

use App\IntegrationLayer\Handlers\HandlerRouter;
use App\IntegrationLayer\Inbox\Models\InboxEvent;
use Illuminate\Bus\Queueable;
use Illuminate\Contracts\Queue\ShouldQueue;
use Illuminate\Foundation\Bus\Dispatchable;
use Illuminate\Queue\InteractsWithQueue;
use Illuminate\Queue\SerializesModels;
use Illuminate\Support\Facades\Log;

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
            $router->resolve($event)->handle($event);

            $event->forceFill([
                'status' => InboxEvent::STATUS_PROCESSED,
                'processed_at' => now(),
            ])->save();
        } catch (\Throwable $exception) {
            $event->forceFill([
                'status' => InboxEvent::STATUS_FAILED,
                'attempts' => $event->attempts + 1,
                'last_error_code' => get_class($exception),
                'last_error_message' => $exception->getMessage(),
            ])->save();

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
}
