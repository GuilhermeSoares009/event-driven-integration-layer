<?php

namespace App\IntegrationLayer\Ingestion\Validators;

class HmacSignatureValidator implements SignatureValidatorInterface
{
    public function __construct(private ProviderSecretResolver $secrets)
    {
    }

    public function isValid(string $provider, string $payload, ?string $signature): bool
    {
        $secret = $this->secrets->resolve($provider);

        if ($secret === null || $signature === null) {
            return false;
        }

        $signature = trim($signature);
        $expected = hash_hmac('sha256', $payload, $secret);

        if (str_starts_with($signature, 'sha256=')) {
            $signature = substr($signature, 7);
        }

        return hash_equals($expected, $signature);
    }
}
