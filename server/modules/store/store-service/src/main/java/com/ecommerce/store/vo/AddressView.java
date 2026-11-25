package com.ecommerce.store.vo;

import com.ecommerce.store.entity.MemberAddress;

/** 地址显示字段，隐藏内部归属和生成列。 */
public record AddressView(Long id, String recipient, String phone, String province, String city,
                          String district, String detail, boolean defaultAddress) {
  public static AddressView from(MemberAddress address) {
    return new AddressView(address.getId(), address.getRecipient(), address.getPhone(), address.getProvince(),
        address.getCity(), address.getDistrict(), address.getDetail(), Integer.valueOf(1).equals(address.getIsDefault()));
  }
}
