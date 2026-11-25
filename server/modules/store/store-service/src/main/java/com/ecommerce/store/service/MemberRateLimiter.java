package com.ecommerce.store.service;

import com.ecommerce.common.core.exception.BusinessException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

/** Redis 原子限流；键内不保存原始账号。 */
@Service
@RequiredArgsConstructor
public class MemberRateLimiter {
  private static final DefaultRedisScript<Long> INCREMENT = new DefaultRedisScript<>(
      "local n=redis.call('INCR',KEYS[1]); if n==1 then redis.call('EXPIRE',KEYS[1],ARGV[1]) end; return n", Long.class);
  private final StringRedisTemplate redis;

  public void check(String scope, String identity, int maximum, int seconds) {
    Long count = redis.execute(INCREMENT, List.of("store:limit:" + scope + ":" + digest(identity)), String.valueOf(seconds));
    if (count == null || count > maximum) {
      throw new BusinessException(429, "操作频繁，请稍后再试");
    }
  }

  public static String digest(String text) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("JDK 缺少 SHA-256", exception);
    }
  }
}
