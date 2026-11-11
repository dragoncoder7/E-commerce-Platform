package com.ecommerce.system.strategy.imp;

import com.ecommerce.system.api.dto.RegisterRequest;
import com.ecommerce.system.api.enums.RegisterType;
import com.ecommerce.system.api.vo.RegisterVO;
import com.ecommerce.system.strategy.RegisterStrategy;
import org.springframework.transaction.annotation.Transactional;

public class PasswordRegisterStrategy implements RegisterStrategy {

    @Override
    public RegisterType getRegisterType() {
        return RegisterType.PASSWORD;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Long register(RegisterRequest request) {
        // todo 先效验是否已经有账号了 账号名称验证

        // todo 密码格式验证 至少8位数 包含大小写以及特殊符号

        // todo 账号创建 成功后提交事务
        return 0L;
    }
}
