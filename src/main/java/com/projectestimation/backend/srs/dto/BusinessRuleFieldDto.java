package com.projectestimation.backend.srs.dto;

public record BusinessRuleFieldDto(
        String fieldName,
        String type,
        String lengthOrFormat,
        Boolean mandatory
) {
}