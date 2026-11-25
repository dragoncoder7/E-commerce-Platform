package com.ecommerce.system.service;

import com.ecommerce.commerce.entity.ProductSku;
import com.ecommerce.commerce.mapper.InventoryLogMapper;
import com.ecommerce.commerce.mapper.ProductSkuMapper;
import com.ecommerce.commerce.service.InventoryService;
import com.ecommerce.common.core.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InventoryValidationTest {
  @Test
  void rejectsDeletedProductBeforeChangingStock() {
    ProductSkuMapper skus = mock(ProductSkuMapper.class);
    JdbcTemplate jdbc = mock(JdbcTemplate.class);
    ProductSku sku = new ProductSku();
    sku.setProductId(7L);
    when(skus.selectById(3L)).thenReturn(sku);
    when(jdbc.queryForList("SELECT id FROM product WHERE id=? AND deleted=0 FOR UPDATE", Long.class, 7L))
        .thenReturn(List.of());
    InventoryService service = new InventoryService(skus, jdbc, mock(InventoryLogMapper.class));
    assertThrows(BusinessException.class, () -> service.adjust(3L, 1, "erp:1", "测试"));
    verify(skus, never()).adjust(anyLong(), anyInt());
    verify(skus, never()).lock(anyLong());
  }
}
