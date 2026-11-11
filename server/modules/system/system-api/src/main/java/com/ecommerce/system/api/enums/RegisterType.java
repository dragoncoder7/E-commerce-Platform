package com.ecommerce.system.api.enums;

import lombok.Getter;

@Getter
public enum RegisterType {

    EMAIL("email"),
    PHONE("phone"),
    PASSWORD("password"),
    THIRD_PARTY("third_party"),
    UNKNOWN("unknown");



    private final String description;

    RegisterType(String description) {
        this.description = description;
    }

}
