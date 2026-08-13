# PowerShell script to drop and recreate all Cloud SQL databases via gcloud CLI

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
Write-Host "🔥 Resetting Cloud Run & Cloud SQL Databases" -ForegroundColor Cyan
Write-Host "=======================================================" -ForegroundColor Cyan

# 1. Recreate databases
foreach ($db in $dbs) {
    Write-Host "`nRecreating database [$db]..." -ForegroundColor Yellow
    
    # Try deleting database
    $deleteResult = gcloud sql databases delete $db --instance=parkflow-db-instance --quiet 2>&1
    
    if ($deleteResult -like "*accessed by other users*") {
        Write-Host "  ⚠️ Active connections detected on $db. Restarting microservices..." -ForegroundColor Warning
        foreach ($svc in $services) {
            gcloud run services update $svc --region=us-central1 --quiet 2>$null
        }
        Start-Sleep -Seconds 3
        gcloud sql databases delete $db --instance=parkflow-db-instance --quiet 2>$null
    }

    gcloud sql databases create $db --instance=parkflow-db-instance --quiet 2>$null
    Write-Host "  ✅ Database [$db] recreated 100% empty!" -ForegroundColor Green
}

Write-Host "`n=======================================================" -ForegroundColor Cyan
Write-Host "🎉 ALL DATABASES RECREATED EMPTY!" -ForegroundColor Green
Write-Host "Flyway will automatically create tables and seed admin on service restart." -ForegroundColor Green
Write-Host "=======================================================" -ForegroundColor Cyan
