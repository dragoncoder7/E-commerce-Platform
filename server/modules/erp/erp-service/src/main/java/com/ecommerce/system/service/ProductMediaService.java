package com.ecommerce.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ecommerce.commerce.entity.Product;
import com.ecommerce.commerce.entity.ProductImage;
import com.ecommerce.commerce.service.ProductImageService;
import com.ecommerce.commerce.service.ProductService;
import com.ecommerce.common.core.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.net.URI;
import java.util.List;

/** ERP 图片关联只接受本站已登记图片或合法外部展示地址。 */
@Service
@RequiredArgsConstructor
public class ProductMediaService {
  private final ProductService products;
  private final ProductImageService images;
  private final JdbcTemplate jdbc;

  public List<String> list(Long id) {
    requireProduct(id);
    return images.list(new LambdaQueryWrapper<ProductImage>().eq(ProductImage::getProductId, id)
        .orderByAsc(ProductImage::getSortOrder)).stream().map(ProductImage::getImageUrl).toList();
  }

  @Transactional(rollbackFor = Exception.class)
  public void save(Long id, List<String> urls, Long actorId) {
    requireProduct(id);
    if (urls == null || urls.size() > 9 || urls.stream().distinct().count() != urls.size()) {
      throw new BusinessException(400, "最多保存 9 张不同图片");
    }
    List<String> existing = list(id);
    validateUrls(urls, existing, actorId);
    images.saveImages(id, urls);
    products.update(new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Product>()
        .eq(Product::getId, id).set(Product::getMainImage, urls.isEmpty() ? "" : urls.getFirst()));
  }

  /** 主图与图库使用相同的媒体类型和上传身份检查。 */
  public void validateMainImage(Long productId, String url, Long actorId) {
    if (url == null || url.isBlank()) {
      return;
    }
    List<String> existing = new java.util.ArrayList<>();
    if (productId != null) {
      Product product = requireProduct(productId);
      existing.addAll(list(productId));
      if (product.getMainImage() != null) {
        existing.add(product.getMainImage());
      }
    }
    validateUrls(List.of(url), existing, actorId);
  }

  private void validateUrls(List<String> urls, List<String> existing, Long actorId) {
    for (String url : urls) {
      if (url == null || url.length() > 500) {
        throw new BusinessException(400, "图片地址无效");
      }
      if (url.startsWith("/api/media/")) {
        Long count = jdbc.queryForObject("SELECT COUNT(*) FROM media_file WHERE url=? AND owner_type='erp' AND owner_id=?", Long.class, url, actorId);
        if (!existing.contains(url) && (count == null || count == 0)) {
          throw new BusinessException(403, "只能关联自己上传的图片");
        }
      } else {
        try {
          URI uri = URI.create(url);
          if (uri.getScheme() == null || !List.of("http", "https").contains(uri.getScheme()) || uri.getHost() == null || uri.getUserInfo() != null) {
            throw new IllegalArgumentException();
          }
        } catch (IllegalArgumentException exception) {
          throw new BusinessException(400, "图片地址必须为本站媒体或 HTTP/HTTPS 地址");
        }
      }
    }
  }

  private Product requireProduct(Long id) {
    Product product = products.getById(id);
    if (product == null) {
      throw new BusinessException(404, "商品不存在");
    }
    return product;
  }
}
