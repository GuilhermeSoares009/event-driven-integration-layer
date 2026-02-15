<?php

namespace App\IntegrationLayer\Ingestion\Validators;

use Illuminate\Support\Str;

class ProviderSecretResolver
{
    public function resolve(string $provider): ?string
    {
        $configured = config('integration-layer.providers.' . $provider . '.secret');

        if (is_string($configured) && $configured !== '') {
            return $configured;
        }

        $envKey = 'WEBHOOK_PROVIDER_' . Str::upper($provider) . '_SECRET';
        $envValue = env($envKey);

        return is_string($envValue) && $envValue !== '' ? $envValue : null;
    }
}
