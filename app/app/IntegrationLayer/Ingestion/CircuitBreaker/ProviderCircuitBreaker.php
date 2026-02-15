<?php

namespace App\IntegrationLayer\Ingestion\CircuitBreaker;

use Illuminate\Cache\CacheManager;

class ProviderCircuitBreaker
{
    public function __construct(private CacheManager $cache)
    {
    }

    public function isOpen(string $provider): bool
    {
        return $this->cache->has($this->openKey($provider));
    }

    public function recordFailure(string $provider): void
    {
        $threshold = (int) config('integration-layer.circuit_breaker.failure_threshold', 5);
        $openSeconds = (int) config('integration-layer.circuit_breaker.open_seconds', 120);

        $count = (int) $this->cache->increment($this->failureKey($provider));
        $this->cache->put($this->failureKey($provider), $count, $openSeconds);

        if ($count >= $threshold) {
            $this->cache->put($this->openKey($provider), true, $openSeconds);
        }
    }

    public function recordSuccess(string $provider): void
    {
        $this->cache->forget($this->failureKey($provider));
        $this->cache->forget($this->openKey($provider));
    }

    private function failureKey(string $provider): string
    {
        return 'cb:failures:' . $provider;
    }

    private function openKey(string $provider): string
    {
        return 'cb:open:' . $provider;
    }
}
