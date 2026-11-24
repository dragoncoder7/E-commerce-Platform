package com.ecommerce.system;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 系统服务启动类
 */
@EnableDiscoveryClient
@MapperScan("com.ecommerce.system.mapper")
@SpringBootApplication(scanBasePackages = "com.ecommerce")
public class SystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(SystemApplication.class, args);
    }
}
