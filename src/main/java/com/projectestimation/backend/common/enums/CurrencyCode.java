package com.projectestimation.backend.common.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.projectestimation.backend.common.exception.BadRequestException;

public enum CurrencyCode {
    INR,
    USD,
    EUR,
    GBP,
    AED,
    SGD;

    @JsonValue
    public String toValue() {
        return name();
    }

    @JsonCreator
    public static CurrencyCode fromValue(String value) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException("Currency is required");
        }
        try {
            return CurrencyCode.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException(
                    "Invalid currency code. Supported values: INR, USD, EUR, GBP, AED, SGD"
            );
        }
    }
}
