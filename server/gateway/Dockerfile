# syntax=docker/dockerfile:1.4

# 多阶段构建
# 阶段1: 构建
FROM maven:3.9-eclipse-temurin-21-alpine AS builder

WORKDIR /build

# 复制 pom 文件并下载依赖（利用 BuildKit 缓存挂载）
COPY pom.xml .
COPY common/common-core/pom.xml common/common-core/
COPY common/common-redis/pom.xml common/common-redis/
COPY common/common-security/pom.xml common/common-security/
COPY common/common-api/pom.xml common/common-api/
COPY gateway/pom.xml gateway/
COPY modules/system/system-api/pom.xml modules/system/system-api/
COPY modules/system/system-service/pom.xml modules/system/system-service/

# 使用 BuildKit 缓存挂载下载依赖
# 每次构建时会重用 /root/.m2 目录中的依赖
RUN --mount=type=cache,target=/root/.m2 \
    mvn dependency:go-offline -B

# 复制源码并构建
COPY . .
RUN --mount=type=cache,target=/root/.m2 \
    mvn clean package -DskipTests -pl gateway -am

# 阶段2: 运行 Gateway
FROM eclipse-temurin:21-jre-alpine AS gateway

WORKDIR /app

# 安装 tzdata 设置时区
RUN apk add --no-cache tzdata && \
    cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo "Asia/Shanghai" > /etc/timezone && \
    apk del tzdata

# 复制构建产物
COPY --from=builder /build/gateway/target/*.jar app.jar

# JVM 参数优化
ENV JAVA_OPTS="-Xms512m -Xmx512m -XX:+UseG1GC -XX:MaxGCPauseMillis=200"

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
