package com.ecommerce.system.service;
import com.ecommerce.commerce.service.ProductService;
import com.ecommerce.commerce.service.ProductImageService;
import com.ecommerce.commerce.service.ProductBrandService;
import com.ecommerce.commerce.service.ProductCategoryService;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.common.core.exception.GlobalExceptionHandler;
import com.ecommerce.system.controller.ProductCategoryController;
import com.ecommerce.system.controller.ProductController;
import com.ecommerce.commerce.entity.Product;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CategoryApiTest {
    @Test
    void bindsCategoryPathIdForDelete() throws Exception {
        var service = mock(ProductCategoryService.class);
        var controller = new ProductCategoryController(service);
        var mvc = MockMvcBuilders.standaloneSetup(controller).setControllerAdvice(new GlobalExceptionHandler()).build();
        mvc.perform(delete("/product/category/42")).andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
        verify(service).deleteCategory(42L);
    }

    @Test
    void rejectsInvalidCategoryBeforeWriting() throws Exception {
        var service = mock(ProductCategoryService.class);
        var controller = new ProductCategoryController(service);
        var mvc = MockMvcBuilders.standaloneSetup(controller).setControllerAdvice(new GlobalExceptionHandler()).build();
        mvc.perform(post("/product/category").contentType(MediaType.APPLICATION_JSON)
                .content("{\"categoryName\":\"\",\"categoryCode\":\"\",\"parentId\":-1}"))
                .andExpect(jsonPath("$.code").value(400));
        verifyNoInteractions(service);
    }

    @Test
    void bindsProductListQueryParameters() throws Exception {
        var products = mock(ProductService.class);
        when(products.page(any(), any())).thenReturn(new Page<Product>(1, 10));
        var controller = new ProductController(new ProductManagementService(products, mock(ProductCategoryService.class), mock(ProductBrandService.class), mock(ProductMediaService.class)), mock(ProductMediaService.class));
        var mvc = MockMvcBuilders.standaloneSetup(controller).setControllerAdvice(new GlobalExceptionHandler()).build();
        mvc.perform(get("/product/list").param("pageNum", "1").param("pageSize", "10").param("categoryId", "42"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
        verify(products).page(any(), any());
    }
}
