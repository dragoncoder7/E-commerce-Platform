package com.ecommerce.store.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** 输入仅包含可由会员修改的字段，不接受身份、状态或角色。 */
public final class MemberRequests {
  private MemberRequests() { }

  public record Login(@NotBlank @Size(max = 254) String account,
                      @NotBlank @Size(max = 64) String password) { }
  public record Register(@NotBlank @Pattern(regexp = "account|phone|email") String type,
                         @NotBlank @Size(max = 254) String account,
                         @NotBlank @Size(min = 8, max = 64) String password,
                         @Size(max = 6) String code) { }
  public record EmailCode(@NotBlank @jakarta.validation.constraints.Email @Size(max = 254) String email) { }
  public record Profile(@NotBlank @Size(max = 50) String nickname) { }
  public record Password(@NotBlank @Size(max = 64) String currentPassword,
                         @NotBlank @Size(min = 8, max = 64) String newPassword) { }
}
