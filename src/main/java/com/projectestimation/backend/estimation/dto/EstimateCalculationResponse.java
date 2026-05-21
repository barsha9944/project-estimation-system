package com.projectestimation.backend.estimation.dto;

import com.projectestimation.backend.common.enums.CurrencyCode;

import java.time.LocalDateTime;

public record EstimateCalculationResponse(
        Long estimateId,
        String projectName,
        double totalEffortHours,
        double estimatedCost,
        CurrencyCode currency,
        double timelineWeeks,
        double confidenceScore,
        String breakdown,
        LocalDateTime calculatedAt
) {
}
