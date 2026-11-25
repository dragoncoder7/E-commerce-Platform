package com.ecommerce.commerce.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ecommerce.commerce.entity.ProductImage;
import com.ecommerce.commerce.mapper.ProductImageMapper;
import com.ecommerce.commerce.service.ProductImageService;
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
        if (imageUrls == null) {
            throw new com.ecommerce.common.core.exception.BusinessException(400, "图片列表不能为空");
        }
        remove(new LambdaQueryWrapper<ProductImage>().eq(ProductImage::getProductId, productId));
        if (imageUrls.isEmpty()) {
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
