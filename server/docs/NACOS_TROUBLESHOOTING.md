# Nacos 启动问题解决方案

## 问题现象

```
✘ Container ecommerce-nacos  Error dependency nacos failed to start
```

---

## 快速修复

### 方式 1: 一键修复（推荐）

```powershell
cd D:\project\code\E-commerce-Platform\server
.\fix-nacos.ps1
```

这个脚本会：
1. 停止所有服务
2. 单独启动 MySQL 并等待就绪
3. 验证数据库初始化
4. 启动所有服务

### 方式 2: 手动诊断

```powershell
cd D:\project\code\E-commerce-Platform\server
.\diagnose-nacos.ps1
```

根据诊断结果，按提示操作。

---

## 常见原因和解决方法

### 原因 1: MySQL 未完全就绪

**现象**: Nacos 启动时 MySQL 还在初始化数据库

**解决**:
```powershell
# 重启 MySQL 确保健康
docker-compose restart mysql

# 等待 30 秒
Start-Sleep -Seconds 30

# 重启 Nacos
docker-compose restart nacos
```

### 原因 2: nacos 数据库未创建

**现象**: 数据库初始化脚本未执行

**检查**:
```powershell
# 进入 MySQL 容器检查
docker exec -it ecommerce-mysql mysql -uroot -proot123456

# 在 MySQL 中执行
SHOW DATABASES;
USE nacos;
SHOW TABLES;
```

**修复**:
```powershell
# 手动执行初始化脚本
docker exec -i ecommerce-mysql mysql -uroot -proot123456 < sql/init.sql

# 验证
docker exec ecommerce-mysql mysql -uroot -proot123456 -e "USE nacos; SHOW TABLES;"
```

### 原因 3: Nacos 配置错误

**检查 docker-compose.yml 中的 Nacos 配置**:

```yaml
nacos:
  environment:
    MODE: standalone
    SPRING_DATASOURCE_PLATFORM: mysql
    MYSQL_SERVICE_HOST: mysql          # ← 容器内使用服务名
    MYSQL_SERVICE_DB_NAME: nacos       # ← 数据库名必须是 nacos
    MYSQL_SERVICE_USER: root
    MYSQL_SERVICE_PASSWORD: root123456
```

### 原因 4: 端口冲突

**检查是否有其他服务占用端口**:

```powershell
# 检查 8848 端口
netstat -ano | findstr :8848

# 如果被占用，停止占用进程或修改端口映射
```

---

## 验证 Nacos 是否启动成功

### 1. 检查容器状态

```powershell
docker-compose ps
```

期望输出：
```
NAME                    STATUS                    PORTS
ecommerce-nacos         Up (healthy)              0.0.0.0:8848->8848/tcp
```

### 2. 查看日志

```powershell
docker-compose logs -f nacos
```

成功启动的日志：
```
Nacos started successfully in stand alone mode.
Nacos is starting...
Nacos started successfully.
```

### 3. 访问控制台

浏览器访问: http://localhost:8848/nacos

- **用户名**: nacos
- **密码**: nacos

### 4. 测试健康检查

```powershell
curl http://localhost:8848/nacos/v1/console/health/readiness
```

期望输出：
```json
{"status":"UP"}
```

---

## Docker 中 MySQL 连接说明

### 从宿主机（Windows）访问

```
Host: localhost (或 127.0.0.1)
Port: 3306                    ← 注意是 3306
User: root
Password: root123456
```

**为什么是 3306？**

看 `docker-compose.yml` 配置：
```yaml
mysql:
  ports:
    - "3306:3306"  # 宿主机端口:容器端口
```

### 从容器内访问

```yaml
# Nacos 配置
MYSQL_SERVICE_HOST: mysql     # 使用服务名
# 连接字符串会是: jdbc:mysql://mysql:3306/nacos

# System Service 配置
SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/ecommerce_system
                                          # ↑ 容器内用 3306
```

**为什么容器内用 3306？**

- 容器内部通过 Docker 网络直连，使用容器内部端口
- 3306 只是宿主机端口映射

---

## 使用工具连接 MySQL

### Navicat

```
连接名: E-commerce Docker MySQL
主机: 127.0.0.1
端口: 3306
用户名: root
密码: root123456
```

### DBeaver

```
Server Host: localhost
Port: 3306
Database: nacos (或 ecommerce_system)
Username: root
Password: root123456
```

### MySQL Workbench

```
Hostname: 127.0.0.1
Port: 3306
Username: root
Password: root123456
```

### 命令行（mysql 客户端）

```powershell
mysql -h 127.0.0.1 -P 3306 -uroot -proot123456

# 或者通过 Docker
docker exec -it ecommerce-mysql mysql -uroot -proot123456
```

---

## 完整重启流程

如果上述方法都不行，尝试完全重启：

```powershell
# 1. 停止并删除所有容器
docker-compose down -v

# 2. 清理悬空镜像（可选）
docker image prune -f

# 3. 重新构建
$env:DOCKER_BUILDKIT=1
$env:COMPOSE_DOCKER_CLI_BUILD=1
docker-compose build --no-cache

# 4. 启动 MySQL
docker-compose up -d mysql

# 5. 等待 MySQL 就绪（约 30 秒）
Start-Sleep -Seconds 30

# 6. 验证数据库
docker exec ecommerce-mysql mysql -uroot -proot123456 -e "SHOW DATABASES;"

# 7. 启动所有服务
docker-compose up -d

# 8. 查看日志
docker-compose logs -f
```

---

## 查看详细日志

### 查看所有服务日志

```powershell
docker-compose logs -f
```

### 只看 Nacos 日志

```powershell
docker-compose logs -f nacos
```

### 只看 MySQL 日志

```powershell
docker-compose logs -f mysql
```

### 查看最近 100 行

```powershell
docker-compose logs --tail 100 nacos
```

---

## 常见错误信息

### 错误 1: Connection refused

```
com.mysql.cj.jdbc.exceptions.CommunicationsException: Communications link failure
```

**原因**: MySQL 还未完全启动  
**解决**: 等待 30 秒后重启 Nacos

### 错误 2: Database 'nacos' doesn't exist

```
java.sql.SQLException: Unknown database 'nacos'
```

**原因**: 数据库初始化脚本未执行  
**解决**: 手动执行 `sql/init.sql`

### 错误 3: Table doesn't exist

```
Table 'nacos.config_info' doesn't exist
```

**原因**: 表未创建  
**解决**: 检查 `sql/init.sql` 是否包含 Nacos 表结构

---

## 排查清单

- [ ] MySQL 容器是否 healthy？
- [ ] nacos 数据库是否存在？
- [ ] nacos 数据库中是否有表？
- [ ] Nacos 环境变量配置是否正确？
- [ ] 端口 8848 是否被占用？
- [ ] Docker 网络是否正常？

---

## 帮助命令

```powershell
# 查看所有容器状态
docker-compose ps

# 进入 MySQL 容器
docker exec -it ecommerce-mysql bash

# 进入 Nacos 容器
docker exec -it ecommerce-nacos bash

# 查看 Docker 网络
docker network ls
docker network inspect server_ecommerce-network

# 测试容器间网络连通性
docker exec ecommerce-nacos ping mysql
```

---

## 需要帮助？

如果问题仍然存在，请提供：

1. `docker-compose ps` 输出
2. `docker-compose logs nacos --tail 100` 输出
3. `docker exec ecommerce-mysql mysql -uroot -proot123456 -e "SHOW DATABASES;"` 输出

这样我可以更准确地诊断问题。
