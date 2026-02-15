<?php

namespace App\IntegrationLayer\Outbox\Publishers;

use App\IntegrationLayer\Outbox\Models\OutboxMessage;

interface PublisherInterface
{
    public function publish(OutboxMessage $message): void;
}
