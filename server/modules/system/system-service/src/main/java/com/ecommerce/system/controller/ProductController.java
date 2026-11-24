package com.ecommerce.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.common.core.domain.Result;
import com.ecommerce.system.entity.Product;
import com.ecommerce.system.service.ProductService;
import com.ecommerce.system.service.ProductImageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/product")
public class ProductController {
    
    @Autowired
    private ProductService productService;
    
    @Autowired
    private ProductImageService productImageService;
    
    @GetMapping("/list")
    public Result<Page<Product>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String productName,
            @RequestParam(required = false) Integer status) {
        
        Page<Product> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        
        if (categoryId != null) {
            wrapper.eq(Product::getCategoryId, categoryId);
        }
        if (productName != null && !productName.isEmpty()) {
            wrapper.like(Product::getProductName, productName);
        }
        if (status != null) {
            wrapper.eq(Product::getStatus, status);
        }
        
        wrapper.orderByDesc(Product::getCreateTime);
        Page<Product> result = productService.page(page, wrapper);
        return Result.success(result);
    }
    
    @GetMapping("/{id}")
    public Result<Product> getById(@PathVariable Long id) {
        Product product = productService.getById(id);
        return Result.success(product);
    }
    
    @PostMapping
    public Result<String> save(@RequestBody Product product) {
        boolean success = productService.save(product);
        return success ? Result.success("添加成功") : Result.fail("添加失败");
    }
    
    @PutMapping
    public Result<String> update(@RequestBody Product product) {
        boolean success = productService.updateById(product);
        return success ? Result.success("更新成功") : Result.fail("更新失败");
    }
    
    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Long id) {
        boolean success = productService.removeById(id);
        return success ? Result.success("删除成功") : Result.fail("删除失败");
    }
    
    @PostMapping("/{id}/images")
    public Result<String> saveImages(@PathVariable Long id, @RequestBody List<String> imageUrls) {
        boolean success = productImageService.saveImages(id, imageUrls);
        return success ? Result.success("保存图片成功") : Result.fail("保存图片失败");
    }
}
