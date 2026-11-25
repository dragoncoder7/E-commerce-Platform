package com.ecommerce.commerce.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;
import java.math.BigDecimal;

/** 独立计价及库存的商品销售规格。 */
@Data
public class ProductSku {
  @TableId(type = IdType.AUTO)
  private Long id;
  private Long productId;
  private String skuCode;
  private String specification;
  private BigDecimal price;
  private Integer stock;
  private Integer status;
}
