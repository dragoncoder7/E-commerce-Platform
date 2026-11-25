package com.ecommerce.system.service;

import com.ecommerce.system.api.dto.LoginDTO;
import com.ecommerce.system.api.dto.RegisterRequest;
import com.ecommerce.system.api.vo.LoginVO;
import com.ecommerce.system.api.vo.MenuVO;
import com.ecommerce.system.api.vo.RegisterVO;
import com.ecommerce.system.api.vo.UserInfoVO;

import java.util.List;

/**
 * 认证服务接口
 */
public interface IAuthService {

    /**
     * 登录
     */
    LoginVO login(LoginDTO loginDTO);

    /**
     * 登出
     */
    void logout(String token);

    /**
     * 获取用户信息
     */
    UserInfoVO getUserInfo(String token);

    /**
     * 获取用户菜单
     */
    List<MenuVO> getMenus(String token);

    /**
     * 注册
     */
    RegisterVO register(RegisterRequest request);
}
