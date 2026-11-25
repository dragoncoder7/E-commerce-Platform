package com.ecommerce.system.service;

import com.ecommerce.common.security.util.JwtUtil;
import com.ecommerce.system.SystemApplication;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServiceBoundaryTest {
  private JwtUtil jwt(String audience, String secret) {
    JwtUtil jwt = new JwtUtil();
    ReflectionTestUtils.setField(jwt, "audience", audience);
    ReflectionTestUtils.setField(jwt, "secret", secret);
    ReflectionTestUtils.setField(jwt, "expiration", 7200000L);
    return jwt;
  }

  @Test
  void rejectsOtherAudienceEvenWhenSecretIsAccidentallyReused() {
    String secret = "test-only-key-with-at-least-thirty-two-bytes-for-jwt";
    JwtUtil erp = jwt("erp", secret);
    JwtUtil store = jwt("store", secret);
    String token = store.generateToken("1");
    assertTrue(store.validateToken(token));
    assertFalse(erp.validateToken(token));
  }

  @Test
  void rejectsOtherSigningKey() {
    JwtUtil erp = jwt("erp", "erp-test-only-key-with-at-least-thirty-two-bytes");
    JwtUtil attacker = jwt("erp", "store-test-only-key-with-at-least-thirty-two-bytes");
    assertFalse(erp.validateToken(attacker.generateToken("1")));
  }

  @Test
  void erpDoesNotScanStoreApplication() {
    String[] packages = SystemApplication.class.getAnnotation(SpringBootApplication.class).scanBasePackages();
    assertFalse(Arrays.asList(packages).contains("com.ecommerce"));
    assertFalse(Arrays.asList(packages).contains("com.ecommerce.store"));
  }
}
