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
];
