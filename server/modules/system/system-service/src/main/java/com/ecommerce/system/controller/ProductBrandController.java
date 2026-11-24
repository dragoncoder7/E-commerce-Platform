package com.ecommerce.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.common.core.domain.Result;
import com.ecommerce.system.entity.ProductBrand;
import com.ecommerce.system.service.ProductBrandService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/product/brand")
public class ProductBrandController {
    
    @Autowired
    private ProductBrandService productBrandService;
    
    @GetMapping("/list")
    public Result<Page<ProductBrand>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String brandName,
            @RequestParam(required = false) Integer status) {
        
        Page<ProductBrand> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<ProductBrand> wrapper = new LambdaQueryWrapper<>();
        
        if (brandName != null && !brandName.isEmpty()) {
            wrapper.like(ProductBrand::getBrandName, brandName);
        }
        if (status != null) {
            wrapper.eq(ProductBrand::getStatus, status);
        }
        
        wrapper.orderByAsc(ProductBrand::getSortOrder);
        Page<ProductBrand> result = productBrandService.page(page, wrapper);
        return Result.success(result);
    }
    
    @GetMapping("/{id}")
    public Result<ProductBrand> getById(@PathVariable Long id) {
        ProductBrand brand = productBrandService.getById(id);
        return Result.success(brand);
    }
    
    @PostMapping
    public Result<String> save(@RequestBody ProductBrand brand) {
        boolean success = productBrandService.save(brand);
        return success ? Result.success("添加成功") : Result.fail("添加失败");
    }
    
    @PutMapping
    public Result<String> update(@RequestBody ProductBrand brand) {
        boolean success = productBrandService.updateById(brand);
        return success ? Result.success("更新成功") : Result.fail("更新失败");
    }
    
    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Long id) {
        boolean success = productBrandService.removeById(id);
        return success ? Result.success("删除成功") : Result.fail("删除失败");
    }
}
