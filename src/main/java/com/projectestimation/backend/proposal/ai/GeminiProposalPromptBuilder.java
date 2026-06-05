package com.projectestimation.backend.proposal.ai;

import java.util.List;

import org.springframework.stereotype.Component;

import com.projectestimation.backend.common.enums.ProposalType;
import com.projectestimation.backend.common.util.CurrencyFormatter;
import com.projectestimation.backend.estimation.model.EstimateResult;
import com.projectestimation.backend.opportunity.model.Opportunity;
import com.projectestimation.backend.parameters.model.Parameters;

@Component
public class GeminiProposalPromptBuilder {

    public String build(Opportunity opportunity, Parameters parameters, EstimateResult estimate, ProposalType proposalType, String workflowsSection,
    		String workflowPlaceholderRules) {
        String currencyCode = estimate.getCurrency().name();
        String formattedCost = CurrencyFormatter.formatAmount(estimate.getEstimatedCost(), estimate.getCurrency());
        String formattedHourlyRate = CurrencyFormatter.formatAmount(parameters.getHourlyRate(), parameters.getCurrency())
                + " per hour";

        return """
                You are an expert enterprise software consulting proposal writer.
                Generate a complete client-facing project proposal as VALID STRUCTURED MARKDOWN ONLY.

        		PLACEHOLDER PRESERVATION RULES

				- Some sections contain protected placeholders.
				- NEVER remove, rewrite, rename, summarize, or replace placeholders.
				- Output placeholders EXACTLY as provided.
				- Placeholders will be replaced later by the backend system.
				- Preserve placeholders character-by-character.
				
				Example:
				{{QUALITY_ASSURANCE}}
				
				must remain EXACTLY:
				{{QUALITY_ASSURANCE}}
				
				For the Solution Architecture section:

				- Generate a detailed enterprise-grade architecture explanation.
				- Explain frontend, backend, APIs, integrations, database, and security layers.
				- Keep the content professional and client-facing.
				- Use bullet points where appropriate.
				- Mention technologies from the opportunity context.
				
                CRITICAL OUTPUT RULES
                - Output valid Markdown only.
                - Do NOT output HTML, JSON, plain-text blobs, or conversational responses.
                - Do NOT wrap the response in code fences.
                - Use proper Markdown headings (#, ##, ###).
                - Use Markdown tables where required (pipe syntax).
                - Use bullet lists where appropriate.
                - Maintain professional enterprise proposal formatting.
                - Use %s for ALL monetary values. Do NOT convert currencies.
                - Every Markdown table must use bold headers.
					- Example:
					| **Column 1** | **Column 2** |

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

        		IMPORTANT PROCESS FLOW RULES

				Use ONLY the following workflows in the Important Process Flows section:
				
				%s
				
				Rules:
				- Use these workflow names exactly as provided.
				- Do not rename workflows.
				- Do not create additional workflows.
				- Do not remove workflows.
				- For each workflow provide:
				  - Objective
				  - Process Description
				  - Systems Involved
				- The Important Process Flows section must contain exactly the workflows listed above.
				
				IMPORTANT PROCESS FLOW PLACEHOLDER RULES

				%s
				
				After describing each workflow:
				
				- Insert the corresponding placeholder immediately below the workflow.
				- Do not modify placeholder names.
				- Do not remove placeholders.
				- Do not create additional placeholders.

				DOCUMENT STRUCTURE
				
				%s
             
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
                workflowsSection,
                workflowPlaceholderRules,
                resolveStructure(proposalType)
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
    
    private String resolveStructure(
            ProposalType type
    ) {

        return switch (type) {

            case BASIC -> """
                    # 1. Introduction

                    # 2. Scope
                    ## 2a. Feature List
                    Generate this section strictly as a Markdown table.

					Use the following columns:
					| Module | Feature | Description |
					
					Include all major business and technical features relevant to the project requirements.

                    ## 2b. Non-Functional Scope
                    Generate this section strictly as a Markdown table.

					Use the following columns:
					| Category | Requirement |
					
					Include performance, scalability, security, availability, maintainability, and usability considerations.
                    ## 2c. Out of Scope
                    Generate this section strictly as a Markdown table.

					Use the following columns:
					| Exclusion | Description |
					
					Include items/features/services not covered under the current proposal scope.

                    # 3. Solution Architecture
                    {{SOLUTION_ARCHITECTURE_IMAGE}}
                    Provide a detailed enterprise-grade solution architecture explanation including:
					- frontend layer
					- backend services
					- APIs
					- database
					- integrations
					- security
					- deployment approach

                    # 4. Technology Stack
                    Generate the Technology Stack section strictly as a Markdown table.

					The table must contain:
					| Layer | Technology | Purpose |
					
					Include frontend, backend, database, integration, security, hosting, and DevOps technologies where applicable.

                    # 5. Commercials
                    ## 5a. Elapsed Time
                    ## 5b. Estimated Cost
                    ## 5c. Payment Milestones
                    Generate Payment Milestones strictly as a Markdown table.

					Use the following columns:
					| Milestone | Deliverable | Payment Percentage | Amount | 
					
					Include:
					1. Analysis & Design Including approval of UI design
					2. Backend Completion
					3. Frontend and Integration
					4. Testing & UAT
                    ## 5d. Execution Plan
                    {{EXECUTION_PLAN}}

                    # 6. Organization Capabilities
                    {{ORGANISATION_CAPABILITIES_BASIC}}
                    
                    # 7. Completion Criteria
                    {{COMPLETION_CRITERIA}}
                    """;

            case INTERMEDIATE -> """
                    # 1. Introduction

                    # 2. Scope
                    ## 2a. Feature List
                    Generate this section strictly as a Markdown table.

					Use the following columns:
					| Module | Feature | Description |
					
					Include all major business and technical features user wise relevant to the project requirements.

                    ## 2b. Non-Functional Scope
                    Generate this section strictly as a Markdown table.

					Use the following columns:
					| Category | Requirement |
					
					Include performance, scalability, security, availability, maintainability, and usability considerations.
                    ## 2c. Out of Scope
                    Generate this section strictly as a Markdown table.

					Use the following columns:
					| Exclusion | Description |
					
					Include items/features/services not covered under the current proposal scope.

                    # 3. Solution Architecture
                    {{SOLUTION_ARCHITECTURE_IMAGE}}
                    Provide a detailed enterprise-grade solution architecture explanation including:
					- frontend layer
					- backend services
					- APIs
					- database
					- integrations
					- security
					- deployment approach

                    # 4. Technology Stack
                    Generate the Technology Stack section strictly as a Markdown table.

					The table must contain:
					| Layer | Technology | Purpose |
					
					Include frontend, backend, database, integration, security, hosting, and DevOps technologies where applicable.

                    # 5. Quality Assurance
                    {{QUALITY_ASSURANCE}}

                    # 6. Commercials
                    ## 6a. Elapsed Time
                    ## 6b. Estimated Cost
                    ## 6c. Payment Milestones
                    Generate Payment Milestones strictly as a Markdown table.

					Use the following columns:
					| Milestone | Deliverable | Payment Percentage | Amount | 
					
					Include:
					1. Analysis & Design Including approval of UI design
					2. Backend Completion
					3. Frontend and Integration
					4. Testing & UAT
                    ## 6d. Execution Plan
                    {{EXECUTION_PLAN}}

                    # 7. Organization Capabilities
                    {{ORGANISATION_CAPABILITIES_DETAILED}}
                    
                    # 8. Data Security
                    {{DATA_SECURITY}}
                    
                    # 9. Completion Criteria
                    {{COMPLETION_CRITERIA}}
                    """;

            case EXPERT -> """
                    # 1. Introduction

                    # 2. Scope
                    ## 2a. Feature List
                    Generate this section strictly as a Markdown table.

					Use the following columns:
					| Module | Feature | Description |
					
					Generate the Feature List STRICTLY from the Requirement Summary provided in the Opportunity Context.
					IMPORTANT Ensure the actor-wise Feature List fully covers all functional requirements described in the Requirement Summary (Do not leave any features).

					Rules:
					- Use Requirement Summary as the PRIMARY source.
					- Identify all business features, user capabilities, workflows, modules, and functions mentioned in the Requirement Summary.
					- Do NOT invent features that are not implied by the Requirement Summary.
					- Do NOT derive features from Technology Categories, Platforms, Enterprise Contexts, or Components unless they are explicitly mentioned in the Requirement Summary.
					- Group related requirements into logical business modules.
					- Each row in the table must represent a feature extracted from the Requirement Summary.
					- IMPORTANT Ensure the actor-wise Feature List fully covers all functional requirements described in the Requirement Summary.

                    ## 2b. Non-Functional Scope
                    Generate this section strictly as a Markdown table.

					Use the following columns:
					| Category | Requirement |
					
					Include performance, scalability, security, availability, maintainability, and usability considerations.
                    ## 2c. Out of Scope
                    Generate this section strictly as a Markdown table.

					Use the following columns:
					| Exclusion | Description |
					
					Include items/features/services not covered under the current proposal scope.

                    # 3. Solution Architecture
                    {{SOLUTION_ARCHITECTURE_IMAGE}}
					Provide a detailed enterprise-grade solution architecture explanation including:
					- frontend layer
					- backend services
					- APIs
					- database
					- integrations
					- security
					- deployment approach
					
					# 4. Technology Stack
                    Generate the Technology Stack section strictly as a Markdown table.
                    The table must contain:
					| Layer | Technology | Purpose |
					
					Include frontend, backend, database, integration, security, hosting, and DevOps technologies where applicable.
                    
                    # 5. Important Process Flows
                    
                    Provide detailed explanations for the major business and system workflows.

					For each important process flow:
					- Explain the workflow objective
					- Describe the step-by-step process
					- Mention involved systems/components
					- Explain validations, approvals, integrations, and notifications where applicable
					- Keep the content enterprise-grade and client-facing
					- Use subsections and bullet points where appropriate

                    # 6. Assumptions

                    # 7. Accountability Distributions
                    {{ACCOUNTIBILITY_DISTRIBUTION}}

                    # 8. Data Security
                    {{DATA_SECURITY}}

                    # 9. Quality Assurance
                    {{QUALITY_ASSURANCE}}

                    # 10. Testing Process
                    {{TESTING_PROCESS}}

                    
                    # 11. Configuration Management
                    {{CONFIGURATION_MANAGEMENT}}
                    
                    # 12. Completion Criteria
                    {{COMPLETION_CRITERIA}}
                    
                    # 13. Commercials
                    ## 13a. Elapsed Time
                    ## 13b. Estimated Cost
                    ## 13c. Payment Milestones
                    Generate Payment Milestones strictly as a Markdown table.

					Use the following columns:
					| Milestone | Deliverable | Payment Percentage | Amount | 
					
					Include:
					1. Analysis & Design Including approval of UI design
					2. Backend Completion
					3. Frontend and Integration
					4. Testing & UAT
                    
                    #14. Terms & Conditions
            		{{TERMS_AND_CONDITIONS}}
            		
            		# 15. Organization Capabilities
                    {{ORGANISATION_CAPABILITIES_DETAILED}}
                    """;
        };
    }
}
