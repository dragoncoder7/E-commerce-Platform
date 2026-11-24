package com.ecommerce.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("product_attribute")
public class ProductAttribute {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long productId;
    
    private String attributeName;
    
    private String attributeValue;
    
    private Integer sortOrder;
    
    private LocalDateTime createTime;
    
    @TableLogic
    private Integer deleted;
}
