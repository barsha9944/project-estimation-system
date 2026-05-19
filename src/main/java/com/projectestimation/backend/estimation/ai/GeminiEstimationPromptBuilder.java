package com.projectestimation.backend.estimation.ai;

import com.projectestimation.backend.opportunity.model.Opportunity;
import com.projectestimation.backend.parameters.model.Parameters;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class GeminiEstimationPromptBuilder {

    public String build(Opportunity opportunity, Parameters parameters) {
        return buildPrompt(
                opportunity.getOpportunityName(),
                opportunity.getClientName(),
                opportunity.getImplementationType().name(),
                joinList(opportunity.getPlatforms()),
                joinList(opportunity.getTechnologyCategories()),
                joinList(opportunity.getEnterpriseContexts()),
                joinList(opportunity.getComponents()),
                opportunity.getRequirementSummary(),
                parameters.getComplexity().name(),
                parameters.getRiskFactor(),
                parameters.getProductivityFactor(),
                parameters.getHourlyRate(),
                parameters.getTeamSize()
        );
    }

    public String buildFromLegacyPayload(
            String projectName,
            String requirementSummary,
            double complexityFactor,
            double riskFactor,
            double productivityFactor,
            double hourlyRate,
            int teamSize
    ) {
        return buildPrompt(
                projectName,
                "N/A",
                "LEGACY",
                "N/A",
                "N/A",
                "N/A",
                "N/A",
                requirementSummary,
                String.valueOf(complexityFactor),
                riskFactor,
                productivityFactor,
                hourlyRate,
                teamSize
        );
    }

    private String buildPrompt(
            String opportunityName,
            String clientName,
            String implementationType,
            String platforms,
            String technologyCategories,
            String enterpriseContexts,
            String components,
            String requirementSummary,
            String complexity,
            double riskFactor,
            double productivityFactor,
            double hourlyRate,
            int teamSize
    ) {
        return """
                You are an expert software project estimation consultant.
                Analyze the opportunity and parameters below and produce a realistic project estimate.

                OPPORTUNITY CONTEXT
                - Opportunity Name: %s
                - Client Name: %s
                - Implementation Type: %s
                - Platforms: %s
                - Technology Categories: %s
                - Enterprise Contexts: %s
                - Components / Modules: %s
                - Requirement Summary: %s

                ESTIMATION PARAMETERS
                - Complexity Level: %s
                - Risk Factor: %.2f
                - Productivity Factor: %.2f
                - Hourly Rate: %.2f
                - Team Size: %d

                INSTRUCTIONS
                - Use the parameters and opportunity context to estimate effort, cost, and timeline.
                - Cost should align with effort, hourly rate, and team size.
                - Confidence score must be between 0 and 100.
                - Provide concise, professional reasoning and a breakdown summary.

                Respond with JSON only (no markdown, no code fences) using exactly these fields:
                {
                  "totalEffortHours": <number>,
                  "estimatedCost": <number>,
                  "timelineWeeks": <number>,
                  "confidenceScore": <number>,
                  "breakdown": "<short summary of major workstreams and assumptions>",
                  "reasoning": "<detailed estimation reasoning>"
                }
                """.formatted(
                opportunityName,
                clientName,
                implementationType,
                platforms,
                technologyCategories,
                enterpriseContexts,
                components,
                requirementSummary,
                complexity,
                riskFactor,
                productivityFactor,
                hourlyRate,
                teamSize
        );
    }

    private String joinList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "None";
        }
        return String.join(", ", values);
    }
}
