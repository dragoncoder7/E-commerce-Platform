package com.ecommerce.store.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** 地址编辑输入，不接受 memberId。 */
public record AddressRequest(
    @NotBlank @Size(max = 50) String recipient,
    @NotBlank @Pattern(regexp = "[0-9+() -]{6,20}", message = "请输入有效收货电话") String phone,
    @NotBlank @Size(max = 50) String province,
    @NotBlank @Size(max = 50) String city,
    @NotBlank @Size(max = 50) String district,
    @NotBlank @Size(max = 200) String detail,
    @NotNull Boolean defaultAddress) { }
