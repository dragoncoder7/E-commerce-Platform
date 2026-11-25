package com.ecommerce.commerce.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;

/** 基础商品编辑字段，不允许客户端修改统计、审计及删除标记。 */
@Data
public class ProductSaveRequest {
    /** 内部审计身份，永不从 JSON 绑定。 */
    @com.fasterxml.jackson.annotation.JsonIgnore
    private String operator = "system";
    @Positive(message = "商品 ID 必须大于 0")
    private Long id;
    @NotBlank(message = "请输入商品编号") @Size(max = 50, message = "商品编号最多 50 字符")
    private String productCode;
    @NotBlank(message = "请输入商品名称") @Size(max = 200, message = "商品名称最多 200 字符")
    private String productName;
    @NotNull(message = "请选择商品分类") @Positive(message = "分类 ID 必须大于 0")
    private Long categoryId;
    @Positive(message = "品牌 ID 必须大于 0")
    private Long brandId;
    @Size(max = 500, message = "图片地址最多 500 字符")
    private String mainImage;
    @Size(max = 300, message = "副标题最多 300 字符")
    private String subTitle;
    @Size(max = 10000, message = "商品描述最多 10000 字符")
    private String description;
    @DecimalMin(value = "0", message = "原价不能为负数") @Digits(integer = 8, fraction = 2, message = "原价格式错误")
    private BigDecimal originalPrice;
    @NotNull(message = "请输入售价") @DecimalMin(value = "0", message = "售价不能为负数") @Digits(integer = 8, fraction = 2, message = "售价格式错误")
    private BigDecimal salePrice;
    @NotNull(message = "请输入库存") @Min(value = 0, message = "库存不能为负数")
    private Integer stock = 0;
    @NotBlank(message = "请输入计量单位") @Size(max = 20, message = "单位最多 20 字符")
    private String unit = "件";
    @NotNull @Min(0) @Max(1)
    private Integer status = 0;
    @NotNull @Min(0) @Max(1)
    private Integer isHot = 0;
    @NotNull @Min(0) @Max(1)
    private Integer isNew = 0;
    @NotNull @Min(0) @Max(1)
    private Integer isRecommend = 0;
    @NotNull @Min(value = 0, message = "排序值不能为负数")
    private Integer sortOrder = 0;
}
