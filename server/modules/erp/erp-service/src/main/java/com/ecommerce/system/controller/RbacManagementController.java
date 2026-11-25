package com.ecommerce.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.common.core.domain.Result;
import com.ecommerce.system.dto.RbacRequests;
import com.ecommerce.system.service.RbacManagementService;
import com.ecommerce.system.vo.RbacViews;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 后台人员与角色 REST 接口，由 ERP 拦截器校验对应资源权限。 */
@RestController
@RequiredArgsConstructor
@Tag(name = "ERP 后台人员与角色")
public class RbacManagementController {
  private final RbacManagementService management;

  @GetMapping("/staff")
  public Result<Page<RbacViews.Staff>> staff(@RequestParam(defaultValue = "1") int pageNum,
      @RequestParam(defaultValue = "10") int pageSize, @RequestParam(required = false) String keyword) {
    return Result.success(management.staff(pageNum, pageSize, keyword));
  }

  @PostMapping("/staff")
  public Result<RbacViews.Staff> createStaff(@Valid @RequestBody RbacRequests.Staff request, @RequestAttribute("actorId") Long actor) {
    return Result.success(management.saveStaff(null, request, actor));
  }

  @PutMapping("/staff/{id}")
  public Result<RbacViews.Staff> updateStaff(@PathVariable Long id, @Valid @RequestBody RbacRequests.Staff request,
      @RequestAttribute("actorId") Long actor) {
    return Result.success(management.saveStaff(id, request, actor));
  }

  @DeleteMapping("/staff/{id}")
  public Result<Void> deleteStaff(@PathVariable Long id, @RequestAttribute("actorId") Long actor) {
    management.deleteStaff(id, actor);
    return Result.success();
  }

  @GetMapping("/roles")
  public Result<List<RbacViews.Role>> roles() {
    return Result.success(management.roles());
  }

  @PostMapping("/roles")
  public Result<RbacViews.Role> createRole(@Valid @RequestBody RbacRequests.Role request) {
    return Result.success(management.saveRole(null, request));
  }

  @PutMapping("/roles/{id}")
  public Result<RbacViews.Role> updateRole(@PathVariable Long id, @Valid @RequestBody RbacRequests.Role request) {
    return Result.success(management.saveRole(id, request));
  }

  @DeleteMapping("/roles/{id}")
  public Result<Void> deleteRole(@PathVariable Long id) {
    management.deleteRole(id);
    return Result.success();
  }
}
