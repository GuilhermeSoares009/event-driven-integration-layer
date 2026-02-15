<?php

namespace Tests\Feature;

use Illuminate\Foundation\Testing\RefreshDatabase;
use Tests\TestCase;

class RateLimitTest extends TestCase
{
    use RefreshDatabase;

    public function test_it_throttles_by_provider(): void
    {
        config()->set('integration-layer.rate_limits.default_per_minute', 1);
        config()->set('integration-layer.providers.test.secret', 'secret');

        $payload = [
            'external_event_id' => 'evt_rate',
            'foo' => 'bar',
        ];

        $body = json_encode($payload, JSON_UNESCAPED_SLASHES);
        $signature = hash_hmac('sha256', $body, 'secret');

        $this->call(
            'POST',
            '/api/webhooks/test/orders',
            [],
            [],
            [],
            [
                'HTTP_X_SIGNATURE' => $signature,
                'CONTENT_TYPE' => 'application/json',
            ],
            $body
        )->assertStatus(202);

        $this->call(
            'POST',
            '/api/webhooks/test/orders',
            [],
            [],
            [],
            [
                'HTTP_X_SIGNATURE' => $signature,
                'CONTENT_TYPE' => 'application/json',
            ],
            $body
        )->assertStatus(429);
    }
}
