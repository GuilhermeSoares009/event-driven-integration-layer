<?php

return [
    'providers' => [
        // 'stripe' => [
        //     'secret' => env('WEBHOOK_PROVIDER_STRIPE_SECRET'),
        // ],
    ],
    'rate_limits' => [
        'default_per_minute' => 60,
        // 'stripe' => 120,
    ],
    'retention' => [
        'inbox_days' => 30,
        'outbox_days' => 30,
    ],
    'circuit_breaker' => [
        'failure_threshold' => 5,
        'open_seconds' => 120,
    ],
];
