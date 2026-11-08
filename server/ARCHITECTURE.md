# 项目架构总览

## 已完成的模块

### ✅ 后端框架搭建完成

```
server/
├── pom.xml                                    # Maven 父工程
│
├── gateway/                                   # 网关服务 (8080)
│   ├── pom.xml
│   ├── src/main/java/
│   │   └── com/ecommerce/gateway/
│   │       └── GatewayApplication.java
│   └── src/main/resources/
│       └── application.yml
│
├── common/                                    # 公共模块
│   ├── common-core/                           # 核心模块
│   │   ├── pom.xml
│   │   └── src/main/java/com/ecommerce/common/core/
│   │       ├── domain/
│   │       │   ├── Result.java               ✅ 统一返回体
│   │       │   └── PageResult.java           ✅ 分页结果
│   │       ├── constant/
│   │       │   └── ResultCode.java           ✅ 返回码常量
│   │       └── exception/
│   │           ├── BusinessException.java    ✅ 业务异常
│   │           └── GlobalExceptionHandler.java ✅ 全局异常处理
│   │
│   ├── common-redis/                          # Redis 模块
│   │   ├── pom.xml
│   │   └── src/main/java/com/ecommerce/common/redis/
│   │       ├── config/
│   │       │   └── RedisConfig.java          ✅ Redis 配置
│   │       └── service/
│   │           └── RedisService.java         ✅ Redis 工具类
│   │
│   ├── common-security/                       # 安全模块
│   │   ├── pom.xml
│   │   └── src/main/java/com/ecommerce/common/security/
│   │       └── util/
│   │           └── JwtUtil.java              ✅ JWT 工具类
│   │
│   └── common-api/                            # API 定义
│       └── pom.xml
│
└── modules/                                   # 业务服务
    └── system/                                # 系统服务 (8081)
        ├── system-api/                        # API 接口
        │   ├── pom.xml
        │   └── src/main/java/com/ecommerce/system/api/
        │       ├── dto/
        │       │   └── LoginDTO.java         ✅ 登录请求
        │       └── vo/
        │           ├── LoginVO.java          ✅ 登录响应
        │           ├── UserInfoVO.java       ✅ 用户信息
        │           └── MenuVO.java           ✅ 菜单数据
        │
        └── system-service/                    # 服务实现
            ├── pom.xml
            ├── src/main/java/com/ecommerce/system/
            │   ├── SystemApplication.java    ✅ 启动类
            │   ├── controller/
            │   │   └── AuthController.java   ✅ 认证控制器
            │   └── service/
            │       ├── IAuthService.java     ✅ 认证服务接口
            │       └── impl/
            │           └── AuthServiceImpl.java ✅ 认证服务实现(Mock)
            └── src/main/resources/
                └── application.yml            ✅ 配置文件
```

## 已实现的功能

### 1. 公共模块
- ✅ **common-core**: 统一返回体、分页、异常处理
- ✅ **common-redis**: Redis 配置与封装
- ✅ **common-security**: JWT 生成与验证
- ✅ **common-api**: 通用 API 定义基础

### 2. 网关服务 (gateway)
- ✅ Spring Cloud Gateway 配置
- ✅ Nacos 服务发现集成
- ✅ Sentinel 限流配置
- ✅ 路由规则配置（/api/** 转发到 system-service）
- ⏳ JWT 认证过滤器（待实现）

### 3. 系统服务 (system-service)
已实现接口（使用 Mock 数据）:
- ✅ `POST /auth/login` - 用户登录
- ✅ `POST /auth/logout` - 用户登出
- ✅ `GET /auth/userinfo` - 获取用户信息
- ✅ `GET /auth/menus` - 获取用户菜单树

待实现接口:
- ⏳ `GET /dashboard/summary` - 工作台数据
- ⏳ `GET /menu/list` - 菜单列表
- ⏳ `POST /menu` - 新增菜单
- ⏳ `PUT /menu/:id` - 修改菜单
- ⏳ `DELETE /menu/:id` - 删除菜单

## 技术栈

| 类别 | 技术 | 版本 |
|------|------|------|
| 基础框架 | Spring Boot | 3.2.0 |
| 微服务 | Spring Cloud | 2023.0.0 |
| 微服务组件 | Spring Cloud Alibaba | 2023.0.1.0 |
| 服务网关 | Spring Cloud Gateway | - |
| 注册中心 | Nacos | - |
| 限流熔断 | Sentinel | - |
| ORM | MyBatis Plus | 3.5.18 |
| 数据库 | MySQL | 8.0.33 |
| 缓存 | Redis | - |
| 连接池 | Druid | 1.2.20 |
| 认证 | JWT (jjwt) | 0.12.3 |
| 工具类 | Hutool | 5.8.23 |
| JSON | Fastjson2 | 2.0.43 |

## 下一步计划

### 阶段 1: 数据库层
- [X] 设计数据库表结构
- [X] 创建建表 SQL 脚本
- [X] 实现 MyBatis Mapper
- [X] 替换 Mock 实现为真实数据查询

### 阶段 2: 完善系统服务
- [ ] 菜单管理 CRUD
- [ ] 用户管理
- [ ] 角色管理
- [ ] 权限分配

### 阶段 3: 新增业务服务
- [ ] 商品服务 (product-service)
- [ ] 订单服务 (order-service)
- [ ] 库存服务 (inventory-service)
- [ ] 支付服务 (payment-service)

### 阶段 4: 完善基础设施
- [ ] Gateway JWT 认证过滤器
- [ ] Sentinel 流控规则配置
- [ ] Seata 分布式事务集成
- [ ] 统一日志处理
- [ ] 监控与链路追踪

## 前后端对接

前端项目 `admin/` 已实现，当前使用 Mock 数据。

**联调步骤**:
1. 启动后端服务（Gateway + System Service）
2. 修改 `admin/.env.development`:
   ```
   VITE_USE_MOCK=false
   VITE_PROXY_TARGET=http://localhost:8080
   ```
3. 启动前端: `cd admin && npm run dev`
4. 访问 http://localhost:5173

**测试账号**: 
- admin / 123456
- editor / 123456
