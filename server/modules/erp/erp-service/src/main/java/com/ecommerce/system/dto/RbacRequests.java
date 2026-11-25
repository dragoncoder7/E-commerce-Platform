package com.ecommerce.system.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

/** 后台人员、角色、菜单编辑输入，不绑定数据库实体。 */
public final class RbacRequests {
  private RbacRequests() { }
  public record Staff(@NotBlank @Pattern(regexp = "[A-Za-z][A-Za-z0-9_]{3,49}") String username,
                      @NotBlank @Size(max = 50) String nickname, @Size(max = 64) String password,
                      @NotNull @Min(0) @Max(1) Integer status,
                      @NotNull @Size(min = 1, max = 50) List<@NotNull @Positive Long> roleIds) { }
  public record Role(@NotBlank @Size(max = 50) String name,
                     @NotBlank @Pattern(regexp = "[a-z][a-z0-9_]{1,49}") String code,
                     @NotNull @Min(0) @Max(1) Integer status, @NotNull @Min(0) Integer sort,
                     @Size(max = 500) String remark, @NotNull @Size(max = 500) List<@NotNull @Positive Long> menuIds) { }
  public record Menu(@NotNull @Min(0) Long parentId, @NotBlank @Size(max = 50) String name,
                     @NotBlank @Size(max = 50) String title,
                     @NotBlank @Pattern(regexp = "catalog|menu|button") String type,
                     @Size(max = 200) String path, @Size(max = 200) String component,
                     @Size(max = 50) String icon, @Size(max = 100) String permission,
                     @NotNull @Min(0) Integer sort, @NotNull Boolean visible,
                     @NotNull Boolean keepAlive, @NotNull @Min(0) @Max(1) Integer status) { }
}
