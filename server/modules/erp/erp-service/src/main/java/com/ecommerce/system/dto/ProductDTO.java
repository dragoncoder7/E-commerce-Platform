package com.ecommerce.system.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class ProductDTO {
    
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
    
    private Integer status;
    
    private Integer isHot;
    
    private Integer isNew;
    
    private Integer isRecommend;
    
    private Integer sortOrder;
    
    private List<String> imageUrls;
    
    private List<AttributeDTO> attributes;
    
    @Data
    public static class AttributeDTO {
        private String attributeName;
        private String attributeValue;
        private Integer sortOrder;
    }
}
