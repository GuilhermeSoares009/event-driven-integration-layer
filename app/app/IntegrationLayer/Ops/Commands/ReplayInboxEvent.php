<?php

namespace App\IntegrationLayer\Ops\Commands;

use App\IntegrationLayer\Inbox\Jobs\ProcessInboxEvent;
use App\IntegrationLayer\Inbox\Models\InboxEvent;
use Illuminate\Console\Command;

class ReplayInboxEvent extends Command
{
    protected $signature = 'inbox:replay {id}';
    protected $description = 'Replay a single inbox event by id';

    public function handle(): int
    {
        $id = (int) $this->argument('id');
        $event = InboxEvent::query()->find($id);

        if ($event === null) {
            $this->error('Inbox event not found');
            return self::FAILURE;
        }

        if (!in_array($event->status, [InboxEvent::STATUS_FAILED, InboxEvent::STATUS_DEAD], true)) {
            $this->warn('Inbox event not eligible for replay');
            return self::INVALID;
        }

        $event->forceFill([
            'status' => InboxEvent::STATUS_RECEIVED,
            'next_retry_at' => null,
        ])->save();

        ProcessInboxEvent::dispatch($event->id);

        $this->info('Replay queued');

        return self::SUCCESS;
    }
}
