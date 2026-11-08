#!/bin/bash

echo "========================================"
echo "E-commerce Platform - Docker Deployment"
echo "========================================"
echo ""

echo "[1/5] Checking Docker..."
if ! command -v docker &> /dev/null; then
    echo "Error: Docker is not installed or not running"
    exit 1
fi
echo "Docker is ready"

echo ""
echo "[2/5] Checking Docker Compose..."
if ! command -v docker-compose &> /dev/null; then
    echo "Error: Docker Compose is not installed"
    exit 1
fi
echo "Docker Compose is ready"

echo ""
echo "[3/5] Stopping existing containers..."
docker-compose down

echo ""
echo "[4/5] Building and starting services..."
echo "This may take a few minutes on first run..."
docker-compose up -d --build

echo ""
echo "[5/5] Waiting for services to be ready..."
sleep 10

echo ""
echo "========================================"
echo "Service Status:"
echo "========================================"
docker-compose ps

echo ""
echo "========================================"
echo "Access Information:"
echo "========================================"
echo "Nacos:          http://localhost:8848/nacos (nacos/nacos)"
echo "API Gateway:    http://localhost:8080"
echo "System Service: http://localhost:8081"
echo "MySQL:          localhost:3306 (root/root123456)"
echo "Redis:          localhost:6379"
echo ""
echo "========================================"
echo "Test Login:"
echo "========================================"
echo 'curl -X POST http://localhost:8080/api/auth/login \'
echo '  -H "Content-Type: application/json" \'
echo '  -d '"'"'{"username":"admin","password":"123456"}'"'"
echo ""
echo "========================================"
echo "View logs: docker-compose logs -f"
echo "Stop:      docker-compose down"
echo "========================================"
