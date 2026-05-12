package com.projectestimation.backend.estimation.engine;

public record EstimationOutput(
        double totalEffortHours,
        double estimatedCost,
        double timelineWeeks,
        double confidenceScore,
        String breakdown
) {
}
