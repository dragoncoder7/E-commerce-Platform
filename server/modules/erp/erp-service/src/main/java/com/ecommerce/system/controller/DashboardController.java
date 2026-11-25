package com.ecommerce.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ecommerce.common.core.domain.Result;
import com.ecommerce.commerce.entity.Product;
import com.ecommerce.commerce.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {
    private final ProductService productService;

    @GetMapping("/summary")
    public Result<Summary> summary() {
        long total = productService.count();
        long onSale = productService.count(new LambdaQueryWrapper<Product>().eq(Product::getStatus, 1));
        long offSale = productService.count(new LambdaQueryWrapper<Product>().eq(Product::getStatus, 0));
        long lowStock = productService.count(new LambdaQueryWrapper<Product>().le(Product::getStock, 10));
        return Result.success(new Summary(List.of(
                new Stat("products", "商品总数", total, "件", null, "未删除的商品"),
                new Stat("onSale", "上架商品", onSale, "件", null, "当前可售商品"),
                new Stat("offSale", "下架商品", offSale, "件", null, "待上架或暂停销售"),
                new Stat("lowStock", "低库存商品", lowStock, "件", null, "库存不超过 10 件")
        ), List.of(), List.of(), LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))));
    }

    public record Stat(String key, String label, long value, String unit, Integer trend, String hint) {}
    public record Summary(List<Stat> stats, List<Object> todos, List<Object> notices, String serverTime) {}
}
