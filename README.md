# E-commerce-Platform

> 自己做的一个电商平台。

本仓库采用**多目录（monorepo）**方式组织，各模块独立成子目录、各自拥有构建与部署配置，互不干扰：

| 目录 | 模块 | 技术栈 | 状态 |
| --- | --- | --- | --- |
| [`admin/`](./admin) | ERP 管理后台前端 | Vue 3 + TypeScript + Element Plus + Tailwind CSS + Docker | ✅ 已实现 |
| `web/` | Web 模块（商城前端） | 待定 | ⏳ 规划中 |
| `server/` | 后端微服务 | SpringBoot、SpringCloud、Sentinel、Spring Gateway、Nacos、Seata、MySQL、Redis | ⏳ 规划中 |

> 根目录只保留仓库级文件（README、`.gitignore`、`.editorconfig` 等），不放置任何工程代码。

---

## 整体架构

### 模块划分

- **ERP 模块**
  - 前端 → `admin/`（本仓库当前内容），面向平台运营/管理人员的后台中台，承载商品、订单、营销、系统设置等管理能力
  - 后端 → 与 Web 模块共用同一套微服务
- **Web 模块**
  - 前端 → 面向 C 端买家的商城站点
  - 后端：SpringBoot、SpringCloud、Sentinel、Spring Gateway、Nacos、Seata、MySQL、Redis

### 前端在架构中的位置

```
浏览器
  │
  ├─ ERP 管理后台 admin/（Nginx 托管静态资源）
  │      │  /api/**
  │      ▼
  └─ Spring Gateway ──► 各业务微服务（Nacos 注册 / Sentinel 保护 / Seata 事务）
                              │
                              └── MySQL / Redis
```

前端通过 `VITE_API_BASE_URL`（默认 `/api`）+ 开发代理 / Nginx 反代接入网关，业务代码无需感知后端服务拆分。

---

## 仓库结构

```
E-commerce-Platform/
├── README.md                 # 仓库级说明（本文件）
├── .gitignore                # 全仓库忽略规则
├── .editorconfig             # 全仓库编码风格
├── .gitattributes            # 换行符规范化
└── admin/                    # ERP 管理后台前端（独立工程）
    ├── Dockerfile            # 多阶段构建（上下文 = admin/）
    ├── docker-compose.yml    # 生产 + dev 两套服务
    ├── nginx/default.conf
    ├── package.json
    ├── vite.config.ts
    ├── tsconfig*.json
    ├── .env.development / .env.production / .env.example
    └── src/
```

各子目录的 Docker / npm 命令都在**该子目录内**执行，仓库根目录不提供聚合脚本，避免模块间耦合。

---

## 一、admin 技术栈

| 分类 | 选型 |
| --- | --- |
| 框架 | Vue 3.5（`<script setup>` + Composition API） |
| 语言 | TypeScript 5.9（strict 模式） |
| 构建 | Vite 7 |
| UI | Element Plus 2.14 + `@element-plus/icons-vue` |
| 样式 | Tailwind CSS 4（`@tailwindcss/vite`） |
| 状态 | Pinia 3 |
| 路由 | Vue Router 4（history 模式 + 动态路由） |
| 请求 | axios 1.x（统一拦截器封装） |
| 容器 | Docker 多阶段构建 + Nginx 1.27 / node:22-alpine |
| 其他 | nprogress（路由进度条） |

---

## 二、admin 页面说明

| 路由 | 页面 | 说明 |
| --- | --- | --- |
| `/login` | 登录页 | 账号密码登录、记住账号、表单校验、演示账号一键填充 |
| `/dashboard` | 工作台 | 登录后默认落地页：经营指标、快捷入口、待办、公告、运行环境 |
| `/system/menu` | 菜单管理 | 树形表格 + 新增/编辑/删除/启停，**保存后自动重建动态路由**，侧边栏实时同步 |
| `/:pathMatch(.*)*` | 404 | 布局内兜底页，菜单组件路径填写错误时仍保留导航 |

演示账号（mock 模式）：

```
账号：admin   密码：123456   （超级管理员，可见全部按钮）
账号：editor  密码：123456   （运营，仅列表权限，用于验证权限控制）
```

---

## 三、快速开始（Docker）

> 所有环境都在容器里，宿主机不需要装 Node。以下命令均在 `admin/` 目录执行。

```bash
cd admin
```

### 1. 生产预览（Nginx 托管构建产物）

```bash
docker compose up -d --build
# 打开 http://localhost:8080
```

### 2. 开发模式（容器内跑 Vite dev server，支持热更新）

```bash
docker compose --profile dev up
# 打开 http://localhost:5173
```

首次启动会在命名卷 `erp-admin_admin_node_modules` 中安装依赖，之后重启不会重复安装。

