package com.ecommerce.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ecommerce.system.entity.ProductCategory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ProductCategoryMapper extends BaseMapper<ProductCategory> {
    // Include deleted rows: the schema reserves codes using (category_code, deleted).
    @Select("SELECT COUNT(*) FROM product_category WHERE category_code = #{code} AND (#{id} IS NULL OR id != #{id})")
    long countCodeUsage(@Param("code") String code, @Param("id") Long id);
}
