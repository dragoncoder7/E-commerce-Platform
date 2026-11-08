# Nacos 启动问题诊断脚本

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Nacos 启动问题诊断工具" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# 1. 检查 MySQL 容器状态
Write-Host "[1] 检查 MySQL 容器状态..." -ForegroundColor Yellow
docker ps -a --filter "name=ecommerce-mysql" --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
Write-Host ""

# 2. 检查 MySQL 健康状态
Write-Host "[2] 检查 MySQL 健康状态..." -ForegroundColor Yellow
$mysqlHealth = docker inspect ecommerce-mysql --format='{{.State.Health.Status}}' 2>$null
if ($mysqlHealth) {
    Write-Host "MySQL 健康状态: $mysqlHealth" -ForegroundColor $(if ($mysqlHealth -eq "healthy") { "Green" } else { "Red" })
} else {
    Write-Host "MySQL 容器未运行或无健康检查" -ForegroundColor Red
}
Write-Host ""

# 3. 测试 MySQL 连接
Write-Host "[3] 测试从宿主机连接 MySQL (3307)..." -ForegroundColor Yellow
try {
    $result = docker exec ecommerce-mysql mysql -uroot -proot123456 -e "SELECT 1 as test;" 2>&1
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✓ MySQL 连接成功" -ForegroundColor Green
    } else {
        Write-Host "✗ MySQL 连接失败: $result" -ForegroundColor Red
    }
} catch {
    Write-Host "✗ 无法执行 MySQL 命令: $_" -ForegroundColor Red
}
Write-Host ""

# 4. 检查 nacos 数据库是否存在
Write-Host "[4] 检查 nacos 数据库..." -ForegroundColor Yellow
try {
    $databases = docker exec ecommerce-mysql mysql -uroot -proot123456 -e "SHOW DATABASES LIKE 'nacos';" 2>&1
    if ($databases -match "nacos") {
        Write-Host "✓ nacos 数据库已创建" -ForegroundColor Green
        
        # 检查表
        Write-Host "   检查 nacos 数据库的表..." -ForegroundColor Gray
        $tables = docker exec ecommerce-mysql mysql -uroot -proot123456 -D nacos -e "SHOW TABLES;" 2>&1
        if ($tables -match "config_info") {
            Write-Host "   ✓ nacos 表结构已初始化" -ForegroundColor Green
        } else {
            Write-Host "   ✗ nacos 表未初始化" -ForegroundColor Red
        }
    } else {
        Write-Host "✗ nacos 数据库不存在" -ForegroundColor Red
    }
} catch {
    Write-Host "✗ 无法检查数据库: $_" -ForegroundColor Red
}
Write-Host ""

# 5. 检查 Nacos 容器状态
Write-Host "[5] 检查 Nacos 容器状态..." -ForegroundColor Yellow
docker ps -a --filter "name=ecommerce-nacos" --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
Write-Host ""

# 6. 查看 Nacos 最近日志
Write-Host "[6] Nacos 容器最近日志 (最后 30 行)..." -ForegroundColor Yellow
Write-Host "----------------------------------------" -ForegroundColor Gray
docker logs ecommerce-nacos --tail 30 2>&1
Write-Host "----------------------------------------" -ForegroundColor Gray
Write-Host ""

# 7. 建议操作
Write-Host "[7] 建议操作" -ForegroundColor Yellow
Write-Host ""

if ($mysqlHealth -ne "healthy") {
    Write-Host "→ MySQL 未就绪，等待 MySQL 完全启动:" -ForegroundColor Cyan
    Write-Host "   docker-compose restart mysql" -ForegroundColor White
    Write-Host ""
}

if (-not ($databases -match "nacos")) {
    Write-Host "→ nacos 数据库未创建，手动初始化:" -ForegroundColor Cyan
    Write-Host "   docker exec -i ecommerce-mysql mysql -uroot -proot123456 < sql/init.sql" -ForegroundColor White
    Write-Host ""
}

Write-Host "→ 重启 Nacos:" -ForegroundColor Cyan
Write-Host "   docker-compose restart nacos" -ForegroundColor White
Write-Host ""

Write-Host "→ 查看实时日志:" -ForegroundColor Cyan
Write-Host "   docker-compose logs -f nacos" -ForegroundColor White
Write-Host ""

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  诊断完成" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
