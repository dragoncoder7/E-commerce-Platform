package com.ecommerce.system.strategy.imp;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ecommerce.common.core.exception.BusinessException;
import com.ecommerce.system.api.constants.Numbers;
import com.ecommerce.system.api.dto.RegisterRequest;
import com.ecommerce.system.api.enums.RegisterType;
import com.ecommerce.system.domain.SysUser;
import com.ecommerce.system.mapper.UserMapper;
import com.ecommerce.system.strategy.RegisterStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class PasswordRegisterStrategy implements RegisterStrategy {

    private static final Pattern USERNAME = Pattern.compile("[A-Za-z0-9_]{3,50}");
    private static final Pattern PASSWORD = Pattern.compile("(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9])(?=.*[^A-Za-z0-9\\s])[\\x21-\\x7E]{8,20}");

    private final UserMapper userMapper;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public RegisterType getRegisterType() {
        return RegisterType.PASSWORD;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Long register(RegisterRequest request) {
        if (request == null || request.getUsername() == null
                || !USERNAME.matcher(request.getUsername()).matches()) {
            throw new BusinessException("用户名须为3到50位字母、数字或下划线");
        }
        if (request.getPassword() == null || !PASSWORD.matcher(request.getPassword()).matches()) {
            throw new BusinessException("密码须为8到20位，包含大小写字母、数字和特殊符号，且不能包含空格");
        }
        Long count = userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, request.getUsername()));
        if (count >= Numbers.ONE) {
            throw new BusinessException("用户名已存在");
        }

        SysUser user = new SysUser();
        user.setUsername(request.getUsername());
        user.setNickname(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setStatus(Numbers.ONE);
        user.setDeleted(Numbers.ZERO);
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        try {
            if (userMapper.insert(user) == Numbers.ZERO || user.getId() == null) {
                throw new BusinessException("注册失败");
            }
        } catch (DuplicateKeyException e) {
            throw new BusinessException("用户名已存在", e);
        }
        return user.getId();
    }
}
