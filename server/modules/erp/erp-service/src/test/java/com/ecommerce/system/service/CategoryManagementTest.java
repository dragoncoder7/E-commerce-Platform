package com.ecommerce.system.service;
import com.ecommerce.commerce.service.ProductService;
import com.ecommerce.commerce.service.ProductBrandService;
import com.ecommerce.commerce.service.ProductCategoryService;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.ecommerce.common.core.exception.BusinessException;
import com.ecommerce.commerce.dto.CategorySaveRequest;
import com.ecommerce.commerce.dto.ProductSaveRequest;
import com.ecommerce.commerce.mapper.ProductCategoryMapper;
import com.ecommerce.commerce.mapper.ProductMapper;
import com.ecommerce.commerce.service.impl.ProductCategoryServiceImpl;
import com.ecommerce.commerce.service.impl.ProductServiceImpl;
import org.apache.ibatis.session.SqlSession;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;

class CategoryManagementTest {
    private SqlSession session;
    private ProductCategoryServiceImpl service;
    private ProductMapper products;

    @BeforeEach
    void setup() throws Exception {
        var ds = new DriverManagerDataSource("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL", "sa", "");
        // Keep the in-memory database alive while MyBatis opens its connection.
        ds.setUrl(ds.getUrl() + ";DB_CLOSE_DELAY=-1");
        new JdbcTemplate(ds).execute("""
                CREATE TABLE product_category (
                  id BIGINT AUTO_INCREMENT PRIMARY KEY, parent_id BIGINT DEFAULT 0,
                  category_name VARCHAR(100) NOT NULL, category_code VARCHAR(50) NOT NULL,
                  level INT DEFAULT 1, sort_order INT DEFAULT 0, icon VARCHAR(255),
                  description VARCHAR(500), status INT DEFAULT 1, create_by VARCHAR(64),
                  update_by VARCHAR(64), create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                  update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, deleted INT DEFAULT 0,
                  UNIQUE(category_code, deleted))
                """);
        var factory = new MybatisSqlSessionFactoryBean();
        factory.setDataSource(ds);
        var config = new MybatisConfiguration();
        config.setMapUnderscoreToCamelCase(true);
        config.addMapper(ProductCategoryMapper.class);
        factory.setConfiguration(config);
        session = factory.getObject().openSession(true);
        products = mock(ProductMapper.class);
        when(products.selectCount(any())).thenReturn(0L);
        service = new ProductCategoryServiceImpl(products);
        ReflectionTestUtils.setField(service, "baseMapper", session.getMapper(ProductCategoryMapper.class));
    }

    @AfterEach
    void close() { if (session != null) session.close(); }

    private CategorySaveRequest request(String code, long parent) {
        var request = new CategorySaveRequest();
        request.setCategoryName("类别 " + code);
        request.setCategoryCode(code);
        request.setParentId(parent);
        return request;
    }

    @Test
    void createsEditsAndListsDisabledCategories() {
        var request = request(" ROOT ", 0);
        var created = service.saveCategory(request, false);
        assertNotNull(created.getId());
        assertEquals("ROOT", created.getCategoryCode());
        request.setId(created.getId());
        request.setStatus(0);
        request.setDescription("说明");
        request.setSortOrder(5);
        service.saveCategory(request, true);
        assertEquals(1, service.list().size());
        assertEquals(0, service.getTreeList().size());
        var saved = service.getById(created.getId());
        assertEquals(5, saved.getSortOrder());
        assertEquals("说明", saved.getDescription());
        request.setDescription("");
        service.saveCategory(request, true);
        assertEquals("", service.getById(created.getId()).getDescription());
    }

    @Test
    void rejectsCyclesAndMissingParents() {
        var parent = service.saveCategory(request("P", 0), false);
        var child = service.saveCategory(request("C", parent.getId()), false);
        var move = request("P", child.getId());
        move.setId(parent.getId());
        assertThrows(BusinessException.class, () -> service.saveCategory(move, true));
        move.setParentId(parent.getId());
        assertThrows(BusinessException.class, () -> service.saveCategory(move, true));
        assertThrows(BusinessException.class, () -> service.saveCategory(request("BAD", 999), false));
    }

