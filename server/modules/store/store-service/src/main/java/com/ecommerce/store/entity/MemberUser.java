package com.ecommerce.store.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/** 商城会员；不得用于 ERP 身份认证。 */
@Data
@TableName("member_user")
public class MemberUser {
  @TableId(type = IdType.AUTO)
  private Long id;
  private String username;
  private String phone;
  private String email;
  private String passwordHash;
  private String nickname;
  private Integer status;
  private Long sessionVersion;
  private LocalDateTime createTime;
  private LocalDateTime updateTime;
}
