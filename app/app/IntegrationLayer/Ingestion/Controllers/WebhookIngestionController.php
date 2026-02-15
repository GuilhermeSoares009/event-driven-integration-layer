<?php

namespace App\IntegrationLayer\Ingestion\Controllers;

use App\IntegrationLayer\Ingestion\Normalizers\PayloadNormalizer;
use App\IntegrationLayer\Ingestion\Validators\SignatureValidatorInterface;
use App\IntegrationLayer\Inbox\Models\InboxEvent;
use Illuminate\Database\QueryException;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Illuminate\Support\Str;

class WebhookIngestionController
{
    public function __construct(
        private SignatureValidatorInterface $validator,
        private PayloadNormalizer $normalizer
    ) {
    }

    public function ingest(Request $request, string $provider, string $topic): JsonResponse
    {
        $payloadRaw = $request->getContent();
        $signature = $request->header('X-Signature');

        if (!$this->validator->isValid($provider, $payloadRaw, $signature)) {
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
                return response()->json([
                    'status' => 'accepted',
                    'deduped' => true,
                    'correlation_id' => $correlationId,
                ], 202);
            }

            throw $exception;
        }

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
