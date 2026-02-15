<?php

namespace App\IntegrationLayer\Handlers;

use App\IntegrationLayer\Inbox\Models\InboxEvent;

class NoopHandler implements HandlerInterface
{
    public function handle(InboxEvent $event): void
    {
    }
}
