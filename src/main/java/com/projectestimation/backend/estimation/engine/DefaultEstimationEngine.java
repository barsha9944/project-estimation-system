package com.projectestimation.backend.estimation.engine;

import org.springframework.stereotype.Component;

@Component
public class DefaultEstimationEngine implements EstimationEngine {

    @Override
    public EstimationOutput compute(EstimationInput input) {
        double baseHours = Math.max(40, input.requirementSummary().length() * 0.8);
        double adjustedEffort = baseHours * input.complexityFactor() * input.riskFactor();
        double normalizedEffort = adjustedEffort / input.productivityFactor();

        double cost = normalizedEffort * input.hourlyRate();
        double timelineWeeks = normalizedEffort / (input.teamSize() * 40.0);

        double confidence = 100.0
                - ((input.riskFactor() - 1.0) * 15.0)
                - ((input.complexityFactor() - 1.0) * 10.0);
        confidence = Math.max(10, Math.min(confidence, 95));

        String breakdown = String.format(
                "baseHours=%.2f, adjustedEffort=%.2f, productivityAdjusted=%.2f",
                baseHours, adjustedEffort, normalizedEffort
        );

        return new EstimationOutput(normalizedEffort, cost, timelineWeeks, confidence, breakdown);
    }
}
