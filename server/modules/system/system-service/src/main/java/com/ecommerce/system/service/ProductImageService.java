package com.ecommerce.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ecommerce.system.entity.ProductImage;
import java.util.List;

public interface ProductImageService extends IService<ProductImage> {
    
    List<ProductImage> getByProductId(Long productId);
    
    boolean saveImages(Long productId, List<String> imageUrls);
}
