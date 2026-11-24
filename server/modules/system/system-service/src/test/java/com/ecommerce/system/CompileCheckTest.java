package com.ecommerce.system;

import com.ecommerce.system.entity.*;
import com.ecommerce.system.mapper.*;
import com.ecommerce.system.service.*;
import com.ecommerce.system.controller.*;

/**
 * 编译检查类 - 用于验证所有类是否能正确编译
 */
public class CompileCheckTest {

    // Entity检查
    private Product product;
    private ProductCategory productCategory;
    private ProductImage productImage;
    private ProductAttribute productAttribute;
    private ProductBrand productBrand;

    // Mapper检查
    private ProductMapper productMapper;
    private ProductCategoryMapper productCategoryMapper;
    private ProductImageMapper productImageMapper;
    private ProductAttributeMapper productAttributeMapper;
    private ProductBrandMapper productBrandMapper;

    // Service检查
    private ProductService productService;
    private ProductCategoryService productCategoryService;
    private ProductImageService productImageService;
    private ProductAttributeService productAttributeService;
    private ProductBrandService productBrandService;

    // Controller检查
    private ProductController productController;
    private ProductCategoryController productCategoryController;
    private ProductBrandController productBrandController;
    private FileUploadController fileUploadController;

    public void testCompile() {
        System.out.println("All classes compile successfully!");
    }
}
