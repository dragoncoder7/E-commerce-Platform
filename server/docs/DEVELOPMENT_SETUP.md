# 开发环境配置指南

## 推荐方案：本地开发 + Docker 基础服务

### 架构

```
┌─────────────────────────────────────────────┐
│  本地 Windows / IDEA                         │
├─────────────────────────────────────────────┤
│  ✅ Gateway (localhost:8080)                │
│     - 热重载（Spring DevTools）              │
│     - 断点调试                               │
│  ✅ System Service (localhost:8081)         │
│     - 热重载（Spring DevTools）              │
│     - 断点调试                               │
│  ↓ 连接                                      │
└─────────────────┬───────────────────────────┘
                  │
┌─────────────────▼───────────────────────────┐
│  Docker (基础服务)                           │
├─────────────────────────────────────────────┤
│  ✅ MySQL (3306)                            │
│  ✅ Redis (6379)                            │
│  ✅ Nacos (8848)                            │
└─────────────────────────────────────────────┘
```

---

## 快速开始

### 1. 启动基础服务

```powershell
cd D:\project\code\E-commerce-Platform\server

# 一键启动 MySQL、Redis、Nacos
.\start-dev.ps1
```

等待提示 "基础服务启动完成！"

### 2. 在 IDEA 中运行应用

#### Gateway

1. 打开 `GatewayApplication.java`
2. 右键 → **Modify Run Configuration**
3. 设置：
   ```
   Active profiles: local
   ```
4. 点击 **Run** 或 **Debug**

#### System Service

1. 打开 `SystemServiceApplication.java`
2. 右键 → **Modify Run Configuration**
3. 设置：
   ```
   Active profiles: local
   ```
4. 点击 **Run** 或 **Debug**

### 3. 验证

```powershell
# 访问 Gateway
curl http://localhost:8080/actuator/health

# 访问 System Service
curl http://localhost:8081/actuator/health

# 访问 Nacos 控制台
# 浏览器打开: http://localhost:8848/nacos
# 用户名/密码: nacos/nacos
```

---

## 为什么这样做？

### ❌ Docker watch 的问题

| 问题 | 说明 |
|-----|------|
| **编译慢** | 每次修改都要在容器内重新编译 |
| **调试困难** | 无法使用 IDEA 断点调试 |
| **重启慢** | 容器重启需要 10-30 秒 |
| **资源占用** | 多个 Java 容器占用大量内存 |

### ✅ 本地开发 + Docker 基础服务的优势

| 优势 | 说明 |
|-----|------|
| **热重载快** | Spring DevTools 秒级重载 |
| **调试方便** | IDEA 完整调试功能 |
| **资源省** | 只运行必要的基础服务 |
| **开发体验好** | 像传统开发一样流畅 |

---

## 配置文件说明

### Gateway 配置文件

| 文件 | 用途 | 何时使用 |
|-----|------|---------|
| `application.yml` | 默认配置 | - |
| `application-local.yml` | 本地开发（**推荐**） | IDEA 运行 |
| `application-docker.yml` | Docker 环境 | `docker-compose up` |

### System Service 配置文件

| 文件 | 用途 | 何时使用 |
|-----|------|---------|
| `application.yml` | 默认配置 | - |
| `application-local.yml` | 本地开发（**推荐**） | IDEA 运行 |
| `application-docker.yml` | Docker 环境 | `docker-compose up` |

### 区别

```yaml
# application-local.yml (连接本地 Docker)
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/ecommerce_system
  data:
    redis:
      host: localhost
  cloud:
    nacos:
      server-addr: localhost:8848

# application-docker.yml (容器内互联)
spring:
  datasource:
    url: jdbc:mysql://mysql:3306/ecommerce_system
  data:
    redis:
      host: redis
  cloud:
    nacos:
      server-addr: nacos:8848
```

---

## 常用命令

### 基础服务管理

```powershell
# 启动基础服务
.\start-dev.ps1

# 查看服务状态
docker-compose -f docker-compose.dev.yml ps

# 查看日志
docker-compose -f docker-compose.dev.yml logs -f

# 只看 MySQL 日志
docker-compose -f docker-compose.dev.yml logs -f mysql

# 停止服务
docker-compose -f docker-compose.dev.yml down

# 停止并删除数据卷（重置数据库）
docker-compose -f docker-compose.dev.yml down -v
```

### 数据库管理

