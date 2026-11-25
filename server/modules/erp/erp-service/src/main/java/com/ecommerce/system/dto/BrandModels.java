package com.ecommerce.system.dto;

import com.ecommerce.commerce.entity.ProductBrand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;

/** 品牌管理请求与响应契约。 */
public final class BrandModels {
  private BrandModels() { }
  public record BrandRequest(Long id, @NotBlank @Size(max=100) String brandName,
      @NotBlank @Size(max=50) String brandCode, @Size(max=500) String logo,
      @Size(max=500) String description, @Min(0) int sortOrder, @Min(0) @Max(1) int status) { }
  public record BrandView(Long id, String brandName, String brandCode, String logo, String description, Integer sortOrder, Integer status) {
    public static BrandView from(ProductBrand b) {
      return new BrandView(b.getId(), b.getBrandName(), b.getBrandCode(), b.getLogo(), b.getDescription(), b.getSortOrder(), b.getStatus());
    }
  }
}
