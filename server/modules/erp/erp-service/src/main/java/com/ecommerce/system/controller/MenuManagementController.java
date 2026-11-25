package com.ecommerce.system.controller;

import com.ecommerce.common.core.domain.Result;
import com.ecommerce.system.api.vo.MenuVO;
import com.ecommerce.system.dto.RbacRequests;
import com.ecommerce.system.service.MenuManagementService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 复用现有后台菜单页面的兼容接口。 */
@RestController
@RequestMapping("/menu")
@RequiredArgsConstructor
@Tag(name = "ERP 菜单与按钮权限")
public class MenuManagementController {
  private final MenuManagementService menus;

  @GetMapping("/list")
  public Result<List<MenuVO>> list(@RequestParam(required = false) String title,
      @RequestParam(required = false) String type, @RequestParam(required = false) Integer status) {
    return Result.success(menus.list(title, type, status));
  }

  @PostMapping
  public Result<MenuVO> create(@Valid @RequestBody RbacRequests.Menu request) {
    return Result.success(menus.save(null, request));
  }

  @PutMapping("/{id}")
  public Result<MenuVO> update(@PathVariable Long id, @Valid @RequestBody RbacRequests.Menu request) {
    return Result.success(menus.save(id, request));
  }

  @DeleteMapping("/{id}")
  public Result<Void> delete(@PathVariable Long id) {
    menus.delete(id);
    return Result.success();
  }
}
