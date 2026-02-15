<?php

namespace App\IntegrationLayer\Outbox\Publishers;

use App\IntegrationLayer\Outbox\Models\OutboxMessage;

class PublisherRouter
{
    public function resolve(OutboxMessage $message): PublisherInterface
    {
        return new NoopPublisher();
    }
}
