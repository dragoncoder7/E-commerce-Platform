package com.ecommerce.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ecommerce.system.entity.Product;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ProductMapper extends BaseMapper<Product> {
}
