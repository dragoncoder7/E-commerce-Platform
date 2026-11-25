package com.ecommerce.commerce.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ecommerce.common.core.exception.BusinessException;
import com.ecommerce.commerce.dto.ProductSaveRequest;
import com.ecommerce.commerce.entity.Product;
import com.ecommerce.commerce.mapper.ProductMapper;
import com.ecommerce.commerce.service.ProductService;
import com.ecommerce.commerce.service.ProductCategoryService;
import com.ecommerce.commerce.service.ProductBrandService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product> implements ProductService {
    private final ProductCategoryService categoryService;
    private final ProductBrandService brandService;
    private final com.ecommerce.commerce.mapper.ProductSkuMapper skuMapper;
    private final com.ecommerce.commerce.mapper.InventoryLogMapper inventoryLogs;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Product saveProduct(ProductSaveRequest request, boolean updating) {
        if (updating && (request.getId() == null || getById(request.getId()) == null)) {
            throw new BusinessException(404, "商品不存在或已删除");
        }
        if (!updating && request.getId() != null) {
            throw new BusinessException(400, "新增商品不能指定 ID");
        }
        if (categoryService.getById(request.getCategoryId()) == null) {
            throw new BusinessException(400, "商品分类不存在");
        }
        categoryService.requireEnabled(request.getCategoryId());
        if (request.getBrandId() != null && brandService.getById(request.getBrandId()) == null) {
            throw new BusinessException(400, "商品品牌不存在");
        }
        String code = request.getProductCode().trim();
        if (baseMapper.countCodeUsage(code, request.getId()) > 0) {
            throw new BusinessException(409, "商品编号已使用，请更换编号（包含已删除商品）");
        }
        Product product = new Product();
        BeanUtils.copyProperties(request, product);
        if (updating) {
            product.setStock(null);
            product.setSalePrice(null);
        }
        product.setProductCode(code);
        product.setProductName(request.getProductName().trim());
        product.setUnit(request.getUnit().trim());
        product.setUpdateTime(LocalDateTime.now());
        try {
            boolean saved = updating ? updateById(product) : save(product);
            if (!saved) throw new BusinessException(404, "商品不存在或已删除");
        } catch (DuplicateKeyException e) {
            throw new BusinessException(409, "商品编号已使用，请更换编号");
        }
        if (!updating) {
            com.ecommerce.commerce.entity.ProductSku sku = new com.ecommerce.commerce.entity.ProductSku();
            sku.setProductId(product.getId());
            sku.setSkuCode(code + "-DEFAULT");
            sku.setSpecification("默认规格");
            sku.setPrice(product.getSalePrice());
            sku.setStock(product.getStock());
            sku.setStatus(1);
            skuMapper.insert(sku);
            if (sku.getStock() > 0) {
                com.ecommerce.commerce.entity.InventoryLog log = new com.ecommerce.commerce.entity.InventoryLog();
                log.setSkuId(sku.getId());
                log.setDelta(sku.getStock());
                log.setBalance(sku.getStock());
                log.setActor(request.getOperator());
                log.setReason("创建商品初始入库");
                inventoryLogs.insert(log);
            }
        }
        return getById(product.getId());
    }
}
