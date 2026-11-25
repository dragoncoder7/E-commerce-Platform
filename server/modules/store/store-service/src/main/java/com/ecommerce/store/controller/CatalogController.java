package com.ecommerce.store.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.common.core.domain.Result;
import com.ecommerce.store.service.CatalogService;
import com.ecommerce.store.vo.CatalogProductVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.math.BigDecimal;
import java.util.List;

/** 无需登录的商城目录接口。 */
@RestController
@RequestMapping("/catalog")
@RequiredArgsConstructor
public class CatalogController {
  private final CatalogService catalog;

  @GetMapping("/categories")
  public Result<List<CatalogService.Option>> categories() {
    return Result.success(catalog.categories());
  }

  @GetMapping("/brands")
  public Result<List<CatalogService.Option>> brands() {
    return Result.success(catalog.brands());
  }

  @GetMapping("/products")
  public Result<Page<CatalogProductVO>> products(@RequestParam(defaultValue = "1") int pageNum,
      @RequestParam(defaultValue = "12") int pageSize, @RequestParam(required = false) String keyword,
      @RequestParam(required = false) Long categoryId, @RequestParam(required = false) Long brandId,
      @RequestParam(required = false) BigDecimal minPrice, @RequestParam(required = false) BigDecimal maxPrice,
      @RequestParam(defaultValue = "default") String sort, @RequestParam(defaultValue = "false") boolean recommended) {
    return Result.success(catalog.search(pageNum, pageSize, keyword, categoryId, brandId, minPrice, maxPrice, sort, recommended));
  }

  @GetMapping("/products/{id}")
  public Result<CatalogProductVO> detail(@PathVariable Long id) {
    return Result.success(catalog.detail(id));
  }
}
