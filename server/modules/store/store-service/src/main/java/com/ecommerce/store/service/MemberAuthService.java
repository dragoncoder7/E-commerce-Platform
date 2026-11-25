package com.ecommerce.store.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ecommerce.common.core.exception.BusinessException;
import com.ecommerce.common.security.util.JwtUtil;
import com.ecommerce.store.dto.MemberRequests;
import com.ecommerce.store.entity.MemberUser;
import com.ecommerce.store.mapper.MemberUserMapper;
import com.ecommerce.store.vo.MemberSessionView;
import com.ecommerce.store.vo.MemberView;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 会员注册和会话服务；身份取自签名且存活的商城会话。 */
@Service
@RequiredArgsConstructor
public class MemberAuthService {
  private static final BCryptPasswordEncoder PASSWORDS = new BCryptPasswordEncoder(12);
  private static final String DUMMY_HASH = PASSWORDS.encode("unused-password-for-timing");
  private final MemberUserMapper members;
  private final JwtUtil jwt;
  private final StringRedisTemplate redis;
  private final MemberRateLimiter limits;
  private final MemberEmailCodeService codes;

  @Transactional(rollbackFor = Exception.class)
  public MemberView register(MemberRequests.Register request, String ip) {
    limits.check("register", ip, 30, 3600);
    validatePassword(request.password());
    String account = request.account().trim().toLowerCase(Locale.ROOT);
    MemberUser member = new MemberUser();
    switch (request.type()) {
      case "account" -> {
        if (!account.matches("[a-z][a-z0-9_]{3,31}")) {
          throw new BusinessException(400, "账号须为 4–32 位字母、数字或下划线，且以字母开头");
        }
        member.setUsername(account);
      }
      case "phone" -> {
        if (!account.matches("1[3-9][0-9]{9}")) {
          throw new BusinessException(400, "请输入有效手机号");
        }
        member.setPhone(account);
      }
      case "email" -> {
        account = MemberEmailCodeService.normalize(account);
        codes.consume(account, request.code());
        member.setEmail(account);
      }
      default -> throw new BusinessException(400, "不支持的注册方式");
    }
    member.setPasswordHash(PASSWORDS.encode(request.password()));
    member.setNickname("拾光会员");
    member.setStatus(1);
    member.setSessionVersion(0L);
    members.insert(member);
    return MemberView.from(member);
  }

  public MemberSessionView login(MemberRequests.Login request, String ip) {
    String account = request.account().trim().toLowerCase(Locale.ROOT);
    limits.check("login-ip", ip, 120, 900);
    limits.check("login-account", account, 15, 900);
    MemberUser user = members.selectOne(new LambdaQueryWrapper<MemberUser>()
        .eq(MemberUser::getUsername, account).or().eq(MemberUser::getPhone, account).or().eq(MemberUser::getEmail, account));
    boolean valid = request.password().getBytes(StandardCharsets.UTF_8).length <= 72
        && PASSWORDS.matches(request.password(), user == null ? DUMMY_HASH : user.getPasswordHash());
    if (!valid || user == null || !Integer.valueOf(1).equals(user.getStatus())) {
      throw new BusinessException(401, "账号或密码错误，或账号不可用");
    }
    String sessionId = UUID.randomUUID().toString();
    String token = jwt.generateToken(user.getId().toString(), Map.of("jti", sessionId, "version", user.getSessionVersion()));
    redis.opsForValue().set("store:session:" + sessionId, user.getId().toString(), Duration.ofMillis(jwt.getExpiration()));
    return new MemberSessionView(token, jwt.getExpiration() / 1000, MemberView.from(user));
  }

  public MemberUser authenticate(String authorization) {
    if (authorization == null || !authorization.startsWith("Bearer ")) {
      throw new BusinessException(401, "请先登录商城");
    }
    String token = authorization.substring(7);
    if (!jwt.validateToken(token)) {
      throw new BusinessException(401, "商城会话无效，请重新登录");
    }
    var claims = jwt.parseToken(token);
    String id = redis.opsForValue().get("store:session:" + claims.getId());
    if (id == null || !id.equals(claims.getSubject())) {
      throw new BusinessException(401, "商城会话已失效");
    }
    MemberUser member = members.selectById(Long.valueOf(id));
    Number version = claims.get("version", Number.class);
    if (member == null || !Integer.valueOf(1).equals(member.getStatus()) || version == null
        || member.getSessionVersion() != version.longValue()) {
      throw new BusinessException(401, "商城会话已撤销，请重新登录");
    }
    return member;
  }

  public void logout(String authorization) {
    redis.delete("store:session:" + jwt.parseToken(authorization.substring(7)).getId());
  }

  @Transactional(rollbackFor = Exception.class)
  public MemberView profile(Long memberId, MemberRequests.Profile request) {
    MemberUser member = members.lockById(memberId);
    member.setNickname(request.nickname().trim());
    members.updateById(member);
    return MemberView.from(member);
  }

  @Transactional(rollbackFor = Exception.class)
  public void changePassword(Long memberId, MemberRequests.Password request) {
    validatePassword(request.newPassword());
    MemberUser member = members.lockById(memberId);
    if (!PASSWORDS.matches(request.currentPassword(), member.getPasswordHash())) {
      throw new BusinessException(400, "原密码不正确");
    }
    members.update(null, new LambdaUpdateWrapper<MemberUser>().eq(MemberUser::getId, memberId)
        .set(MemberUser::getPasswordHash, PASSWORDS.encode(request.newPassword()))
        .set(MemberUser::getSessionVersion, member.getSessionVersion() + 1));
  }

  public static void validatePassword(String password) {
    if (password == null || password.length() < 8 || password.length() > 64
        || password.getBytes(StandardCharsets.UTF_8).length > 72
        || !password.matches(".*[A-Za-z].*") || !password.matches(".*[0-9].*")) {
      throw new BusinessException(400, "密码须为 8–64 位，包含字母和数字，UTF-8 长度不超过 72 字节");
    }
  }
}
