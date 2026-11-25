package com.ecommerce.store.service;

import com.ecommerce.common.core.exception.BusinessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** 邮件关闭和测试捕获的环境边界；不发送真实邮件。 */
class MemberMailSenderTest {
  @Test
  void missingConfigurationDisablesEmailRegistration() {
    MemberMailSender sender = new MemberMailSender("resend", "", "", ".runtime/test-mail",
        new MockEnvironment(), new ObjectMapper());
    assertEquals("disabled", sender.mode());
    assertThrows(BusinessException.class, () -> sender.sendCode("test@example.com", "123456"));
  }

  @Test
  void captureCannotRunInProduction() {
    assertThrows(IllegalStateException.class, () -> new MemberMailSender("capture", "", "",
        ".runtime/test-mail", new MockEnvironment().withProperty("spring.profiles.active", "production"), new ObjectMapper()));
  }
}