```powershell
# 进入 MySQL 容器
docker exec -it ecommerce-mysql mysql -uroot -proot123456

# 执行 SQL 文件
docker exec -i ecommerce-mysql mysql -uroot -proot123456 ecommerce_system < sql/schema.sql

# 查看数据库
docker exec ecommerce-mysql mysql -uroot -proot123456 -e "SHOW DATABASES;"

# 查看表
docker exec ecommerce-mysql mysql -uroot -proot123456 -D ecommerce_system -e "SHOW TABLES;"
```

### Redis 管理

```powershell
# 进入 Redis CLI
docker exec -it ecommerce-redis redis-cli -a redis123456

# 查看所有 key
docker exec ecommerce-redis redis-cli -a redis123456 keys '*'

# 清空 Redis
docker exec ecommerce-redis redis-cli -a redis123456 FLUSHALL
```

---

## 添加 Spring DevTools（热重载）

### 1. 添加依赖（已在父 pom.xml 中配置）

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-devtools</artifactId>
    <scope>runtime</scope>
    <optional>true</optional>
</dependency>
```

### 2. IDEA 配置

#### 启用自动编译

1. `File` → `Settings` → `Build, Execution, Deployment` → `Compiler`
2. 勾选 **Build project automatically**

#### 启用运行时自动 Make

1. `Ctrl + Shift + A` 搜索 `Registry`
2. 勾选 `compiler.automake.allow.when.app.running`

### 3. 验证热重载

1. 启动应用
2. 修改任意 Java 文件
3. `Ctrl + F9` 或 `Build` → `Recompile`
4. 观察控制台输出：
   ```
   Restarting due to 1 class path change...
   ```

---

## Docker Compose watch 的正确用法（不推荐）

如果你仍然想用 Docker Compose watch：

### 1. 修复路径配置

```yaml
# docker-compose.yml
services:
  gateway:
    build:
      context: ./gateway
    develop:
      watch:
        # ❌ 错误：./src/main/java
        # ✅ 正确：相对于 gateway 目录
        - path: ./gateway/target/gateway-1.0.0.jar
          target: /app/app.jar
          action: rebuild
```

### 2. 启动 watch

```powershell
# 方式 1: 前台运行
docker compose watch

# 方式 2: 后台运行（需要两个终端）
docker compose up -d
docker compose watch
```

### 3. 问题

- ❌ 每次修改需要重新编译整个项目
- ❌ 构建镜像需要 1-3 分钟
- ❌ 容器重启需要 10-30 秒
- ❌ 无法使用 IDEA 断点调试

---

## 不同场景的选择

| 场景 | 推荐方案 | 命令 |
|-----|---------|------|
| **日常开发** | 本地 IDEA + Docker 基础服务 | `.\start-dev.ps1` |
| **集成测试** | 完整 Docker Compose | `docker-compose up` |
| **生产部署** | Kubernetes / Docker Swarm | `kubectl apply` |

---

## 故障排查

### 问题 1: 端口冲突

```
Error: bind: address already in use
```

**解决**:
```powershell
# 查看占用端口的进程
netstat -ano | findstr :8080
netstat -ano | findstr :3306
netstat -ano | findstr :6379
netstat -ano | findstr :8848

# 停止占用进程
taskkill /PID <PID> /F
```

### 问题 2: 无法连接 Nacos

```
com.alibaba.nacos.shaded.io.grpc.StatusRuntimeException: UNAVAILABLE
```

**解决**:
1. 检查 Nacos 是否启动：`docker ps | findstr nacos`
2. 检查健康状态：`docker inspect ecommerce-nacos`
3. 查看日志：`docker logs ecommerce-nacos`
4. 访问控制台：http://localhost:8848/nacos

### 问题 3: 数据库连接失败

```
com.mysql.cj.jdbc.exceptions.CommunicationsException
```

**解决**:
1. 确认端口：`localhost:3306`（不是 3306）
2. 检查密码：`root123456`
3. 测试连接：
   ```powershell
   docker exec ecommerce-mysql mysql -uroot -proot123456 -e "SELECT 1"
   ```

---

## 总结

### ✅ 推荐方案

```
本地开发: IDEA + Docker 基础服务
集成测试: 完整 Docker Compose
生产部署: Kubernetes
```

### 🚀 开始开发

```powershell
# 1. 启动基础服务
cd D:\project\code\E-commerce-Platform\server
.\start-dev.ps1

# 2. IDEA 中运行应用（profile: local）
# - GatewayApplication
# - SystemServiceApplication

# 3. 开始编码！
```

---

## 参考资料

- [Spring Boot DevTools 文档](https://docs.spring.io/spring-boot/docs/current/reference/html/using.html#using.devtools)
- [Docker Compose Watch 文档](https://docs.docker.com/compose/file-watch/)
- [Spring Cloud 配置管理](https://spring.io/projects/spring-cloud-config)
