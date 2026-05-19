package com.projectestimation.backend.estimation.ai;

public record AiEstimationResult(
        double totalEffortHours,
        double estimatedCost,
        double timelineWeeks,
        double confidenceScore,
        String breakdown,
        String reasoning
) {
}
