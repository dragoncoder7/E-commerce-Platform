package com.ecommerce.system.api.dto;

import com.ecommerce.system.api.enums.RegisterThirdType;
import com.ecommerce.system.api.enums.RegisterType;
import lombok.Data;

@Data
public class RegisterRequest {
    private RegisterType type;
    private String username;
    private String password;
    private String phone;
    private String code;
    private RegisterThirdType thirdType; // WECHAT, ALIPAY
    private String authCode;
}
