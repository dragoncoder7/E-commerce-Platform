package com.ecommerce.system.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ecommerce.system.entity.ProductBrand;
import com.ecommerce.system.mapper.ProductBrandMapper;
import com.ecommerce.system.service.ProductBrandService;
import org.springframework.stereotype.Service;

@Service
public class ProductBrandServiceImpl extends ServiceImpl<ProductBrandMapper, ProductBrand> implements ProductBrandService {
}
