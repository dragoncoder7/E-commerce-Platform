# Docker 化部署检查清单

部署前请确认以下内容：

## ✅ 前置检查

### 1. Docker 环境
```bash
# 检查 Docker 版本（需要 19.03+）
docker --version

# 检查 Docker Compose 版本（需要 1.27+）
docker-compose --version

# 检查 Docker 是否运行
docker ps
```

### 2. 端口占用检查
```bash
# Windows
netstat -an | findstr "3306 6379 8080 8081 8848 9848"

# Linux/Mac
lsof -i :3306,6379,8080,8081,8848,9848
```

如果有端口被占用，需要：
- 停止占用端口的服务
- 或修改 `docker-compose.yml` 中的端口映射

### 3. 磁盘空间
至少需要 3GB 可用空间（镜像 + 数据卷）

---

## 📦 文件完整性检查

### 核心文件
- [x] `server/docker-compose.yml`
- [x] `server/start.bat`（Windows）
- [x] `server/start.sh`（Linux/Mac）
- [x] `server/gateway/Dockerfile`
- [x] `server/modules/system/system-service/Dockerfile`
- [x] `server/sql/init.sql`

### 配置文件
- [x] `server/gateway/src/main/resources/application.yml`
- [x] `server/modules/system/system-service/src/main/resources/application.yml`

### 代码文件
- [x] `server/modules/system/system-service/src/main/java/com/ecommerce/system/domain/User.java`
- [x] `server/modules/system/system-service/src/main/java/com/ecommerce/system/domain/Role.java`
- [x] `server/modules/system/system-service/src/main/java/com/ecommerce/system/domain/Menu.java`
- [x] `server/modules/system/system-service/src/main/java/com/ecommerce/system/mapper/UserMapper.java`
- [x] `server/modules/system/system-service/src/main/java/com/ecommerce/system/mapper/MenuMapper.java`
- [x] `server/modules/system/system-service/src/main/resources/mapper/UserMapper.xml`
- [x] `server/modules/system/system-service/src/main/resources/mapper/MenuMapper.xml`
- [x] `server/modules/system/system-service/src/main/java/com/ecommerce/system/service/impl/AuthServiceImpl.java`

---

## 🚀 部署步骤

### 1. 进入目录
```bash
cd server
```

### 2. 启动服务

**Windows**:
```bash
start.bat
```

**Linux/Mac**:
```bash
chmod +x start.sh
./start.sh
```

### 3. 等待启动（约 30-60 秒）

观察日志输出，确认：
- [x] MySQL 初始化完成
- [x] Redis 启动成功
- [x] Nacos 启动成功
- [x] Gateway 注册到 Nacos
- [x] System Service 注册到 Nacos

---

## ✅ 启动验证

### 1. 检查容器状态
```bash
docker-compose ps
```

**预期输出**（所有服务 State 为 Up）:
```
Name                          State    Ports
-------------------------------------------------------
ecommerce-gateway            Up       0.0.0.0:8080->8080/tcp
ecommerce-mysql              Up       0.0.0.0:3306->3306/tcp
ecommerce-nacos              Up       0.0.0.0:8848->8848/tcp, ...
ecommerce-redis              Up       0.0.0.0:6379->6379/tcp
ecommerce-system-service     Up       0.0.0.0:8081->8081/tcp
```

### 2. 检查 Nacos 服务注册
访问 http://localhost:8848/nacos
- 用户名/密码: `nacos / nacos`
- 进入「服务管理 -> 服务列表」
- 确认看到：
  - [x] `gateway`（实例数 1）
  - [x] `system-service`（实例数 1）

### 3. 测试登录接口
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"123456"}'
```

**预期响应**（返回 Token）:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "token": "eyJhbGci...",
    "expiresIn": 7200000
  }
}
```

### 4. 测试用户信息接口
```bash
# 先获取 Token（从上一步的响应中复制）
TOKEN="your_token_here"

curl -X GET http://localhost:8080/api/auth/userinfo \
  -H "Authorization: Bearer $TOKEN"
```

**预期响应**（返回用户信息）:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "id": 1,
    "username": "admin",
    "nickname": "超级管理员",
    "roles": ["admin"],
    "permissions": [...]
  }
}
```

### 5. 验证数据库
```bash
docker exec -it ecommerce-mysql mysql -uroot -proot123456 ecommerce_system -e "SELECT username, nickname FROM sys_user;"
```

**预期输出**:
```
+----------+----------------+
| username | nickname       |
+----------+----------------+
| admin    | 超级管理员     |
| editor   | 运营人员       |
+----------+----------------+
```

### 6. 验证 Redis
```bash
docker exec -it ecommerce-redis redis-cli KEYS "user:token:*"
```

**预期输出**（至少有一个 Token Key，如果你刚登录过）:
```
1) "user:token:eyJhbGci..."
```

---

## 🔧 故障排查

### 问题 1: 容器启动失败

**查看日志**:
```bash
docker-compose logs -f service-name
```

**常见原因**:
- 端口被占用 -> 修改端口或停止占用的服务
- 内存不足 -> 调整 JVM 参数或增加 Docker 内存限制
- 镜像拉取失败 -> 配置国内镜像源

### 问题 2: 服务未注册到 Nacos

**检查 Nacos 日志**:
```bash
docker-compose logs nacos
```

**检查服务日志**:
```bash
docker-compose logs gateway
docker-compose logs system-service
```

**常见原因**:
- Nacos 未完全启动（等待更长时间）
- 网络配置问题（检查 `docker-compose.yml` 的 networks 配置）

### 问题 3: 登录接口返回 500

**检查 System Service 日志**:
```bash
docker-compose logs -f system-service
```

**常见原因**:
- 数据库连接失败（检查 MySQL 容器状态）
- 初始化 SQL 未执行（检查 MySQL 日志）
- Redis 连接失败（检查 Redis 容器状态）

### 问题 4: 数据库初始化失败

**手动执行初始化**:
```bash
docker exec -i ecommerce-mysql mysql -uroot -proot123456 < sql/init.sql
```

---

## 🛑 停止服务

### 停止所有容器
```bash
docker-compose down
```

### 停止并删除数据卷（清空所有数据）
```bash
docker-compose down -v
```

### 重新构建并启动
```bash
docker-compose up -d --build
```

---

## 📊 性能调优建议

### 1. JVM 参数调整

编辑 `docker-compose.yml`:
```yaml
environment:
  JAVA_OPTS: "-Xms1g -Xmx1g -XX:+UseG1GC"
```

### 2. MySQL 连接池

编辑 `system-service/src/main/resources/application.yml`:
```yaml
spring:
  datasource:
    druid:
      initial-size: 10
      max-active: 50
```

### 3. Redis 连接池

编辑配置文件:
```yaml
spring:
  data:
    redis:
      lettuce:
        pool:
          max-active: 20
          max-idle: 10
```

---

## ✨ 成功标志

当所有检查通过后，你应该能够：

- [x] 5 个容器全部运行（docker-compose ps）
- [x] Nacos 控制台看到 2 个服务注册
- [x] curl 登录接口返回 Token
- [x] curl 用户信息接口返回正确数据
- [x] 数据库有 2 个测试用户
- [x] Redis 缓存了 Token

**恭喜！Docker 化部署成功！** 🎉

下一步：
1. 前端联调测试（参考 [DOCKER.md](DOCKER.md) 的"前后端联调"章节）
2. 完善剩余接口（菜单管理 CRUD、工作台数据等）
3. 添加更多业务服务
