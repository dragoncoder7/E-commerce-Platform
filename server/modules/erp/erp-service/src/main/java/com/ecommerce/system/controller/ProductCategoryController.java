package com.ecommerce.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.common.core.domain.Result;
import com.ecommerce.common.core.exception.BusinessException;
import com.ecommerce.commerce.dto.CategorySaveRequest;
import jakarta.validation.Valid;
import com.ecommerce.commerce.entity.ProductCategory;
import com.ecommerce.commerce.service.ProductCategoryService;
import lombok.RequiredArgsConstructor;
import com.ecommerce.system.vo.CategoryTreeVO;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/product/category")
@RequiredArgsConstructor
public class ProductCategoryController {
    
    private final ProductCategoryService productCategoryService;

    private CategoryTreeVO view(ProductCategory entity) {
        if (entity == null) { throw new BusinessException(404, "类别不存在"); }
        CategoryTreeVO result = new CategoryTreeVO();
        BeanUtils.copyProperties(entity, result);
        return result;
    }
    
    @GetMapping("/all")
    public Result<List<CategoryTreeVO>> all() {
        return Result.success(productCategoryService.list(new LambdaQueryWrapper<ProductCategory>()
                .orderByAsc(ProductCategory::getSortOrder).orderByAsc(ProductCategory::getId)).stream().map(this::view).toList());
    }

    @GetMapping("/tree")
    public Result<List<CategoryTreeVO>> getTree() {
        List<ProductCategory> list = productCategoryService.getTreeList();
        return Result.success(list.stream().map(this::view).toList());
    }
    
    @GetMapping("/children/{parentId}")
    public Result<List<CategoryTreeVO>> getChildren(@PathVariable Long parentId) {
        List<ProductCategory> list = productCategoryService.getChildrenByParentId(parentId);
        return Result.success(list.stream().map(this::view).toList());
    }
    
    @GetMapping("/list")
    public Result<Page<CategoryTreeVO>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Long parentId,
            @RequestParam(required = false) Integer status) {
        
        if (pageNum < 1 || pageSize < 1 || pageSize > 100) { throw new BusinessException(400, "分页参数无效"); }
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
        Page<CategoryTreeVO> response = new Page<>(pageNum, pageSize, result.getTotal());
        response.setRecords(result.getRecords().stream().map(this::view).toList());
        return Result.success(response);
    }
    
    @GetMapping("/{id}")
    public Result<CategoryTreeVO> getById(@PathVariable Long id) {
        ProductCategory category = productCategoryService.getById(id);
        return Result.success(view(category));
    }
    
    @PostMapping
    public Result<CategoryTreeVO> save(@Valid @RequestBody CategorySaveRequest category) {
        return Result.success(view(productCategoryService.saveCategory(category, false)));
    }

    @PutMapping
    public Result<CategoryTreeVO> update(@Valid @RequestBody CategorySaveRequest category) {
        if (category.getId() == null) throw new BusinessException(400, "请选择要编辑的类别");
        return Result.success(view(productCategoryService.saveCategory(category, true)));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        productCategoryService.deleteCategory(id);
        return Result.success();
    }
}
