package com.ecommerce.system.strategy.imp;

import com.ecommerce.common.core.exception.BusinessException;
import com.ecommerce.system.api.dto.RegisterRequest;
import com.ecommerce.system.domain.SysUser;
import com.ecommerce.system.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PasswordRegisterStrategyTest {

    private final UserMapper userMapper = mock(UserMapper.class);
    private final PasswordRegisterStrategy strategy = new PasswordRegisterStrategy(userMapper);

    private RegisterRequest request(String username, String password) {
        RegisterRequest request = new RegisterRequest();
        request.setUsername(username);
        request.setPassword(password);
        return request;
    }

    @Test
    void registersWithEncodedPassword() {
        when(userMapper.selectCount(any())).thenReturn(0L);
        when(userMapper.insert(any(SysUser.class))).thenAnswer(invocation -> {
            SysUser user = invocation.getArgument(0);
            user.setId(42L);
            assertEquals("account_1", user.getUsername());
            assertEquals(1, user.getStatus());
            assertNotEquals("Password1!", user.getPassword());
            assertTrue(new BCryptPasswordEncoder().matches("Password1!", user.getPassword()));
            return 1;
        });

        assertEquals(42L, strategy.register(request("account_1", "Password1!")));
    }

    @Test
    void rejectsInvalidInputBeforeQueryingDatabase() {
        assertThrows(BusinessException.class, () -> strategy.register(request("x", "Password1!")));
        assertThrows(BusinessException.class, () -> strategy.register(request("account_1", "password1!")));
        assertThrows(BusinessException.class, () -> strategy.register(request("account_1", "Password1")));
        verifyNoInteractions(userMapper);
    }

    @Test
    void rejectsExistingUsername() {
        when(userMapper.selectCount(any())).thenReturn(1L);
        assertThrows(BusinessException.class, () -> strategy.register(request("account_1", "Password1!")));
        verify(userMapper, never()).insert(any(SysUser.class));
    }

    @Test
    void convertsUniqueKeyRaceIntoBusinessError() {
        when(userMapper.selectCount(any())).thenReturn(0L);
        when(userMapper.insert(any(SysUser.class))).thenThrow(new DuplicateKeyException("duplicate"));
        BusinessException error = assertThrows(BusinessException.class,
                () -> strategy.register(request("account_1", "Password1!")));
        assertEquals("用户名已存在", error.getMessage());
    }
}
