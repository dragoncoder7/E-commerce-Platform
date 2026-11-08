# E-commerce Platform Backend

## 快速启动

### 方式一：Docker Compose（推荐）

#### Windows

```powershell
cd server

# 启用 BuildKit 缓存加速（首次配置）
$env:DOCKER_BUILDKIT=1
$env:COMPOSE_DOCKER_CLI_BUILD=1

# 构建并启动所有服务
docker-compose up -d --build
```

或使用快捷脚本：

```powershell
cd server
.\build.ps1           # 构建镜像
docker-compose up -d  # 启动服务
```

#### Linux / macOS

```bash
cd server

# 启用 BuildKit 缓存加速（首次配置）
export DOCKER_BUILDKIT=1
export COMPOSE_DOCKER_CLI_BUILD=1

# 构建并启动所有服务
docker-compose up -d --build
```

或使用快捷脚本：

```bash
cd server
chmod +x build.sh
./build.sh            # 构建镜像
docker-compose up -d  # 启动服务
```

#### 验证服务

等待约 30-60 秒后：

```bash
# 查看服务状态
docker-compose ps

# 测试登录接口
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"123456"}'

# 访问 Nacos 控制台
open http://localhost:8848/nacos  # 用户名/密码: nacos/nacos
```

> 💡 **性能提示**: 使用 BuildKit 缓存后，首次构建约 3 分钟，后续构建仅需 30 秒！  
> 详见: [Docker Maven 缓存加速指南](docs/DOCKER_MAVEN_CACHE.md)

---

### 方式二：本地开发

### 前置要求
- JDK 17+
- Maven 3.6+
- MySQL 8.0+
- Redis 6.0+
- Nacos 2.x

### 启动步骤

1. **启动基础服务**
   ```bash
   # 启动 Nacos（下载: https://github.com/alibaba/nacos/releases）
   startup.cmd -m standalone  # Windows
   
   # 启动 Redis
   redis-server
   ```

2. **编译项目**
   ```bash
   cd server
   mvn clean install -DskipTests
   ```

3. **启动微服务**
   - 先启动 `gateway/GatewayApplication.java`（端口 8080）
   - 再启动 `modules/system/system-service/SystemApplication.java`（端口 8081）

4. **验证**
   - Nacos: http://localhost:8848/nacos (nacos/nacos)
   - 测试接口:
     ```bash
     curl -X POST http://localhost:8080/api/auth/login ^
       -H "Content-Type: application/json" ^
       -d "{\"username\":\"admin\",\"password\":\"123456\"}"
     ```

## 目录结构

```
server/
├── gateway/              # 网关服务 (8080)
├── common/               # 公共模块
│   ├── common-core/      # 核心工具
│   ├── common-redis/     # Redis 封装
│   ├── common-security/  # JWT 认证
│   └── common-api/       # API 定义
└── modules/              # 业务服务
    └── system/           # 系统服务 (8081)
        ├── system-api/       # 接口定义
        └── system-service/   # 服务实现
```

## 配置说明

修改 `modules/system/system-service/src/main/resources/application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/ecommerce_system?...
    username: root
    password: your_password
  data:
    redis:
      host: 127.0.0.1
      port: 6379
```

## 测试账号

- 用户名: `admin` / 密码: `123456`
- 用户名: `editor` / 密码: `123456`

当前使用 Mock 数据，数据库建表后会替换为真实实现。