### 3. 常用运维命令

```bash
cd admin
docker compose logs -f admin-web          # 查看日志
docker compose down                       # 停止并移除容器
docker compose build --no-cache admin-web # 无缓存重建
docker volume rm erp-admin_admin_node_modules  # 清空依赖卷（依赖变更后使用）
```

Docker 相关文件的路径都以 `admin/` 为根，如需把整个仓库挂进构建上下文，请改 `docker-compose.yml` 的 `context`。

---

## 四、admin 目录结构

```
admin/
├── docker-compose.yml          # 生产（默认）+ 开发（--profile dev）两套服务
├── Dockerfile                  # deps -> build -> runtime(nginx) 多阶段
├── nginx/default.conf          # SPA history 回退、静态资源缓存、/api 反向代理
├── .env.development            # 开发环境变量（mock 开启）
├── .env.production             # 生产环境变量（mock 关闭）
├── index.html                  # 首屏骨架屏（避免白屏）
└── src
    ├── api/                    # 接口定义（auth / menu / dashboard）
    ├── components/             # 通用组件（预留）
    ├── config/                 # 应用常量：标题、版本、演示账号
    ├── layouts/                # 主框架：侧边栏 + 顶栏 + 内容区
    │   └── components/         # AppSidebar / AppHeader / SidebarMenu（递归菜单）
    ├── mock/                   # 内置 mock（axios adapter 实现，零依赖）
    ├── router/
    │   ├── index.ts            # createRouter
    │   ├── routes.ts           # 静态路由（登录 / 工作台 / 404）
    │   └── guard.ts            # 登录校验 + 动态路由注册
    ├── stores/                 # Pinia：app / user / permission
    ├── styles/index.css        # Tailwind 4 + Element Plus 分层入口
    ├── types/                  # api / menu / router 类型
    ├── utils/
    │   ├── request.ts          # axios 封装（详见第八节）
    │   ├── auth.ts             # token / 记住账号
    │   └── icons.ts            # 图标解析与可选项
    └── views/
        ├── login/index.vue
        ├── dashboard/index.vue
        ├── system/menu/index.vue
        └── error/404.vue
```

---

## 五、环境变量（admin）

| 变量 | 说明 | 开发默认 | 生产默认 |
| --- | --- | --- | --- |
| `VITE_APP_TITLE` | 应用标题 | 云商 ERP | 云商 ERP |
| `VITE_APP_VERSION` | 版本号 | 0.1.0 | 0.1.0 |
| `VITE_API_BASE_URL` | 接口前缀 | `/api` | `/api` |
| `VITE_USE_MOCK` | 是否启用内置 mock | `true` | `false` |
| `VITE_PROXY_TARGET` | 开发代理目标（后端网关） | `http://localhost:8080` | - |

Docker 构建时可通过 build args 覆盖，`admin/docker-compose.yml` 中默认把 `VITE_USE_MOCK` 设为 `true`，
因此镜像开箱即用；后端就绪后把它改成 `false` 重新构建即可。

---

## 六、Mock 与后端联调

### 6.1 mock 实现方式

`admin/src/mock/index.ts` 用 **axios adapter** 劫持请求，不依赖 mockjs / vite-plugin-mock：

- 支持 `:id` 形式的路径参数、query 过滤、JSON body
- 未命中的接口返回 404 并提示 `Mock 接口未定义：GET /xxx`，方便发现漏配
- 菜单数据持久化在 `localStorage` 的 `erp:mock:menus`，刷新后仍保留菜单管理页的改动
- 调试时可执行 `window.__resetErpMock()` 还原初始菜单

### 6.2 切换真实后端（三步）

1. 把 `admin/.env.development` / `admin/.env.production` 的 `VITE_USE_MOCK` 改为 `false`
2. 开发环境把 `VITE_PROXY_TARGET` 指向后端网关（`admin/vite.config.ts` 会把 `/api` 代理过去）
3. 生产环境修改 `admin/nginx/default.conf` 中 `location /api/` 的 `proxy_pass`

### 6.3 后端接口约定

统一响应体（`code` 为 `0` 或 `200` 表示成功）：

