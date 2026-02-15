<?php

namespace App\IntegrationLayer\Ingestion\Middleware;

use App\IntegrationLayer\Ops\Metrics\MetricsLogger;
use Closure;
use Illuminate\Cache\RateLimiter;
use Illuminate\Http\Request;
use Symfony\Component\HttpFoundation\Response;

class ProviderRateLimit
{
    public function __construct(
        private RateLimiter $limiter,
        private MetricsLogger $metrics
    ) {
    }

    public function handle(Request $request, Closure $next): Response
    {
        $provider = $request->route('provider');
        $providerKey = is_string($provider) ? $provider : 'unknown';

        $limit = (int) config('integration-layer.rate_limits.' . $providerKey);
        if ($limit <= 0) {
            $limit = (int) config('integration-layer.rate_limits.default_per_minute', 60);
        }

        $key = 'webhook:' . $providerKey . ':' . $request->ip();

        if ($this->limiter->tooManyAttempts($key, $limit)) {
            $this->metrics->increment('webhook_rate_limited_total', 1, [
                'provider' => $providerKey,
            ]);

            $retryAfter = $this->limiter->availableIn($key);

            return response()->json([
                'error' => 'rate_limited',
                'retry_after' => $retryAfter,
            ], 429)->withHeaders([
                'Retry-After' => $retryAfter,
            ]);
        }

        $this->limiter->hit($key, 60);

        return $next($request);
    }
}
