<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        Schema::create('inbox_events', function (Blueprint $table) {
            $table->bigIncrements('id');
            $table->string('provider');
            $table->string('topic');
            $table->string('external_event_id')->nullable();
            $table->string('payload_hash');
            $table->json('payload_json');
            $table->string('status');
            $table->unsignedInteger('attempts')->default(0);
            $table->timestamp('next_retry_at')->nullable();
            $table->timestamp('received_at')->useCurrent();
            $table->timestamp('processed_at')->nullable();
            $table->string('last_error_code')->nullable();
            $table->text('last_error_message')->nullable();
            $table->string('correlation_id');

            $table->unique(['provider', 'external_event_id']);
            $table->index(['status', 'received_at']);
            $table->index(['provider', 'topic', 'received_at']);
        });

        if (DB::getDriverName() === 'pgsql') {
            DB::statement(
                'CREATE UNIQUE INDEX inbox_events_provider_payload_hash_unique '
                . 'ON inbox_events (provider, payload_hash) '
                . 'WHERE external_event_id IS NULL'
            );
        }
    }

    public function down(): void
    {
        if (DB::getDriverName() === 'pgsql') {
            DB::statement('DROP INDEX IF EXISTS inbox_events_provider_payload_hash_unique');
        }

        Schema::dropIfExists('inbox_events');
    }
};
