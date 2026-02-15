<?php

namespace App\IntegrationLayer\Handlers;

use App\IntegrationLayer\Inbox\Models\InboxEvent;

class HandlerRouter
{
    public function resolve(InboxEvent $event): HandlerInterface
    {
        return new NoopHandler();
    }
}
