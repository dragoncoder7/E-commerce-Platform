package com.ecommerce.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.common.core.domain.Result;
import com.ecommerce.system.dto.BrandModels.BrandRequest;
import com.ecommerce.system.dto.BrandModels.BrandView;
import com.ecommerce.system.service.BrandManagementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 品牌 HTTP 接口。 */
@RestController
@RequestMapping("/product/brand")
@RequiredArgsConstructor
public class ProductBrandController {
  private final BrandManagementService brands;

  @GetMapping("/list")
  public Result<Page<BrandView>> list(@RequestParam(defaultValue = "1") int pageNum,
      @RequestParam(defaultValue = "10") int pageSize, @RequestParam(required = false) String brandName) {
    return Result.success(brands.list(pageNum, pageSize, brandName));
  }

  @PostMapping
  public Result<BrandView> create(@Valid @RequestBody BrandRequest request) {
    return Result.success(brands.save(request, false));
  }

  @PutMapping
  public Result<BrandView> update(@Valid @RequestBody BrandRequest request) {
    return Result.success(brands.save(request, true));
  }

  @DeleteMapping("/{id}")
  public Result<Void> delete(@PathVariable Long id) {
    brands.delete(id);
    return Result.success();
  }
}
