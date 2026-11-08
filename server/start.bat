@echo off
echo ========================================
echo E-commerce Platform - Docker Deployment
echo ========================================
echo.

echo [1/5] Checking Docker...
docker --version >nul 2>&1
if %errorlevel% neq 0 (
    echo Error: Docker is not installed or not running
    exit /b 1
)
echo Docker is ready

echo.
echo [2/5] Checking Docker Compose...
docker-compose --version >nul 2>&1
if %errorlevel% neq 0 (
    echo Error: Docker Compose is not installed
    exit /b 1
)
echo Docker Compose is ready

echo.
echo [3/5] Stopping existing containers...
docker-compose down

echo.
echo [4/5] Building and starting services...
echo This may take a few minutes on first run...
docker-compose up -d --build

echo.
echo [5/5] Waiting for services to be ready...
timeout /t 10 /nobreak >nul

echo.
echo ========================================
echo Service Status:
echo ========================================
docker-compose ps

echo.
echo ========================================
echo Access Information:
echo ========================================
echo Nacos:          http://localhost:8848/nacos (nacos/nacos)
echo API Gateway:    http://localhost:8080
echo System Service: http://localhost:8081
echo MySQL:          localhost:3306 (root/root123456)
echo Redis:          localhost:6379
echo.
echo ========================================
echo Test Login:
echo ========================================
echo curl -X POST http://localhost:8080/api/auth/login ^
echo   -H "Content-Type: application/json" ^
echo   -d "{\"username\":\"admin\",\"password\":\"123456\"}"
echo.
echo ========================================
echo View logs: docker-compose logs -f
echo Stop:      docker-compose down
echo ========================================
