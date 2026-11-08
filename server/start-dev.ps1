# 开发环境启动脚本
# 只启动基础服务（MySQL、Redis、Nacos），应用在 IDEA 中运行

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  启动开发环境（基础服务）" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# 1. 检查 Docker 是否运行
Write-Host "[1/4] 检查 Docker..." -ForegroundColor Yellow
$dockerRunning = docker info 2>$null
if (-not $dockerRunning) {
    Write-Host "✗ Docker 未运行，请先启动 Docker Desktop" -ForegroundColor Red
    exit 1
}
Write-Host "✓ Docker 运行中" -ForegroundColor Green
Write-Host ""

# 2. 停止旧的容器
Write-Host "[2/4] 停止旧容器..." -ForegroundColor Yellow
docker-compose -f docker-compose.dev.yml down
Write-Host ""

# 3. 启动基础服务
Write-Host "[3/4] 启动基础服务（MySQL、Redis、Nacos）..." -ForegroundColor Yellow
docker-compose -f docker-compose.dev.yml up -d
Write-Host ""

# 4. 等待服务就绪
Write-Host "[4/4] 等待服务就绪..." -ForegroundColor Yellow
Start-Sleep -Seconds 5

$maxWait = 60
$waited = 0

# 检查 MySQL
Write-Host "  等待 MySQL..." -ForegroundColor Gray
while ($waited -lt $maxWait) {
    $mysqlHealth = docker inspect ecommerce-mysql --format='{{.State.Health.Status}}' 2>$null
    if ($mysqlHealth -eq "healthy") {
        Write-Host "  ✓ MySQL 就绪" -ForegroundColor Green
        break
    }
    Start-Sleep -Seconds 5
    $waited += 5
}

# 检查 Redis
Write-Host "  等待 Redis..." -ForegroundColor Gray
$redisReady = docker exec ecommerce-redis redis-cli -a redis123456 ping 2>$null
if ($redisReady -match "PONG") {
    Write-Host "  ✓ Redis 就绪" -ForegroundColor Green
}

# 检查 Nacos
Write-Host "  等待 Nacos..." -ForegroundColor Gray
$waited = 0
while ($waited -lt $maxWait) {
    try {
        $nacosHealth = Invoke-RestMethod -Uri "http://localhost:8848/nacos/v1/console/health/readiness" -Method Get -TimeoutSec 2 2>$null
        if ($nacosHealth) {
            Write-Host "  ✓ Nacos 就绪" -ForegroundColor Green
            break
        }
    } catch {
        # 继续等待
    }
    Start-Sleep -Seconds 5
    $waited += 5
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  基础服务启动完成！" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

Write-Host "服务信息:" -ForegroundColor Yellow
Write-Host "  MySQL:   localhost:3307" -ForegroundColor White
Write-Host "           用户: root / 密码: root123456" -ForegroundColor Gray
Write-Host ""
Write-Host "  Redis:   localhost:6379" -ForegroundColor White
Write-Host "           密码: redis123456" -ForegroundColor Gray
Write-Host ""
Write-Host "  Nacos:   http://localhost:8848/nacos" -ForegroundColor White
Write-Host "           用户: nacos / 密码: nacos" -ForegroundColor Gray
Write-Host ""

Write-Host "下一步:" -ForegroundColor Yellow
Write-Host "  1. 在 IDEA 中以 'local' profile 运行 Gateway" -ForegroundColor White
Write-Host "     Run Configuration → Active profiles: local" -ForegroundColor Gray
Write-Host ""
Write-Host "  2. 在 IDEA 中以 'local' profile 运行 System Service" -ForegroundColor White
Write-Host "     Run Configuration → Active profiles: local" -ForegroundColor Gray
Write-Host ""

Write-Host "查看日志:" -ForegroundColor Yellow
Write-Host "  docker-compose -f docker-compose.dev.yml logs -f" -ForegroundColor White
Write-Host ""

Write-Host "停止服务:" -ForegroundColor Yellow
Write-Host "  docker-compose -f docker-compose.dev.yml down" -ForegroundColor White
Write-Host ""
