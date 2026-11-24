package com.ecommerce.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("product_image")
public class ProductImage {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long productId;
    
    private String imageUrl;
    
    private Integer imageType;
    
    private Integer sortOrder;
    
    private LocalDateTime createTime;
    
    @TableLogic
    private Integer deleted;
}
