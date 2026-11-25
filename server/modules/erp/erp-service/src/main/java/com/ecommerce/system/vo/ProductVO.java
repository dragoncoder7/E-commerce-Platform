package com.ecommerce.system.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class ProductVO {
    
    private Long id;
    
    private String productCode;
    
    private String productName;
    
    private Long categoryId;
    
    private String categoryName;
    
    private Long brandId;
    
    private String brandName;
    
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
    
    private List<ProductImageVO> images;
    
    private List<ProductAttributeVO> attributes;
    
    @Data
    public static class ProductImageVO {
        private Long id;
        private String imageUrl;
        private Integer imageType;
        private Integer sortOrder;
    }
    
    @Data
    public static class ProductAttributeVO {
        private Long id;
        private String attributeName;
        private String attributeValue;
        private Integer sortOrder;
    }
}
