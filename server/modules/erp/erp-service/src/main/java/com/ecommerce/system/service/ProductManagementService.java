package com.ecommerce.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.commerce.dto.ProductSaveRequest;
import com.ecommerce.commerce.entity.Product;
import com.ecommerce.commerce.entity.ProductBrand;
import com.ecommerce.commerce.entity.ProductCategory;
import com.ecommerce.commerce.service.ProductBrandService;
import com.ecommerce.commerce.service.ProductCategoryService;
import com.ecommerce.commerce.service.ProductService;
import com.ecommerce.common.core.exception.BusinessException;
import com.ecommerce.system.vo.ProductManagementVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

/** ERP 商品列表与编辑编排。 */
@Service
@RequiredArgsConstructor
public class ProductManagementService {
  private final ProductService products;
  private final ProductCategoryService categories;
  private final ProductBrandService brands;
  private final ProductMediaService media;

  public record CategoryOption(Long id, String categoryName, String categoryCode, Long parentId, Integer status) { }
  public record BrandOption(Long id, String brandName, Integer status) { }
  public record Options(List<CategoryOption> categories, List<BrandOption> brands) { }

  public Page<ProductManagementVO> list(int pageNum, int pageSize, Long categoryId, Long brandId,
      String code, String name, Integer status) {
    if (pageNum < 1 || pageSize < 1 || pageSize > 100 || (status != null && status != 0 && status != 1)) {
      throw new BusinessException(400, "分页或状态参数无效");
    }
    LambdaQueryWrapper<Product> query = new LambdaQueryWrapper<Product>()
        .eq(categoryId != null, Product::getCategoryId, categoryId)
        .eq(brandId != null, Product::getBrandId, brandId)
        .eq(status != null, Product::getStatus, status);
    if (name != null && !name.isBlank()) {
      query.like(Product::getProductName, name.trim());
    }
    if (code != null && !code.isBlank()) {
      query.like(Product::getProductCode, code.trim());
    }
    query.orderByAsc(Product::getSortOrder).orderByDesc(Product::getId);
    Page<Product> result = products.page(new Page<>(pageNum, pageSize), query);
    Page<ProductManagementVO> page = new Page<>(pageNum, pageSize, result.getTotal());
    page.setRecords(result.getRecords().stream().map(ProductManagementVO::from).toList());
    return page;
  }

  public Options options() {
    return new Options(categories.list(new LambdaQueryWrapper<ProductCategory>().orderByAsc(ProductCategory::getSortOrder))
        .stream().map(c -> new CategoryOption(c.getId(), c.getCategoryName(), c.getCategoryCode(), c.getParentId(), c.getStatus())).toList(),
        brands.list(new LambdaQueryWrapper<ProductBrand>().orderByAsc(ProductBrand::getSortOrder))
        .stream().map(b -> new BrandOption(b.getId(), b.getBrandName(), b.getStatus())).toList());
  }

  public ProductManagementVO get(Long id) {
    Product product = products.getById(id);
    if (product == null) {
      throw new BusinessException(404, "商品不存在或已删除");
    }
    return ProductManagementVO.from(product);
  }

  public ProductManagementVO save(ProductSaveRequest request, boolean updating, Long actorId) {
    media.validateMainImage(updating ? request.getId() : null, request.getMainImage(), actorId);
    return ProductManagementVO.from(products.saveProduct(request, updating));
  }

  public void delete(Long id) {
    if (!products.removeById(id)) {
      throw new BusinessException(404, "商品不存在或已删除");
    }
  }
}
