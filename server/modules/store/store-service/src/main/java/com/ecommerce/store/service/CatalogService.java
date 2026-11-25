package com.ecommerce.store.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.commerce.entity.Product;
import com.ecommerce.commerce.entity.ProductCategory;
import com.ecommerce.commerce.service.InventoryService;
import com.ecommerce.commerce.service.ProductCategoryService;
import com.ecommerce.commerce.service.ProductService;
import com.ecommerce.common.core.exception.BusinessException;
import com.ecommerce.store.vo.CatalogProductVO;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** 商城只查询已启用分类中的上架商品。 */
@Service
@RequiredArgsConstructor
public class CatalogService {
  private final ProductService products;
  private final ProductCategoryService categories;
  private final InventoryService inventory;
  private final JdbcTemplate jdbc;

  public record Option(Long id, Long parentId, String name) { }

  public List<Option> categories() {
    List<ProductCategory> all = categories.getTreeList();
    Set<Long> enabled = new HashSet<>();
    enabled.add(0L);
    boolean changed;
    do {
      changed = false;
      for (ProductCategory category : all) {
        if (enabled.contains(category.getParentId())) {
          changed |= enabled.add(category.getId());
        }
      }
    } while (changed);
    return all.stream().filter(c -> enabled.contains(c.getId()))
        .map(c -> new Option(c.getId(), c.getParentId(), c.getCategoryName())).toList();
  }

  public List<Option> brands() {
    return jdbc.query("SELECT id,brand_name FROM product_brand WHERE deleted=0 AND status=1 ORDER BY sort_order,id",
        (rs, row) -> new Option(rs.getLong("id"), 0L, rs.getString("brand_name")));
  }

  public Page<CatalogProductVO> search(int pageNum, int pageSize, String keyword, Long categoryId,
      Long brandId, BigDecimal minPrice, BigDecimal maxPrice, String sort, boolean recommended) {
    if (pageNum < 1 || pageSize < 1 || pageSize > 100 || (minPrice != null && minPrice.signum() < 0)
        || (maxPrice != null && maxPrice.signum() < 0) || (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0)) {
      throw new BusinessException(400, "分页或价格范围无效");
    }
    List<Option> options = categories();
    Set<Long> ids = new HashSet<>();
    if (categoryId == null) {
      options.forEach(c -> ids.add(c.id()));
    } else if (options.stream().anyMatch(c -> c.id().equals(categoryId))) {
      ids.add(categoryId);
      for (int i = 0; i < options.size(); i++) {
        options.stream().filter(c -> ids.contains(c.parentId())).forEach(c -> ids.add(c.id()));
      }
    }
    if (ids.isEmpty()) {
      return new Page<>(pageNum, pageSize, 0);
    }
    LambdaQueryWrapper<Product> query = new LambdaQueryWrapper<Product>().eq(Product::getStatus, 1)
        .in(Product::getCategoryId, ids)
        .inSql(Product::getId, "SELECT product_id FROM product_sku WHERE status=1").eq(brandId != null, Product::getBrandId, brandId)
        .like(keyword != null && !keyword.isBlank(), Product::getProductName, keyword)
        .ge(minPrice != null, Product::getSalePrice, minPrice).le(maxPrice != null, Product::getSalePrice, maxPrice)
        .eq(recommended, Product::getIsRecommend, 1);
    switch (sort) {
      case "priceAsc" -> query.orderByAsc(Product::getSalePrice);
      case "priceDesc" -> query.orderByDesc(Product::getSalePrice);
      case "sales" -> query.orderByDesc(Product::getSalesCount);
      case "newest" -> query.orderByDesc(Product::getCreateTime);
      case "default" -> query.orderByAsc(Product::getSortOrder);
      default -> throw new BusinessException(400, "排序方式无效");
    }
    query.orderByDesc(Product::getId);
    Page<Product> result = products.page(new Page<>(pageNum, pageSize), query);
    Page<CatalogProductVO> response = new Page<>(pageNum, pageSize, result.getTotal());
    response.setRecords(result.getRecords().stream().map(p -> CatalogProductVO.from(p, List.of(), List.of())).toList());
    return response;
  }

  public CatalogProductVO detail(Long id) {
    Product p = products.getById(id);
    if (p == null || !Integer.valueOf(1).equals(p.getStatus()) || categories().stream().noneMatch(c -> c.id().equals(p.getCategoryId()))) {
      throw new BusinessException(404, "商品不存在或已下架");
    }
    List<CatalogProductVO.SkuVO> skus = inventory.list(id).stream().filter(s -> Integer.valueOf(1).equals(s.getStatus()))
        .map(s -> new CatalogProductVO.SkuVO(s.getId(), s.getSpecification(), s.getPrice(), s.getStock())).toList();
    if (skus.isEmpty()) {
      throw new BusinessException(404, "商品暂无可售规格");
    }
    List<String> images = new ArrayList<>();
    if (p.getMainImage() != null && !p.getMainImage().isBlank()) {
      images.add(p.getMainImage());
    }
    images.addAll(jdbc.queryForList("SELECT image_url FROM product_image WHERE product_id=? AND deleted=0 ORDER BY sort_order,id", String.class, id));
    return CatalogProductVO.from(p, skus, images.stream().distinct().toList());
  }
}
