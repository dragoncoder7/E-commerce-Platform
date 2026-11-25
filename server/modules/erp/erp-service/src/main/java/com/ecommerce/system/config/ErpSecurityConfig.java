package com.ecommerce.system.config;

import com.ecommerce.common.core.exception.BusinessException;
import com.ecommerce.common.redis.service.RedisService;
import com.ecommerce.common.security.util.JwtUtil;
import com.ecommerce.system.domain.SysUser;
import com.ecommerce.system.mapper.UserMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import java.util.List;
import java.util.Set;

/** ERP 接口鉴权独立于网关，直连业务端口也必须校验。 */
@Configuration
@RequiredArgsConstructor
public class ErpSecurityConfig implements WebMvcConfigurer, HandlerInterceptor {
  private final JwtUtil jwt;
  private final RedisService redis;
  private final UserMapper users;

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(this).addPathPatterns("/**")
        .excludePathPatterns("/auth/login", "/error", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/doc.html", "/webjars/**", "/media/**");
  }

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
    String authorization = request.getHeader("Authorization");
    if (authorization == null || !authorization.startsWith("Bearer ")) {
      throw new BusinessException(401, "请先登录后台");
    }
    String token = authorization.substring(7);
    if (!jwt.validateToken(token)) {
      throw new BusinessException(401, "后台会话无效");
    }
    Object cached = redis.get("erp:token:" + token);
    if (cached == null) {
      throw new BusinessException(401, "后台会话已过期");
    }
    Long userId = Long.valueOf(cached.toString());
    SysUser user = users.selectById(userId);
    List<String> roles = users.selectRoleCodesByUserId(userId);
    if (user == null || !Integer.valueOf(1).equals(user.getStatus()) || roles.isEmpty()) {
      throw new BusinessException(403, "后台账号不可用");
    }
    Number version = jwt.parseToken(token).get("version", Number.class);
    long currentVersion = user.getSessionVersion() == null ? 0L : user.getSessionVersion();
    if (version == null || version.longValue() != currentVersion) {
      throw new BusinessException(401, "后台会话已撤销，请重新登录");
    }
    request.setAttribute("actorId", userId);
    String path = request.getRequestURI();
    if (path.startsWith("/auth/") && !path.equals("/auth/register")) {
      return true;
    }
    String resource = path.split("/").length > 1 ? path.split("/")[1] : "unknown";
    boolean reading = Set.of("GET", "HEAD").contains(request.getMethod());
    String permission = resource + (reading ? ":read" : ":write");
    if (!roles.contains("admin") && !users.selectPermissionsByUserId(userId).contains(permission)) {
      throw new BusinessException(403, "没有执行此操作的权限");
    }
    return true;
  }
}
