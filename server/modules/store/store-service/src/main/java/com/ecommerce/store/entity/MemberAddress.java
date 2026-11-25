package com.ecommerce.store.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/** 收货地址归属于商城会员，默认唯一由数据库兜底。 */
@Data
@TableName("member_address")
public class MemberAddress {
  @TableId(type = IdType.AUTO)
  private Long id;
  private Long memberId;
  private String recipient;
  private String phone;
  private String province;
  private String city;
  private String district;
  private String detail;
  private Integer isDefault;
  private LocalDateTime createTime;
  private LocalDateTime updateTime;
}
