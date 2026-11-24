package com.ecommerce.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ecommerce.system.entity.ProductCategory;
import com.ecommerce.system.dto.CategorySaveRequest;
import java.util.List;

public interface ProductCategoryService extends IService<ProductCategory> {
    ProductCategory saveCategory(CategorySaveRequest request, boolean updating);

    void deleteCategory(Long id);

    void requireEnabled(Long id);
    
    List<ProductCategory> getTreeList();
    
    List<ProductCategory> getChildrenByParentId(Long parentId);
}
