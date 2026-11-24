package com.ecommerce.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ecommerce.system.entity.ProductImage;
import com.ecommerce.system.mapper.ProductImageMapper;
import com.ecommerce.system.service.ProductImageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.List;

@Service
public class ProductImageServiceImpl extends ServiceImpl<ProductImageMapper, ProductImage> implements ProductImageService {
    
    @Override
    public List<ProductImage> getByProductId(Long productId) {
        return list(new LambdaQueryWrapper<ProductImage>()
                .eq(ProductImage::getProductId, productId)
                .orderByAsc(ProductImage::getSortOrder));
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveImages(Long productId, List<String> imageUrls) {
        if (imageUrls == null || imageUrls.isEmpty()) {
            return true;
        }
        
        List<ProductImage> images = new ArrayList<>();
        for (int i = 0; i < imageUrls.size(); i++) {
            ProductImage image = new ProductImage();
            image.setProductId(productId);
            image.setImageUrl(imageUrls.get(i));
            image.setImageType(i == 0 ? 1 : 2);
            image.setSortOrder(i);
            images.add(image);
        }
        
        return saveBatch(images);
    }
}
