package com.ecommerce.system.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/** ERP 会员管理读模型，不包含密码，也不依赖商城应用类。 */
@Data
@TableName("member_user")
public class MemberAccount {
  @TableId
  private Long id;
  private String username;
  private String phone;
  private String email;
  private String nickname;
  private Integer status;
  private Long sessionVersion;
  private LocalDateTime createTime;
}
