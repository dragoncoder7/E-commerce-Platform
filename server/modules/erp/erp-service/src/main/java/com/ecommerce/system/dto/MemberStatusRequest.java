package com.ecommerce.system.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** ERP 只能在启用和禁用两个状态间切换会员。 */
public record MemberStatusRequest(@NotNull @Min(0) @Max(1) Integer status) { }
