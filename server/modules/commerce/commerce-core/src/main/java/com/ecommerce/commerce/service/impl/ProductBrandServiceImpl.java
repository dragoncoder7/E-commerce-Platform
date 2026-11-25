package com.ecommerce.commerce.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ecommerce.commerce.entity.ProductBrand;
import com.ecommerce.commerce.mapper.ProductBrandMapper;
import com.ecommerce.commerce.service.ProductBrandService;
import org.springframework.stereotype.Service;

@Service
public class ProductBrandServiceImpl extends ServiceImpl<ProductBrandMapper, ProductBrand> implements ProductBrandService {
}
