package com.projectestimation.backend.estimation.dto;

import java.time.LocalDateTime;

public record EstimateCalculationResponse(
        Long estimateId,
        String projectName,
        double totalEffortHours,
        double estimatedCost,
        double timelineWeeks,
        double confidenceScore,
        String breakdown,
        LocalDateTime calculatedAt
) {
}
