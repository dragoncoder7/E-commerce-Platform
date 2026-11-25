package com.ecommerce.store.service;

import com.ecommerce.common.core.exception.BusinessException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

/** 验证码摘要存储、限频、错误次数及一次性消费均由 Redis 保证。 */
@Service
public class MemberEmailCodeService {
  private static final SecureRandom RANDOM = new SecureRandom();
  private static final DefaultRedisScript<Long> ISSUE = new DefaultRedisScript<>(
      "if not redis.call('SET',KEYS[2],'1','NX','EX',60) then return 0 end; "
      + "redis.call('HSET',KEYS[1],'digest',ARGV[1],'attempts',0); redis.call('EXPIRE',KEYS[1],600); return 1", Long.class);
  private static final DefaultRedisScript<Long> CONSUME = new DefaultRedisScript<>(
      "local d=redis.call('HGET',KEYS[1],'digest'); if not d then return 0 end; "
      + "if d==ARGV[1] then redis.call('DEL',KEYS[1]); return 1 end; "
      + "local n=redis.call('HINCRBY',KEYS[1],'attempts',1); if n>=5 then redis.call('DEL',KEYS[1]) end; return 0", Long.class);
  private final StringRedisTemplate redis;
  private final MemberRateLimiter limits;
  private final MemberMailSender mail;
  private final String secret;

  public MemberEmailCodeService(StringRedisTemplate redis, MemberRateLimiter limits, MemberMailSender mail,
      @Value("${jwt.secret}") String secret) {
    this.redis = redis;
    this.limits = limits;
    this.mail = mail;
    this.secret = secret;
  }

  public void issue(String input, String ip) {
    if ("disabled".equals(mail.mode())) {
      throw new BusinessException(503, "邮箱注册暂未启用，请使用账号或手机号注册");
    }
    String email = normalize(input);
    limits.check("email-ip", ip, 20, 3600);
    limits.check("email", email, 10, 3600);
    String code = String.format(Locale.ROOT, "%06d", RANDOM.nextInt(1_000_000));
    String key = key(email);
    if (!Long.valueOf(1).equals(redis.execute(ISSUE, List.of(key, key + ":cooldown"), digest(email, code)))) {
      throw new BusinessException(429, "请在 60 秒后重新发送验证码");
    }
    try {
      mail.sendCode(email, code);
    } catch (RuntimeException exception) {
      redis.delete(List.of(key, key + ":cooldown"));
      throw exception;
    }
  }

  public void consume(String input, String code) {
    String email = normalize(input);
    if (code == null || !code.matches("[0-9]{6}")
        || !Long.valueOf(1).equals(redis.execute(CONSUME, List.of(key(email)), digest(email, code)))) {
      throw new BusinessException(400, "验证码错误或已过期，请重新获取");
    }
  }

  public static String normalize(String input) {
    String email = input.trim().toLowerCase(Locale.ROOT);
    if (email.length() > 254 || !email.matches("[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]+@[A-Za-z0-9](?:[A-Za-z0-9.-]*[A-Za-z0-9])?\\.[A-Za-z]{2,63}")) {
      throw new BusinessException(400, "请输入有效邮箱");
    }
    return email;
  }

  private String key(String email) {
    return "store:email-code:" + MemberRateLimiter.digest(email);
  }

  private String digest(String email, String code) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
      return HexFormat.of().formatHex(mac.doFinal((email + ":" + code).getBytes(StandardCharsets.UTF_8)));
    } catch (GeneralSecurityException exception) {
      throw new IllegalStateException("无法生成验证码摘要", exception);
    }
  }
}
