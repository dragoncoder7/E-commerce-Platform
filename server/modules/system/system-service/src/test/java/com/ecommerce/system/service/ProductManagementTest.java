package com.ecommerce.system.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.ecommerce.common.core.exception.BusinessException;
import com.ecommerce.system.config.MybatisPlusConfig;
import com.ecommerce.system.controller.ProductController;
import com.ecommerce.system.dto.ProductSaveRequest;
import com.ecommerce.system.entity.ProductCategory;
import com.ecommerce.system.mapper.ProductMapper;
import com.ecommerce.system.mapper.UserMapper;
import com.ecommerce.system.mapper.MenuMapper;
import com.ecommerce.system.domain.SysUser;
import com.ecommerce.system.api.dto.LoginDTO;
import com.ecommerce.system.factory.RegisterStrategyFactory;
import com.ecommerce.system.service.impl.AuthServiceImpl;
import com.ecommerce.common.redis.service.RedisService;
import com.ecommerce.common.security.util.JwtUtil;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import com.ecommerce.system.service.impl.ProductServiceImpl;
import org.apache.ibatis.session.SqlSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

/** 真实 SQL 执行，验证 LIMIT、总数、可清空字段及逻辑删除。 */
class ProductManagementTest {
    private SqlSession session;
    private ProductServiceImpl service;
    private ProductController controller;

    @BeforeEach
    void setup() throws Exception {
        var ds = new DriverManagerDataSource("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
        new JdbcTemplate(ds).execute("""
                CREATE TABLE product (
                  id BIGINT AUTO_INCREMENT PRIMARY KEY, product_code VARCHAR(50) NOT NULL,
                  product_name VARCHAR(200), category_id BIGINT, brand_id BIGINT,
                  main_image VARCHAR(500), sub_title VARCHAR(300), description TEXT,
                  original_price DECIMAL(10,2), sale_price DECIMAL(10,2), stock INT DEFAULT 0,
                  unit VARCHAR(20), sales_count INT DEFAULT 0, view_count INT DEFAULT 0,
                  status INT DEFAULT 0, is_hot INT DEFAULT 0, is_new INT DEFAULT 0,
                  is_recommend INT DEFAULT 0, sort_order INT DEFAULT 0, create_by VARCHAR(64),
                  update_by VARCHAR(64), create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                  update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, deleted INT DEFAULT 0,
                  UNIQUE(product_code, deleted))
                """);
        var factory = new MybatisSqlSessionFactoryBean();
        new JdbcTemplate(ds).execute("""
                CREATE TABLE sys_user (
                  id BIGINT PRIMARY KEY, username VARCHAR(50), password VARCHAR(100), nickname VARCHAR(50),
                  avatar VARCHAR(255), email VARCHAR(100), phone VARCHAR(20), status INT DEFAULT 1,
                  deleted INT DEFAULT 0, create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, last_login_time TIMESTAMP)
                """);
        factory.setDataSource(ds);
        var config = new MybatisConfiguration();
        config.setMapUnderscoreToCamelCase(true);
        config.addMapper(ProductMapper.class);
        config.addMapper(UserMapper.class);
        factory.setConfiguration(config);
        factory.setPlugins(new MybatisPlusConfig().mybatisPlusInterceptor());
        session = factory.getObject().openSession(true);
        var categories = mock(ProductCategoryService.class);
        when(categories.getById(1L)).thenReturn(new ProductCategory());
        var brands = mock(ProductBrandService.class);
        service = new ProductServiceImpl(categories, brands);
        ReflectionTestUtils.setField(service, "baseMapper", session.getMapper(ProductMapper.class));
        controller = new ProductController(service, mock(ProductImageService.class), categories, brands);
    }

    @AfterEach
    void cleanup() { if (session != null) session.close(); }

    private ProductSaveRequest request(String code) {
        var dto = new ProductSaveRequest();
        dto.setProductCode(code);
        dto.setProductName("分页测试 " + code);
        dto.setCategoryId(1L);
        dto.setSalePrice(new BigDecimal("12.50"));
        return dto;
    }

    @Test
    void paginatesFiltersAndCountsInDatabase() {
        for (int i = 0; i < 5; i++) service.saveProduct(request("PAGE-" + i), false);
        var first = controller.list(1, 2, 1L, null, "PAGE-", null, 0).getData();
        var second = controller.list(2, 2, 1L, null, "PAGE-", null, 0).getData();
        assertEquals(5, first.getTotal());
        assertEquals(3, first.getPages());
        assertEquals(2, first.getRecords().size());
        assertEquals(2, second.getRecords().size());
        assertNotEquals(first.getRecords().getFirst().getId(), second.getRecords().getFirst().getId());
        assertEquals(0, controller.list(1, 2, null, null, null, null, 1).getData().getTotal());
        assertThrows(BusinessException.class, () -> controller.list(0, 10, null, null, null, null, null));
        assertThrows(BusinessException.class, () -> controller.list(1, 101, null, null, null, null, null));
    }

    @Test
    void clearsOptionalFieldsAndLogicallyDeletes() {
        var dto = request("EDIT-1");
        dto.setOriginalPrice(new BigDecimal("20.00"));
        var created = service.saveProduct(dto, false);
        dto.setId(created.getId());
        dto.setOriginalPrice(null);
        dto.setProductName("更新后的商品");
        var updated = service.saveProduct(dto, true);
        assertNull(updated.getOriginalPrice());
        assertEquals(0, updated.getSalesCount());
        assertEquals("更新后的商品", updated.getProductName());
        assertEquals(200, controller.delete(created.getId()).getCode());
        assertNull(service.getById(created.getId()));
        assertEquals(0, service.count());
        assertThrows(BusinessException.class, () -> service.saveProduct(request("EDIT-1"), false));
        assertThrows(BusinessException.class, () -> controller.getById(created.getId()));
    }

    @Test
    void loginPersistsTimeWithNonNullableAuditColumns() {
        var users = session.getMapper(UserMapper.class);
        var user = new SysUser();
        user.setId(42L);
        user.setUsername("sql_login");
        user.setPassword(new BCryptPasswordEncoder().encode("Test1234!"));
        user.setCreateTime(java.time.LocalDateTime.now());
        user.setUpdateTime(user.getCreateTime());
        user.setStatus(1);
        users.insert(user);
        var jwt = mock(JwtUtil.class);
        when(jwt.generateToken(anyString(), anyMap())).thenReturn("test-session");
        when(jwt.getExpiration()).thenReturn(7200000L);
        var auth = new AuthServiceImpl(jwt, mock(RedisService.class), users,
                mock(MenuMapper.class), mock(RegisterStrategyFactory.class));
        var login = new LoginDTO();
        login.setUsername("sql_login");
        login.setPassword("Test1234!");
        auth.login(login);
        var saved = users.selectById(42L);
        assertNotNull(saved.getLastLoginTime());
        assertNotNull(saved.getUpdateTime());
        assertEquals(user.getPassword(), saved.getPassword());
    }

    @Test
    void rejectsUnknownCategoryBrandAndDuplicateCodes() {
        var dto = request("UNIQUE-1");
        dto.setCategoryId(999L);
        assertThrows(BusinessException.class, () -> service.saveProduct(dto, false));
        dto.setCategoryId(1L);
        dto.setBrandId(999L);
        assertThrows(BusinessException.class, () -> service.saveProduct(dto, false));
        dto.setBrandId(null);
        service.saveProduct(dto, false);
        assertThrows(BusinessException.class, () -> service.saveProduct(request("UNIQUE-1"), false));
    }
}
