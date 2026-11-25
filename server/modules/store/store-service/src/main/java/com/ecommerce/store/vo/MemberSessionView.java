package com.ecommerce.store.vo;

/** 商城独立会话。 */
public record MemberSessionView(String token, long expiresIn, MemberView member) { }
