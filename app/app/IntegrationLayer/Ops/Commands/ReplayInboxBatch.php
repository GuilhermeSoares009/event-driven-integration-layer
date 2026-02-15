<?php

namespace App\IntegrationLayer\Ops\Commands;

use App\IntegrationLayer\Inbox\Jobs\ProcessInboxEvent;
use App\IntegrationLayer\Inbox\Models\InboxEvent;
use Illuminate\Console\Command;

class ReplayInboxBatch extends Command
{
    protected $signature = 'inbox:replay-batch {--provider=} {--topic=} {--limit=100}';
    protected $description = 'Replay a batch of inbox events from FAILED or DEAD';

    public function handle(): int
    {
        $query = InboxEvent::query()
            ->whereIn('status', [InboxEvent::STATUS_FAILED, InboxEvent::STATUS_DEAD]);

        $provider = $this->option('provider');
        if (is_string($provider) && $provider !== '') {
            $query->where('provider', $provider);
        }

        $topic = $this->option('topic');
        if (is_string($topic) && $topic !== '') {
            $query->where('topic', $topic);
        }

        $limit = (int) $this->option('limit');
        if ($limit <= 0) {
            $limit = 100;
        }

        $events = $query->orderBy('received_at')->limit($limit)->get();

        if ($events->isEmpty()) {
            $this->info('No events to replay');
            return self::SUCCESS;
        }

        foreach ($events as $event) {
            $event->forceFill([
                'status' => InboxEvent::STATUS_RECEIVED,
                'next_retry_at' => null,
            ])->save();

            ProcessInboxEvent::dispatch($event->id);
        }

        $this->info('Replay queued for ' . $events->count() . ' events');

        return self::SUCCESS;
    }
}
