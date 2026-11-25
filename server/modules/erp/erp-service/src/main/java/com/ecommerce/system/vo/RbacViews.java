package com.ecommerce.system.vo;

import java.time.LocalDateTime;
import java.util.List;

/** 后台授权页面专用响应，不返回密码及会话版本。 */
public final class RbacViews {
  private RbacViews() { }
  public record Staff(Long id, String username, String nickname, Integer status,
                      LocalDateTime createTime, List<Long> roleIds) { }
  public record Role(Long id, String name, String code, Integer status, Integer sort,
                     String remark, List<Long> menuIds) { }
}
