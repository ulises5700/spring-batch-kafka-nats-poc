Write-Host "🚀 Starting Fintech POC Microservices..." -ForegroundColor Cyan

# 1. Start Fraud Stub Service (Port 8082)
Write-Host "Starting Fraud Service..." -ForegroundColor Yellow
Start-Process "powershell" -ArgumentList "-NoExit", "-Command", "& {HOST.UI.RawUI.WindowTitle = 'Fraud Service'; cd fraud-stub-service; mvn spring-boot:run}"

# 2. Start Settlement Batch Job (Port 8083)
Write-Host "Starting Settlement Service (PostgreSQL Migrations)..." -ForegroundColor Yellow
Start-Process "powershell" -ArgumentList "-NoExit", "-Command", "& {HOST.UI.RawUI.WindowTitle = 'Settlement Service'; cd settlement-batch-job; mvn spring-boot:run}"

# 3. Start Payment Gateway (Port 8081)
Write-Host "Starting Payment Gateway..." -ForegroundColor Yellow
Start-Process "powershell" -ArgumentList "-NoExit", "-Command", "& {HOST.UI.RawUI.WindowTitle = 'Payment Gateway'; cd payment-gateway-service; mvn spring-boot:run}"

Write-Host "✅ Launch commands sent! Please wait ~30 seconds for services to initialize." -ForegroundColor Green
Write-Host "📊 Dashboard will be available at: http://localhost:8081" -ForegroundColor Cyan
