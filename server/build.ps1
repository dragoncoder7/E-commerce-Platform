# PowerShell 脚本用于在 Windows 上构建 Docker 镜像
# 使用 BuildKit 缓存挂载共享 Maven 本地仓库

$env:DOCKER_BUILDKIT=1
$env:COMPOSE_DOCKER_CLI_BUILD=1

Write-Host "开始构建 Docker 镜像（使用 Maven 缓存）..." -ForegroundColor Green

# 方式1: 使用 docker-compose 构建（推荐）
Write-Host "`n使用 docker-compose 构建..." -ForegroundColor Cyan
docker-compose build

Write-Host "`n构建完成！" -ForegroundColor Green
Write-Host "启动服务: docker-compose up -d" -ForegroundColor Yellow
