package com.ecommerce.system.factory;

import com.ecommerce.common.core.exception.UnsupportedRegisterTypeException;
import com.ecommerce.system.api.enums.RegisterType;
import com.ecommerce.system.strategy.RegisterStrategy;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class RegisterStrategyFactory {

    private final Map<RegisterType, RegisterStrategy> strategies;

    public RegisterStrategyFactory(List<RegisterStrategy> strategyList) {
        this.strategies = strategyList.stream()
                .collect(Collectors.toMap(
                        RegisterStrategy::getRegisterType,
                        Function.identity()
                ));
    }

    public RegisterStrategy get(RegisterType type) {
        RegisterStrategy strategy = strategies.get(type);
        if (strategy == null) {
            throw new UnsupportedRegisterTypeException("不支持的注册方式: " + type);
        }
        return strategy;
    }
}