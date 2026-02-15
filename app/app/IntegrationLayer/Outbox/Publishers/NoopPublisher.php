<?php

namespace App\IntegrationLayer\Outbox\Publishers;

use App\IntegrationLayer\Outbox\Models\OutboxMessage;

class NoopPublisher implements PublisherInterface
{
    public function publish(OutboxMessage $message): void
    {
    }
}
