# Nacos 注册不上 / 控制台看不到服务 —— 排查指南

## 你遇到的问题

网关和系统服务都配置了：

```yaml
spring:
  cloud:
    nacos:
      discovery:
        server-addr: 127.0.0.1:8848
        namespace: dev
```

但是 Nacos 控制台里看不到服务，即"没监听到"。

---

## 根因 1：namespace 必须填「命名空间 ID」，不是名称（最常见）

### Nacos 的 namespace 规则

| 项目 | 值 |
|------|-----|
| 默认命名空间 | `public` |
| `public` 的命名空间 **ID** | 空字符串 `""` |
| 手工新建命名空间的 ID | 默认是自动生成的 UUID，例如 `a1b2c3d4-....` |

**关键点**：`namespace: dev` 中的 `dev` 会被当作 **namespace ID** 传给 Nacos 服务端。

如果控制台里没有 **ID 恰好等于 `dev`** 的命名空间：

- 服务会尝试注册到一个不存在的 tenant
- 控制台在 `public` 视角下 **完全看不到** 这些服务
- 客户端有时不报错（静默失败），所以表现为"没监听到"

### 正确做法（二选一）

#### 方案 A：使用 public（开箱即用，推荐先这样验证）

```yaml
namespace: public
```

或者干脆不写 `namespace`。

#### 方案 B：真正使用 dev 命名空间

1. 打开 http://localhost:8848/nacos
2. 左侧「命名空间」→「新建命名空间」
3. **命名空间 ID 手动填 `dev`**（不要留空让它自动生成 UUID！）
4. 名称可填「开发环境」，然后保存
5. 再在配置里保留 `namespace: dev`

> ⚠️ 如果第 3 步让 Nacos 自动生成了 UUID，那么配置里必须写那个 UUID，写 `dev` 是无效的。

---

## 根因 2：dataId 扩展名与 file-extension 不一致

你的原配置：

```yaml
spring:
  config:
    import:
      - optional:nacos:${spring.application.name}.yaml   # ← 拉取 gateway.yaml
  cloud:
    nacos:
      config:
        file-extension: yml                              # ← 但声明的是 yml
```

`spring.config.import` 里的 dataId 是**字面量**，`file-extension` 只影响自动推导的 dataId。
两者不一致会导致拉取的是 `gateway.yaml`，而你的配置在控制台里通常叫 `gateway.yml`。

之前启动日志里已经出现过这个证据：

```
WARN c.a.cloud.nacos.configdata.NacosConfigDataLoader
  - [Nacos Config] config[dataId=gateway.yaml, group=DEFAULT_GROUP] is empty
```

因为写了 `optional:` 前缀，失败被静默忽略，所以"配置没生效"但也不报错。

**已修复**：统一为 `yml`。

---

## 根因 3：Nacos 2.x 需要 9848 端口（gRPC）

Nacos 2.x 客户端走 gRPC，除 8848 外还需要：

| 端口 | 用途 |
|------|------|
| 8848 | HTTP（控制台 / OpenAPI） |
| 9848 | gRPC（客户端注册与配置） |
| 9849 | gRPC（集群内部通信，单机可忽略） |

`docker-compose.yml` 已正确映射：

```yaml
ports:
  - "8848:8848"
  - "9848:9848"
```

如果漏了 9848，典型报错是：

```
com.alibaba.nacos.shaded.io.grpc.StatusRuntimeException: UNAVAILABLE
Client not connected, current status: STARTING
```

---

## 根因 4：profile 覆盖导致配置没被读到

原先 `application.yml` 里硬编码了：

```yaml
spring:
  profiles:
    active: docker
```

这会让本地 IDEA 启动时也激活 `docker` profile。若同时存在 `application-docker.yml`，其中的 `server-addr` / `namespace` 会覆盖你看到的这段配置。

**已修复**：移除硬编码，改由运行环境注入（docker-compose 里已通过 `SPRING_PROFILES_ACTIVE` 设置）。

---

## 已修改的配置

`server/gateway/src/main/resources/application.yml`
`server/modules/system/system-service/src/main/resources/application.yml`

