<?php

namespace App\IntegrationLayer\Ops\Commands;

use App\IntegrationLayer\Outbox\Models\OutboxMessage;
use Illuminate\Console\Command;

class PruneOutboxMessages extends Command
{
    protected $signature = 'outbox:prune {--days=}';
    protected $description = 'Prune outbox messages older than retention window';

    public function handle(): int
    {
        $days = (int) ($this->option('days') ?: config('integration-layer.retention.outbox_days', 30));
        if ($days <= 0) {
            $this->error('Retention days must be greater than zero');
            return self::FAILURE;
        }

        $cutoff = now()->subDays($days);

        $deleted = OutboxMessage::query()
            ->where('created_at', '<', $cutoff)
            ->delete();

        $this->info('Pruned ' . $deleted . ' outbox messages');

        return self::SUCCESS;
    }
}
