package com.ecommerce.system.controller;

import com.ecommerce.common.core.domain.Result;
import com.ecommerce.system.api.dto.LoginDTO;
import com.ecommerce.system.api.vo.LoginVO;
import com.ecommerce.system.api.vo.MenuVO;
import com.ecommerce.system.api.vo.UserInfoVO;
import com.ecommerce.system.service.IAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 认证控制器
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final IAuthService authService;

    /**
     * 登录
     */
    @PostMapping("/login")
    public Result<LoginVO> login(@Validated @RequestBody LoginDTO loginDTO) {
        LoginVO loginVO = authService.login(loginDTO);
        return Result.ok(loginVO);
    }

    /**
     * 登出
     */
    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader(value = "Authorization", required = false) String token) {
        authService.logout(token);
        return Result.ok();
    }

    /**
     * 获取当前用户信息
     */
    @GetMapping("/userinfo")
    public Result<UserInfoVO> getUserInfo(@RequestHeader("Authorization") String token) {
        UserInfoVO userInfo = authService.getUserInfo(token);
        return Result.ok(userInfo);
    }

    /**
     * 获取当前用户菜单
     */
    @GetMapping("/menus")
    public Result<List<MenuVO>> getMenus(@RequestHeader("Authorization") String token) {
        List<MenuVO> menus = authService.getMenus(token);
        return Result.ok(menus);
    }

    public Result<?> register() {

    }
}
