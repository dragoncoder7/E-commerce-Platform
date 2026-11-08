# Gateway 依赖冲突修复说明

## 问题现象

启动 Gateway 时报错：

```
***************************
APPLICATION FAILED TO START
***************************

Description:

Spring MVC found on classpath, which is incompatible with Spring Cloud Gateway.

Action:

Please set spring.main.web-application-type=reactive or remove spring-boot-starter-web dependency.
```

---

## 问题原因

### 根本原因

**Spring Cloud Gateway** 基于 **Spring WebFlux**（响应式编程模型），而 **Spring MVC** 是传统的 Servlet 容器模型，两者**不兼容**。

### 依赖传递链

```
gateway
├── common-core
│   └── spring-boot-starter-web ← 引入了 Spring MVC
└── common-security
    └── spring-boot-starter-web ← 也引入了 Spring MVC
```

因为 `common-core` 和 `common-security` 都依赖了 `spring-boot-starter-web`，导致 Gateway 间接引入了 Spring MVC。

---

## 解决方案

### 方案：排除依赖

在 Gateway 的 `pom.xml` 中，排除从 `common-core` 和 `common-security` 传递过来的 Spring MVC 依赖：

```xml
<dependencies>
    <!-- Common Core (排除 Spring MVC，Gateway 使用 WebFlux) -->
    <dependency>
        <groupId>com.ecommerce</groupId>
        <artifactId>common-core</artifactId>
        <exclusions>
            <exclusion>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-starter-web</artifactId>
            </exclusion>
            <exclusion>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-starter-tomcat</artifactId>
            </exclusion>
        </exclusions>
    </dependency>

    <!-- Common Security (排除 Spring MVC) -->
    <dependency>
        <groupId>com.ecommerce</groupId>
        <artifactId>common-security</artifactId>
        <exclusions>
            <exclusion>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-starter-web</artifactId>
            </exclusion>
            <exclusion>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-starter-tomcat</artifactId>
            </exclusion>
        </exclusions>
    </dependency>

    <!-- Spring Cloud Gateway -->
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-gateway</artifactId>
    </dependency>
    
    <!-- 其他依赖... -->
</dependencies>
```

---

## Spring MVC vs WebFlux 对比

| 特性 | Spring MVC | Spring WebFlux |
|------|-----------|----------------|
| **编程模型** | 命令式（阻塞） | 响应式（非阻塞） |
| **容器** | Servlet 容器（Tomcat） | Netty / Reactor |
| **适用场景** | 传统 REST API | 高并发、流式数据 |
| **Gateway** | ❌ 不支持 | ✅ 必须使用 |

### Gateway 必须使用 WebFlux

```
Gateway 本身就是一个路由转发服务：

客户端 → Gateway (WebFlux) → 下游服务 (MVC/WebFlux 都行)
         ↑ 响应式处理
         ↑ 非阻塞 I/O
         ↑ 高并发性能
```

---

## 验证修复

### 1. 清理并重新编译

```bash
cd D:\project\code\E-commerce-Platform\server

# 清理
mvn clean

# 编译 Gateway
mvn compile -pl gateway -am
```

### 2. 验证依赖树

```bash
# 查看 Gateway 的依赖树
mvn dependency:tree -pl gateway

# 确认没有 spring-boot-starter-web
mvn dependency:tree -pl gateway | findstr "spring-boot-starter-web"
# 应该没有输出
```

### 3. 启动测试

```bash
# 方式 1: Maven 启动
cd gateway
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# 方式 2: IDEA 启动
# 运行 GatewayApplication.main()
```

### 4. 验证成功标志

看到以下日志表示启动成功：

```
Netty started on port 8080
Started GatewayApplication in X.XXX seconds
```

**注意**：不再是 `Tomcat initialized with port`，而是 `Netty`。

---

## 其他模块说明

### System Service 可以使用 Spring MVC

```xml
<!-- system-service/pom.xml -->
<dependencies>
    <!-- Common Core (不需要排除，Service 层用 MVC) -->
    <dependency>
        <groupId>com.ecommerce</groupId>
        <artifactId>common-core</artifactId>
    </dependency>
    
    <!-- Spring Web (传统 REST API) -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
</dependencies>
```

### 架构说明

```
┌─────────────────────────────────────────┐
│  Gateway (WebFlux)                      │
│  - 路由转发                              │
│  - 统一认证                              │
│  - 限流熔断                              │
└─────────┬───────────────────────────────┘
          │
          ├─────────────────────┬────────────────────┐
          │                     │                    │
  ┌───────▼────────┐   ┌────────▼───────┐   ┌──────▼──────┐
  │ System Service │   │  User Service  │   │ Order Svc   │
  │ (Spring MVC)   │   │ (Spring MVC)   │   │ (MVC)       │
  └────────────────┘   └────────────────┘   └─────────────┘
```

- **Gateway**: 必须用 WebFlux（响应式）
- **下游服务**: 可以用 Spring MVC（传统）

---

## 常见问题

### Q1: 为什么不在 common-core 中排除？

**A**: 因为其他服务（system-service、user-service 等）需要使用 Spring MVC。只有 Gateway 需要排除。

### Q2: 排除后会影响功能吗？

**A**: 不会。WebFlux 提供了类似的功能：

| Spring MVC | WebFlux 等价物 |
|-----------|---------------|
| `@RestController` | `@RestController` (相同) |
| `HttpServletRequest` | `ServerHttpRequest` |
| `HttpServletResponse` | `ServerHttpResponse` |
| `Filter` | `WebFilter` |

### Q3: 如何判断当前用的是 MVC 还是 WebFlux？

**A**: 看启动日志：

```bash
# Spring MVC
Tomcat initialized with port 8080

# Spring WebFlux
Netty started on port 8080
```

### Q4: common-core 的全局异常处理怎么办？

**A**: WebFlux 有自己的全局异常处理机制：

```java
// Spring MVC
@ControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result> handleException(Exception e) {
        // ...
    }
}

// WebFlux
@Component
@Order(-2)  // 优先级高于默认处理器
public class GlobalErrorWebExceptionHandler extends AbstractErrorWebExceptionHandler {
    // ...
}
```

---

## 总结

✅ **已修复**: Gateway 的 pom.xml 中排除了 Spring MVC 依赖  
✅ **不影响**: 其他服务仍然可以使用 Spring MVC  
✅ **架构合理**: Gateway 用 WebFlux，下游服务用 MVC  

---

## 参考资料

- [Spring Cloud Gateway 官方文档](https://docs.spring.io/spring-cloud-gateway/docs/current/reference/html/)
- [Spring WebFlux 官方文档](https://docs.spring.io/spring-framework/docs/current/reference/html/web-reactive.html)
- [依赖冲突排查工具](https://maven.apache.org/plugins/maven-dependency-plugin/tree-mojo.html)
