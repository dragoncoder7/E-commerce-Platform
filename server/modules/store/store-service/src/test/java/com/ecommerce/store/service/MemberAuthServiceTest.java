package com.ecommerce.store.service;

import com.ecommerce.common.core.exception.BusinessException;
import com.ecommerce.common.security.util.JwtUtil;
import com.ecommerce.store.dto.MemberRequests;
import com.ecommerce.store.entity.MemberUser;
import com.ecommerce.store.mapper.MemberUserMapper;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 密码、身份边界及撤销语义的核心回归。 */
class MemberAuthServiceTest {
  private MemberUserMapper mapper;
  private MemberAuthService service;
  private MemberEmailCodeService codes;
  private StringRedisTemplate redis;
  private JwtUtil jwt;

  @BeforeEach
  void setUp() {
    mapper = mock(MemberUserMapper.class);
    codes = mock(MemberEmailCodeService.class);
    redis = mock(StringRedisTemplate.class, org.mockito.Mockito.RETURNS_DEEP_STUBS);
    jwt = new JwtUtil();
    ReflectionTestUtils.setField(jwt, "secret", "store-test-secret-012345678901234567890123456789");
    ReflectionTestUtils.setField(jwt, "audience", "store");
    ReflectionTestUtils.setField(jwt, "expiration", 7200000L);
    service = new MemberAuthService(mapper, jwt, redis, mock(MemberRateLimiter.class), codes);
  }

  @Test
  void publicRegistrationCreatesOnlyMemberWithPasswordHash() {
    when(mapper.insert(any(MemberUser.class))).thenAnswer(invocation -> {
      MemberUser user = invocation.getArgument(0);
      user.setId(4L);
      assertEquals("alice_1", user.getUsername());
      assertEquals(1, user.getStatus());
      assertEquals(0L, user.getSessionVersion());
      assertFalse(user.getPasswordHash().contains("Password123"));
      assertTrue(new BCryptPasswordEncoder().matches("Password123", user.getPasswordHash()));
      return 1;
    });
    assertEquals(4L, service.register(new MemberRequests.Register("account", "Alice_1", "Password123", null), "local").id());
  }

  @Test
  void emailRegistrationRequiresConsumedCode() {
    service.register(new MemberRequests.Register("email", "Alice@Example.com", "Password123", "123456"), "local");
    verify(codes).consume("alice@example.com", "123456");
  }

  @Test
  void passwordPolicyRejectsWeakAndBcryptTruncation() {
    assertThrows(BusinessException.class, () -> MemberAuthService.validatePassword("12345678"));
    assertThrows(BusinessException.class, () -> MemberAuthService.validatePassword("密码".repeat(20) + "Aa1234"));
  }

  @Test
  void numericUsernameCannotCollideWithPhoneIdentity() {
    assertThrows(BusinessException.class, () -> service.register(
        new MemberRequests.Register("account", "13800138000", "Password123", null), "local"));
  }

  @Test
  void erpTokenRejectedEvenWithSameSigningKey() {
    ReflectionTestUtils.setField(jwt, "audience", "erp");
    String token = jwt.generateToken("1", Map.of("jti", "test", "version", 0));
    ReflectionTestUtils.setField(jwt, "audience", "store");
    assertThrows(BusinessException.class, () -> service.authenticate("Bearer " + token));
  }

  @Test
  void revokedVersionAndDisabledAccountCannotAuthenticate() {
    MemberUser member = new MemberUser();
    member.setStatus(1);
    member.setSessionVersion(1L);
    when(redis.opsForValue().get(anyString())).thenReturn("1");
    when(mapper.selectById(1L)).thenReturn(member);
    String token = jwt.generateToken("1", Map.of("jti", "test", "version", 0));
    assertThrows(BusinessException.class, () -> service.authenticate("Bearer " + token));
    member.setSessionVersion(0L);
    member.setStatus(0);
    assertThrows(BusinessException.class, () -> service.authenticate("Bearer " + token));
  }

  @Test
  void missingOrLoggedOutSessionCannotAuthenticate() {
    assertThrows(BusinessException.class, () -> service.authenticate(null));
    String token = jwt.generateToken("1", Map.of("jti", "missing", "version", 0));
    assertThrows(BusinessException.class, () -> service.authenticate("Bearer " + token));
  }
}
