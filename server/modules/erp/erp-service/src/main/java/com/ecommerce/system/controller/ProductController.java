package com.ecommerce.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.common.core.domain.Result;
import com.ecommerce.commerce.dto.ProductSaveRequest;
import com.ecommerce.system.service.ProductManagementService;
import com.ecommerce.system.service.ProductMediaService;
import com.ecommerce.system.vo.ProductManagementVO;
import jakarta.servlet.http.HttpServletRequest;
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
import java.util.List;

/** 后台商品接口，兼容现有路径。 */
@RestController
@RequestMapping("/product")
@RequiredArgsConstructor
public class ProductController {
  private final ProductManagementService products;
  private final ProductMediaService media;

  @GetMapping("/list")
  public Result<Page<ProductManagementVO>> list(@RequestParam(defaultValue = "1") Integer pageNum,
      @RequestParam(defaultValue = "10") Integer pageSize, @RequestParam(required = false) Long categoryId,
      @RequestParam(required = false) Long brandId, @RequestParam(required = false) String productCode,
      @RequestParam(required = false) String productName, @RequestParam(required = false) Integer status) {
    return Result.success(products.list(pageNum, pageSize, categoryId, brandId, productCode, productName, status));
  }

  @GetMapping("/options")
  public Result<ProductManagementService.Options> options() {
    return Result.success(products.options());
  }

  @GetMapping("/{id}")
  public Result<ProductManagementVO> getById(@PathVariable Long id) {
    return Result.success(products.get(id));
  }

  @PostMapping
  public Result<ProductManagementVO> save(@Valid @RequestBody ProductSaveRequest product, HttpServletRequest request) {
    product.setOperator("erp:" + request.getAttribute("actorId"));
    return Result.success(products.save(product, false, (Long) request.getAttribute("actorId")));
  }

  @PutMapping
  public Result<ProductManagementVO> update(@Valid @RequestBody ProductSaveRequest product, HttpServletRequest request) {
    product.setOperator("erp:" + request.getAttribute("actorId"));
    return Result.success(products.save(product, true, (Long) request.getAttribute("actorId")));
  }

  @DeleteMapping("/{id}")
  public Result<Void> delete(@PathVariable Long id) {
    products.delete(id);
    return Result.success();
  }

  @GetMapping("/{id}/images")
  public Result<List<String>> images(@PathVariable Long id) {
    return Result.success(media.list(id));
  }

  @PostMapping("/{id}/images")
  public Result<Void> saveImages(@PathVariable Long id, @RequestBody List<String> urls, HttpServletRequest request) {
    media.save(id, urls, (Long) request.getAttribute("actorId"));
    return Result.success();
  }
}
