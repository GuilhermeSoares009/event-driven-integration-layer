<?php

use App\IntegrationLayer\Ops\Controllers\InboxDashboardController;
use Illuminate\Support\Facades\Route;

/*
|--------------------------------------------------------------------------
| Web Routes
|--------------------------------------------------------------------------
|
| Here is where you can register web routes for your application. These
| routes are loaded by the RouteServiceProvider and all of them will
| be assigned to the "web" middleware group. Make something great!
|
*/

Route::get('/', function () {
    return view('welcome');
});

Route::get('/inbox', [InboxDashboardController::class, 'index']);
Route::get('/inbox/{id}', [InboxDashboardController::class, 'show']);
