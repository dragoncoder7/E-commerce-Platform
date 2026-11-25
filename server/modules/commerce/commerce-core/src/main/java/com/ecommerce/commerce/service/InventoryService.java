package com.ecommerce.commerce.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ecommerce.commerce.entity.ProductSku;
import com.ecommerce.commerce.mapper.ProductSkuMapper;
import com.ecommerce.common.core.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;

/** SKU 是库存事实来源，商品字段仅为查询汇总。 */
@Service
@RequiredArgsConstructor
public class InventoryService {
  private final ProductSkuMapper skuMapper;
  private final JdbcTemplate jdbc;
  private final com.ecommerce.commerce.mapper.InventoryLogMapper logs;

  public com.baomidou.mybatisplus.extension.plugins.pagination.Page<com.ecommerce.commerce.entity.InventoryLog> history(Long skuId, int pageNum, int pageSize) {
    if (pageNum < 1 || pageSize < 1 || pageSize > 100) {
      throw new BusinessException(400, "分页参数无效");
    }
    return logs.selectPage(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(pageNum, pageSize),
        new LambdaQueryWrapper<com.ecommerce.commerce.entity.InventoryLog>()
            .eq(com.ecommerce.commerce.entity.InventoryLog::getSkuId, skuId)
            .orderByDesc(com.ecommerce.commerce.entity.InventoryLog::getId));
  }

  public List<ProductSku> list(Long productId) {
    return skuMapper.selectList(new LambdaQueryWrapper<ProductSku>()
        .eq(ProductSku::getProductId, productId).orderByAsc(ProductSku::getId));
  }

  @Transactional(rollbackFor = Exception.class)
  public void adjust(Long skuId, int delta, String actor, String reason) {
    if (delta == 0 || reason == null || reason.isBlank() || reason.length() > 200) {
      throw new BusinessException(400, "请输入库存变化数量和调整原因");
    }
    ProductSku known = skuMapper.selectById(skuId);
    if (known == null) {
      throw new BusinessException(404, "规格不存在");
    }
    List<Long> ids = jdbc.queryForList("SELECT id FROM product WHERE id=? AND deleted=0 FOR UPDATE", Long.class, known.getProductId());
    if (ids.isEmpty()) {
      throw new BusinessException(404, "商品不存在或已删除");
    }
    ProductSku sku = skuMapper.lock(skuId);
    if (sku == null) {
      throw new BusinessException(404, "规格不存在");
    }
    if ((long) sku.getStock() + delta > Integer.MAX_VALUE || skuMapper.adjust(skuId, delta) != 1) {
      throw new BusinessException(409, "库存不足或数量超出范围");
    }
    jdbc.update("INSERT INTO inventory_log(sku_id,delta,balance,actor,reason) VALUES(?,?,?,?,?)",
        skuId, delta, sku.getStock() + delta, actor, reason);
    refresh(sku.getProductId());
  }

  public void refresh(Long productId) {
    Long total = jdbc.queryForObject("SELECT COALESCE(SUM(stock),0) FROM product_sku WHERE product_id=? AND status=1", Long.class, productId);
    if (total != null && total > Integer.MAX_VALUE) {
      throw new BusinessException(409, "商品总库存超过允许上限");
    }
    jdbc.update("""
        UPDATE product SET stock = (SELECT COALESCE(SUM(stock),0) FROM product_sku WHERE product_id=? AND status=1),
        sale_price = (SELECT COALESCE(MIN(price),0) FROM product_sku WHERE product_id=? AND status=1)
        WHERE id=?
        """, productId, productId, productId);
  }

  @Transactional(rollbackFor = Exception.class)
  public ProductSku save(Long productId, ProductSku command) {
    // 产品行锁串行化同一商品下的规格编辑和汇总。
    List<Long> ids = jdbc.queryForList("SELECT id FROM product WHERE id=? AND deleted=0 FOR UPDATE", Long.class, productId);
    if (ids.isEmpty()) {
      throw new BusinessException(404, "商品不存在");
    }
    if (command.getSkuCode() == null || !command.getSkuCode().matches("[A-Za-z0-9_-]{1,64}")
        || command.getSpecification() == null || command.getSpecification().isBlank() || command.getSpecification().length() > 200
        || command.getPrice() == null || command.getPrice().compareTo(BigDecimal.ZERO) < 0
        || command.getPrice().scale() > 2 || command.getPrice().compareTo(new BigDecimal("99999999.99")) > 0
        || command.getStatus() == null || command.getStatus() < 0 || command.getStatus() > 1) {
      throw new BusinessException(400, "请填写有效的规格编码、名称、价格和状态");
    }
    command.setProductId(productId);
    if (command.getId() == null) {
      command.setStock(0);
      skuMapper.insert(command);
    } else {
      ProductSku existing = skuMapper.lock(command.getId());
      if (existing == null || !productId.equals(existing.getProductId())) {
        throw new BusinessException(404, "规格不存在");
      }
      command.setStock(existing.getStock());
      skuMapper.updateById(command);
    }
    refresh(productId);
    return command;
  }
}
