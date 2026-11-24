package com.ecommerce.system.vo;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class CategoryTreeVO {
    
    private Long id;
    
    private Long parentId;
    
    private String categoryName;
    
    private String categoryCode;
    
    private Integer level;
    
    private Integer sortOrder;
    
    private String icon;
    
    private String description;
    
    private Integer status;
    
    private LocalDateTime createTime;
    
    private List<CategoryTreeVO> children;
}
