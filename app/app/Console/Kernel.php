<?php

namespace App\Console;

use Illuminate\Console\Scheduling\Schedule;
use Illuminate\Foundation\Console\Kernel as ConsoleKernel;
use App\IntegrationLayer\Ops\Commands\ReplayInboxEvent;
use App\IntegrationLayer\Ops\Commands\ReplayInboxBatch;
use App\IntegrationLayer\Ops\Commands\PruneInboxEvents;
use App\IntegrationLayer\Ops\Commands\PruneOutboxMessages;

class Kernel extends ConsoleKernel
{
    /**
     * Define the application's command schedule.
     */
    protected function schedule(Schedule $schedule): void
    {
        // $schedule->command('inspire')->hourly();
    }

    /**
     * Register the commands for the application.
     */
    protected function commands(): void
    {
        $this->load(__DIR__.'/Commands');
        $this->load(__DIR__.'/../IntegrationLayer/Ops/Commands');

        require base_path('routes/console.php');
    }

    protected $commands = [
        ReplayInboxEvent::class,
        ReplayInboxBatch::class,
        PruneInboxEvents::class,
        PruneOutboxMessages::class,
    ];
}
