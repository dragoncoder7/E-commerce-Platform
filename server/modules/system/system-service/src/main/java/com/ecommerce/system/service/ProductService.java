package com.ecommerce.system.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.ecommerce.system.entity.Product;
import com.ecommerce.system.dto.ProductSaveRequest;

public interface ProductService extends IService<Product> {
    Product saveProduct(ProductSaveRequest request, boolean updating);
}
