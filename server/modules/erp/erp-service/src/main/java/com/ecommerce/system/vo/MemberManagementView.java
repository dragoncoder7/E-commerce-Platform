package com.ecommerce.system.vo;

import com.ecommerce.system.domain.MemberAccount;
import java.time.LocalDateTime;

/** 后台会员列表字段。 */
public record MemberManagementView(Long id, String username, String phone, String email, String nickname,
                                   Integer status, LocalDateTime createTime) {
  public static MemberManagementView from(MemberAccount member) {
    return new MemberManagementView(member.getId(), member.getUsername(), member.getPhone(), member.getEmail(),
        member.getNickname(), member.getStatus(), member.getCreateTime());
  }
}
