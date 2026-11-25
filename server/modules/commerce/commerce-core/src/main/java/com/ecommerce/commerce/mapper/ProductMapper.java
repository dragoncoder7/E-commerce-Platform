package com.ecommerce.commerce.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ecommerce.commerce.entity.Product;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ProductMapper extends BaseMapper<Product> {
    // 包含已删除记录，避免复用编号后再次软删除时触发唯一键冲突。
    @org.apache.ibatis.annotations.Select("SELECT COUNT(*) FROM product WHERE product_code = #{code} AND (#{id} IS NULL OR id != #{id})")
    long countCodeUsage(@org.apache.ibatis.annotations.Param("code") String code, @org.apache.ibatis.annotations.Param("id") Long id);
}
