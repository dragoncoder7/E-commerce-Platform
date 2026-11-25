package com.ecommerce.store.controller;

import com.ecommerce.common.core.domain.Result;
import com.ecommerce.store.dto.AddressRequest;
import com.ecommerce.store.entity.MemberUser;
import com.ecommerce.store.service.MemberAddressService;
import com.ecommerce.store.vo.AddressView;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 所有地址操作从已验证的会员会话确定归属。 */
@RestController
@RequestMapping("/addresses")
@RequiredArgsConstructor
@Tag(name = "商城收货地址")
public class MemberAddressController {
  private final MemberAddressService addresses;

  @GetMapping
  public Result<List<AddressView>> list(@RequestAttribute("member") MemberUser member) {
    return Result.success(addresses.list(member.getId()));
  }

  @GetMapping("/{id}")
  public Result<AddressView> get(@RequestAttribute("member") MemberUser member, @PathVariable Long id) {
    return Result.success(addresses.get(member.getId(), id));
  }

  @PostMapping
  public Result<AddressView> create(@RequestAttribute("member") MemberUser member, @Valid @RequestBody AddressRequest request) {
    return Result.success(addresses.save(member.getId(), null, request));
  }

  @PutMapping("/{id}")
  public Result<AddressView> update(@RequestAttribute("member") MemberUser member, @PathVariable Long id,
      @Valid @RequestBody AddressRequest request) {
    return Result.success(addresses.save(member.getId(), id, request));
  }

  @PutMapping("/{id}/default")
  public Result<Void> setDefault(@RequestAttribute("member") MemberUser member, @PathVariable Long id) {
    addresses.setDefault(member.getId(), id);
    return Result.success();
  }

  @DeleteMapping("/{id}")
  public Result<Void> delete(@RequestAttribute("member") MemberUser member, @PathVariable Long id) {
    addresses.delete(member.getId(), id);
    return Result.success();
  }
}
