package com.projectestimation.backend.pmp.ai;

import java.util.List;

import org.springframework.stereotype.Service;

import com.projectestimation.backend.common.ai.GeminiClient;
import com.projectestimation.backend.estimation.model.EstimationUseCase;
import com.projectestimation.backend.opportunity.model.Opportunity;

@Service
public class GeminiPmpOrchestrator {

    /*
     * The PMP is a large document.
     * Use a larger output limit so that Gemini does not truncate
     * the generated JSON.
     */
    private static final int MAX_OUTPUT_TOKENS = 16384;

    private final GeminiClient geminiClient;

    public GeminiPmpOrchestrator(GeminiClient geminiClient) {
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
                        .append(safe(useCase.getUseCaseName()))
                        .append(" | Complexity: ")
                        .append(safe(useCase.getComplexity()))
                        .append("\n");
            }

        } else {

            useCaseContext.append(
                    "No existing use cases are available."
            );
        }

        return """
                You are a senior software project manager, PMP specialist,
                quality-management specialist and enterprise software
                documentation specialist.

                Generate a COMPLETE Project Management Plan (PMP) for the
                software project described below.

                IMPORTANT:

                The output will be converted directly into a professional
                Microsoft Word document.

                The generated PMP must follow the structure and level of
                detail of the provided BEAS-style Project Management Plan.

                DO NOT generate the old simplified PMP structure.

                The PMP must contain all of the following major sections:

                1. INTRODUCTION
                2. PROJECT GOALS AND QUALITY OBJECTIVES
                3. DEFINED PROCESS / PROJECT LIFE CYCLE
                4. PROJECT ENVIRONMENTS
                5. PROJECT MANAGEMENT ISSUES
                6. ORGANIZATION AND RESOURCES
                7. PROJECT MONITORING AND CONTROL
                8. INTER-GROUP SUPPORT
                9. ESTIMATED SIZE AND EFFORT
                10. PROJECT SCHEDULE
                11. METRICATION PLAN
                12. QUALITY CONTROL PLAN
                13. VALIDATION PLAN
                14. QUALITY AUDIT PLAN
                15. CONFIGURATION MANAGEMENT PLAN

                ============================================================
                PROJECT INFORMATION
                ============================================================

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

                ============================================================
                EXISTING USE CASES
                ============================================================

                %s

                ============================================================
                GENERAL PMP RULES
                ============================================================

                1. Generate a realistic professional PMP.

                2. Use the project information as the primary source.

                3. Use the existing use cases as additional project context.

                4. Do not invent unsupported business functionality.

                5. Do not introduce unrelated technologies, modules or
                   business processes.

                6. Keep the entire PMP internally consistent.

                7. Project objectives, scope, deliverables, schedule,
                   resources, risks, quality activities and metrics must
                   describe the same project.

                8. Do not invent specific dates unless dates are available
                   from the project information.

                9. Do not invent specific effort, duration, budget or
                   resource numbers unless sufficient project information
                   is available.

                10. If an exact value is unavailable, use a professional
                    descriptive value rather than fabricating a number.

                11. Arrays MUST always be JSON arrays.

                12. Never return null for an array.

                13. If a section has no applicable entries, return [].

                14. Return JSON only.

                15. Do not return Markdown.

                16. Do not use ```json.

                17. Do not include explanations before or after the JSON.

                ============================================================
                CRITICAL JSON TYPE RULES
                ============================================================

                FOLLOW THESE RULES EXACTLY.

                The Java backend will deserialize your response into Java
                DTO classes. Therefore the JSON data types MUST match
                the following definitions exactly.

                ------------------------------------------------------------
                SIMPLE STRING ARRAYS
                ------------------------------------------------------------

                The following fields MUST contain ONLY strings:

                projectOverview.objectives
                projectOverview.deliverables
                projectOverview.assumptions
                projectOverview.constraints
                projectOverview.acceptanceCriteria

                These fields MUST look like this:

                "objectives": [
                    "Eliminate duplicate administration across five travel websites.",
                    "Implement a centralized Laravel application with a single database.",
                    "Support 10 languages across all system components."
                ]

                "deliverables": [
                    "Technical Architecture Specification",
                    "Figma Wireframes and Designs",
                    "Centralized Laravel 12 Platform"
                ]

                "assumptions": [
                    "Client will provide timely feedback on design and functional milestones."
                ]

                "constraints": [
                    "Fixed budget of $10,000 USD."
                ]

                "acceptanceCriteria": [
                    "Successful technical review of architecture and code quality."
                ]

                VERY IMPORTANT:

                NEVER generate an object inside these arrays.

                WRONG:

                "objectives": [
                    {
                        "name": "Eliminate duplicate administration"
                    }
                ]

                WRONG:

                "objectives": [
                    {
                        "description": "Eliminate duplicate administration"
                    }
                ]

                WRONG:

                "objectives": [
                    {
                        "objective": "Eliminate duplicate administration"
                    }
                ]

                CORRECT:

                "objectives": [
                    "Eliminate duplicate administration across five travel websites."
                ]

                The same rule applies to:

                - objectives
                - deliverables
                - assumptions
                - constraints
                - acceptanceCriteria

                ------------------------------------------------------------
                PMP ITEM ARRAYS
                ------------------------------------------------------------

                The following arrays contain PmpItemDto objects:

                - organization
                - resources
                - estimation
                - schedule
                - communication
                - configurationManagement
                - qualityStandards
                - reviews
                - testing
                - metrics
                - risks
                - mitigationStrategies
                - contingencyPlans

                Every element in these arrays MUST be a JSON OBJECT.

                Every object MUST contain these six fields:

                {
                    "name": "...",
                    "description": "...",
                    "responsible": "...",
                    "timing": "...",
                    "target": "...",
                    "status": "..."
                }

                Example:

                "organization": [
                    {
                        "name": "Lead Laravel Architect",
                        "description": "Responsible for system architecture, core development, and technical decisions.",
                        "responsible": "Lead Developer",
                        "timing": "Entire Project",
                        "target": "Technical Excellence",
                        "status": "Active"
                    }
                ]

                NEVER generate:

                "organization": [
                    "Lead Laravel Architect"
                ]

                NEVER generate:

                "organization": [
                    {
                        "role": "Lead Laravel Architect"
                    }
                ]

                Use the exact six field names specified above.

                ============================================================
                DOCUMENT CONTROL
                ============================================================

                Generate:

                - Document release history
                - Circulation details
                - Amendments

                Release history should contain appropriate information such
                as version, date, author, reason for change and approver.

                Circulation details should contain appropriate recipient,
                designation, organization, email and purpose information.

                Amendments should contain section, description, reason,
                date and author.

                ============================================================
                SECTION 1 - INTRODUCTION
                ============================================================

                Generate:

                - Project overview
                - Customer/client interface
                - Scope of work
                - Compliance requirements
                - Project deliverables
                - Project milestones
                - Acceptance criteria

                Customer interface should support:

                - Name
                - Designation
                - Phone number
                - Fax number
                - Email
                - Skype ID

                Detailed deliverables should contain:

                - Serial number
                - Item description
                - Delivery date
                - Delivery location
                - Quantity
                - Remarks

                Milestones should contain:

                - Phase
                - Milestone
                - Description
                - Target date
                - Deliverable

                ============================================================
                SECTION 2 - PROJECT GOALS AND QUALITY OBJECTIVES
                ============================================================

                Generate:

                - Project objectives
                - Mapping between project objectives and business objectives
                - Project goals
                - Quality objectives
                - Quantifiable quality targets where supported

                Objective/business-objective mapping should explain how each
                project objective contributes to the business objective.

                ============================================================
                SECTION 3 - DEFINED PROCESS / PROJECT LIFE CYCLE
                ============================================================

                Define an appropriate software development lifecycle.

                Include:

                - Project lifecycle
                - Software development lifecycle
                - Critical processes
                - Process goals
                - Tailored processes
                - Decision Analysis and Resolution activities

                Use appropriate lifecycle activities such as:

                - Planning
                - Requirement Analysis
                - Design
                - Development
                - Unit Testing
                - Integration / SIT
                - System Testing
                - UAT
                - Deployment
                - Project Closure

                Do not include irrelevant lifecycle phases.

                ============================================================
                SECTION 4 - PROJECT ENVIRONMENTS
                ============================================================

                Describe:

                - Development environment
                - Testing environment
                - Production / Operation environment

                For each environment describe:

                - Environment name
                - Purpose
                - Hardware
                - Software
                - Configuration
                - Responsible person/role

                ============================================================
                SECTION 5 - PROJECT MANAGEMENT ISSUES
                ============================================================

                Generate realistic:

                - Dependencies
                - Assumptions
                - Project risks

                Risks should include:

                - Risk name
                - Description
                - Responsible person/role
                - Timing
                - Impact
                - Probability
                - Mitigation
                - Contingency
                - Status

                Do not generate unrelated risks.

                ============================================================
                SECTION 6 - ORGANIZATION AND RESOURCES
                ============================================================

                Generate:

                6.1 Hardware and Networking
                6.2 Software and Tools
                6.3 Manpower and Competency
                6.4 Project Team
                6.5 Training Plan

                Hardware and networking should contain appropriate:

                - Name
                - Configuration
                - Quantity
                - Purpose
                - Responsibility

                Software and tools should contain appropriate:

                - Software/tool
                - Version
                - Purpose
                - License
                - Responsibility

                Manpower should contain:

                - Role
                - Competency
                - Experience
                - Quantity
                - Responsibility

                Project team should contain:

                - Name
                - Designation
                - Role
                - Responsibility
                - Allocation

                Training plan should contain:

                - Training
                - Audience
                - Trainer
                - Timing
                - Objective
                - Status

                ============================================================
                SECTION 7 - PROJECT MONITORING AND CONTROL
                ============================================================

                Generate:

                - Monitoring mechanisms
                - Quantitative monitoring

                Monitoring mechanisms should explain:

                - Activity
                - Frequency
                - Who monitors
                - Measurement
                - Target
                - Corrective action

                Quantitative monitoring should contain:

                - Parameter
                - Measurement method
                - Frequency
                - Target
                - Threshold
                - Action

                ============================================================
                SECTION 8 - INTER-GROUP SUPPORT
                ============================================================

                Generate realistic inter-group support requirements.

                Include:

                - Supporting group
                - Supported group
                - Support required
                - Timing
                - Responsible person
                - Status

                ============================================================
                SECTION 9 - ESTIMATED SIZE AND EFFORT
                ============================================================

                Use available estimation information and existing use cases.

                Include:

                - Estimated size details
                - Estimation method
                - Estimated size
                - Unit
                - Basis

                Also include effort details:

                - Activity
                - Role
                - Effort
                - Unit
                - Basis

                If exact values are not available, do not fabricate
                precise numbers.

                ============================================================
                SECTION 10 - PROJECT SCHEDULE
                ============================================================

                Generate a realistic project schedule based on the available
                project information.

                Each schedule item should contain:

                - Serial number
                - Phase
                - Activity
                - Start date
                - End date
                - Duration
                - Responsible person
                - Status

                Do not invent calendar dates when dates are not provided.

                ============================================================
                SECTION 11 - METRICATION PLAN
                ============================================================

                Generate:

                11.1 Critical Process Metrics
                11.2 Other Metrics
                11.3 Data Capturing

                Metrics should contain:

                - Serial number
                - Metric
                - Purpose
                - Measurement method
                - Frequency
                - Target
                - Responsible person

                Data capturing should contain:

                - Data item
                - Source
                - Collection method
                - Frequency
                - Owner
                - Storage

                Include useful software project metrics such as quality,
                defects, schedule, effort, productivity, testing and
                performance only where applicable.

                ============================================================
                SECTION 12 - QUALITY CONTROL PLAN
                ============================================================

                Generate:

                12.1 Standards Applicable
                12.2 Product Review and Testing

                Standards should contain:

                - Serial number
                - Standard
                - Applicable area
                - Requirement
                - Responsible person
                - Evidence

                Product review/testing should contain:

                - Serial number
                - Work product
                - Type of review/testing
                - Reviewer/tester level
                - Timing
                - Remarks

                Include appropriate reviews such as:

                - Requirement review
                - SRS/FS review
                - Design review
                - Architecture review
                - Code review
                - Test plan review
                - Test case review
                - User documentation review

                Include appropriate testing such as:

                - Unit Testing
                - Integration Testing
                - System Testing
                - UAT
                - Regression Testing
                - Performance Testing

                Only include testing that is appropriate to the project.

                ============================================================
                SECTION 13 - VALIDATION PLAN
                ============================================================

                Generate:

                - Validation plan name
                - Description
                - Responsible
                - Timing
                - Target
                - Status
                - Validation activities

                Validation activities should contain:

                - Serial number
                - Activity
                - Acceptance criteria
                - Method
                - Responsible
                - Timing
                - Evidence
                - Status

                ============================================================
                SECTION 14 - QUALITY AUDIT PLAN
                ============================================================

                Generate a quality audit plan.

                Each audit should contain:

                - Serial number
                - Audit type
                - Audit item / lifecycle activities covered
                - Periodicity of audit
                - Target date
                - Person responsible for conducting audit
                - Remarks

                ============================================================
                SECTION 15 - CONFIGURATION MANAGEMENT PLAN
                ============================================================

                Generate:

                15.1 Configuration Items
                15.2 Baselining
                15.3 Release Procedure
                15.4 Version Control
                15.5 Status Accounting
                15.6 Configuration Audit
                15.7 Backup

                Configuration items should contain:

                - Serial number
                - Configuration item
                - Type
                - Repository
                - Owner
                - Baseline
                - Remarks

                The remaining configuration management sections should
                contain professional project-specific descriptions.

                ============================================================
                OUTPUT JSON STRUCTURE
                ============================================================

                Return exactly ONE JSON object.

                The root object MUST contain:

                {
                  "pmp": {
                    ...
                  }
                }

                The "pmp" object MUST contain exactly these sections:

                {
                  "documentControl": {},
                  "projectOverview": {},
                  "projectManagement": {},
                  "qualityManagement": {},
                  "riskManagement": {},
                  "validationPlan": {},
                  "environment": {},
                  "organizationResources": {},
                  "monitoringControl": {},
                  "interGroupSupport": {},
                  "estimatedSizeEffort": {},
                  "schedule": {},
                  "metricationPlan": {},
                  "qualityControlPlan": {},
                  "qualityAuditPlan": {},
                  "configurationManagementPlan": {}
                }

                ============================================================
                REQUIRED JSON SHAPE
                ============================================================

                {
                  "pmp": {
                    "documentControl": {
                      "releaseHistory": [],
                      "circulationDetails": [],
                      "amendments": []
                    },

                    "projectOverview": {
                      "projectName": "",
                      "projectDescription": "",
                      "projectScope": "",

                      "objectives": [
                        "Objective text"
                      ],

                      "deliverables": [
                        "Deliverable text"
                      ],

                      "assumptions": [
                        "Assumption text"
                      ],

                      "constraints": [
                        "Constraint text"
                      ],

                      "acceptanceCriteria": [
                        "Acceptance criterion text"
                      ],

                      "customerInterface": {
                        "name": "",
                        "designation": "",
                        "phoneNumber": "",
                        "faxNumber": "",
                        "email": "",
                        "skypeId": ""
                      },

                      "complianceRequirements": [],
                      "detailedDeliverables": [],
                      "milestones": []
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
                      ],

                      "projectGoals": [],
                      "qualityObjectives": [],
                      "processGoals": [],
                      "tailoredProcesses": [],
                      "decisionAnalysis": []
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
                      ],

                      "qualityObjectives": [],
                      "audits": [],
                      "productReviews": []
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
                      ],

                      "dependencies": [],
                      "assumptions": []
                    },

                    "validationPlan": {
                      "name": "",
                      "description": "",
                      "responsible": "",
                      "timing": "",
                      "target": "",
                      "status": "",
                      "activities": []
                    },

                    "environment": {
                      "development": {},
                      "testing": {},
                      "operation": {}
                    },

                    "organizationResources": {
                      "hardwareNetworking": [],
                      "softwareTools": [],
                      "manpowerCompetency": [],
                      "projectTeam": [],
                      "trainingPlan": []
                    },

                    "monitoringControl": {
                      "monitoringMechanism": [],
                      "quantitativeMonitoring": []
                    },

                    "interGroupSupport": {
                      "supportItems": []
                    },

                    "estimatedSizeEffort": {
                      "sizeDetails": [],
                      "effortDetails": []
                    },

                    "schedule": {
                      "scheduleItems": []
                    },

                    "metricationPlan": {
                      "criticalProcessMetrics": [],
                      "otherMetrics": [],
                      "dataCapturing": []
                    },

                    "qualityControlPlan": {
                      "standardsApplicable": [],
                      "productReviewTesting": []
                    },

                    "qualityAuditPlan": {
                      "audits": []
                    },

                    "configurationManagementPlan": {
                      "configurationItems": [],
                      "baselining": "",
                      "releaseProcedure": "",
                      "versionControl": "",
                      "statusAccounting": "",
                      "audit": "",
                      "backup": ""
                    }
                  }
                }

                ============================================================
                FINAL VALIDATION BEFORE RETURNING
                ============================================================

                Before returning the response, verify ALL of the following:

                1. The response is valid JSON.

                2. The response contains only the root "pmp" object.

                3. All required PMP sections are present.

                4. All arrays are JSON arrays.

                5. No array is null.

                6. No Markdown exists.

                7. No code fences exist.

                8. No explanatory text exists outside the JSON.

                9. The PMP is internally consistent.

                10. Content is specific to the supplied project.

                11. Unsupported functionality is not invented.

                12. Unsupported exact dates/numbers are not fabricated.

                13. The generated content is detailed enough to populate
                    a professional multi-page PMP document.

                ============================================================
                FINAL TYPE CHECK - MANDATORY
                ============================================================

                Before returning JSON, check these fields specifically:

                projectOverview.objectives
                projectOverview.deliverables
                projectOverview.assumptions
                projectOverview.constraints
                projectOverview.acceptanceCriteria

                EVERY ELEMENT of these five arrays MUST be a STRING.

                Example:

                "objectives": [
                    "Objective one",
                    "Objective two",
                    "Objective three"
                ]

                NEVER:

                "objectives": [
                    {
                      "name": "Objective one"
                    }
                ]

                NEVER:

                "objectives": [
                    {
                      "description": "Objective one"
                    }
                ]

                The following arrays MUST contain OBJECTS:

                organization
                resources
                estimation
                schedule
                communication
                configurationManagement
                qualityStandards
                reviews
                testing
                metrics
                risks
                mitigationStrategies
                contingencyPlans

                Each object in those arrays MUST contain:

                name
                description
                responsible
                timing
                target
                status

                Do not change these JSON types.

                END OF INSTRUCTIONS

                """.formatted(
                        safe(opportunity.getOpportunityName()),
                        safe(opportunity.getImplementationType()),
                        safe(opportunity.getPriority()),
                        safe(opportunity.getRequirementSummary()),
                        safe(opportunity.getComponents()),
                        useCaseContext
                );
    }

    private String safe(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}