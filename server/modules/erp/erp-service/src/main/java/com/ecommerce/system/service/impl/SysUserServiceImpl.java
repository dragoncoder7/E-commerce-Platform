package com.ecommerce.system.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ecommerce.system.domain.SysUser;
import com.ecommerce.system.service.SysUserService;
import com.ecommerce.system.mapper.SysUserMapper;
import org.springframework.stereotype.Service;

/**
* @author 10262
* @description 针对表【sys_user(系统用户表)】的数据库操作Service实现
* @createDate 2026-11-11 23:44:50
*/
@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser>
    implements SysUserService{

}




