package com.projectestimation.backend.pmp.ai;

import java.util.List;

import org.springframework.stereotype.Service;

import com.projectestimation.backend.common.ai.GeminiClient;
import com.projectestimation.backend.estimation.model.EstimationUseCase;
import com.projectestimation.backend.opportunity.model.Opportunity;

@Service
public class GeminiPmpOrchestrator {

    private static final int MAX_OUTPUT_TOKENS = 8192;

    private final GeminiClient geminiClient;

    public GeminiPmpOrchestrator(
            GeminiClient geminiClient
    ) {
        this.geminiClient = geminiClient;
    }

    public String generate(
            Opportunity opportunity,
            List<EstimationUseCase> useCases
    ) {

        String prompt = buildPrompt(
                opportunity,
                useCases
        );

        return geminiClient.generateJsonContent(
                prompt,
                MAX_OUTPUT_TOKENS
        );
    }

    private String buildPrompt(
            Opportunity opportunity,
            List<EstimationUseCase> useCases
    ) {

        StringBuilder useCaseContext = new StringBuilder();

        if (useCases != null && !useCases.isEmpty()) {

            for (EstimationUseCase useCase : useCases) {

                useCaseContext
                        .append("- ")
                        .append(useCase.getUseCaseName())
                        .append(" | Complexity: ")
                        .append(useCase.getComplexity())
                        .append("\n");
            }

        } else {

            useCaseContext.append(
                    "No existing use cases are available."
            );
        }

        return """
                You are a senior software project manager and PMP specialist.

                Generate a comprehensive Project Management Plan (PMP)
                for the software project described below.

                The PMP must be realistic, professional, internally consistent,
                and based only on the information provided.

                Do NOT invent specific business functionality that is not
                supported by the project information.

                =========================================================
                PROJECT INFORMATION
                =========================================================

                Project Name:
                %s

                Implementation Type:
                %s

                Priority:
                %s

                Requirement Summary:
                %s

                Components:
                %s

                =========================================================
                EXISTING USE CASES
                =========================================================

                %s

                =========================================================
                PMP GENERATION REQUIREMENTS
                =========================================================

                Generate the PMP using the following major sections:

                1. PROJECT OVERVIEW
                2. PROJECT MANAGEMENT
                3. QUALITY MANAGEMENT
                4. RISK MANAGEMENT
                5. VALIDATION PLAN

                =========================================================
                PROJECT OVERVIEW
                =========================================================

                Include:

                - Project name
                - Project description
                - Project scope
                - Project objectives
                - Major deliverables
                - Assumptions
                - Constraints
                - Acceptance criteria

                =========================================================
                PROJECT MANAGEMENT
                =========================================================

                Include:

                - Development methodology
                - Project lifecycle phases
                - Project organization and responsibilities
                - Required project resources
                - Estimation approach
                - Schedule management
                - Communication management
                - Configuration management

                Lifecycle phases may include appropriate software
                development activities such as:

                - Planning
                - Requirement Analysis
                - Design
                - Development / Construction
                - Unit Testing
                - Integration / SIT
                - UAT
                - Deployment
                - Project Closure

                Use only phases that are appropriate for the project.

                =========================================================
                QUALITY MANAGEMENT
                =========================================================

                Include:

                - Quality standards
                - Requirement/document reviews
                - Design reviews
                - Test plan and test case reviews
                - Code reviews
                - Testing activities
                - Quality metrics
                - Quality targets
                - Measurement methods
                - Review frequency

                =========================================================
                RISK MANAGEMENT
                =========================================================

                Identify realistic project risks.

                For every risk include:

                - Risk name
                - Description
                - Responsible person/role
                - Timing
                - Impact
                - Probability
                - Mitigation strategy
                - Contingency plan

                Do not create unrealistic or unrelated risks.

                =========================================================
                VALIDATION PLAN
                =========================================================

                Include appropriate validation activities such as:

                - Requirement validation
                - Functional validation
                - System/integration testing
                - UAT
                - Client validation
                - Acceptance verification

                Every validation activity should contain:

                - Activity
                - Responsible party
                - Timing
                - Acceptance criteria

                =========================================================
                OUTPUT FORMAT
                =========================================================

                Return ONLY valid JSON.

                Do NOT include:

                - Markdown
                - Code fences
                - Explanations before JSON
                - Explanations after JSON

                The JSON MUST follow exactly this structure:

                {
                  "pmp": {
                    "projectOverview": {
                      "projectName": "",
                      "projectDescription": "",
                      "projectScope": "",
                      "objectives": [],
                      "deliverables": [],
                      "assumptions": [],
                      "constraints": [],
                      "acceptanceCriteria": []
                    },

                    "projectManagement": {
                      "methodology": "",
                      "lifecyclePhases": [],

                      "organization": [
                        {
                          "name": "",
                          "description": "",
                          "responsible": "",
                          "timing": "",
                          "target": "",
                          "status": ""
                        }
                      ],

                      "resources": [
                        {
                          "name": "",
                          "description": "",
                          "responsible": "",
                          "timing": "",
                          "target": "",
                          "status": ""
                        }
                      ],

                      "estimation": [
                        {
                          "name": "",
                          "description": "",
                          "responsible": "",
                          "timing": "",
                          "target": "",
                          "status": ""
                        }
                      ],

                      "schedule": [
                        {
                          "name": "",
                          "description": "",
                          "responsible": "",
                          "timing": "",
                          "target": "",
                          "status": ""
                        }
                      ],

                      "communication": [
                        {
                          "name": "",
                          "description": "",
                          "responsible": "",
                          "timing": "",
                          "target": "",
                          "status": ""
                        }
                      ],

                      "configurationManagement": [
                        {
                          "name": "",
                          "description": "",
                          "responsible": "",
                          "timing": "",
                          "target": "",
                          "status": ""
                        }
                      ]
                    },

                    "qualityManagement": {
                      "qualityStandards": [
                        {
                          "name": "",
                          "description": "",
                          "responsible": "",
                          "timing": "",
                          "target": "",
                          "status": ""
                        }
                      ],

                      "reviews": [
                        {
                          "name": "",
                          "description": "",
                          "responsible": "",
                          "timing": "",
                          "target": "",
                          "status": ""
                        }
                      ],

                      "testing": [
                        {
                          "name": "",
                          "description": "",
                          "responsible": "",
                          "timing": "",
                          "target": "",
                          "status": ""
                        }
                      ],

                      "metrics": [
                        {
                          "name": "",
                          "description": "",
                          "responsible": "",
                          "timing": "",
                          "target": "",
                          "status": ""
                        }
                      ]
                    },

                    "riskManagement": {
                      "risks": [
                        {
                          "name": "",
                          "description": "",
                          "responsible": "",
                          "timing": "",
                          "target": "",
                          "status": ""
                        }
                      ],

                      "mitigationStrategies": [
                        {
                          "name": "",
                          "description": "",
                          "responsible": "",
                          "timing": "",
                          "target": "",
                          "status": ""
                        }
                      ],

                      "contingencyPlans": [
                        {
                          "name": "",
                          "description": "",
                          "responsible": "",
                          "timing": "",
                          "target": "",
                          "status": ""
                        }
                      ]
                    },

                    "validationPlan": {
                      "name": "",
                      "description": "",
                      "responsible": "",
                      "timing": "",
                      "target": "",
                      "status": ""
                    }
                  }
                }

                =========================================================
                IMPORTANT RULES
                =========================================================

                1. Return valid JSON only.

                2. Every array must be a JSON array.

                3. Do not return null for arrays.
                   Return [] when there is no applicable item.

                4. Do not invent project-specific functionality.

                5. Use the Requirement Summary as the primary source
                   of project functionality.

                6. Use Existing Use Cases as additional project context.

                7. Keep all PMP sections consistent with one another.

                8. Use professional enterprise software project-management
                   terminology.

                9. Do not generate arbitrary dates unless dates are provided
                   in the project information.

                10. Do not invent effort or duration numbers when they are
                    not available.

                11. The generated PMP should be suitable for review by
                    project managers, technical leads, QA teams and clients.

                =========================================================
                END
                =========================================================
                """.formatted(
                        opportunity.getOpportunityName(),
                        opportunity.getImplementationType(),
                        opportunity.getPriority(),
                        opportunity.getRequirementSummary(),
                        opportunity.getComponents(),
                        useCaseContext
                );
    }
}