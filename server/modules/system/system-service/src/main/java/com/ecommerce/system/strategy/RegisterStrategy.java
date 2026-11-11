package com.ecommerce.system.strategy;

import com.ecommerce.system.api.dto.RegisterRequest;
import com.ecommerce.system.api.enums.RegisterType;
import com.ecommerce.system.api.vo.RegisterVO;

public interface RegisterStrategy {

    RegisterType getRegisterType();

    Long register(RegisterRequest request);
}
