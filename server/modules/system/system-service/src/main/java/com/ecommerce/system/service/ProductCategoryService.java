package com.ecommerce.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ecommerce.system.entity.ProductCategory;
import java.util.List;

public interface ProductCategoryService extends IService<ProductCategory> {
    
    List<ProductCategory> getTreeList();
    
    List<ProductCategory> getChildrenByParentId(Long parentId);
}
