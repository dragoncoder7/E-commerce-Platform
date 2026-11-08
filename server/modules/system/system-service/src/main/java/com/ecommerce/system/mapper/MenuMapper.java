package com.ecommerce.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ecommerce.system.domain.Menu;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 菜单 Mapper
 */
@Mapper
public interface MenuMapper extends BaseMapper<Menu> {

    /**
     * 根据用户ID查询菜单列表
     */
    List<Menu> selectMenusByUserId(@Param("userId") Long userId);
}
