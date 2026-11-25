package com.ecommerce.store.service;

import com.ecommerce.common.core.exception.BusinessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Service;

/** 按配置选择 Resend 或仅本地可用的邮件捕获，不在日志及响应中泄露验证码。 */
@Service
public class MemberMailSender {
  private final String mode;
  private final String apiKey;
  private final String from;
  private final Path capturePath;
  private final ObjectMapper json;
  private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

  public MemberMailSender(@Value("${store.mail.mode:disabled}") String mode,
      @Value("${store.mail.api-key:}") String apiKey, @Value("${store.mail.from:}") String from,
      @Value("${store.mail.capture-path:.runtime/mail}") String capturePath,
      Environment environment, ObjectMapper json) {
    if ("capture".equals(mode) && !environment.acceptsProfiles(Profiles.of("local", "verify", "test"))) {
      throw new IllegalStateException("邮件捕获仅允许 local、verify、test 环境");
    }
    this.mode = mode;
    this.apiKey = apiKey;
    this.from = from;
    this.capturePath = Path.of(capturePath);
    this.json = json;
  }

  public String mode() {
    if ("capture".equals(mode)) {
      return mode;
    }
    return "resend".equals(mode) && !apiKey.isBlank() && !from.isBlank() ? mode : "disabled";
  }

  public void sendCode(String email, String code) {
    try {
      if ("capture".equals(mode())) {
        Files.createDirectories(capturePath);
        Files.writeString(capturePath.resolve(MemberRateLimiter.digest(email) + ".json"),
            json.writeValueAsString(Map.of("email", email, "code", code)));
        return;
      }
      if (!"resend".equals(mode())) {
        throw new BusinessException(503, "邮箱注册暂未启用，请使用账号或手机号注册");
      }
      String body = json.writeValueAsString(Map.of("from", from, "to", List.of(email),
          "subject", "拾光商城注册验证码", "text", "您的注册验证码为 " + code + "，10 分钟内有效。请勿向他人提供。"));
      HttpRequest request = HttpRequest.newBuilder(URI.create("https://api.resend.com/emails"))
          .timeout(Duration.ofSeconds(15)).header("Authorization", "Bearer " + apiKey)
          .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body)).build();
      int status = client.send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
      if (status < 200 || status >= 300) {
        throw new BusinessException(503, "验证码邮件发送失败，请稍后重试");
      }
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new BusinessException(503, "验证码邮件发送中断");
    } catch (IOException exception) {
      throw new BusinessException(503, "验证码邮件发送失败，请稍后重试");
    }
  }
}
