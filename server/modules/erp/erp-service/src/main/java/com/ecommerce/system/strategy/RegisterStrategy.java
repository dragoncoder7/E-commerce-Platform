package com.ecommerce.system.strategy;

import com.ecommerce.system.api.dto.RegisterRequest;
import com.ecommerce.system.api.enums.RegisterType;

public interface RegisterStrategy {

    RegisterType getRegisterType();

    Long register(RegisterRequest request);
}
