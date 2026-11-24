package com.ecommerce.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ecommerce.system.entity.ProductCategory;
import com.ecommerce.system.mapper.ProductCategoryMapper;
import com.ecommerce.system.service.ProductCategoryService;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ProductCategoryServiceImpl extends ServiceImpl<ProductCategoryMapper, ProductCategory> implements ProductCategoryService {
    
    @Override
    public List<ProductCategory> getTreeList() {
        return list(new LambdaQueryWrapper<ProductCategory>()
                .eq(ProductCategory::getStatus, 1)
                .orderByAsc(ProductCategory::getSortOrder));
    }
    
    @Override
    public List<ProductCategory> getChildrenByParentId(Long parentId) {
        return list(new LambdaQueryWrapper<ProductCategory>()
                .eq(ProductCategory::getParentId, parentId)
                .eq(ProductCategory::getStatus, 1)
                .orderByAsc(ProductCategory::getSortOrder));
    }
}
