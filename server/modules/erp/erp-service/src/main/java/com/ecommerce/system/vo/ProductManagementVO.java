package com.ecommerce.system.vo;

import com.ecommerce.commerce.entity.Product;
import lombok.Data;
import org.springframework.beans.BeanUtils;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 后台商品响应，不暴露数据库删除标记或创建人内部字段。 */
@Data
public class ProductManagementVO {
  private Long id;
  private String productCode;
  private String productName;
  private Long categoryId;
  private Long brandId;
  private String mainImage;
  private String subTitle;
  private String description;
  private BigDecimal originalPrice;
  private BigDecimal salePrice;
  private Integer stock;
  private String unit;
  private Integer salesCount;
  private Integer viewCount;
  private Integer status;
  private Integer isHot;
  private Integer isNew;
  private Integer isRecommend;
  private Integer sortOrder;
  private LocalDateTime createTime;
  private LocalDateTime updateTime;

  public static ProductManagementVO from(Product product) {
    ProductManagementVO view = new ProductManagementVO();
    BeanUtils.copyProperties(product, view);
    return view;
  }
}
