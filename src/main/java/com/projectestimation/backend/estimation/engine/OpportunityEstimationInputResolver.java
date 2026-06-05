package com.projectestimation.backend.estimation.engine;

import com.projectestimation.backend.opportunity.model.Opportunity;
import com.projectestimation.backend.parameters.model.ComplexityLevel;
import com.projectestimation.backend.parameters.model.Parameters;

/**
 * Resolves a numeric complexity factor for {@link DefaultEstimationEngine} from
 * persisted opportunity and parameters data.
 */
public final class OpportunityEstimationInputResolver {

    private OpportunityEstimationInputResolver() {
    }

    public static EstimationInput resolve(Opportunity opportunity, Parameters parameters) {
        double complexityFactor = resolveComplexityFactor(parameters.getComplexity(), opportunity);
        return new EstimationInput(
                opportunity.getRequirementSummary(),
                complexityFactor,
                parameters.getRiskFactor(),
                parameters.getProductivityFactor(),
                parameters.getHourlyRate(),
                parameters.getTeamSize()
        );
    }

    public static double resolveComplexityFactor(ComplexityLevel complexity, Opportunity opportunity) {
        double factor = switch (complexity) {
            case LOW -> 1.0;
            case MEDIUM -> 1.2;
            case HIGH -> 1.5;
            case VERY_HIGH -> 1.8;
        };

        factor *= switch (opportunity.getImplementationType()) {
            case NEW_DEVELOPMENT -> 1.1;
            case ENHANCEMENT -> 1.0;
            case MIGRATION -> 1.15;
            case SUPPORT -> 0.95;
        };

        int componentCount = opportunity.getComponents() != null ? opportunity.getComponents().size() : 0;
        int contextCount = opportunity.getEnterpriseContexts() != null ? opportunity.getEnterpriseContexts().size() : 0;

        factor += componentCount * 0.03;
        factor += contextCount * 0.02;

        return Math.max(0.1, factor);
    }
}
