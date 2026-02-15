<?php

namespace App\IntegrationLayer\Inbox\Models;

use Illuminate\Database\Eloquent\Model;

class InboxEvent extends Model
{
    public const STATUS_RECEIVED = 'RECEIVED';
    public const STATUS_PROCESSING = 'PROCESSING';
    public const STATUS_PROCESSED = 'PROCESSED';
    public const STATUS_FAILED = 'FAILED';
    public const STATUS_DEAD = 'DEAD';

    protected $table = 'inbox_events';

    public $timestamps = false;

    protected $fillable = [
        'provider',
        'topic',
        'external_event_id',
        'payload_hash',
        'payload_json',
        'status',
        'attempts',
        'next_retry_at',
        'received_at',
        'processed_at',
        'last_error_code',
        'last_error_message',
        'correlation_id',
    ];

    protected $casts = [
        'payload_json' => 'array',
        'attempts' => 'integer',
        'next_retry_at' => 'datetime',
        'received_at' => 'datetime',
        'processed_at' => 'datetime',
    ];
}
