package com.ecommerce.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("product_brand")
public class ProductBrand {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String brandName;
    
    private String brandCode;
    
    private String logo;
    
    private String description;
    
    private Integer sortOrder;
    
    private Integer status;
    
    private String createBy;
    
    private LocalDateTime createTime;
    
    private String updateBy;
    
    private LocalDateTime updateTime;
    
    @TableLogic
    private Integer deleted;
}
