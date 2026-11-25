package com.ecommerce.store.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 商城分页配置。 */
@Configuration
public class StoreConfig {
  @Bean
  public MybatisPlusInterceptor pagination() {
    MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
    PaginationInnerInterceptor pagination = new PaginationInnerInterceptor(DbType.MYSQL);
    pagination.setMaxLimit(100L);
    interceptor.addInnerInterceptor(pagination);
    return interceptor;
  }
}
