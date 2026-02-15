<?php

namespace App\IntegrationLayer\Handlers;

use App\IntegrationLayer\Inbox\Models\InboxEvent;

interface HandlerInterface
{
    public function handle(InboxEvent $event): void;
}
