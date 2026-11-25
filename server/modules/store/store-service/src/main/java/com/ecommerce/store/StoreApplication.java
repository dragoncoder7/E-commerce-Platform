package com.ecommerce.store;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** 商城独立启动入口，不扫描 ERP 包。 */
@MapperScan({"com.ecommerce.commerce.mapper", "com.ecommerce.store.mapper"})
@SpringBootApplication(scanBasePackages = {"com.ecommerce.store", "com.ecommerce.commerce", "com.ecommerce.common"})
public class StoreApplication {
  public static void main(String[] args) {
    SpringApplication.run(StoreApplication.class, args);
  }
}
