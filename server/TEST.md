# Backend API Test Guide

## 一键启动测试环境

```bash
cd server

# Windows
start.bat

# Linux/Mac
chmod +x start.sh && ./start.sh
```

等待所有服务启动完成（约 30-60 秒）。

---

## 接口测试

### 1. 登录接口

**超级管理员登录**：
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"123456"}'
```

**预期响应**：
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOjEsInVzZXJuYW1lIjoiYWRtaW4iLCJpYXQiOjE3MzEwNzM...",
    "expiresIn": 7200000
  },
  "timestamp": 1731073200000
}
```

**运营人员登录**：
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"editor","password":"123456"}'
```

**错误情况测试**：
```bash
# 错误的密码
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"wrong"}'

# 不存在的用户
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"notexist","password":"123456"}'
```

---

### 2. 获取用户信息

先登录获取 Token，然后：

```bash
curl -X GET http://localhost:8080/api/auth/userinfo \
  -H "Authorization: Bearer YOUR_TOKEN_HERE"
```

**admin 预期响应**：
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "id": 1,
    "username": "admin",
    "nickname": "超级管理员",
    "avatar": "https://avatars.githubusercontent.com/u/1?v=4",
    "roles": ["admin"],
    "permissions": [
      "system:dashboard:view",
      "system:menu:list",
      "system:menu:add",
      "system:menu:edit",
      "system:menu:delete"
    ]
  },
  "timestamp": 1731073200000
}
```

**editor 预期响应**（权限较少）：
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "id": 2,
    "username": "editor",
    "nickname": "运营人员",
    "avatar": "https://avatars.githubusercontent.com/u/2?v=4",
    "roles": ["editor"],
    "permissions": [
      "system:dashboard:view",
      "system:menu:list"
    ]
  },
  "timestamp": 1731073200000
}
```

---

### 3. 获取用户菜单

```bash
curl -X GET http://localhost:8080/api/auth/menus \
  -H "Authorization: Bearer YOUR_TOKEN_HERE"
```

**admin 预期响应**（完整菜单树）：
```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "id": 1,
      "parentId": 0,
      "name": "Dashboard",
      "title": "工作台",
      "type": "menu",
      "path": "/dashboard",
      "component": "dashboard/index",
      "icon": "DataAnalysis",
      "permission": "system:dashboard:view",
      "sort": 1,
      "visible": true,
      "keepAlive": true,
      "status": 1
    },
    {
      "id": 2,
      "parentId": 0,
      "name": "System",
      "title": "系统管理",
      "type": "catalog",
      "path": "/system",
      "icon": "Setting",
      "sort": 2,
      "visible": true,
      "keepAlive": false,
      "status": 1,
      "children": [
        {
          "id": 3,
          "parentId": 2,
          "name": "Menu",
          "title": "菜单管理",
          "type": "menu",
          "path": "/system/menu",
          "component": "system/menu/index",
          "icon": "Menu",
          "permission": "system:menu:list",
          "sort": 1,
          "visible": true,
          "keepAlive": true,
          "status": 1
        }
      ]
    }
  ],
  "timestamp": 1731073200000
}
```

**editor 预期响应**（权限受限，无子菜单按钮权限）：
```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "id": 1,
      "parentId": 0,
      "name": "Dashboard",
      "title": "工作台",
      "type": "menu",
      "path": "/dashboard",
      "component": "dashboard/index",
      "icon": "DataAnalysis",
      "permission": "system:dashboard:view",
      "sort": 1,
      "visible": true,
      "keepAlive": true,
      "status": 1
    },
    {
      "id": 2,
      "parentId": 0,
      "name": "System",
      "title": "系统管理",
      "type": "catalog",
      "path": "/system",
      "icon": "Setting",
      "sort": 2,
      "visible": true,
      "keepAlive": false,
      "status": 1,
      "children": [
        {
          "id": 3,
          "parentId": 2,
          "name": "Menu",
          "title": "菜单管理",
          "type": "menu",
          "path": "/system/menu",
          "component": "system/menu/index",
          "icon": "Menu",
          "permission": "system:menu:list",
          "sort": 1,
          "visible": true,
          "keepAlive": true,
          "status": 1
        }
      ]
    }
  ],
  "timestamp": 1731073200000
}
```

> **注意**：editor 用户在前端的菜单管理页面中，"新增"、"编辑"、"删除" 按钮会被隐藏，因为没有对应权限。

---

### 4. 登出接口

```bash
curl -X POST http://localhost:8080/api/auth/logout \
  -H "Authorization: Bearer YOUR_TOKEN_HERE"
```

**预期响应**：
```json
{
  "code": 200,
  "message": "操作成功",
  "data": null,
  "timestamp": 1731073200000
}
```

登出后，该 Token 会从 Redis 中删除，再次使用会返回 401。

---

### 5. Token 过期测试

使用一个无效或过期的 Token：

```bash
curl -X GET http://localhost:8080/api/auth/userinfo \
  -H "Authorization: Bearer invalid_token"
