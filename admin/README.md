# 前后端 Docker Compose 联调

先启动 Docker Desktop（Linux containers），再分别启动后端和前端。

## 启动后端

在 `server/` 下，首次使用时将 `.env.example` 复制为 `.env.local`，填写 `RESEND_API_KEY`。该文件已被 Git 和 Docker 构建忽略，请勿提交真实密钥。Docker 模式使用环境变量，不读取 Java 本地开发用的 `application-local.yml`。

```powershell
cd server
# 仅首次创建，已有 .env.local 时不要覆盖
Copy-Item .env.example .env.local
# 编辑 .env.local，填写 RESEND_API_KEY 后运行
docker compose --env-file .env.local up -d --build
docker compose --env-file .env.local ps
docker compose --env-file .env.local logs --tail=100 gateway system-service
```

后端发布端口：网关 `8080`、系统服务 `8081`、MySQL `3306`、Redis `6379`、Nacos `8848` 和 `9848`。启动前请停止占用相同端口的本地 Java 服务或其他容器。

## 启动前端

在另一个终端进入 `admin/`：

```powershell
cd admin
docker compose up -d --build admin-web
```

访问 http://localhost:3000。Compose 默认关闭 Mock，真实请求路径为：

```text
浏览器 :3000/api/auth/login
  → 前端 Nginx → host.docker.internal:8080/api/auth/login
  → 后端 Gateway → system-service:8081/auth/login
```

前后端可以使用各自的 Compose 网络，通过网关发布到宿主机的 `8080` 端口通信。`host.docker.internal` 指宿主机；容器内的 `localhost` 指容器自身。

若此前使用过 Mock 登录，请退出原账号再登录真实账号。Mock 演示账号不代表数据库中一定存在相同账号或密码。

## 热更新开发模式

```powershell
docker compose --profile dev up -d admin-web-dev
```

访问 http://localhost:5173。Vite 也默认关闭 Mock，并代理到 `host.docker.internal:8080`。显式指定服务可避免同时启动生产预览容器。

修改生产预览的 Mock 开关或代理配置后，需要重新执行 `docker compose up -d --build admin-web`。

## 检查接口

```powershell
curl.exe -i http://localhost:3000/api/auth/login
```

登录接口仅支持 POST。若返回 HTTP 405，说明 GET 请求已到达后端；502 通常表示前端无法连接网关，503 可能表示网关未发现系统服务，404 需检查网关路由及 Nacos 下发配置。

当前后端源码尚无独立的 `/menu/list` 和 `/dashboard/summary` 控制器；即使代理连通，对应页面仍可能返回 404，需单独补齐接口。`/auth/menus` 已有控制器。
