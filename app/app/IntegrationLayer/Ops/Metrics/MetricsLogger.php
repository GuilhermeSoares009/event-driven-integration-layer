<?php

namespace App\IntegrationLayer\Ops\Metrics;

use Illuminate\Support\Facades\Log;

class MetricsLogger
{
    public function increment(string $metric, int $value = 1, array $context = []): void
    {
        Log::info('metric', array_merge([
            'metric' => $metric,
            'value' => $value,
        ], $context));
    }
}
