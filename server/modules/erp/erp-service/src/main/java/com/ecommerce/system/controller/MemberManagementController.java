package com.ecommerce.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ecommerce.common.core.domain.Result;
import com.ecommerce.system.dto.MemberStatusRequest;
import com.ecommerce.system.service.MemberManagementService;
import com.ecommerce.system.vo.MemberManagementView;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 会员管理端，受 ERP members:read / members:write 权限约束。 */
@RestController
@RequestMapping("/members")
@RequiredArgsConstructor
@Tag(name = "ERP 会员管理")
public class MemberManagementController {
  private final MemberManagementService members;

  @GetMapping
  public Result<Page<MemberManagementView>> list(@RequestParam(defaultValue = "1") int pageNum,
      @RequestParam(defaultValue = "10") int pageSize, @RequestParam(required = false) String keyword,
      @RequestParam(required = false) Integer status) {
    return Result.success(members.list(pageNum, pageSize, keyword, status));
  }

  @PutMapping("/{id}/status")
  public Result<Void> status(@PathVariable Long id, @Valid @RequestBody MemberStatusRequest request) {
    members.changeStatus(id, request.status());
    return Result.success();
  }
}
