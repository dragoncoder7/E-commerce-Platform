package com.ecommerce.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ecommerce.system.entity.ProductAttribute;
import java.util.List;

public interface ProductAttributeService extends IService<ProductAttribute> {
    
    List<ProductAttribute> getByProductId(Long productId);
}
