<?php

namespace Tests\Feature;

use Illuminate\Foundation\Testing\RefreshDatabase;
use Illuminate\Testing\TestResponse;
use Tests\TestCase;

class WebhookIngestionTest extends TestCase
{
    use RefreshDatabase;

    public function test_it_persists_webhook_and_dedupes(): void
    {
        config()->set('integration-layer.providers.test.secret', 'secret');

        $payload = [
            'external_event_id' => 'evt_1',
            'foo' => 'bar',
        ];

        $first = $this->postWebhook($payload, 'secret');
        $first->assertStatus(202);

        $this->assertDatabaseCount('inbox_events', 1);
        $this->assertDatabaseHas('inbox_events', [
            'provider' => 'test',
            'topic' => 'orders',
            'external_event_id' => 'evt_1',
        ]);

        $second = $this->postWebhook($payload, 'secret');
        $second->assertStatus(202);

        $this->assertDatabaseCount('inbox_events', 1);
    }

    public function test_it_rejects_invalid_signature(): void
    {
        config()->set('integration-layer.providers.test.secret', 'secret');

        $payload = [
            'external_event_id' => 'evt_2',
            'foo' => 'bar',
        ];

        $response = $this->postWebhook($payload, 'wrong-secret');
        $response->assertStatus(401);

        $this->assertDatabaseCount('inbox_events', 0);
    }

    private function postWebhook(array $payload, string $secret): TestResponse
    {
        $body = json_encode($payload, JSON_UNESCAPED_SLASHES);
        $signature = hash_hmac('sha256', $body, $secret);

        $response = $this->call(
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
        );

        return TestResponse::fromBaseResponse($response);
    }
}
