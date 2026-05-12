package com.projectestimation.backend.estimation.engine;

public record EstimationInput(
        String requirementSummary,
        double complexityFactor,
        double riskFactor,
        double productivityFactor,
        double hourlyRate,
        int teamSize
) {
}
