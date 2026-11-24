package com.ecommerce.system;

import com.ecommerce.system.service.EmailService;
import com.resend.core.exception.ResendException;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 邮件发送集成测试
 * 
 * 注意：此测试会真实发送邮件，需要：
 * 1. 有效的 Resend API Key
 * 2. 发件人邮箱域名已在 Resend 验证
 * 3. 收件人邮箱真实存在
 * 
 * 默认禁用（@Disabled），手动运行时移除该注解
 */
@Slf4j
@SpringBootTest
@ActiveProfiles("integration")
// @Disabled("真实邮件发送测试，需要手动启用")  // 临时启用测试
public class EmailIntegrationTest {

    @Autowired
    private EmailService emailService;

    /**
     * 测试真实邮件发送
     * 
     * 注意事项：
     * 1. from 邮箱必须是已验证的域名（如 onboarding@resend.dev 或自己的域名）
     * 2. 测试前确认 API Key 有效
     * 3. 检查收件箱验证邮件是否成功送达
     */
    @Test
    public void testSendRealEmail() throws ResendException {
        // 使用 Resend 官方测试域名（需要替换为你自己验证的域名）
        String from = "onboarding@resend.dev";
        List<String> to = List.of("1026225160@qq.com");
        String subject = "集成测试 - 邮件发送验证";
        String content = "<h1>测试邮件</h1><p>这是一封来自 E-commerce-Platform 的测试邮件。</p><p>发送时间：" + System.currentTimeMillis() + "</p>";

        log.info("开始发送测试邮件...");
        log.info("发件人: {}", from);
        log.info("收件人: {}", to);
        log.info("主题: {}", subject);

        String emailId = emailService.sendEmail(from, to, subject, content);

        log.info("邮件发送成功！Email ID: {}", emailId);
        
        assertNotNull(emailId, "邮件 ID 不应为空");
        assertFalse(emailId.isEmpty(), "邮件 ID 不应为空字符串");
    }

    /**
     * 测试发送到多个收件人
     */
    @Test
    public void testSendToMultipleRecipients() throws ResendException {
        String from = "onboarding@resend.dev";
        List<String> to = List.of("1026225160@qq.com", "1434964624@qq.com", "1206614724@qq.com");
        String subject = "批量邮件测试";
        String content = "<h2>批量邮件测试</h2><p>这封邮件发送给多个收件人。</p>";

        log.info("开始发送批量邮件...");
        String emailId = emailService.sendEmail(from, to, subject, content);

        log.info("批量邮件发送成功！Email ID: {}", emailId);
        assertNotNull(emailId);
    }

    /**
     * 测试发送失败场景（无效的发件人）
     */
    @Test
    public void testSendEmailWithInvalidFrom() {
        String invalidFrom = "invalid@example.com"; // 未验证的域名
        List<String> to = List.of("test@example.com");
        String subject = "测试";
        String content = "测试内容";

        log.info("测试无效发件人场景...");
        
        assertThrows(ResendException.class, () -> {
            emailService.sendEmail(invalidFrom, to, subject, content);
        }, "使用未验证的域名应该抛出异常");
    }
}
