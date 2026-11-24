package com.ecommerce.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("product")
public class Product {
    
    @TableId(type = IdType.AUTO)
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
    
    private String createBy;
    
    private LocalDateTime createTime;
    
    private String updateBy;
    
    private LocalDateTime updateTime;
    
    @TableLogic
    private Integer deleted;
}
