package com.ecommerce.store.controller;

import com.ecommerce.common.core.domain.Result;
import com.ecommerce.store.dto.MemberRequests;
import com.ecommerce.store.entity.MemberUser;
import com.ecommerce.store.service.MemberAuthService;
import com.ecommerce.store.service.MemberEmailCodeService;
import com.ecommerce.store.service.MemberMailSender;
import com.ecommerce.store.vo.MemberSessionView;
import com.ecommerce.store.vo.MemberView;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 商城公开注册及本人资料接口。 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "商城会员认证")
public class MemberAuthController {
  private final MemberAuthService auth;
  private final MemberEmailCodeService codes;
  private final MemberMailSender mail;

  public record Capabilities(boolean emailRegistration, String mailMode) { }

  @GetMapping("/capabilities")
  public Result<Capabilities> capabilities() {
    return Result.success(new Capabilities(!"disabled".equals(mail.mode()), mail.mode()));
  }

  @PostMapping("/email-code")
  public Result<Void> emailCode(@Valid @RequestBody MemberRequests.EmailCode request, HttpServletRequest http) {
    codes.issue(request.email(), http.getRemoteAddr());
    return Result.success();
  }

  @PostMapping("/register")
  public Result<MemberView> register(@Valid @RequestBody MemberRequests.Register request, HttpServletRequest http) {
    return Result.success(auth.register(request, http.getRemoteAddr()));
  }

  @PostMapping("/login")
  public Result<MemberSessionView> login(@Valid @RequestBody MemberRequests.Login request, HttpServletRequest http) {
    return Result.success(auth.login(request, http.getRemoteAddr()));
  }

  @PostMapping("/logout")
  public Result<Void> logout(@RequestHeader("Authorization") String authorization) {
    auth.logout(authorization);
    return Result.success();
  }

  @GetMapping("/me")
  public Result<MemberView> me(@RequestAttribute("member") MemberUser member) {
    return Result.success(MemberView.from(member));
  }

  @PutMapping("/me")
  public Result<MemberView> profile(@RequestAttribute("member") MemberUser member,
      @Valid @RequestBody MemberRequests.Profile request) {
    return Result.success(auth.profile(member.getId(), request));
  }

  @PutMapping("/password")
  public Result<Void> password(@RequestAttribute("member") MemberUser member,
      @Valid @RequestBody MemberRequests.Password request) {
    auth.changePassword(member.getId(), request);
    return Result.success();
  }
}
