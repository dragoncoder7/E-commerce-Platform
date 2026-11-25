package com.ecommerce.system;

import com.ecommerce.system.service.impl.EmailServiceImpl;
import com.resend.Resend;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** 不加载本地配置，不调用真实邮件供应商。 */
class EmailTest {
  @Test
  void returnsProviderMessageId() throws Exception {
    Resend resend = mock(Resend.class, RETURNS_DEEP_STUBS);
    CreateEmailResponse response = mock(CreateEmailResponse.class);
    when(response.getId()).thenReturn("test-message-id");
    when(resend.emails().send(any(CreateEmailOptions.class))).thenReturn(response);
    EmailServiceImpl service = new EmailServiceImpl();
    ReflectionTestUtils.setField(service, "resend", resend);
    assertEquals("test-message-id", service.sendEmail("sender@example.com",
        List.of("recipient@example.com"), "测试主题", "测试正文"));
  }
}
