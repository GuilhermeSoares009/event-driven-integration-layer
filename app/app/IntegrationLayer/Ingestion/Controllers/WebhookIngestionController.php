<?php

namespace App\IntegrationLayer\Ingestion\Controllers;

use App\IntegrationLayer\Ingestion\Normalizers\PayloadNormalizer;
use App\IntegrationLayer\Ingestion\Validators\SignatureValidatorInterface;
use App\IntegrationLayer\Inbox\Jobs\ProcessInboxEvent;
use App\IntegrationLayer\Inbox\Models\InboxEvent;
use App\IntegrationLayer\Ingestion\CircuitBreaker\ProviderCircuitBreaker;
use App\IntegrationLayer\Ops\Metrics\MetricsLogger;
use App\IntegrationLayer\Ops\Tracing\TraceLogger;
use Illuminate\Database\QueryException;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Illuminate\Support\Str;
use Illuminate\Support\Facades\Log;

class WebhookIngestionController
{
    public function __construct(
        private SignatureValidatorInterface $validator,
        private PayloadNormalizer $normalizer,
        private MetricsLogger $metrics,
        private ProviderCircuitBreaker $circuitBreaker,
        private TraceLogger $tracing
    ) {
    }

    public function ingest(Request $request, string $provider, string $topic): JsonResponse
    {
        $payloadRaw = $request->getContent();
        $signature = $request->header('X-Signature');

        $trace = $this->tracing->start('webhook.ingest', [
            'provider' => $provider,
            'topic' => $topic,
            'correlation_id' => $request->header('X-Correlation-Id'),
        ]);

        if ($this->circuitBreaker->isOpen($provider)) {
            $this->metrics->increment('webhook_circuit_open_total', 1, [
                'provider' => $provider,
            ]);

            $this->tracing->end($trace, ['result' => 'circuit_open']);
            return response()->json(['error' => 'circuit_open'], 503);
        }

        if (!$this->validator->isValid($provider, $payloadRaw, $signature)) {
            Log::warning('webhook.invalid_signature', [
                'provider' => $provider,
                'topic' => $topic,
                'correlation_id' => $request->header('X-Correlation-Id'),
            ]);

            $this->circuitBreaker->recordFailure($provider);
            $this->tracing->end($trace, ['result' => 'invalid_signature']);
            return response()->json(['error' => 'invalid_signature'], 401);
        }

        $payload = json_decode($payloadRaw, true);
        if (!is_array($payload)) {
            return response()->json(['error' => 'invalid_payload'], 400);
        }

        $externalEventId = $payload['external_event_id'] ?? null;
        $externalEventId = is_string($externalEventId) && $externalEventId !== '' ? $externalEventId : null;

        $payloadHash = hash('sha256', $this->normalizer->normalize($payload));
        $correlationId = $request->header('X-Correlation-Id') ?? (string) Str::uuid();

        if ($this->isDuplicate($provider, $externalEventId, $payloadHash)) {
            $this->metrics->increment('inbox_received_total', 1, [
                'deduped' => true,
                'provider' => $provider,
                'topic' => $topic,
            ]);
            $this->tracing->end($trace, ['result' => 'deduped']);
            return response()->json([
                'status' => 'accepted',
                'deduped' => true,
                'correlation_id' => $correlationId,
            ], 202);
        }

        try {
            $event = InboxEvent::create([
                'provider' => $provider,
                'topic' => $topic,
                'external_event_id' => $externalEventId,
                'payload_hash' => $payloadHash,
                'payload_json' => $payload,
                'status' => InboxEvent::STATUS_RECEIVED,
                'attempts' => 0,
                'correlation_id' => $correlationId,
            ]);
        } catch (QueryException $exception) {
            if ($this->isUniqueViolation($exception)) {
                $this->metrics->increment('inbox_received_total', 1, [
                    'deduped' => true,
                    'provider' => $provider,
                    'topic' => $topic,
                ]);
                $this->tracing->end($trace, ['result' => 'deduped']);
                return response()->json([
                    'status' => 'accepted',
                    'deduped' => true,
                    'correlation_id' => $correlationId,
                ], 202);
            }

            throw $exception;
        }

        ProcessInboxEvent::dispatch($event->id);

        $this->metrics->increment('inbox_received_total', 1, [
            'deduped' => false,
            'provider' => $provider,
            'topic' => $topic,
        ]);

        $this->circuitBreaker->recordSuccess($provider);

        Log::info('webhook.received', [
            'inbox_event_id' => $event->id,
            'provider' => $provider,
            'topic' => $topic,
            'correlation_id' => $correlationId,
        ]);

        $this->tracing->end($trace, ['result' => 'accepted']);

        return response()->json([
            'status' => 'accepted',
            'deduped' => false,
            'correlation_id' => $correlationId,
        ], 202);
    }

    private function isDuplicate(string $provider, ?string $externalEventId, string $payloadHash): bool
    {
        if ($externalEventId !== null) {
            return InboxEvent::query()
                ->where('provider', $provider)
                ->where('external_event_id', $externalEventId)
                ->exists();
        }

        return InboxEvent::query()
            ->where('provider', $provider)
            ->whereNull('external_event_id')
            ->where('payload_hash', $payloadHash)
            ->exists();
    }

    private function isUniqueViolation(QueryException $exception): bool
    {
        $sqlState = $exception->errorInfo[0] ?? null;

        if ($sqlState === '23505') {
            return true;
        }

        $message = $exception->getMessage();

        return str_contains($message, 'UNIQUE') || str_contains($message, 'unique');
    }
}