```json
{ "code": 200, "message": "ok", "data": {}, "timestamp": 1762500000000 }
```

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/auth/login` | 入参 `{ username, password }`，返回 `{ token, expiresIn }` |
| POST | `/auth/logout` | 退出登录 |
| GET | `/auth/userinfo` | 当前用户信息（含 `roles`、`permissions`） |
| GET | `/auth/menus` | 当前用户菜单树（生成动态路由） |
| GET | `/dashboard/summary` | 工作台概览数据 |
| GET | `/menu/list` | 菜单列表（支持 `title` / `type` / `status`） |
| POST | `/menu` | 新增菜单 |
| PUT | `/menu/:id` | 修改菜单 |
| DELETE | `/menu/:id` | 删除菜单（有子节点时返回业务错误） |

菜单字段：`id / parentId / name / title / type(catalog|menu|button) / path / component / icon / permission / sort / visible / keepAlive / status / createTime`。
其中 `component` 为相对 `admin/src/views` 的路径，例如 `system/menu/index`。

---

## 七、动态路由工作原理

```
登录成功 → 存 token
   ↓
路由守卫（无 token 且非白名单）→ 重定向 /login?redirect=xxx
   ↓ 有 token 且路由未初始化
GET /auth/userinfo + GET /auth/menus
   ↓
permission.generateRoutes(menus)
   ├─ 过滤 button 类型与停用菜单
   ├─ component 字符串 → import.meta.glob('../../views/**/*.vue') 中的异步组件（找不到降级 404）
   ├─ catalog → 布局容器 + 子路由；顶层 menu → 自动包一层布局
   └─ router.addRoute() 注册
   ↓
return { path: to.path, replace: true } → 重新匹配目标路由
   ↓
侧边栏 sidebarMenus = 静态路由（工作台）+ 动态菜单
```

**新增一个页面**：先在 `admin/src/views` 下建 `xxx/index.vue`，再到「菜单管理」新增菜单，
路由路径填 `/xxx`、组件路径填 `xxx/index`，保存后侧边栏立即出现，无需改代码。

菜单增删改后 `permission.rebuild()` 会精确注销旧路由（按 name 记录）再重新注册，避免残留脏路由。

---

## 八、请求层封装（`admin/src/utils/request.ts`）

| 能力 | 说明 |
| --- | --- |
| Token 注入 | 自动加 `Authorization: Bearer xxx`，可用 `withToken: false` 关闭 |
| 重复请求取消 | 相同 `method + url + params + data` 时取消前一个；`allowRepeat: true` 可放行 |
| 业务码处理 | `0 / 200` 解包 `data`，其余统一提示并 reject |
| 401 兜底 | 清除 token、去重跳转 `/login?redirect=...`（整页跳转，彻底清理 store 与动态路由） |
| 网络异常 | 超时 / 断网 / 5xx 分场景文案；`showError: false` 可单次静默 |
| 文件下载 | `http.download(url, params, filename)` |

使用示例：

```ts
import { http } from '@/utils/request'

interface Row { id: number; title: string }

const rows = await http.get<Row[]>('/menu/list', { title: '订单' })
await http.post('/menu', payload)
await http.delete(`/menu/${id}`, undefined, { showError: false })
```

---

## 九、样式方案说明

`admin/src/styles/index.css` 使用 Tailwind 4 的原生级联层，把 Element Plus 放进 `components` 层：

```css
@layer theme, base, components, utilities;

@import 'tailwindcss/theme.css' layer(theme);
@import 'tailwindcss/preflight.css' layer(base);
@import 'tailwindcss/utilities.css' layer(utilities);
@import 'element-plus/dist/index.css' layer(components);
```

这样 `utilities` 层永远压过组件库样式，可以直接给 Element Plus 组件写 `class="h-11 w-full"`，
不需要 `!important`；同时 `preflight` 的 reset 又不会破坏组件内部样式。

主题色在 `:root` 中覆盖 `--el-color-primary` 等变量（品牌色 `#2563eb`）。

---

## 十、可选：本地命令（非必需）

Docker 是唯一推荐运行方式。若本机已装 Node 22+，也可在 `admin/` 下直接跑：

```bash
cd admin
npm install
npm run dev        # http://localhost:5173
npm run build      # vue-tsc 类型检查 + 构建到 admin/dist
npm run preview    # 预览 admin/dist
npm run typecheck  # 仅类型检查
```

---

## 十一、后续可扩展

按整体架构规划，后续依次推进：

**ERP 模块（`admin/`）**

- 商品 / 订单 / 库存 / 营销等业务页面（沿用菜单管理配置的组件路径即可挂载）
- 用户 / 角色 / 部门管理页与按钮级权限指令 `v-permission`
- 标签页（tags-view）与页面缓存（`keepAlive` 字段已预留）
- 登录验证码、刷新 token（`refreshToken` 字段已预留）
- 表格列配置、导出 Excel、暗色主题

**Web 模块（`web/`）**

- 商城前台站点，与 `admin/` 并列独立构建

**后端（`server/`）**

- Spring Cloud Gateway 统一网关 + Nacos 注册配置中心
- Sentinel 限流熔断、Seata 分布式事务
- 与 `admin/`、`web/` 通过网关 `/api` 对接
