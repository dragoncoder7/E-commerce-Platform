package com.ecommerce.commerce.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("product_category")
public class ProductCategory {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long parentId;
    
    private String categoryName;
    
    private String categoryCode;
    
    private Integer level;
    
    private Integer sortOrder;
    
    private String icon;
    
    private String description;
    
    private Integer status;
    
    private String createBy;
    
    private LocalDateTime createTime;
    
    private String updateBy;
    
    private LocalDateTime updateTime;
    
    @TableLogic
    private Integer deleted;
}
