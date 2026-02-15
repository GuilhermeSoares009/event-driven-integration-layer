<?php

namespace Tests\Feature;

use App\IntegrationLayer\Inbox\Models\InboxEvent;
use Illuminate\Foundation\Testing\RefreshDatabase;
use Tests\TestCase;

class InboxDashboardTest extends TestCase
{
    use RefreshDatabase;

    public function test_it_renders_inbox_list(): void
    {
        InboxEvent::query()->create([
            'provider' => 'test',
            'topic' => 'orders',
            'external_event_id' => 'evt_101',
            'payload_hash' => hash('sha256', 'payload'),
            'payload_json' => ['foo' => 'bar'],
            'status' => InboxEvent::STATUS_RECEIVED,
            'attempts' => 0,
            'correlation_id' => 'corr-101',
        ]);

        $response = $this->get('/inbox');

        $response->assertStatus(200);
        $response->assertSee('Inbox Events');
        $response->assertSee('test');
        $response->assertSee('orders');
    }

    public function test_it_renders_inbox_detail(): void
    {
        $event = InboxEvent::query()->create([
            'provider' => 'test',
            'topic' => 'orders',
            'external_event_id' => 'evt_102',
            'payload_hash' => hash('sha256', 'payload'),
            'payload_json' => ['foo' => 'bar'],
            'status' => InboxEvent::STATUS_RECEIVED,
            'attempts' => 0,
            'correlation_id' => 'corr-102',
        ]);

        $response = $this->get('/inbox/' . $event->id);

        $response->assertStatus(200);
        $response->assertSee('Inbox Event');
        $response->assertSee('corr-102');
    }
}
