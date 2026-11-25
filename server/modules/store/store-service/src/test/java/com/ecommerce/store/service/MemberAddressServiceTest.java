package com.ecommerce.store.service;

import com.ecommerce.common.core.exception.BusinessException;
import com.ecommerce.store.dto.AddressRequest;
import com.ecommerce.store.entity.MemberAddress;
import com.ecommerce.store.entity.MemberUser;
import com.ecommerce.store.mapper.MemberAddressMapper;
import com.ecommerce.store.mapper.MemberUserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 地址归属与数量限制的核心业务测试；并发与唯一约束由真实 MySQL 验证。 */
class MemberAddressServiceTest {
  private MemberAddressMapper addresses;
  private MemberUserMapper members;
  private MemberAddressService service;
  private final AddressRequest request = new AddressRequest("收货人", "13800138000", "浙江省", "杭州市", "西湖区", "测试路1号", false);

  @BeforeEach
  void setUp() {
    com.baomidou.mybatisplus.core.metadata.TableInfoHelper.initTableInfo(
        new org.apache.ibatis.builder.MapperBuilderAssistant(new com.baomidou.mybatisplus.core.MybatisConfiguration(), "test"), MemberAddress.class);
    addresses = mock(MemberAddressMapper.class);
    members = mock(MemberUserMapper.class);
    service = new MemberAddressService(addresses, members);
    MemberUser member = new MemberUser();
    member.setId(1L);
    member.setStatus(1);
    when(members.lockById(1L)).thenReturn(member);
  }

  @Test
  void firstAddressBecomesDefaultAndUsesServerMemberIdentity() {
    when(addresses.selectCount(any())).thenReturn(0L);
    service.save(1L, null, request);
    ArgumentCaptor<MemberAddress> saved = ArgumentCaptor.forClass(MemberAddress.class);
    verify(addresses).insert(saved.capture());
    assertEquals(1L, saved.getValue().getMemberId());
    assertEquals(1, saved.getValue().getIsDefault());
    verify(members).lockById(1L);
  }

  @Test
  void twentyAddressLimitPreventsInsert() {
    when(addresses.selectCount(any())).thenReturn(20L);
    assertThrows(BusinessException.class, () -> service.save(1L, null, request));
    verify(addresses, never()).insert(any(MemberAddress.class));
  }

  @Test
  void foreignOrMissingAddressCannotBeEdited() {
    when(addresses.selectOne(any())).thenReturn(null);
    assertThrows(BusinessException.class, () -> service.save(1L, 99L, request));
    verify(addresses, never()).updateById(any(MemberAddress.class));
  }

  @Test
  void disabledMemberCannotMutateAddresses() {
    MemberUser disabled = new MemberUser();
    disabled.setStatus(0);
    when(members.lockById(1L)).thenReturn(disabled);
    assertThrows(BusinessException.class, () -> service.setDefault(1L, 2L));
    verify(addresses, never()).selectOne(any());
  }
}
