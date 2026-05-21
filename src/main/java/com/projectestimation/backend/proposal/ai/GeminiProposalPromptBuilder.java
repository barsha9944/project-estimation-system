package com.projectestimation.backend.proposal.ai;

import com.projectestimation.backend.common.util.CurrencyFormatter;
import com.projectestimation.backend.estimation.model.EstimateResult;
import com.projectestimation.backend.opportunity.model.Opportunity;
import com.projectestimation.backend.parameters.model.Parameters;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class GeminiProposalPromptBuilder {

    public String build(Opportunity opportunity, Parameters parameters, EstimateResult estimate) {
        String currencyCode = estimate.getCurrency().name();
        String formattedCost = CurrencyFormatter.formatAmount(estimate.getEstimatedCost(), estimate.getCurrency());
        String formattedHourlyRate = CurrencyFormatter.formatAmount(parameters.getHourlyRate(), parameters.getCurrency())
                + " per hour";

        return """
                You are an expert enterprise software consulting proposal writer.
                Generate a complete client-facing project proposal as VALID STRUCTURED MARKDOWN ONLY.

                CRITICAL OUTPUT RULES
                - Output valid Markdown only.
                - Do NOT output HTML, JSON, plain-text blobs, or conversational responses.
                - Do NOT wrap the response in code fences.
                - Use proper Markdown headings (#, ##, ###).
                - Use Markdown tables where required (pipe syntax).
                - Use bullet lists where appropriate.
                - Maintain professional enterprise proposal formatting.
                - Use %s for ALL monetary values. Do NOT convert currencies.

                OPPORTUNITY CONTEXT
                - Opportunity Name: %s
                - Client Name: %s
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
                - Hourly Rate: %s
                - Currency: %s
                - Team Size: %d

                AI ESTIMATE
                - Total Effort Hours: %.2f
                - Estimated Cost: %s
                - Timeline (weeks): %.2f
                - Confidence Score: %.2f
                - Breakdown: %s
                - Reasoning: %s

                REQUIRED DOCUMENT STRUCTURE (use these exact top-level sections as Markdown headings)

                # 1. Introduction

                # 2. Scope of Work
                Include a Markdown table with columns: Work Package | Description | Deliverables

                # 3. Solution Architecture

                # 4. Technology Stack

                # 5. Quality Assurance

                # 6. Project Governance
                Include an escalation matrix as a Markdown table with columns: Level | Role | Contact | Response Time

                # 7. Commercials
                ## i. Elapsed Time
                ## ii. Payment Milestones
                - Display total estimated cost as: %s
                - All payment milestone amounts must use %s labels (e.g. %s 25,000.00 or %s 12,00,000.00 as appropriate).
                Include payment milestones as a Markdown table with columns: Milestone | Description | %% Payment | Amount (%s) | Target Date


                # 8. Organization Capabilities

                Align timeline, effort, and cost content with the AI estimate provided above.
                Be specific to the opportunity context.
                """.formatted(
                currencyCode,
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
                formattedHourlyRate,
                currencyCode,
                parameters.getTeamSize(),
                estimate.getTotalEffortHours(),
                formattedCost,
                estimate.getTimelineWeeks(),
                estimate.getConfidenceScore(),
                nullSafe(estimate.getBreakdown()),
                nullSafe(estimate.getReasoning()),
                formattedCost,
                currencyCode,
                currencyCode,
                currencyCode,
                currencyCode,
                currencyCode
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
