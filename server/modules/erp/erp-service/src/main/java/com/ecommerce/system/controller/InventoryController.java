package com.ecommerce.system.controller;

import com.ecommerce.common.core.domain.Result;
import com.ecommerce.commerce.entity.ProductSku;
import com.ecommerce.commerce.service.InventoryService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import java.math.BigDecimal;
import java.util.List;

/** 商品规格及库存操作。 */
@RestController
@RequestMapping("/product/{productId}/skus")
@RequiredArgsConstructor
public class InventoryController {
  private final InventoryService inventory;

  public record SkuView(Long id, String skuCode, String specification, BigDecimal price, Integer stock, Integer status) {
    public static SkuView from(ProductSku sku) {
      return new SkuView(sku.getId(), sku.getSkuCode(), sku.getSpecification(), sku.getPrice(), sku.getStock(), sku.getStatus());
    }
  }
  public record SkuRequest(Long id, String skuCode, String specification, BigDecimal price, Integer status) { }
  public record Adjustment(@NotNull Integer delta, @NotBlank @Size(max = 200) String reason) { }

  @GetMapping
  public Result<List<SkuView>> list(@PathVariable Long productId) {
    return Result.success(inventory.list(productId).stream().map(SkuView::from).toList());
  }

  @PutMapping
  public Result<SkuView> save(@PathVariable Long productId, @RequestBody SkuRequest request) {
    ProductSku sku = new ProductSku();
    sku.setId(request.id());
    sku.setSkuCode(request.skuCode());
    sku.setSpecification(request.specification());
    sku.setPrice(request.price());
    sku.setStatus(request.status());
    return Result.success(SkuView.from(inventory.save(productId, sku)));
  }

  public record StockLogView(Long id, Integer delta, Integer balance, String actor, String reason, java.time.LocalDateTime createTime) { }

  @GetMapping("/{skuId}/history")
  public Result<com.baomidou.mybatisplus.extension.plugins.pagination.Page<StockLogView>> history(@PathVariable Long productId,
      @PathVariable Long skuId, @org.springframework.web.bind.annotation.RequestParam(defaultValue = "1") int pageNum,
      @org.springframework.web.bind.annotation.RequestParam(defaultValue = "10") int pageSize) {
    if (inventory.list(productId).stream().noneMatch(sku -> sku.getId().equals(skuId))) {
      throw new com.ecommerce.common.core.exception.BusinessException(404, "规格不存在");
    }
    var result = inventory.history(skuId, pageNum, pageSize);
    var page = new com.baomidou.mybatisplus.extension.plugins.pagination.Page<StockLogView>(pageNum, pageSize, result.getTotal());
    page.setRecords(result.getRecords().stream().map(log -> new StockLogView(log.getId(), log.getDelta(), log.getBalance(),
        log.getActor(), log.getReason(), log.getCreateTime())).toList());
    return Result.success(page);
  }

  @PostMapping("/{skuId}/stock")
  public Result<Void> adjust(@PathVariable Long productId, @PathVariable Long skuId,
      @Valid @RequestBody Adjustment request, HttpServletRequest servletRequest) {
    if (inventory.list(productId).stream().noneMatch(s -> s.getId().equals(skuId))) {
      throw new com.ecommerce.common.core.exception.BusinessException(404, "规格不存在");
    }
    inventory.adjust(skuId, request.delta(), "erp:" + servletRequest.getAttribute("actorId"), request.reason());
    return Result.success();
  }
}
