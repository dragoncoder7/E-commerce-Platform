package com.ecommerce.store.config;

import com.ecommerce.store.service.MemberAuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** 商城保护接口默认拒绝匿名访问，不信任请求中的会员 ID。 */
@Configuration
@RequiredArgsConstructor
public class MemberSecurityConfig implements WebMvcConfigurer, HandlerInterceptor {
  private final MemberAuthService auth;

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(this).addPathPatterns("/**").excludePathPatterns(
        "/auth/login", "/auth/register", "/auth/email-code", "/auth/capabilities", "/catalog/**",
        "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/doc.html", "/webjars/**", "/error", "/media/**");
  }

  @Override
  public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
    request.setAttribute("member", auth.authenticate(request.getHeader("Authorization")));
    return true;
  }
}
