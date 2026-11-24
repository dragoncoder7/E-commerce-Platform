package com.ecommerce.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.common.core.domain.Result;
import com.ecommerce.common.core.exception.BusinessException;
import com.ecommerce.system.dto.ProductSaveRequest;
import com.ecommerce.system.entity.Product;
import com.ecommerce.system.entity.ProductCategory;
import com.ecommerce.system.entity.ProductBrand;
import com.ecommerce.system.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/product")
@RequiredArgsConstructor
public class ProductController {
    private final ProductService productService;
    private final ProductImageService productImageService;
    private final ProductCategoryService categoryService;
    private final ProductBrandService brandService;

    @GetMapping("/list")
    public Result<Page<Product>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long brandId,
            @RequestParam(required = false) String productCode,
            @RequestParam(required = false) String productName,
            @RequestParam(required = false) Integer status) {
        if (pageNum < 1 || pageSize < 1 || pageSize > 100) {
            throw new BusinessException(400, "页码必须大于 0，每页数量必须在 1 到 100 之间");
        }
        if (status != null && status != 0 && status != 1) {
            throw new BusinessException(400, "商品状态只能为 0 或 1");
        }
        LambdaQueryWrapper<Product> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(categoryId != null, Product::getCategoryId, categoryId)
                .eq(brandId != null, Product::getBrandId, brandId)
                .eq(status != null, Product::getStatus, status);
        if (productName != null && !productName.isBlank()) wrapper.like(Product::getProductName, productName.trim());
        if (productCode != null && !productCode.isBlank()) wrapper.like(Product::getProductCode, productCode.trim());
        wrapper.orderByAsc(Product::getSortOrder).orderByDesc(Product::getId);
        return Result.success(productService.page(new Page<>(pageNum, pageSize), wrapper));
    }

    @GetMapping("/options")
    public Result<Map<String, Object>> options() {
        return Result.success(Map.of(
                "categories", categoryService.list(new LambdaQueryWrapper<ProductCategory>().orderByAsc(ProductCategory::getSortOrder)),
                "brands", brandService.list(new LambdaQueryWrapper<ProductBrand>().orderByAsc(ProductBrand::getSortOrder))));
    }

    @GetMapping("/{id}")
    public Result<Product> getById(@PathVariable Long id) {
        Product product = productService.getById(id);
        if (product == null) throw new BusinessException(404, "商品不存在或已删除");
        return Result.success(product);
    }

    @PostMapping
    public Result<Product> save(@Valid @RequestBody ProductSaveRequest product) {
        return Result.success(productService.saveProduct(product, false));
    }

    @PutMapping
    public Result<Product> update(@Valid @RequestBody ProductSaveRequest product) {
        return Result.success(productService.saveProduct(product, true));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        if (!productService.removeById(id)) throw new BusinessException(404, "商品不存在或已删除");
        return Result.success();
    }

    @PostMapping("/{id}/images")
    public Result<String> saveImages(@PathVariable Long id, @RequestBody List<String> imageUrls) {
        if (productService.getById(id) == null) throw new BusinessException(404, "商品不存在或已删除");
        boolean success = productImageService.saveImages(id, imageUrls);
        return success ? Result.success("保存图片成功") : Result.fail("保存图片失败");
    }
}
