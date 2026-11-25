package com.ecommerce.store.vo;

import com.ecommerce.store.entity.MemberUser;

/** 会员本人可见资料，不返回密码摘要和内部会话版本。 */
public record MemberView(Long id, String username, String phone, String email, String nickname,
                         boolean phoneVerified) {
  public static MemberView from(MemberUser user) {
    return new MemberView(user.getId(), user.getUsername(), user.getPhone(), user.getEmail(),
        user.getNickname(), false);
  }
}
