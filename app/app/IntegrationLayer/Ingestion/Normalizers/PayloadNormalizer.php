<?php

namespace App\IntegrationLayer\Ingestion\Normalizers;

class PayloadNormalizer
{
    public function normalize(array $payload): string
    {
        $normalized = $this->normalizeValue($payload);

        return json_encode($normalized, JSON_UNESCAPED_SLASHES);
    }

    private function normalizeValue(mixed $value): mixed
    {
        if (!is_array($value)) {
            return $value;
        }

        if (array_is_list($value)) {
            return array_map(fn ($item) => $this->normalizeValue($item), $value);
        }

        ksort($value);

        $normalized = [];
        foreach ($value as $key => $item) {
            $normalized[$key] = $this->normalizeValue($item);
        }

        return $normalized;
    }
}
