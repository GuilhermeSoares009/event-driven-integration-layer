<?php

namespace App\IntegrationLayer\Outbox\Models;

use Illuminate\Database\Eloquent\Model;

class OutboxMessage extends Model
{
    public const STATUS_PENDING = 'PENDING';
    public const STATUS_SENDING = 'SENDING';
    public const STATUS_SENT = 'SENT';
    public const STATUS_FAILED = 'FAILED';
    public const STATUS_DEAD = 'DEAD';

    protected $table = 'outbox_messages';

    public $timestamps = false;

    protected $fillable = [
        'type',
        'payload_json',
        'status',
        'attempts',
        'next_retry_at',
        'last_error',
        'created_at',
        'sent_at',
        'correlation_id',
    ];

    protected $casts = [
        'payload_json' => 'array',
        'attempts' => 'integer',
        'next_retry_at' => 'datetime',
        'created_at' => 'datetime',
        'sent_at' => 'datetime',
    ];
}
