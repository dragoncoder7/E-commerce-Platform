package com.ecommerce.commerce.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ecommerce.commerce.entity.ProductSku;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/** 所有库存变更使用数据库条件更新，跨进程也不会超卖。 */
public interface ProductSkuMapper extends BaseMapper<ProductSku> {
  @Select("SELECT * FROM product_sku WHERE id = #{id} FOR UPDATE")
  ProductSku lock(@Param("id") Long id);

  @Update("UPDATE product_sku SET stock = stock + #{delta} WHERE id = #{id} AND stock + #{delta} >= 0")
  int adjust(@Param("id") Long id, @Param("delta") int delta);
}