```

**预期响应**：
```json
{
  "code": 401,
  "message": "Token 已过期",
  "data": null,
  "timestamp": 1731073200000
}
```

---

## 数据库验证

### 连接数据库

```bash
docker exec -it ecommerce-mysql mysql -uroot -proot123456 ecommerce_system
```

### 查询用户表

```sql
SELECT id, username, nickname, status FROM sys_user;
```

**预期结果**：
```
+----+----------+----------------+--------+
| id | username | nickname       | status |
+----+----------+----------------+--------+
|  1 | admin    | 超级管理员     |      1 |
|  2 | editor   | 运营人员       |      1 |
+----+----------+----------------+--------+
```

### 查询角色表

```sql
SELECT id, name, code FROM sys_role;
```

**预期结果**：
```
+----+----------------+--------+
| id | name           | code   |
+----+----------------+--------+
|  1 | 超级管理员     | admin  |
|  2 | 运营人员       | editor |
+----+----------------+--------+
```

### 查询菜单表

```sql
SELECT id, parent_id, name, title, type FROM sys_menu ORDER BY sort;
```

**预期结果**：
```
+----+-----------+------------+--------------+----------+
| id | parent_id | name       | title        | type     |
+----+-----------+------------+--------------+----------+
|  1 |         0 | Dashboard  | 工作台       | menu     |
|  2 |         0 | System     | 系统管理     | catalog  |
|  3 |         2 | Menu       | 菜单管理     | menu     |
|  4 |         3 | MenuAdd    | 新增菜单     | button   |
|  5 |         3 | MenuEdit   | 编辑菜单     | button   |
|  6 |         3 | MenuDelete | 删除菜单     | button   |
+----+-----------+------------+--------------+----------+
```

### 查询用户角色关联

```sql
SELECT u.username, r.code 
FROM sys_user u 
JOIN sys_user_role ur ON u.id = ur.user_id 
JOIN sys_role r ON ur.role_id = r.id;
```

**预期结果**：
```
+----------+--------+
| username | code   |
+----------+--------+
| admin    | admin  |
| editor   | editor |
+----------+--------+
```

### 查询角色菜单权限

```sql
SELECT r.code, m.title, m.permission 
FROM sys_role r 
JOIN sys_role_menu rm ON r.id = rm.role_id 
JOIN sys_menu m ON rm.menu_id = m.id 
ORDER BY r.id, m.sort;
```

---

## Redis 验证

### 查看 Token 缓存

```bash
docker exec -it ecommerce-redis redis-cli
```

**查看所有 Token Key**：
```redis
KEYS user:token:*
```

**查看某个 Token 的值（用户ID）**：
```redis
GET user:token:eyJhbGciOiJIUzI1NiJ9...
```

**查看 Token 过期时间（秒）**：
```redis
TTL user:token:eyJhbGciOiJIUzI1NiJ9...
```

---

## Nacos 验证

访问 Nacos 控制台：http://localhost:8848/nacos（nacos/nacos）

**服务管理 -> 服务列表**：
- ✅ `gateway` (端口 8080)
- ✅ `system-service` (端口 8081)

---

## 前后端联调测试

### 1. 启动后端

```bash
cd server
start.bat  # 或 ./start.sh
```

### 2. 配置前端

编辑 `admin/.env.development`:
```env
VITE_USE_MOCK=false
VITE_PROXY_TARGET=http://localhost:8080
```

### 3. 启动前端

```bash
cd admin
npm run dev
```

### 4. 测试流程

1. 访问 http://localhost:5173
2. 使用 `admin/123456` 登录
3. 检查左侧菜单是否正确显示：
   - ✅ 工作台
   - ✅ 系统管理 > 菜单管理
4. 点击"菜单管理"，检查是否有"新增"、"编辑"、"删除"按钮
5. 退出登录
6. 使用 `editor/123456` 登录
7. 点击"菜单管理"，检查按钮是否隐藏（editor 无操作权限）

---

## 故障排查

### 服务无法启动

```bash
# 查看服务状态
docker-compose ps

# 查看日志
docker-compose logs -f service-name
```

### 登录失败

1. 检查数据库是否初始化：
   ```bash
   docker exec -it ecommerce-mysql mysql -uroot -proot123456 -e "USE ecommerce_system; SELECT COUNT(*) FROM sys_user;"
   ```

2. 检查 Redis 连接：
   ```bash
   docker exec -it ecommerce-redis redis-cli PING
   ```

3. 检查 Nacos 服务注册：
   访问 http://localhost:8848/nacos

### Token 无效

1. 确认 JWT 密钥一致（Gateway 和 System Service 的 `jwt.secret` 配置）
2. 检查 Redis 中是否有对应的 Key
3. 确认 Token 未过期（默认 2 小时）

---

## 测试完成标志

所有测试通过后，你应该能够：

- ✅ 使用 admin 和 editor 账号登录
- ✅ 获取用户信息（含不同的角色和权限）
- ✅ 获取动态菜单（admin 和 editor 看到不同的按钮权限）
- ✅ Token 在 Redis 中正确缓存
- ✅ 登出后 Token 失效
- ✅ 前端 admin 能正常对接后端接口
- ✅ 权限控制生效（editor 看不到操作按钮）

**恭喜！ERP 后台用户登录注册部分 Docker 化完成！** 🎉
