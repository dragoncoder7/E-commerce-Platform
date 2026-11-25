package com.ecommerce.commerce.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ecommerce.commerce.entity.ProductCategory;
import com.ecommerce.commerce.dto.CategorySaveRequest;
import java.util.List;

public interface ProductCategoryService extends IService<ProductCategory> {
    ProductCategory saveCategory(CategorySaveRequest request, boolean updating);

    void deleteCategory(Long id);

    void requireEnabled(Long id);
    
    List<ProductCategory> getTreeList();
    
    List<ProductCategory> getChildrenByParentId(Long parentId);
}
