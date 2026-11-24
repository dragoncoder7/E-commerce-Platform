package com.ecommerce.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.common.core.domain.Result;
import com.ecommerce.common.core.exception.BusinessException;
import com.ecommerce.system.dto.CategorySaveRequest;
import jakarta.validation.Valid;
import com.ecommerce.system.entity.ProductCategory;
import com.ecommerce.system.service.ProductCategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/product/category")
public class ProductCategoryController {
    
    @Autowired
    private ProductCategoryService productCategoryService;
    
    @GetMapping("/all")
    public Result<List<ProductCategory>> all() {
        return Result.success(productCategoryService.list(new LambdaQueryWrapper<ProductCategory>()
                .orderByAsc(ProductCategory::getSortOrder).orderByAsc(ProductCategory::getId)));
    }

    @GetMapping("/tree")
    public Result<List<ProductCategory>> getTree() {
        List<ProductCategory> list = productCategoryService.getTreeList();
        return Result.success(list);
    }
    
    @GetMapping("/children/{parentId}")
    public Result<List<ProductCategory>> getChildren(@PathVariable Long parentId) {
        List<ProductCategory> list = productCategoryService.getChildrenByParentId(parentId);
        return Result.success(list);
    }
    
    @GetMapping("/list")
    public Result<Page<ProductCategory>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Long parentId,
            @RequestParam(required = false) Integer status) {
        
        Page<ProductCategory> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<ProductCategory> wrapper = new LambdaQueryWrapper<>();
        
        if (parentId != null) {
            wrapper.eq(ProductCategory::getParentId, parentId);
        }
        if (status != null) {
            wrapper.eq(ProductCategory::getStatus, status);
        }
        
        wrapper.orderByAsc(ProductCategory::getSortOrder);
        Page<ProductCategory> result = productCategoryService.page(page, wrapper);
        return Result.success(result);
    }
    
    @GetMapping("/{id}")
    public Result<ProductCategory> getById(@PathVariable Long id) {
        ProductCategory category = productCategoryService.getById(id);
        return Result.success(category);
    }
    
    @PostMapping
    public Result<ProductCategory> save(@Valid @RequestBody CategorySaveRequest category) {
        return Result.success(productCategoryService.saveCategory(category, false));
    }

    @PutMapping
    public Result<ProductCategory> update(@Valid @RequestBody CategorySaveRequest category) {
        if (category.getId() == null) throw new BusinessException(400, "请选择要编辑的类别");
        return Result.success(productCategoryService.saveCategory(category, true));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        productCategoryService.deleteCategory(id);
        return Result.success();
    }
}
