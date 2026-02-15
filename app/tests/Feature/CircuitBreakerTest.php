<?php

namespace Tests\Feature;

use Illuminate\Foundation\Testing\RefreshDatabase;
use Tests\TestCase;

class CircuitBreakerTest extends TestCase
{
    use RefreshDatabase;

    public function test_it_opens_circuit_after_failures(): void
    {
        config()->set('integration-layer.circuit_breaker.failure_threshold', 2);
        config()->set('integration-layer.circuit_breaker.open_seconds', 120);
        config()->set('integration-layer.providers.test.secret', 'secret');

        $payload = [
            'external_event_id' => 'evt_cb',
            'foo' => 'bar',
        ];

        $body = json_encode($payload, JSON_UNESCAPED_SLASHES);

        $this->call(
            'POST',
            '/api/webhooks/test/orders',
            [],
            [],
            [],
            [
                'HTTP_X_SIGNATURE' => 'bad',
                'CONTENT_TYPE' => 'application/json',
            ],
            $body
        )->assertStatus(401);

        $this->call(
            'POST',
            '/api/webhooks/test/orders',
            [],
            [],
            [],
            [
                'HTTP_X_SIGNATURE' => 'bad',
                'CONTENT_TYPE' => 'application/json',
            ],
            $body
        )->assertStatus(401);

        $this->call(
            'POST',
            '/api/webhooks/test/orders',
            [],
            [],
            [],
            [
                'HTTP_X_SIGNATURE' => 'bad',
                'CONTENT_TYPE' => 'application/json',
            ],
            $body
        )->assertStatus(503);
    }
}
