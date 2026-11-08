#!/bin/bash

# 使用 BuildKit 缓存挂载构建 Docker 镜像
# 这样可以在多次构建之间共享 Maven 本地仓库

export DOCKER_BUILDKIT=1

echo "构建 Gateway 服务..."
docker build \
  --build-arg BUILDKIT_INLINE_CACHE=1 \
  --secret id=maven_settings,src=$HOME/.m2/settings.xml \
  --cache-from maven_cache \
  -t ecommerce-gateway:latest \
  -f gateway/Dockerfile \
  .

echo "构建 System Service 服务..."
docker build \
  --build-arg BUILDKIT_INLINE_CACHE=1 \
  --secret id=maven_settings,src=$HOME/.m2/settings.xml \
  --cache-from maven_cache \
  -t ecommerce-system-service:latest \
  -f modules/system/system-service/Dockerfile \
  .

echo "构建完成！"
