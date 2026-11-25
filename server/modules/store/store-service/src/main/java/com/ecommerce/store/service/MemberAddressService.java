package com.ecommerce.store.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.ecommerce.common.core.exception.BusinessException;
import com.ecommerce.store.dto.AddressRequest;
import com.ecommerce.store.entity.MemberAddress;
import com.ecommerce.store.entity.MemberUser;
import com.ecommerce.store.mapper.MemberAddressMapper;
import com.ecommerce.store.mapper.MemberUserMapper;
import com.ecommerce.store.vo.AddressView;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 锁定会员行后修改地址，串行化同一会员的默认地址切换及数量检查。 */
@Service
@RequiredArgsConstructor
public class MemberAddressService {
  private static final int MAX_ADDRESSES = 20;
  private final MemberAddressMapper addresses;
  private final MemberUserMapper members;

  public List<AddressView> list(Long memberId) {
    return addresses.selectList(ownedBy(memberId).orderByDesc(MemberAddress::getIsDefault)
        .orderByAsc(MemberAddress::getId)).stream().map(AddressView::from).toList();
  }

  public AddressView get(Long memberId, Long id) {
    return AddressView.from(requireOwned(memberId, id));
  }

  @Transactional(rollbackFor = Exception.class)
  public AddressView save(Long memberId, Long id, AddressRequest request) {
    lockMember(memberId);
    MemberAddress address = id == null ? new MemberAddress() : requireOwned(memberId, id);
    long count = addresses.selectCount(ownedBy(memberId));
    if (id == null && count >= MAX_ADDRESSES) {
      throw new BusinessException(409, "最多保存 20 个收货地址");
    }
    boolean makeDefault = Boolean.TRUE.equals(request.defaultAddress()) || count == 0;
    // 默认地址的取消由选择另一地址完成，避免编辑文字时意外丢失默认设置。
    if (makeDefault) {
      clearDefault(memberId);
    }
    address.setMemberId(memberId);
    address.setRecipient(request.recipient().trim());
    address.setPhone(request.phone().trim());
    address.setProvince(request.province().trim());
    address.setCity(request.city().trim());
    address.setDistrict(request.district().trim());
    address.setDetail(request.detail().trim());
    address.setIsDefault(makeDefault || Integer.valueOf(1).equals(address.getIsDefault()) ? 1 : 0);
    if (id == null) {
      addresses.insert(address);
    } else {
      addresses.updateById(address);
    }
    return AddressView.from(address);
  }

  @Transactional(rollbackFor = Exception.class)
  public void setDefault(Long memberId, Long id) {
    lockMember(memberId);
    requireOwned(memberId, id);
    clearDefault(memberId);
    addresses.update(null, new LambdaUpdateWrapper<MemberAddress>()
        .eq(MemberAddress::getMemberId, memberId).eq(MemberAddress::getId, id).set(MemberAddress::getIsDefault, 1));
  }

  @Transactional(rollbackFor = Exception.class)
  public void delete(Long memberId, Long id) {
    lockMember(memberId);
    MemberAddress address = requireOwned(memberId, id);
    addresses.delete(ownedBy(memberId).eq(MemberAddress::getId, id));
    if (Integer.valueOf(1).equals(address.getIsDefault())) {
      List<MemberAddress> remaining = addresses.selectList(ownedBy(memberId).orderByAsc(MemberAddress::getId));
      if (!remaining.isEmpty()) {
        addresses.update(null, new LambdaUpdateWrapper<MemberAddress>()
            .eq(MemberAddress::getMemberId, memberId).eq(MemberAddress::getId, remaining.getFirst().getId())
            .set(MemberAddress::getIsDefault, 1));
      }
    }
  }

  private void clearDefault(Long memberId) {
    addresses.update(null, new LambdaUpdateWrapper<MemberAddress>()
        .eq(MemberAddress::getMemberId, memberId).eq(MemberAddress::getIsDefault, 1).set(MemberAddress::getIsDefault, 0));
  }

  private MemberAddress requireOwned(Long memberId, Long id) {
    MemberAddress address = addresses.selectOne(ownedBy(memberId).eq(MemberAddress::getId, id));
    if (address == null) {
      throw new BusinessException(404, "收货地址不存在");
    }
    return address;
  }

  private void lockMember(Long memberId) {
    MemberUser member = members.lockById(memberId);
    if (member == null || !Integer.valueOf(1).equals(member.getStatus())) {
      throw new BusinessException(401, "会员账号不可用");
    }
  }

  private LambdaQueryWrapper<MemberAddress> ownedBy(Long memberId) {
    return new LambdaQueryWrapper<MemberAddress>().eq(MemberAddress::getMemberId, memberId);
  }
}
