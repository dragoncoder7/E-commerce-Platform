package com.ecommerce.commerce.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.ecommerce.commerce.entity.Product;
import com.ecommerce.commerce.dto.ProductSaveRequest;

public interface ProductService extends IService<Product> {
    Product saveProduct(ProductSaveRequest request, boolean updating);
}