```yaml
spring:
  application:
    name: gateway
  config:
    import:
      - optional:nacos:${spring.application.name}.yml   # 与 file-extension 一致
  cloud:
    nacos:
      discovery:
        server-addr: ${SPRING_CLOUD_NACOS_DISCOVERY_SERVER_ADDR:127.0.0.1:8848}
        namespace: ${NACOS_NAMESPACE:public}              # 默认 public，可被环境变量覆盖
        group: DEFAULT_GROUP
        register-enabled: true
      config:
        server-addr: ${SPRING_CLOUD_NACOS_CONFIG_SERVER_ADDR:127.0.0.1:8848}
        namespace: ${NACOS_NAMESPACE:public}
        group: DEFAULT_GROUP
        file-extension: yml
```

好处：

- 本地开发默认 `public`，不需要先建命名空间就能注册成功
- 需要切环境时不改代码，只传环境变量：`NACOS_NAMESPACE=dev`
- docker-compose 可通过 `environment` 注入

---

## 如何确认注册成功

### 1. 看启动日志（最直接）

注册**成功**的日志类似：

```
INFO  c.a.c.n.registry.NacosServiceRegistry - nacos registry, DEFAULT_GROUP gateway 192.168.x.x:8080 register finished
```

注册**失败**时会出现：

```
ERROR c.a.c.n.registry.NacosServiceRegistry - nacos registry, DEFAULT_GROUP gateway ... register failed
```

或者 gRPC 连接异常（见根因 3）。

### 2. 看控制台「命名空间」是否选对

控制台左上角有命名空间下拉框。**默认停在 `public`**，如果你把服务注册在 `dev`，必须切到 `dev` 才能看到。

这是"明明注册了却看不到"最常见的误判来源。

### 3. 用 OpenAPI 直接查（绕过界面）

```powershell
# 查 public 命名空间下的服务列表
curl "http://127.0.0.1:8848/nacos/v1/ns/service/list?pageNo=1&pageSize=20"

# 查 dev 命名空间（tenant 参数 = 命名空间 ID）
curl "http://127.0.0.1:8848/nacos/v1/ns/service/list?pageNo=1&pageSize=20&namespaceId=dev"
```

返回 `"doms":["gateway","system-service"]` 说明注册正常。

### 4. 确认 Nacos 端口连通

```powershell
Test-NetConnection -ComputerName 127.0.0.1 -Port 8848
Test-NetConnection -ComputerName 127.0.0.1 -Port 9848
```

两个都要 `TcpTestSucceeded : True`。

---

## 常见错误日志对照表

| 日志关键字 | 原因 | 处理 |
|-----------|------|------|
| `config[...] is empty` | dataId 与 file-extension 不一致，或配置不存在 | 统一扩展名，或忽略（optional 无害） |
| `register failed` | 命名空间不存在 / 服务端异常 | 检查 namespace ID |
| `UNAVAILABLE` / `Client not connected` | 9848 端口不通 | 映射 9848，检查防火墙 |
| `403` / `no right` | Nacos 开启鉴权但未配账号 | 配 `username` / `password` |
| 无任何 Nacos 日志 | 依赖未引入或自动注册被关闭 | 检查 `spring-cloud-starter-alibaba-nacos-discovery`、`register-enabled` |
| 控制台看不到但 API 能查到 | 命名空间下拉框选错 | 切换命名空间 |

---

## 快速验证流程

```powershell
# 1. 确认基础服务在跑
docker ps --filter "name=ecommerce-nacos"

# 2. 确认端口连通
Test-NetConnection 127.0.0.1 -Port 8848
Test-NetConnection 127.0.0.1 -Port 9848

# 3. 用 API 看有没有服务（不依赖界面）
curl "http://127.0.0.1:8848/nacos/v1/ns/service/list?pageNo=1&pageSize=20"

# 4. 重启服务，盯注册日志
#    在 IDEA 控制台搜索 "nacos registry"
```

---

## 切换到 dev 命名空间的完整步骤

```powershell
# 1. 控制台新建命名空间，ID 必须手填 dev
#    http://localhost:8848/nacos → 命名空间 → 新建

# 2. 启动时注入环境变量（IDEA 的 Run Configuration → Environment variables）
NACOS_NAMESPACE=dev

# 3. 或用 docker-compose 注入
#    environment:
#      NACOS_NAMESPACE: dev

# 4. 控制台左上角切到 dev 命名空间查看
```
