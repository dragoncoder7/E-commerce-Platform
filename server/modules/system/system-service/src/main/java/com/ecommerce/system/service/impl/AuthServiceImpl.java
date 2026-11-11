package com.ecommerce.system.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ecommerce.common.core.exception.BusinessException;
import com.ecommerce.common.redis.service.RedisService;
import com.ecommerce.common.security.util.JwtUtil;
import com.ecommerce.system.api.dto.LoginDTO;
import com.ecommerce.system.api.dto.RegisterRequest;
import com.ecommerce.system.api.vo.LoginVO;
import com.ecommerce.system.api.vo.MenuVO;
import com.ecommerce.system.api.vo.RegisterVO;
import com.ecommerce.system.api.vo.UserInfoVO;
import com.ecommerce.system.domain.Menu;
import com.ecommerce.system.domain.SysUser;
import com.ecommerce.system.factory.RegisterStrategyFactory;
import com.ecommerce.system.mapper.MenuMapper;
import com.ecommerce.system.mapper.UserMapper;
import com.ecommerce.system.service.IAuthService;
import com.ecommerce.system.strategy.RegisterStrategy;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 认证服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements IAuthService {


    private final JwtUtil jwtUtil;
    private final RedisService redisService;
    private final UserMapper userMapper;
    private final MenuMapper menuMapper;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final RegisterStrategyFactory registerFactory;

    private static final String CACHE_KEY_PREFIX = "user:token:";

    @Override
    public LoginVO login(LoginDTO loginDTO) {
        // 1. 查询用户
        SysUser user = userMapper.selectOne(
            new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, loginDTO.getUsername())
        );

        if (user == null) {
            throw new BusinessException("用户名或密码错误");
        }

        // 2. 验证密码
        if (!passwordEncoder.matches(loginDTO.getPassword(), user.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }

        // 3. 检查用户状态
        if (user.getStatus() == 0) {
            throw new BusinessException("账号已被禁用");
        }

        Result result = generateLoginToken(user);
        return new LoginVO(result.token(), result.expiration());
    }

    private @NonNull Result generateLoginToken(SysUser user) {
        // 4. 生成 Token
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("username", user.getUsername());
        String token = jwtUtil.generateToken(user.getUsername(), claims);
        Long expiration = jwtUtil.getExpiration();

        // 5. 缓存用户信息到 Redis
        String cacheKey = CACHE_KEY_PREFIX + token;
        redisService.set(cacheKey, user.getId(), expiration, TimeUnit.MILLISECONDS);

        log.info("用户 {} 登录成功", user.getUsername());
        Result result = new Result(token, expiration);
        return result;
    }

    private record Result(String token, Long expiration) {
    }

    @Override
    public void logout(String token) {
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
            String cacheKey = CACHE_KEY_PREFIX + token;
            redisService.delete(cacheKey);
            log.info("用户退出登录");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public UserInfoVO getUserInfo(String token) {
        Long userId = extractUserId(token);

        // 查询用户信息
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(401, "用户不存在");
        }

        // 查询角色
        List<String> roles = userMapper.selectRoleCodesByUserId(userId);

        // 查询权限
        List<String> permissions = userMapper.selectPermissionsByUserId(userId);

        // 组装返回数据
        UserInfoVO userInfo = new UserInfoVO();
        userInfo.setId(user.getId());
        userInfo.setUsername(user.getUsername());
        userInfo.setNickname(user.getNickname());
        userInfo.setAvatar(user.getAvatar());
        userInfo.setRoles(roles);
        userInfo.setPermissions(permissions);

        return userInfo;
    }

    @Override
    public List<MenuVO> getMenus(String token) {
        Long userId = extractUserId(token);

        // 查询用户菜单列表
        List<Menu> menus = menuMapper.selectMenusByUserId(userId);

        // 转换为树形结构
        return buildMenuTree(menus, 0L);
    }



    @Override
    public RegisterVO register(RegisterRequest request) {
        if (request == null) {
            throw new BusinessException("注册参数不能为空");
        }
        RegisterStrategy strategy = registerFactory.get(request.getType());
        Long userId = strategy.register(request);
        RegisterVO response = new RegisterVO();
        response.setId(userId);
        return response;
    }

  /**
     * 构建菜单树
     */
    private List<MenuVO> buildMenuTree(List<Menu> menus, Long parentId) {
        List<MenuVO> tree = new ArrayList<>();

        for (Menu menu : menus) {
            if (menu.getParentId().equals(parentId)) {
                MenuVO menuVO = new MenuVO();
                BeanUtils.copyProperties(menu, menuVO);
                menuVO.setVisible(menu.getVisible() == 1);
                menuVO.setKeepAlive(menu.getKeepAlive() == 1);

                // 递归查找子菜单
                List<MenuVO> children = buildMenuTree(menus, menu.getId());
                if (CollUtil.isNotEmpty(children)) {
                    menuVO.setChildren(children);
                }

                tree.add(menuVO);
            }
        }

        return tree;
    }

    /**
     * 从 Token 中提取用户ID
     */
    private Long extractUserId(String token) {
        if (token == null || !token.startsWith("Bearer ")) {
            throw new BusinessException(401, "未登录或登录已过期");
        }
        token = token.substring(7);

        // 从 Redis 获取用户ID
        String cacheKey = CACHE_KEY_PREFIX + token;
        Long userId = redisService.get(cacheKey);

        if (userId == null) {
            throw new BusinessException(401, "Token 已过期");
        }

        return userId;
    }
}
