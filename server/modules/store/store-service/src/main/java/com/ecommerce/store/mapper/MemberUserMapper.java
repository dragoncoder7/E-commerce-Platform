package com.ecommerce.store.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ecommerce.store.entity.MemberUser;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** 会员持久层。 */
public interface MemberUserMapper extends BaseMapper<MemberUser> {
  @Select("SELECT * FROM member_user WHERE id = #{id} FOR UPDATE")
  MemberUser lockById(@Param("id") Long id);
}
