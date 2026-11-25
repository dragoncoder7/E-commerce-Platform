package com.ecommerce.commerce.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class CategorySaveRequest {
    private Long id;
    @NotNull @Min(0)
    private Long parentId = 0L;
    @NotBlank(message = "类别名称不能为空") @Size(max = 100)
    private String categoryName;
    @NotBlank(message = "类别编号不能为空") @Size(max = 50)
    private String categoryCode;
    @NotNull @Min(0)
    private Integer sortOrder = 0;
    @Size(max = 500)
    private String description = "";
    @NotNull @Min(0) @Max(1)
    private Integer status = 1;
}
