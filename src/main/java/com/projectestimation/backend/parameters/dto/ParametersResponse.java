package com.projectestimation.backend.parameters.dto;

import com.projectestimation.backend.common.enums.CurrencyCode;
import com.projectestimation.backend.parameters.model.ComplexityLevel;

import java.time.LocalDateTime;

public record ParametersResponse(
        Long id,
        Long opportunityId,
        ComplexityLevel complexity,
        Double riskFactor,
        Double productivityFactor,
        Double hourlyRate,
        CurrencyCode currency,
        Integer teamSize,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
