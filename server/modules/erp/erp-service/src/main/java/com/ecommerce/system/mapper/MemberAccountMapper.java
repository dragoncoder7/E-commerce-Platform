package com.ecommerce.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ecommerce.system.domain.MemberAccount;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** 后台会员读模型和状态变更。 */
public interface MemberAccountMapper extends BaseMapper<MemberAccount> {
  @Select("SELECT id, username, phone, email, nickname, status, session_version, create_time FROM member_user WHERE id=#{id} FOR UPDATE")
  MemberAccount lockById(@Param("id") Long id);
}
