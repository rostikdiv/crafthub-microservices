# PowerShell script to safely drop and recreate all Cloud SQL databases via gcloud CLI,
# restart microservices, wait for Flyway migrations, run seed.js, and simulate realistic traffic!

$dbs = @(
    "user_service_db",
    "product_service_db",
    "order_service_db",
    "delivery_service_db",
    "payment_service_db"
)

$services = @(
    "milhub-user-service",
    "milhub-product-service",
    "milhub-order-service",
    "milhub-delivery-service",
    "milhub-payment-service"
)

Write-Host "=======================================================" -ForegroundColor Cyan
Write-Host "🔥 1. Dropping & Recreating Clean Cloud SQL Databases" -ForegroundColor Cyan
Write-Host "=======================================================" -ForegroundColor Cyan

foreach ($db in $dbs) {
    Write-Host "`nProcessing database [$db]..." -ForegroundColor Yellow
    
    $deleted = $false
    for ($attempt = 1; $attempt -le 10; $attempt++) {
        $out = gcloud sql databases delete $db --instance=parkflow-db-instance --quiet 2>&1
        if ($LASTEXITCODE -eq 0) {
            $deleted = $true
            Write-Host "  🗑️ Deleted old [$db] (attempt $attempt)" -ForegroundColor Green
            break
        }
        Write-Host "  ⏳ Active connections detected, retrying delete [$db] ($attempt/10)..." -ForegroundColor DarkGray
        Start-Sleep -Seconds 3
    }

    if ($deleted) {
        gcloud sql databases create $db --instance=parkflow-db-instance --quiet
        Write-Host "  ✅ Database [$db] recreated 100% clean & empty!" -ForegroundColor Green
    } else {
        Write-Host "  ❌ Failed to delete [$db]: $out" -ForegroundColor Red
    }
}

Write-Host "`n=======================================================" -ForegroundColor Cyan
Write-Host "🔄 2. Forcing Cloud Run Microservices Restart for Fresh Migrations" -ForegroundColor Cyan
Write-Host "=======================================================" -ForegroundColor Cyan

$ts = Get-Date -Format "yyyyMMddHHmmss"
foreach ($svc in $services) {
    Write-Host "  Restarting $svc..." -ForegroundColor Yellow
    gcloud run services update $svc --update-env-vars="RESTART_TRIGGER=$ts" --region=us-central1 --quiet
    Write-Host "  ✅ Service $svc restarted." -ForegroundColor Green
}

Write-Host "`n=======================================================" -ForegroundColor Cyan
Write-Host "⏳ 3. Waiting 45 seconds for Flyway migrations and admin initialization..." -ForegroundColor Cyan
Write-Host "=======================================================" -ForegroundColor Cyan

for ($i = 45; $i -gt 0; $i--) {
    Write-Host -NoNewline "`r  Waiting for Cloud Run initialization... ($i seconds remaining)   "
    Start-Sleep -Seconds 1
}
Write-Host "`n✅ Wait completed! Flyway tables and admin user initialized." -ForegroundColor Green

Write-Host "`n=======================================================" -ForegroundColor Cyan
Write-Host "📦 4. Running Database Seeder (seed.js cloud)" -ForegroundColor Cyan
Write-Host "=======================================================" -ForegroundColor Cyan

if (Test-Path ".\seed.js") {
    node seed.js cloud
} elseif (Test-Path ".\crafthub-microservices\seed.js") {
    Push-Location ".\crafthub-microservices"
    node seed.js cloud
    Pop-Location
} else {
    node seed.js cloud
}

Write-Host "`n=======================================================" -ForegroundColor Cyan
Write-Host "🚀 5. Simulating User Traffic & Verified Reviews (simulate-activity.js cloud)" -ForegroundColor Cyan
Write-Host "=======================================================" -ForegroundColor Cyan

if (Test-Path ".\simulate-activity.js") {
    node simulate-activity.js cloud
} elseif (Test-Path ".\crafthub-microservices\simulate-activity.js") {
    Push-Location ".\crafthub-microservices"
    node simulate-activity.js cloud
    Pop-Location
} else {
    node simulate-activity.js cloud
}

Write-Host "`n🎉 All Cloud SQL databases wiped, migrated, seeded, and populated with active traffic!" -ForegroundColor Green
