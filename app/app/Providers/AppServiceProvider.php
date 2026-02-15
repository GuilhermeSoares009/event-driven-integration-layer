<?php

namespace App\Providers;

use App\IntegrationLayer\Ingestion\Validators\HmacSignatureValidator;
use App\IntegrationLayer\Ingestion\Validators\SignatureValidatorInterface;
use Illuminate\Support\ServiceProvider;

class AppServiceProvider extends ServiceProvider
{
    /**
     * Register any application services.
     */
    public function register(): void
    {
        $this->app->bind(SignatureValidatorInterface::class, HmacSignatureValidator::class);
    }

    /**
     * Bootstrap any application services.
     */
    public function boot(): void
    {
        //
    }
}
