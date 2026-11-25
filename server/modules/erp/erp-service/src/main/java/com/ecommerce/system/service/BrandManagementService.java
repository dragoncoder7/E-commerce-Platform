package com.ecommerce.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.common.core.exception.BusinessException;
import com.ecommerce.commerce.entity.ProductBrand;
import com.ecommerce.commerce.entity.Product;
import com.ecommerce.commerce.service.ProductBrandService;
import com.ecommerce.commerce.service.ProductService;
import com.ecommerce.system.dto.BrandModels.BrandRequest;
import com.ecommerce.system.dto.BrandModels.BrandView;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 品牌应用服务。 */
@Service
@RequiredArgsConstructor
public class BrandManagementService {
  private final ProductBrandService brands;
  private final ProductService products;

  public Page<BrandView> list(int pageNum, int pageSize, String brandName) {
    if (pageNum < 1 || pageSize < 1 || pageSize > 100) {
      throw new BusinessException(400, "分页参数无效");
    }
    Page<ProductBrand> result = brands.page(new Page<>(pageNum, pageSize), new LambdaQueryWrapper<ProductBrand>()
        .like(brandName != null && !brandName.isBlank(), ProductBrand::getBrandName, brandName)
        .orderByAsc(ProductBrand::getSortOrder).orderByDesc(ProductBrand::getId));
    Page<BrandView> view = new Page<>(pageNum, pageSize, result.getTotal());
    view.setRecords(result.getRecords().stream().map(BrandView::from).toList());
    return view;
  }

  @Transactional(rollbackFor = Exception.class)
  public BrandView save(BrandRequest request, boolean updating) {
    if (updating && (request.id() == null || brands.getById(request.id()) == null)) {
      throw new BusinessException(404, "品牌不存在");
    }
    if (!updating && request.id() != null) {
      throw new BusinessException(400, "新增品牌不能指定 ID");
    }
    if (brands.count(new LambdaQueryWrapper<ProductBrand>().eq(ProductBrand::getBrandCode, request.brandCode().trim())
        .ne(request.id() != null, ProductBrand::getId, request.id())) > 0) {
      throw new BusinessException(409, "品牌编码已存在");
    }
    ProductBrand brand = new ProductBrand();
    brand.setId(request.id());
    brand.setBrandName(request.brandName().trim());
    brand.setBrandCode(request.brandCode().trim());
    brand.setLogo(request.logo());
    brand.setDescription(request.description());
    brand.setSortOrder(request.sortOrder());
    brand.setStatus(request.status());
    if (!(updating ? brands.updateById(brand) : brands.save(brand))) {
      throw new BusinessException(409, "品牌已发生变化，请刷新后重试");
    }
    return BrandView.from(brand);
  }

  @Transactional(rollbackFor = Exception.class)
  public void delete(Long id) {
    if (products.count(new LambdaQueryWrapper<Product>().eq(Product::getBrandId, id)) > 0) {
      throw new BusinessException(409, "品牌仍被商品引用");
    }
    if (!brands.removeById(id)) {
      throw new BusinessException(404, "品牌不存在");
    }
  }
}
