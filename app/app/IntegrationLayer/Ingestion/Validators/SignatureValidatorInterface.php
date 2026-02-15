<?php

namespace App\IntegrationLayer\Ingestion\Validators;

interface SignatureValidatorInterface
{
    public function isValid(string $provider, string $payload, ?string $signature): bool;
}
