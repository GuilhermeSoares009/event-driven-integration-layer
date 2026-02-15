<?php

namespace App\IntegrationLayer\Ops\Tracing;

use Illuminate\Support\Facades\Log;

class TraceLogger
{
    public function start(string $span, array $context = []): array
    {
        $start = hrtime(true);

        Log::info('trace.start', array_merge([
            'span' => $span,
        ], $context));

        return ['span' => $span, 'start' => $start, 'context' => $context];
    }

    public function end(array $trace, array $context = []): void
    {
        $durationMs = (hrtime(true) - $trace['start']) / 1_000_000;

        Log::info('trace.end', array_merge([
            'span' => $trace['span'],
            'duration_ms' => (int) round($durationMs),
        ], $trace['context'], $context));
    }
}
