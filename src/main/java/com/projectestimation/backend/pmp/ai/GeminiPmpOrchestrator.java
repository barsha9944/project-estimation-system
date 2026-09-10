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

                The output will be converted directly into a professional
                Microsoft Word document.

                The PMP must follow the structure, organization, terminology
                and level of detail of the provided BEAS-style Project
                Management Plan.

                DO NOT generate the old simplified PMP structure.

                ============================================================
                MANDATORY PMP SECTIONS
                ============================================================

                The PMP must contain ALL of these sections:

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
                IMPORTANT CONTENT GENERATION RULE
                ============================================================

                Every major PMP section must contain meaningful,
                project-specific content.

                DO NOT leave sections empty merely because the supplied
                project information does not contain an exact value.

                If an exact value is unavailable:

                - Use a professional descriptive value.
                - Use "To Be Confirmed" where appropriate.
                - Use "Based on approved project schedule" where appropriate.
                - Use "Project-defined" where appropriate.
                - Use "As applicable" where appropriate.

                DO NOT fabricate unsupported exact dates, monetary amounts,
                effort numbers, resource quantities, infrastructure
                specifications or other precise values.

                DO NOT invent specific technologies, tools, products,
                infrastructure, team members, budgets, percentages,
                dates or numerical targets unless they are supported by
                the supplied project information.

                If a specific value is not available, use a professional
                descriptive statement instead of inventing a precise value.

                However, DO NOT return empty arrays for a section simply
                because exact information is unavailable.

                Each major section should contain multiple useful entries
                wherever the section logically supports multiple entries.

                ============================================================
                GENERAL PMP RULES
                ============================================================

                1. Generate a realistic professional PMP.

                2. Use the supplied project information as the PRIMARY source.

                3. Use the existing use cases as additional project context.

                4. Keep the PMP specific to the supplied project.

                5. Do not invent unsupported business functionality.

                6. Do not introduce unrelated technologies, modules,
                   products or business processes.

                7. Keep the entire PMP internally consistent.

                8. Project objectives, scope, deliverables, lifecycle,
                   resources, risks, quality activities, metrics, schedule
                   and configuration management must describe the same
                   project.

                9. Use the supplied implementation type, requirements,
                   components and use cases when determining appropriate
                   project activities.

                10. Do not invent specific calendar dates unless dates are
                    available from the project information.

                11. Do not invent precise effort, duration, budget or
                    resource quantities unless sufficient information exists.

                12. Descriptive values are preferred over fabricated
                    numerical values.

                13. Arrays MUST always be JSON arrays.

                14. NEVER return null for an array.

                15. Every required section must be populated with useful
                    content unless it is genuinely not applicable.

                16. Return JSON only.

                17. Do not return Markdown.

                18. Do not use code fences.

                19. Do not include explanations before or after the JSON.

                20. Do not create fields that are not defined by the required
                    JSON structure below.

                21. Do not create additional DTO fields.

                ============================================================
                MINIMUM CONTENT DEPTH
                ============================================================

                Generate multiple meaningful entries where appropriate.

                Use the following as content-depth targets:

                organization: 3-5 items

                resources: 3-5 items

                estimation: 3-5 items

                schedule: 6-10 items

                communication: 3-5 items

                configurationManagement: 3-5 items

                qualityStandards: 4-6 items

                reviews: 3-5 items

                testing: 5-8 items

                metrics: 4-6 items

                audits: 3-5 items

                productReviews: 4-6 items

                risks: 4-6 items

                mitigationStrategies: 4-6 items

                contingencyPlans: 3-5 items

                dependencies: 3-5 items

                riskManagement.assumptions: 3-5 items

                hardwareNetworking: 2-4 items

                softwareTools: 3-6 items

                manpowerCompetency: 3-5 items

                projectTeam: 4-6 items

                trainingPlan: 2-4 items

                monitoringMechanism: 4-6 items

                quantitativeMonitoring: 4-6 items

                supportItems: 3-5 items

                sizeDetails: 3-5 items

                effortDetails: 3-5 items

                criticalProcessMetrics: 4-6 items

                otherMetrics: 4-6 items

                dataCapturing: 3-5 items

                standardsApplicable: 4-6 items

                productReviewTesting: 6-10 items

                validationPlan.activities: 4-6 items

                qualityAuditPlan.audits: 3-5 items

                configurationItems: 5-8 items

                These are content-depth targets, not permission to invent
                unsupported project facts.

                If project information does not support a specific
                technology, quantity, date, budget, person or numerical
                target, use "To Be Confirmed" or another descriptive
                professional value.

                ============================================================
                CRITICAL JSON TYPE RULES
                ============================================================

                The Java backend will deserialize your response into Java
                DTO classes.

                Therefore JSON types MUST match these rules exactly.

                ============================================================
                SIMPLE STRING ARRAYS
                ============================================================

                The following fields MUST contain ONLY strings:

                projectOverview.objectives

                projectOverview.deliverables

                projectOverview.assumptions

                projectOverview.constraints

                projectOverview.acceptanceCriteria

                projectManagement.lifecyclePhases

                projectManagement.qualityObjectives

                qualityManagement.qualityObjectives

                complianceRequirements

                Example:

                "objectives": [
                  "Establish a centralized application architecture.",
                  "Improve maintainability and operational consistency.",
                  "Provide reliable and secure project delivery."
                ]

                NEVER place objects inside these arrays.

                WRONG:

                "objectives": [
                  {
                    "name": "Centralized Architecture"
                  }
                ]

                CORRECT:

                "objectives": [
                  "Establish a centralized application architecture."
                ]

                ============================================================
                PMP ITEM ARRAYS
                ============================================================

                The following fields MUST contain arrays of PmpItemDto
                objects:

                projectManagement.organization

                projectManagement.resources

                projectManagement.estimation

                projectManagement.schedule

                projectManagement.communication

                projectManagement.configurationManagement

                qualityManagement.qualityStandards

                qualityManagement.reviews

                qualityManagement.testing

                qualityManagement.metrics

                qualityManagement.audits

                qualityManagement.productReviews

                riskManagement.risks

                riskManagement.mitigationStrategies

                riskManagement.contingencyPlans

                riskManagement.dependencies

                riskManagement.assumptions

                documentControl.releaseHistory

                documentControl.circulationDetails

                documentControl.amendments

                organizationResources.hardwareNetworking

                organizationResources.softwareTools

                organizationResources.manpowerCompetency

                organizationResources.projectTeam

                organizationResources.trainingPlan

                monitoringControl.monitoringMechanism

                monitoringControl.quantitativeMonitoring

                interGroupSupport.supportItems

                estimatedSizeEffort.sizeDetails

                estimatedSizeEffort.effortDetails

                schedule.scheduleItems

                metricationPlan.criticalProcessMetrics

                metricationPlan.otherMetrics

                metricationPlan.dataCapturing

                qualityControlPlan.standardsApplicable

                qualityControlPlan.productReviewTesting

                qualityAuditPlan.audits

                configurationManagementPlan.configurationItems

                validationPlan.activities

                ============================================================
                PMP ITEM OBJECT FORMAT
                ============================================================

                Every PmpItemDto object MUST contain exactly these fields:

                {
                  "name": "...",
                  "description": "...",
                  "responsible": "...",
                  "timing": "...",
                  "target": "...",
                  "status": "..."
                }

                Do not replace these field names with other names.

                Do not return simple strings in these arrays.

                Example:

                "organization": [
                  {
                    "name": "Project Manager",
                    "description": "Coordinates project planning,
                    tracking, communication, risks and delivery activities.",
                    "responsible": "Project Manager",
                    "timing": "Entire Project",
                    "target": "Controlled Project Delivery",
                    "status": "Planned"
                  }
                ]

                ============================================================
                HOW TO USE PMP ITEM FIELDS
                ============================================================

                Because the backend uses a common six-field PmpItemDto,
                combine table-specific information into these fields.

                name
                = Main item, activity, role, tool, metric, standard,
                  configuration item or subject.

                description
                = Detailed information including additional attributes
                  such as configuration, quantity, purpose, method,
                  acceptance criteria, repository, evidence, threshold,
                  phase or other relevant information.

                responsible
                = Person or role responsible.

                timing
                = Project phase, frequency or applicable period.

                target
                = Expected outcome, threshold, objective or control target.

                status
                = Planned, Active, Approved, Controlled, In Progress,
                  Completed, To Be Confirmed or another appropriate status.

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
                - Project assumptions
                - Project constraints

                customerInterface MUST contain exactly:

                - name
                - designation
                - phoneNumber
                - faxNumber
                - email
                - skypeId

                If contact information is not supplied, use "N/A"
                rather than inventing personal contact information.

                detailedDeliverables MUST contain objects with:

                - serialNumber
                - itemDescription
                - deliveryDate
                - deliveryLocation
                - quantity
                - remarks

                If exact dates or quantities are unavailable, use
                descriptive values such as "To Be Confirmed".

                milestones MUST contain objects with:

                - phase
                - milestone
                - description
                - targetDate
                - deliverable

                ============================================================
                SECTION 2 - PROJECT GOALS AND QUALITY OBJECTIVES
                ============================================================

                Generate meaningful project goals and quality objectives.

                The Java DTO does NOT contain a separate projectGoals field.

                Therefore:

                1. Do NOT generate a projectGoals field.

                2. Represent project goals using:
                   projectOverview.objectives

                3. Represent quality objectives using:
                   projectManagement.qualityObjectives

                   AND

                   qualityManagement.qualityObjectives

                projectManagement.qualityObjectives MUST be a JSON array
                containing ONLY string values.

                qualityManagement.qualityObjectives MUST be a JSON array
                containing ONLY string values.

                Both quality objective arrays MUST always be present.

                Neither array may be null.

                Quality objectives should cover relevant areas such as:

                - functionality
                - performance
                - reliability
                - security
                - maintainability
                - usability
                - compliance
                - test quality

                Only include quality objectives relevant to the project.

                Do NOT generate projectGoals as a separate field.

                Do NOT generate processGoals as a separate field.

                Do NOT generate tailoredProcesses as a separate field.

                Do NOT generate decisionAnalysis as a separate field.

                ============================================================
                SECTION 3 - DEFINED PROCESS / PROJECT LIFE CYCLE
                ============================================================

                Define an appropriate software development lifecycle.

                lifecyclePhases MUST be populated with string values.

                Use appropriate phases such as:

                - Planning
                - Requirement Analysis
                - Design
                - Architecture
                - Development
                - Unit Testing
                - Integration / SIT
                - System Testing
                - UAT
                - Deployment
                - Project Closure

                Do not include irrelevant phases.

                The methodology must be consistent with the supplied
                implementation type and project requirements.

                Use the following supported fields only:

                - methodology
                - lifecyclePhases
                - organization
                - resources
                - estimation
                - schedule
                - communication
                - configurationManagement
                - qualityObjectives

                Do NOT create additional fields for:

                - processGoals
                - tailoredProcesses
                - decisionAnalysis
                - projectGoals

                ============================================================
                SECTION 4 - PROJECT ENVIRONMENTS
                ============================================================

                Populate all three environments:

                development

                testing

                operation

                Each environment MUST contain:

                - name
                - description
                - responsible
                - timing
                - target
                - status

                Put hardware, software and configuration details inside
                description.

                Do NOT leave development, testing or operation empty.

                Do not invent specific infrastructure technologies.

                If infrastructure details are unavailable, use descriptive
                values such as:

                "Project-defined development environment"

                "Project-defined test environment"

                "Production environment as approved for deployment"

                ============================================================
                SECTION 5 - PROJECT MANAGEMENT ISSUES
                ============================================================

                Generate project-specific:

                - dependencies
                - assumptions
                - risks
                - mitigation strategies
                - contingency plans

                Each risk should describe:

                - risk
                - impact
                - probability
                - mitigation
                - contingency

                Because the DTO uses six fields, include these details
                inside description where necessary.

                Generate multiple realistic risks where applicable.

                Examples of generic risk areas that may be considered
                ONLY when relevant:

                - requirement changes
                - integration dependency
                - resource availability
                - schedule dependency
                - quality defects
                - security concerns
                - performance concerns
                - client approval delays
                - environment availability

                Do not introduce risks unrelated to the supplied project.

                ============================================================
                SECTION 6 - ORGANIZATION AND RESOURCES
                ============================================================

                Populate all five subsections:

                6.1 Hardware and Networking

                6.2 Software and Tools

                6.3 Manpower and Competency

                6.4 Project Team

                6.5 Training Plan

                Do NOT leave these arrays empty.

                HARDWARE AND NETWORKING:

                Use name for the hardware/network item.

                Put configuration, quantity and purpose in description.

                Put responsibility in responsible.

                Put required phase in timing.

                Put availability objective in target.

                Do not invent exact hardware specifications.

                SOFTWARE AND TOOLS:

                Use name for software/tool.

                Put version, purpose and license information in description
                ONLY when such information is known.

                Do not invent software tools.

                If a tool is not specified, use a generic project-related
                description rather than inventing a product name.

                MANPOWER:

                Use name for role.

                Put competency, experience and quantity in description.

                Do not invent exact resource quantities.

                PROJECT TEAM:

                Use name for role/person/team position.

                Put designation, responsibility and allocation in description.

                Do not invent personal names unless supplied.

                TRAINING:

                Use name for training activity.

                Put audience, trainer and objective in description.

                ============================================================
                SECTION 7 - PROJECT MONITORING AND CONTROL
                ============================================================

                Populate:

                monitoringMechanism

                quantitativeMonitoring

                Do NOT leave either array empty.

                Monitoring mechanisms should cover relevant controls such as:

                - project progress tracking
                - requirements tracking
                - defect tracking
                - issue tracking
                - risk monitoring
                - change monitoring
                - quality reviews
                - milestone tracking

                Put frequency, measurement and corrective action in
                description.

                Quantitative monitoring should include relevant parameters
                such as:

                - schedule progress
                - defect levels
                - test execution
                - requirement completion
                - quality indicators
                - performance indicators

                Do not fabricate unsupported numerical thresholds.

                Use descriptive targets where necessary.

                ============================================================
                SECTION 8 - INTER-GROUP SUPPORT
                ============================================================

                Populate supportItems.

                Include realistic collaboration between groups such as:

                - project management
                - development
                - testing / QA
                - infrastructure / deployment
                - client / business stakeholders

                Only include groups relevant to the project.

                Put supporting group, supported group and support required
                into name and description.

                ============================================================
                SECTION 9 - ESTIMATED SIZE AND EFFORT
                ============================================================

                Use the supplied estimation information and use cases.

                Populate both:

                sizeDetails

                effortDetails

                Do NOT leave these arrays empty when estimation/use-case
                information exists.

                For sizeDetails:

                name = size measure or estimation area

                description = estimation method, estimated size, unit
                and basis.

                For effortDetails:

                name = activity or effort area

                description = role, effort, unit and basis.

                Existing use cases should influence the estimation.

                If precise numerical values are unavailable, use
                descriptive statements rather than fabricated numbers.

                Do not invent effort hours, person-days, costs or duration.

                ============================================================
                SECTION 10 - PROJECT SCHEDULE
                ============================================================

                Populate schedule.scheduleItems.

                Do NOT leave the schedule empty.

                Generate a logical sequence of project activities.

                Use phases such as:

                - Planning
                - Requirement Analysis
                - Design
                - Architecture
                - Development
                - Unit Testing
                - Integration / SIT
                - System Testing
                - UAT
                - Deployment
                - Closure

                Use name for the activity.

                Put phase, duration and date information in description.

                Put responsible role in responsible.

                Put project phase/timing in timing.

                Put expected outcome in target.

                Put status in status.

                If exact calendar dates are unavailable, do NOT invent them.

                Use "To Be Confirmed" or "Based on approved project schedule"
                where appropriate.

                ============================================================
                SECTION 11 - METRICATION PLAN
                ============================================================

                Populate:

                criticalProcessMetrics

                otherMetrics

                dataCapturing

                Do NOT leave these arrays empty.

                Critical process metrics may include:

                - requirements completion
                - schedule adherence
                - defect detection
                - defect resolution
                - review completion
                - test progress
                - change request tracking

                Other metrics may include:

                - productivity
                - quality
                - performance
                - reliability
                - maintainability
                - customer feedback

                Data capturing should describe:

                - data item
                - source
                - collection method
                - frequency
                - owner
                - storage

                Put additional metric attributes into description.

                Do not fabricate numerical thresholds.

                ============================================================
                SECTION 12 - QUALITY CONTROL PLAN
                ============================================================

                Populate:

                standardsApplicable

                productReviewTesting

                Do NOT leave these arrays empty.

                Standards may include relevant project standards such as:

                - coding standards
                - architecture standards
                - security practices
                - accessibility requirements
                - documentation standards
                - testing standards

                Only include standards relevant to the supplied project.

                Product review/testing may include:

                - requirement review
                - SRS/FS review
                - architecture review
                - design review
                - code review
                - test plan review
                - test case review
                - unit testing
                - integration testing
                - system testing
                - UAT
                - regression testing
                - performance testing

                Only include activities appropriate to the project.

                ============================================================
                SECTION 13 - VALIDATION PLAN
                ============================================================

                Populate:

                validationPlan.name

                validationPlan.description

                validationPlan.responsible

                validationPlan.timing

                validationPlan.target

                validationPlan.status

                validationPlan.activities

                Do NOT leave activities empty.

                Validation activities MUST use PmpItemDto objects.

                Use name for the validation activity.

                Put acceptance criteria, method and evidence in description.

                Do not fabricate specific acceptance percentages or
                numerical thresholds.

                ============================================================
                SECTION 14 - QUALITY AUDIT PLAN
                ============================================================

                Populate qualityAuditPlan.audits.

                Do NOT leave this array empty.

                Include appropriate audits such as:

                - process compliance audit
                - documentation audit
                - configuration audit
                - quality management audit
                - release readiness audit

                Only include relevant audits.

                Put audit type, covered activities, periodicity and remarks
                into the six PmpItemDto fields.

                Do not invent an audit frequency unless supported.
                "As applicable" or "Project-defined" may be used.

                ============================================================
                SECTION 15 - CONFIGURATION MANAGEMENT PLAN
                ============================================================

                Populate:

                configurationItems

                baselining

                releaseProcedure

                versionControl

                statusAccounting

                audit

                backup

                configurationItems MUST NOT be empty.

                Configuration items may include:

                - source code
                - requirements
                - architecture/design documents
                - database scripts
                - test artifacts
                - deployment configuration
                - user documentation

                Put type, repository, owner, baseline and remarks inside
                description.

                Only mention a specific repository or version-control
                platform if it is supplied in the project information.

                The following scalar fields must contain meaningful
                project-specific descriptions:

                baselining

                releaseProcedure

                versionControl

                statusAccounting

                audit

                backup

                Do not invent specific backup frequencies or tools.

                ============================================================
                OUTPUT JSON STRUCTURE
                ============================================================

                Return exactly ONE JSON object.

                The root object MUST contain ONLY:

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
                      "objectives": [],
                      "deliverables": [],
                      "assumptions": [],
                      "constraints": [],
                      "acceptanceCriteria": [],
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
                      "organization": [],
                      "resources": [],
                      "estimation": [],
                      "schedule": [],
                      "communication": [],
                      "configurationManagement": [],
                      "qualityObjectives": []
                    },

                    "qualityManagement": {
                      "qualityStandards": [],
                      "reviews": [],
                      "testing": [],
                      "metrics": [],
                      "qualityObjectives": [],
                      "audits": [],
                      "productReviews": []
                    },

                    "riskManagement": {
                      "risks": [],
                      "mitigationStrategies": [],
                      "contingencyPlans": [],
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

                Before returning the JSON, verify ALL of the following:

                1. The response is valid JSON.

                2. The root contains ONLY "pmp".

                3. All required PMP sections are present.

                4. All arrays are JSON arrays.

                5. NO array is null.

                6. projectManagement.qualityObjectives is present.

                7. projectManagement.qualityObjectives contains ONLY strings.

                8. qualityManagement.qualityObjectives is present.

                9. qualityManagement.qualityObjectives contains ONLY strings.

                10. No Markdown exists.

                11. No code fences exist.

                12. No explanatory text exists outside the JSON.

                13. The PMP is internally consistent.

                14. Content is specific to the supplied project.

                15. Unsupported functionality is not invented.

                16. Unsupported technologies are not invented.

                17. Unsupported exact dates and numbers are not fabricated.

                18. Every major section contains useful project-specific
                    content.

                19. Do not leave arrays empty merely because an exact
                    numerical value is unavailable.

                20. Use descriptive professional values instead.

                21. Do not create fields outside the specified JSON shape.

                22. Every PmpItemDto object contains exactly six fields.

                23. No PmpItemDto array contains strings instead of objects.

                24. No simple string array contains objects.

                25. Do not generate projectGoals.

                26. Do not generate processGoals.

                27. Do not generate tailoredProcesses.

                28. Do not generate decisionAnalysis.

                ============================================================
                FINAL TYPE CHECK
                ============================================================

                The following fields MUST contain STRING elements:

                projectOverview.objectives

                projectOverview.deliverables

                projectOverview.assumptions

                projectOverview.constraints

                projectOverview.acceptanceCriteria

                projectOverview.complianceRequirements

                projectManagement.lifecyclePhases

                projectManagement.qualityObjectives

                qualityManagement.qualityObjectives

                IMPORTANT:

                projectManagement.qualityObjectives MUST contain ONLY
                STRING VALUES.

                Example:

                "qualityObjectives": [
                  "Ensure functional requirements are implemented correctly.",
                  "Maintain acceptable application performance.",
                  "Ensure security and reliability requirements are satisfied.",
                  "Ensure compliance with applicable project standards."
                ]

                NEVER return objects inside
                projectManagement.qualityObjectives.

                NEVER return null for
                projectManagement.qualityObjectives.

                IMPORTANT:

                qualityManagement.qualityObjectives MUST contain ONLY
                STRING VALUES.

                Example:

                "qualityObjectives": [
                  "Ensure functional requirements are implemented correctly.",
                  "Maintain acceptable application performance.",
                  "Ensure security and reliability requirements are satisfied.",
                  "Ensure compliance with applicable project standards."
                ]

                NEVER return objects inside
                qualityManagement.qualityObjectives.

                NEVER return null for
                qualityManagement.qualityObjectives.

                The following fields MUST contain PmpItemDto objects:

                projectManagement.organization

                projectManagement.resources

                projectManagement.estimation

                projectManagement.schedule

                projectManagement.communication

                projectManagement.configurationManagement

                qualityManagement.qualityStandards

                qualityManagement.reviews

                qualityManagement.testing

                qualityManagement.metrics

                qualityManagement.audits

                qualityManagement.productReviews

                riskManagement.risks

                riskManagement.mitigationStrategies

                riskManagement.contingencyPlans

                riskManagement.dependencies

                riskManagement.assumptions

                documentControl.releaseHistory

                documentControl.circulationDetails

                documentControl.amendments

                validationPlan.activities

                organizationResources.hardwareNetworking

                organizationResources.softwareTools

                organizationResources.manpowerCompetency

                organizationResources.projectTeam

                organizationResources.trainingPlan

                monitoringControl.monitoringMechanism

                monitoringControl.quantitativeMonitoring

                interGroupSupport.supportItems

                estimatedSizeEffort.sizeDetails

                estimatedSizeEffort.effortDetails

                schedule.scheduleItems

                metricationPlan.criticalProcessMetrics

                metricationPlan.otherMetrics

                metricationPlan.dataCapturing

                qualityControlPlan.standardsApplicable

                qualityControlPlan.productReviewTesting

                qualityAuditPlan.audits

                configurationManagementPlan.configurationItems

                Every PmpItemDto object MUST contain exactly:

                name

                description

                responsible

                timing

                target

                status

                Do not change these JSON types.

                Do not add additional fields.

                ============================================================
                FINAL OUTPUT REQUIREMENT
                ============================================================

                Return ONLY valid JSON.

                Do not return:

                - Markdown
                - code fences
                - comments
                - explanations
                - introductory text
                - trailing text

                The first character of the response must be '{'.

                The last character of the response must be '}'.

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