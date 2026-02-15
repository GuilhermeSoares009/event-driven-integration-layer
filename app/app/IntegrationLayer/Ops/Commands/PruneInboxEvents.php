<?php

namespace App\IntegrationLayer\Ops\Commands;

use App\IntegrationLayer\Inbox\Models\InboxEvent;
use Illuminate\Console\Command;

class PruneInboxEvents extends Command
{
    protected $signature = 'inbox:prune {--days=}';
    protected $description = 'Prune inbox events older than retention window';

    public function handle(): int
    {
        $days = (int) ($this->option('days') ?: config('integration-layer.retention.inbox_days', 30));
        if ($days <= 0) {
            $this->error('Retention days must be greater than zero');
            return self::FAILURE;
        }

        $cutoff = now()->subDays($days);

        $deleted = InboxEvent::query()
            ->where('received_at', '<', $cutoff)
            ->delete();

        $this->info('Pruned ' . $deleted . ' inbox events');

        return self::SUCCESS;
    }
}
