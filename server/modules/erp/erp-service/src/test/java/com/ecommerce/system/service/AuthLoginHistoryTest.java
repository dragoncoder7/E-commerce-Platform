package com.ecommerce.system.service;

import com.ecommerce.common.core.exception.BusinessException;
import com.ecommerce.common.redis.service.RedisService;
import com.ecommerce.common.security.util.JwtUtil;
import com.ecommerce.system.api.dto.LoginDTO;
import com.ecommerce.system.domain.SysUser;
import com.ecommerce.system.factory.RegisterStrategyFactory;
import com.ecommerce.system.mapper.MenuMapper;
import com.ecommerce.system.mapper.UserMapper;
import com.ecommerce.system.service.impl.AuthServiceImpl;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AuthLoginHistoryTest {
    private final UserMapper users = mock(UserMapper.class);
    private final JwtUtil jwt = mock(JwtUtil.class);
    private final RedisService redis = mock(RedisService.class);
    private final AuthServiceImpl auth = new AuthServiceImpl(jwt, redis, users,
            mock(MenuMapper.class), mock(RegisterStrategyFactory.class));

    private SysUser user(LocalDateTime previous) {
        SysUser user = new SysUser();
        user.setId(10L);
        user.setUsername("history_test");
        user.setPassword(new BCryptPasswordEncoder().encode("Test1234!"));
        user.setStatus(1);
        user.setLastLoginTime(previous);
        when(users.selectOne(any())).thenReturn(user);
        when(jwt.generateToken(anyString(), anyMap())).thenReturn("test-session");
        when(jwt.getExpiration()).thenReturn(7200000L);
        return user;
    }

    private LoginDTO login(String password) {
        LoginDTO dto = new LoginDTO();
        dto.setUsername("history_test");
        dto.setPassword(password);
        return dto;
    }

    @Test
    @SuppressWarnings("unchecked")
    void capturesPreviousLoginBeforePersistingCurrentTime() {
        LocalDateTime previous = LocalDateTime.of(2026, 1, 1, 10, 0);
        user(previous);
        auth.login(login("Test1234!"));
        ArgumentCaptor<Map<String, Object>> claims = ArgumentCaptor.forClass(Map.class);
        verify(jwt).generateToken(eq("history_test"), claims.capture());
        assertEquals(previous.toString(), claims.getValue().get("lastLoginTime"));
        ArgumentCaptor<SysUser> update = ArgumentCaptor.forClass(SysUser.class);
        verify(users).updateById(update.capture());
        assertTrue(update.getValue().getLastLoginTime().isAfter(previous));
        assertNotNull(update.getValue().getUpdateTime());
        assertNull(update.getValue().getPassword());
    }

    @Test
    void failedLoginDoesNotChangeHistory() {
        user(null);
        assertThrows(BusinessException.class, () -> auth.login(login("wrong")));
        verify(users, never()).updateById(any(SysUser.class));
        verifyNoInteractions(redis);
    }

    @Test
    void userInfoReturnsSessionHistoryInsteadOfLatestDatabaseTime() {
        SysUser user = user(LocalDateTime.now());
        when(users.selectById(10L)).thenReturn(user);
        when(redis.get("erp:token:test-session")).thenReturn(10L);
        when(jwt.parseToken("test-session")).thenReturn(Jwts.claims()
                .add("lastLoginTime", "2026-01-01T10:00:00").build());
        assertEquals("2026-01-01T10:00:00", auth.getUserInfo("Bearer test-session").getLastLoginTime());
    }

    @Test
    @SuppressWarnings("unchecked")
    void firstLoginDoesNotInventPreviousTime() {
        user(null);
        auth.login(login("Test1234!"));
        ArgumentCaptor<Map<String, Object>> claims = ArgumentCaptor.forClass(Map.class);
        verify(jwt).generateToken(anyString(), claims.capture());
        assertFalse(claims.getValue().containsKey("lastLoginTime"));
    }
}
