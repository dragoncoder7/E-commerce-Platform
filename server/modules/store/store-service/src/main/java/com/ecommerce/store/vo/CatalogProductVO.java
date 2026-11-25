package com.ecommerce.store.vo;

import com.ecommerce.commerce.entity.Product;
import java.math.BigDecimal;
import java.util.List;

/** 商城商品投影，不暴露后台审计字段。 */
public record CatalogProductVO(Long id, String name, String subtitle, String image, String description,
    Long categoryId, Long brandId, BigDecimal price, Integer stock, Integer sales,
    List<SkuVO> skus, List<String> images) {
  public record SkuVO(Long id, String specification, BigDecimal price, Integer stock) { }

  public static CatalogProductVO from(Product p, List<SkuVO> skus, List<String> images) {
    return new CatalogProductVO(p.getId(), p.getProductName(), p.getSubTitle(), p.getMainImage(),
        p.getDescription(), p.getCategoryId(), p.getBrandId(), p.getSalePrice(), p.getStock(), p.getSalesCount(), skus, images);
  }
}
