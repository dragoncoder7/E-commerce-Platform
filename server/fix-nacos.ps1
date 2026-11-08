# 快速修复 Nacos 启动问题

Write-Host "========================================" -ForegroundColor Green
Write-Host "  Nacos 快速修复工具" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host ""

# 1. 停止所有服务
Write-Host "[1/5] 停止所有服务..." -ForegroundColor Yellow
docker-compose down
Write-Host ""

# 2. 启动 MySQL
Write-Host "[2/5] 启动 MySQL..." -ForegroundColor Yellow
docker-compose up -d mysql
Write-Host ""

# 3. 等待 MySQL 就绪
Write-Host "[3/5] 等待 MySQL 就绪..." -ForegroundColor Yellow
$maxWait = 60
$waited = 0
while ($waited -lt $maxWait) {
    $health = docker inspect ecommerce-mysql --format='{{.State.Health.Status}}' 2>$null
    if ($health -eq "healthy") {
        Write-Host "✓ MySQL 已就绪" -ForegroundColor Green
        break
    }
    Write-Host "   等待中... ($waited/$maxWait 秒)" -ForegroundColor Gray
    Start-Sleep -Seconds 5
    $waited += 5
}

if ($waited -ge $maxWait) {
    Write-Host "✗ MySQL 启动超时，请检查日志: docker-compose logs mysql" -ForegroundColor Red
    exit 1
}
Write-Host ""

# 4. 验证数据库初始化
Write-Host "[4/5] 验证数据库初始化..." -ForegroundColor Yellow
$databases = docker exec ecommerce-mysql mysql -uroot -proot123456 -e "SHOW DATABASES;" 2>&1

if ($databases -match "nacos") {
    Write-Host "✓ nacos 数据库存在" -ForegroundColor Green
} else {
    Write-Host "✗ nacos 数据库不存在，尝试手动初始化..." -ForegroundColor Yellow
    docker exec -i ecommerce-mysql mysql -uroot -proot123456 < sql/init.sql
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✓ 数据库初始化成功" -ForegroundColor Green
    } else {
        Write-Host "✗ 数据库初始化失败" -ForegroundColor Red
        exit 1
    }
}

if ($databases -match "ecommerce_system") {
    Write-Host "✓ ecommerce_system 数据库存在" -ForegroundColor Green
} else {
    Write-Host "✗ ecommerce_system 数据库不存在" -ForegroundColor Red
}
Write-Host ""

# 5. 启动所有服务
Write-Host "[5/5] 启动所有服务..." -ForegroundColor Yellow
docker-compose up -d
Write-Host ""

Write-Host "========================================" -ForegroundColor Green
Write-Host "  修复完成！" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host ""
Write-Host "查看服务状态:" -ForegroundColor Cyan
Write-Host "  docker-compose ps" -ForegroundColor White
Write-Host ""
Write-Host "查看 Nacos 日志:" -ForegroundColor Cyan
Write-Host "  docker-compose logs -f nacos" -ForegroundColor White
Write-Host ""
Write-Host "访问 Nacos 控制台:" -ForegroundColor Cyan
Write-Host "  http://localhost:8848/nacos" -ForegroundColor White
Write-Host "  用户名/密码: nacos/nacos" -ForegroundColor Gray
Write-Host ""