    @Test
    void movingSubtreeUpdatesDescendantLevels() {
        var parent = service.saveCategory(request("P", 0), false);
        var child = service.saveCategory(request("C", parent.getId()), false);
        var leaf = service.saveCategory(request("L", child.getId()), false);
        var move = request("C", 0);
        move.setId(child.getId());
        service.saveCategory(move, true);
        assertEquals(1, service.getById(child.getId()).getLevel());
        assertEquals(2, service.getById(leaf.getId()).getLevel());
    }

    @Test
    void preventsDeletingParentsAndCategoriesUsedByProducts() {
        var parent = service.saveCategory(request("P", 0), false);
        var child = service.saveCategory(request("C", parent.getId()), false);
        assertThrows(BusinessException.class, () -> service.deleteCategory(parent.getId()));
        when(products.selectCount(any())).thenReturn(1L);
        assertThrows(BusinessException.class, () -> service.deleteCategory(child.getId()));
        assertNotNull(service.getById(child.getId()));
        when(products.selectCount(any())).thenReturn(0L);
        service.deleteCategory(child.getId());
        service.deleteCategory(parent.getId());
        assertEquals(0, service.count());
    }

    @Test
    void reservesCodesIncludingDeletedCategories() {
        var category = service.saveCategory(request("UNIQUE", 0), false);
        assertThrows(BusinessException.class, () -> service.saveCategory(request("UNIQUE", 0), false));
        service.deleteCategory(category.getId());
        assertThrows(BusinessException.class, () -> service.saveCategory(request("UNIQUE", 0), false));
    }

    @Test
    void validatesDisableAndReenableOrder() {
        var parent = service.saveCategory(request("P", 0), false);
        var child = service.saveCategory(request("C", parent.getId()), false);
        var change = request("P", 0);
        change.setId(parent.getId());
        change.setStatus(0);
        assertThrows(BusinessException.class, () -> service.saveCategory(change, true));
        var childChange = request("C", parent.getId());
        childChange.setId(child.getId());
        childChange.setStatus(0);
        when(products.selectCount(any())).thenReturn(1L);
        assertThrows(BusinessException.class, () -> service.saveCategory(childChange, true));
        when(products.selectCount(any())).thenReturn(0L);
        service.saveCategory(childChange, true);
        service.saveCategory(change, true);
        childChange.setStatus(1);
        assertThrows(BusinessException.class, () -> service.saveCategory(childChange, true));
        assertThrows(BusinessException.class, () -> service.requireEnabled(child.getId()));
        change.setStatus(1);
        service.saveCategory(change, true);
        service.saveCategory(childChange, true);
        assertDoesNotThrow(() -> service.requireEnabled(child.getId()));
    }

    @Test
    void rejectsInvalidFieldsAndMissingRecords() {
        var invalid = request("  ", 0);
        assertThrows(BusinessException.class, () -> service.saveCategory(invalid, false));
        invalid.setCategoryCode("VALID");
        invalid.setStatus(2);
        assertThrows(BusinessException.class, () -> service.saveCategory(invalid, false));
        assertThrows(BusinessException.class, () -> service.saveCategory(request("MISSING", 0), true));
        assertThrows(BusinessException.class, () -> service.deleteCategory(999L));
        assertThrows(BusinessException.class, () -> service.requireEnabled(999L));
    }

    @Test
    void productCannotUseDisabledCategory() {
        var request = request("DISABLED", 0);
        request.setStatus(0);
        var category = service.saveCategory(request, false);
        var productService = new ProductServiceImpl(service, mock(ProductBrandService.class), mock(com.ecommerce.commerce.mapper.ProductSkuMapper.class), mock(com.ecommerce.commerce.mapper.InventoryLogMapper.class));
        var product = new ProductSaveRequest();
        product.setCategoryId(category.getId());
        assertThrows(BusinessException.class, () -> productService.saveProduct(product, false));
    }
}
