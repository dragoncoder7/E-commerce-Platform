package com.ecommerce.system.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.ecommerce.commerce.entity.Product;
import com.ecommerce.commerce.service.ProductImageService;
import com.ecommerce.commerce.service.ProductService;
import com.ecommerce.common.core.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProductMediaValidationTest {
  private final ProductService products = mock(ProductService.class);
  private final ProductImageService images = mock(ProductImageService.class);
  private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
  private final ProductMediaService service = new ProductMediaService(products, images, jdbc);

  @BeforeEach
  void setup() {
    when(jdbc.queryForObject(anyString(), eq(Long.class), any(), any())).thenReturn(0L);
  }

  @Test
  void rejectsUnsafeMainImageSchemes() {
    for (String url : List.of("javascript:alert(1)", "data:image/png;base64,AA", "//evil.example/image")) {
      assertThrows(BusinessException.class, () -> service.validateMainImage(null, url, 1L));
    }
  }

  @Test
  void rejectsUnownedLocalImageForCreateAndUpdate() {
    assertThrows(BusinessException.class, () -> service.validateMainImage(null, "/api/media/other.png", 1L));
    when(products.getById(9L)).thenReturn(new Product());
    when(images.list(any(Wrapper.class))).thenReturn(List.of());
    assertThrows(BusinessException.class, () -> service.validateMainImage(9L, "/api/media/other.png", 1L));
  }

  @Test
  void acceptsOwnedImage() {
    when(jdbc.queryForObject(anyString(), eq(Long.class), any(), any())).thenReturn(1L);
    assertDoesNotThrow(() -> service.validateMainImage(null, "/api/media/own.png", 1L));
  }

  @Test
  void preservesExistingMainImageWhenAnotherOperatorEdits() {
    Product product = new Product();
    product.setMainImage("/api/media/existing.png");
    when(products.getById(9L)).thenReturn(product);
    when(images.list(any(Wrapper.class))).thenReturn(List.of());
    assertDoesNotThrow(() -> service.validateMainImage(9L, product.getMainImage(), 2L));
  }

  @Test
  void acceptsEmptyAndPublicHttpImage() {
    assertDoesNotThrow(() -> service.validateMainImage(null, "", 1L));
    assertDoesNotThrow(() -> service.validateMainImage(null, "https://example.com/product.png", 1L));
  }
}
