package com.ecommerce.commerce.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;
import java.time.LocalDateTime;

/** 不可修改的库存流水。 */
@Data
public class InventoryLog {
  @TableId(type = IdType.AUTO)
  private Long id;
  private Long skuId;
  private Integer delta;
  private Integer balance;
  private String actor;
  private String reason;
  private LocalDateTime createTime;
}
