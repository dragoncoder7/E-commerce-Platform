# E-commerce Platform Backend - Docker 部署

## 快速启动

### 一键启动所有服务

```bash
cd server
docker-compose up -d
```

这将启动以下服务：
- **MySQL** (3306) - 数据库
- **Redis** (6379) - 缓存
- **Nacos** (8848, 9848) - 注册中心和配置中心
- **Gateway** (8080) - API 网关
- **System Service** (8081) - 系统服务

### 查看服务状态

```bash
docker-compose ps
```

### 查看日志

```bash
# 查看所有服务日志
docker-compose logs -f

# 查看特定服务日志
docker-compose logs -f gateway
docker-compose logs -f system-service
```

### 停止服务

```bash
docker-compose down
```

### 完全清理（包括数据卷）

```bash
docker-compose down -v
```

## 服务访问

| 服务 | 地址 | 说明 |
|------|------|------|
| Nacos 控制台 | http://localhost:8848/nacos | 用户名/密码: nacos/nacos |
| API 网关 | http://localhost:8080 | 统一入口 |
| 系统服务 | http://localhost:8081 | 直接访问（通常通过网关） |
| MySQL | localhost:3306 | root/root123456 |
| Redis | localhost:6379 | 无密码 |

## 测试接口

### 登录

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"123456"}'
```

响应示例：
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "expiresIn": 7200000
  },
  "timestamp": 1762500000000
}
```

### 获取用户信息

```bash
curl -X GET http://localhost:8080/api/auth/userinfo \
  -H "Authorization: Bearer YOUR_TOKEN"
```

### 获取菜单

```bash
curl -X GET http://localhost:8080/api/auth/menus \
  -H "Authorization: Bearer YOUR_TOKEN"
```

## 测试账号

| 用户名 | 密码 | 角色 | 说明 |
|--------|------|------|------|
| admin | 123456 | 超级管理员 | 拥有所有权限 |
| editor | 123456 | 运营人员 | 仅查看权限 |

## 前后端联调

1. **启动后端服务**
   ```bash
   cd server
   docker-compose up -d
   ```

2. **修改前端配置**
   编辑 `admin/.env.development`:
   ```env
   VITE_USE_MOCK=false
   VITE_PROXY_TARGET=http://localhost:8080
   ```

3. **启动前端**
   ```bash
   cd admin
   npm run dev
   ```

4. **访问应用**
   打开 http://localhost:5173，使用 `admin/123456` 登录

## 服务健康检查

所有服务都配置了健康检查，确保依赖服务启动完成后再启动依赖它们的服务。

启动顺序：
1. MySQL
2. Redis
3. Nacos（依赖 MySQL）
4. Gateway（依赖 Nacos、Redis）
5. System Service（依赖 MySQL、Redis、Nacos）

## 数据持久化

以下数据使用 Docker 卷持久化：
- `mysql_data`: MySQL 数据
- `redis_data`: Redis 数据
- `nacos_logs`: Nacos 日志

## 故障排查

### 服务无法启动

1. 检查端口占用：
   ```bash
   netstat -an | findstr "3306 6379 8080 8081 8848"
   ```

2. 查看服务日志：
   ```bash
   docker-compose logs service-name
   ```

### Nacos 服务注册失败

1. 确认 Nacos 已启动：
   ```bash
   docker-compose logs nacos
   ```

2. 访问 Nacos 控制台检查服务列表：
   http://localhost:8848/nacos

### 数据库连接失败

1. 检查 MySQL 容器状态：
   ```bash
   docker-compose ps mysql
   ```

2. 查看 MySQL 日志：
   ```bash
   docker-compose logs mysql
   ```

3. 手动连接测试：
   ```bash
   docker exec -it ecommerce-mysql mysql -uroot -proot123456
   ```

## 开发模式

如果需要在本地开发，可以只启动基础设施：

```bash
# 只启动 MySQL、Redis、Nacos
docker-compose up -d mysql redis nacos

# 在 IDE 中启动 Gateway 和 System Service
```

## 生产部署建议

1. 修改所有默认密码
2. 使用外部 MySQL 集群
3. 使用 Redis 集群
4. 配置 Nacos 集群模式
5. 调整 JVM 参数
6. 配置日志收集
7. 添加监控告警
