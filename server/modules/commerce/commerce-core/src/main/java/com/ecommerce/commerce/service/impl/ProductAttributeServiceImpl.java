package com.ecommerce.commerce.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ecommerce.commerce.entity.ProductAttribute;
import com.ecommerce.commerce.mapper.ProductAttributeMapper;
import com.ecommerce.commerce.service.ProductAttributeService;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ProductAttributeServiceImpl extends ServiceImpl<ProductAttributeMapper, ProductAttribute> implements ProductAttributeService {
    
    @Override
    public List<ProductAttribute> getByProductId(Long productId) {
        return list(new LambdaQueryWrapper<ProductAttribute>()
                .eq(ProductAttribute::getProductId, productId)
                .orderByAsc(ProductAttribute::getSortOrder));
    }
}
