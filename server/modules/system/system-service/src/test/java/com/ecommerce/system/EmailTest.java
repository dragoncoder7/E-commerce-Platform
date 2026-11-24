package com.ecommerce.system;

import com.ecommerce.system.service.EmailService;
import com.resend.Resend;
import com.resend.core.exception.ResendException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

@SpringBootTest
@ActiveProfiles("test")
public class EmailTest {

    @Autowired
    private EmailService emailService;

    @MockBean
    private Resend resend;

    @Test
    public void testSendEmail() throws ResendException {
        System.out.println(emailService.sendEmail("1026225160@qq.com", List.of("1434964624@qq.com", "1206614724@qq.com"), "subject", "邮件测试"));
    }
}
