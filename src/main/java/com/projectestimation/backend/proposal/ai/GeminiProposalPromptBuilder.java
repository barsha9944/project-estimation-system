package com.projectestimation.backend.proposal.ai;

import com.projectestimation.backend.estimation.model.EstimateResult;
import com.projectestimation.backend.opportunity.model.Opportunity;
import com.projectestimation.backend.parameters.model.Parameters;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class GeminiProposalPromptBuilder {

    public String build(Opportunity opportunity, Parameters parameters, EstimateResult estimate) {
        return """
                You are an expert enterprise software consulting proposal writer.
                Generate a professional client-facing project proposal using the context below.

                OPPORTUNITY
                - Name: %s
                - Client: %s
                - Implementation Type: %s
                - Platforms: %s
                - Technology Categories: %s
                - Enterprise Contexts: %s
                - Components: %s
                - Priority: %s
                - Expected Delivery Date: %s
                - Requirement Summary: %s

                PARAMETERS
                - Complexity: %s
                - Risk Factor: %.2f
                - Productivity Factor: %.2f
                - Hourly Rate: %.2f
                - Team Size: %d

                AI ESTIMATE
                - Total Effort Hours: %.2f
                - Estimated Cost: %.2f
                - Timeline (weeks): %.2f
                - Confidence Score: %.2f
                - Breakdown: %s
                - Reasoning: %s

                INSTRUCTIONS
                - Write professional, enterprise-grade proposal content.
                - Align timeline and cost with the AI estimate.
                - Be specific to the opportunity context.
                - Use clear business language suitable for client presentation.

                Respond with JSON only (no markdown, no code fences) using exactly these fields:
                {
                  "executiveSummary": "...",
                  "projectOverview": "...",
                  "scopeOfWork": "...",
                  "technologyStack": "...",
                  "componentsAndFeatures": "...",
                  "deliveryApproach": "...",
                  "teamStructure": "...",
                  "timelineEstimate": "...",
                  "costEstimate": "...",
                  "risksAndAssumptions": "...",
                  "supportAndMaintenance": "...",
                  "conclusion": "..."
                }
                """.formatted(
                opportunity.getOpportunityName(),
                opportunity.getClientName(),
                opportunity.getImplementationType().name(),
                joinList(opportunity.getPlatforms()),
                joinList(opportunity.getTechnologyCategories()),
                joinList(opportunity.getEnterpriseContexts()),
                joinList(opportunity.getComponents()),
                opportunity.getPriority().name(),
                opportunity.getExpectedDeliveryDate() != null
                        ? opportunity.getExpectedDeliveryDate().toString()
                        : "Not specified",
                opportunity.getRequirementSummary(),
                parameters.getComplexity().name(),
                parameters.getRiskFactor(),
                parameters.getProductivityFactor(),
                parameters.getHourlyRate(),
                parameters.getTeamSize(),
                estimate.getTotalEffortHours(),
                estimate.getEstimatedCost(),
                estimate.getTimelineWeeks(),
                estimate.getConfidenceScore(),
                nullSafe(estimate.getBreakdown()),
                nullSafe(estimate.getReasoning())
        );
    }

    private String joinList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "None";
        }
        return String.join(", ", values);
    }

    private String nullSafe(String value) {
        return value == null || value.isBlank() ? "Not provided" : value;
    }
}
