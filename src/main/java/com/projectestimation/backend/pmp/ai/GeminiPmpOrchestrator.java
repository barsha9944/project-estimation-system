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

                STRUCTURE RULES:

                1. Use the exact section and subsection structure listed below.
                2. Keep every section and subsection in the specified order.
                3. Do not create additional sections or subsections.
                4. Do not duplicate any section or subsection.
                5. Do not rename any heading.
                6. Methodology must appear only under 3.2 Methodology.
                7. Schedule must appear only under 10.0 Schedule.
                8. Section 3.6 is Schedule Management and must not become another Schedule section.
                9. Every listed section must have useful project-specific content through the
                   fields available in the required JSON structure.
                10. Do not create JSON fields solely to represent headings that do not exist
                    in the DTO. Use the closest existing DTO field as instructed below.
                11. Keep the amount of detail professional and moderate, similar to the
                    original BEAS PMP template; do not produce unnecessarily long prose.

                ============================================================
                MANDATORY PMP SECTIONS
                ============================================================

                The PMP must contain ALL of the following sections and subsections,
                in exactly this order. Do not add, remove, merge, rename or duplicate them.

                A. Document Release History
                B. Circulation Details
                C. List of Amendments Made on the Previous Version No.:

                1.0 Introduction
                1.1 Project Overview
                1.2 Customer Interface
                1.3 Scope of Work
                1.4 Project Compliance Requirements
                1.5 Project Deliverables to Customer
                1.6 List of Milestones
                1.7 Acceptance Criteria

                2.0 Project Goals and Quality Objectives
                2.1 Project Objectives
                2.2 Project Quality Objectives
                2.3 Quality Management Objectives

                3.0 The Project's Defined Process
                3.1 Project Life Cycle Phases
                3.2 Methodology
                3.3 Organization
                3.4 Resources
                3.5 Estimation
                3.6 Schedule Management
                3.7 Communication
                3.8 Configuration Management

                4.0 Project Environments
                4.1 Development Environment
                4.2 Testing Environment
                4.3 Operational Environment

                5.0 Project Management Issues
                5.1 Project Risks
                5.2 Risk Mitigation Strategies
                5.3 Contingency Plans
                5.4 Dependencies
                5.5 Assumptions

                6.0 Organization and Resources
                6.1 Hardware and Networking
                6.2 Software and Tools
                6.3 Manpower and Competency
                6.4 Project Team
                6.5 Training Plan

                7.0 Project Monitoring & Control Mechanism
                7.1 Project Monitoring & Control Mechanism
                7.2 Quantitative Project Monitoring

                8.0 Requirement of Inter Group Support & Co-ordination

                9.0 Estimated Size & Effort
                9.1 Estimated Size Details of the Project
                9.2 Estimated Effort Details of the Project

                10.0 Schedule

                11.0 Metrication Plan
                11.1 Metrication Plan Measurements of Critical Processes / Sub-processes
                11.2 Other Metrics for the Project and Corresponding Goals
                11.3 Metrics Data Capturing

                12.0 Quality Control Plan
                12.1 Standards Applicable
                12.2 Product Review & Testing
                12.3 Quality Standards
                12.4 Reviews
                12.5 Testing
                12.6 Quality Metrics
                12.7 Quality Audits
                12.8 Product Reviews

                13.0 Validation Plan
                13.1 Validation Activities

                14.0 Quality Audit Plan

                15.0 Configuration Management Plan
                15.1 List of Configuration Items (CI)
                15.2 Procedure for Baselining a CI
                15.3 Release Procedure
                15.4 Version Control & Nomenclature
                15.5 CI Status Accounting & Reporting
                15.6 Configuration Management Audit
                15.7 Back-up Plan

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

                Generate a separate project introduction in:
                projectOverview.introduction

                The Introduction must describe the ACTUAL PROJECT and must
                be based strictly on the provided Requirement Summary,
                Project Name, Components and Existing Use Cases.

                The Introduction should explain:
                - What the project is.
                - Why the project is being developed.
                - The main business or operational requirement/problem being addressed.
                - The proposed solution and its purpose.
                - The major capabilities or functional areas supported by the project.

                Do NOT describe what a Project Management Plan is.
                Do NOT write a generic PMP introduction.
                Do NOT duplicate the Project Scope.

                Keep the Introduction moderately detailed, approximately
                2-3 well-developed paragraphs.

                The value of projectOverview.introduction MUST NOT be null
                or empty.

                Store the Introduction ONLY in:
                projectOverview.introduction

                Generate:

                - Project overview
                - Customer/client interface
                - Scope of work
                - Compliance requirements

                PROJECT SCOPE - MODERATE DETAIL REQUIREMENT

                Generate a clear and moderately detailed Project Scope based
                strictly on the provided project requirements, project
                description, components, use cases and other available project
                information.

                The scope should explain:
                - The overall solution and its purpose.
                - The major modules or functional areas included in the project.
                - The key functionality and activities covered under those areas.
                - Application development, integration, database and testing
                  activities where applicable.
                - Major project deliverables included within the scope.
                - Important assumptions, dependencies or exclusions where
                  supported by the requirements.

                The scope must be specific to the current project and written
                in professional Project Management Plan language.

                Do not generate a generic scope.
                Do not invent modules, functionality, integrations, technologies,
                deliverables, dates, costs or numerical values that are not
                supported by the provided information.

                Keep the scope concise but sufficiently detailed to clearly
                communicate what is included in the project. Normally generate
                approximately 3-5 well-developed paragraphs rather than a very
                long multi-section explanation.

                Put the complete scope into the existing
                projectOverview.projectScope STRING field.

                    SECTION 2.1 - PROJECT OBJECTIVES / BUSINESS OBJECTIVE TABLE
                ============================================================

                Section 2.1 MUST be generated as a structured table based on
                the approved Development and Maintenance Business Objective
                reference data provided below.

                The final PMP table MUST contain exactly these SIX columns,
                in exactly this order:

                1. Serial No.
                2. Business Objective
                3. Project Objective
                4. Metrics Identified
                5. Organizational Goals
                6. Project's Goals

                The final table structure is:

                Serial No. | Business Objective |
                Metrics Identified | Organizational Goals | Project's Goals

                IMPORTANT:
                - Business Objective MUST come from the approved master data below.
                - Project Objective MUST be generated by Gemini specifically for
                  the CURRENT PROJECT.
                - Metrics Identified MUST contain the MAIN metric from the approved
                  Related Metrics value for the selected Business Objective.
                - Organizational Goals MUST come from the approved QPPO -
                  Goal/KPI value for the selected Business Objective.
                - Project's Goals MUST be generated specifically for the CURRENT
                  PROJECT from the selected Business Objective and its approved
                  Related Metrics. The project goal must explain what the project
                  aims to achieve for that metric and must not introduce a different
                  metric or unrelated goal.
                - Description, Procedure in brief, Tracking & monitoring interval
                  and Guiding Note are reference information used for objective
                  selection and project-objective generation. They are NOT output
                  columns in the final PMP table.

                ------------------------------------------------------------
                APPROVED DEVELOPMENT BUSINESS OBJECTIVES
                ------------------------------------------------------------

                Development Business Objective 1

                Serial No.: 1
                Business Objective: Fulfil Delivery Commitments
                Description: Meeting delivery commitments is a major key to success
                in enhancing business and customer loyalty. So, that should be a priority.
                Related Metrics: Schedule Variance - Revised (in %%)
        		QPPO - Goal/KPI: USL: 5%%; LSL: -5%%

                Development Business Objective 2

                Serial No.: 1
                Business Objective: Manage projects within budget
                Description: As the project cost may depend on various factors
                including and other than cost of effort it is essential that
                the project cost is contained within the approved estimated effort.
                Procedure in brief: Effort use should be optimized so that project
                could be delivered within schedule and within budget.
                Related Metrics: Cycle Time measures person-hour needed to produce
                1 unit size (UCP) [decrease in value is positive]
                QPPO - Goal/KPI: Mean 14/ SD 1
                Tracking & monitoring interval: At every significant delivery milestone
                Guiding Note: For Development, Enhancement, Migration etc type of job

                Development Business Objective 3

                Serial No.: 2
                Business Objective: Ensure quality of products delivered to customer -
                for software development projects
                Description: Ensure that defects in products delivered to customer
                for their perusal (including UAT) are always at a minimum level.
                That is another major key to achieving customer satisfaction,
                customer loyalty, and building company goodwill.
                Procedure in brief: Defect Removal Efficiency measures ratio between
                all the review and testing defects found by us and
                (all the review and testing defects found by us + UAT defects)
                Related Metrics: DRE [increase in value is positive]
                QPPO - Goal/KPI: Mean: 0.9/ SD 0.1
                Tracking & monitoring interval: After UAT of every release
                Guiding Note: For Development, Enhancement etc type of job

                Development Business Objective 4

                Serial No.: 4
                Business Objective: Improve quality of software engineering while the products are being made ready for delivery
                Description: Productivity and cost in a software project would always depend on the quality of engineering work being done. So, that should be closely monitored.
                Related Metrics: Average Pre-delivery Defect Rate (No. of defects per Person Hour)
                QPPO - Goal/KPI: USL: 1 defects per Person hour; LSL: 0 defects per Person hour

                Development Business Objective 5

                Serial No.: 3
                Business Objective: Continually Improve productivity
                Description: Improving the productivity of the project team is key
                to minimizing project cost and the time to market. So, major focus
                should be on maximising productivity.
                Procedure in brief: productivity should be measured in terms of
                software size delivered Vs. effort spent in software engineering.
                Related Metrics: productivity (UCP per Person-Hour)
                [increase in value is positive]
                QPPO - Goal/KPI: productivity (UCP per Person-Hour) target mean
                0.07 UCP/p-h and 0.01 SD for the year 23-24
                Tracking & monitoring interval: After UAT of every product release -
                or completion of each Use-Case
                Guiding Note: For Development, Enhancement etc type of job

                ------------------------------------------------------------
                APPROVED MAINTENANCE BUSINESS OBJECTIVES
                ------------------------------------------------------------

                Maintenance Business Objective 1

                Serial No.: 1
                Business Objective: Continually Improve productivity
                Description: Improving the productivity of the project team is key
                to efficiently close a ticket within timeline.
                Procedure in brief: Productivity in maintenance is measured hours
                needed to close a ticket.
                Related Metrics: Productivity (in hours per ticket)
                [decrease in value is positive]
                QPPO - Goal/KPI: target of 8 hr mean and SD 2
                Tracking & monitoring interval: End of every month / Every week
                Guiding Note: For Maintenance project

                Maintenance Business Objective 2

                Serial No.: 1
                Business Objective: Ensure quality of mnt. service delivered to
                customer - for software maintenance projects
                Description: Ensure that defects in maintenance service delivered
                to customer for their perusal (including UAT) are minimum to
                achieve customer satisfaction.
                Procedure in brief: Weighted Defect per Ticket is measured adding
                all the defects per tickets and multiplying them with certain value.
                Review defects value 0.5, testing defect value 1 and UAT value 2.
                Related Metrics: WDT [decrease in value is positive]
                QPPO - Goal/KPI: target mean 2 and SD 1
                Tracking & monitoring interval: End of every month / every week
                Guiding Note: For Maintenance project

                ============================================================
                PROJECT TYPE CLASSIFICATION AND BUSINESS OBJECTIVE SELECTION
                ============================================================

                Determine the CURRENT PROJECT TYPE from the supplied project
                information.

                Use Requirement Summary and Project Scope as the PRIMARY evidence,
                supported by Project Description, Components, Existing Use Cases,
                Deliverables, Implementation Type and other supplied project context.

                Classify the project as exactly one of:

                - DEVELOPMENT
                - MAINTENANCE
                - BOTH

                Do NOT classify the project from an isolated keyword. Consider
                the overall nature of the work and the actual activities described.

                DEVELOPMENT:
                Evaluate ONLY the five approved Development Business Objectives.
                Select ONLY the Development objectives that are genuinely relevant
                to the CURRENT PROJECT.

                MAINTENANCE:
                Evaluate ONLY the two approved Maintenance Business Objectives.
                Select ONLY the Maintenance objectives that are genuinely relevant
                to the CURRENT PROJECT.

                BOTH:
                Evaluate the Development and Maintenance Business Objective lists
                independently. Select only the genuinely relevant objectives from
                either list.

                It is NOT mandatory to select all Business Objectives.
                A project may have one or more applicable objectives from the
                relevant list(s). Do not force irrelevant objectives into the PMP
                merely to fill the table.

                Do NOT invent a new Business Objective.
                Do NOT rename an approved Business Objective.
                Do NOT modify the approved Business Objective text.
                Do NOT modify the approved Related Metrics or QPPO - Goal/KPI
                reference values.

                Description, Procedure in brief, Tracking & monitoring interval
                and Guiding Note are reference data used to judge applicability.
                They are NOT separate output fields in
                projectOverview.businessObjectives.

                ============================================================
                BUSINESS OBJECTIVE OUTPUT MAPPING
                ============================================================

                For EACH selected Business Objective, generate exactly one object
                in projectOverview.businessObjectives.

                Every object MUST contain EXACTLY these six fields:

                {
                  "serialNumber": 1,
                  "businessObjective": "...",
                  "metricIdentified": "...",
                  "organizationalGoal": "...",
                  "projectGoal": "..."
                }

                Do NOT add fields.
                Do NOT remove fields.
                Do NOT use PmpItemDto format for this array.
                Do NOT use simple strings.

                Field rules:

                serialNumber
                = the approved Serial No. from the selected master Business
                  Objective. Preserve that value exactly.

                businessObjective
                = the approved Business Objective text exactly as supplied in the
                  selected Development or Maintenance master data.

                projectObjective
                = a project-specific objective generated by Gemini for the
                  CURRENT PROJECT. It must explain how this specific project
                  supports the selected Business Objective.

                metricIdentified
                = the MAIN metric identified in the approved Related Metrics value
                  for the selected Business Objective. Preserve the metric terminology.
                  Do not replace it with a different metric.

                organizationalGoal
                = the approved QPPO - Goal/KPI value for the selected Business
                  Objective. Preserve the supplied goal/KPI information exactly.

                projectGoal
                = a project-specific goal derived from the selected Business Objective
                  and its approved Related Metrics, using the CURRENT PROJECT context.
                  It must explain the project's goal for managing or improving the
                  selected metric. Do not invent a different metric or unrelated goal.

                The following reference fields MUST NOT be emitted inside
                projectOverview.businessObjectives:

                - description
                - procedureInBrief
                - procedure in brief
                - trackingAndMonitoringInterval
                - guidingNote
                - relatedMetrics
                - qppoGoalKpi

                The projectGoal may be generated from the selected Related Metrics
                and the actual project context because it is the project-specific goal
                for that metric. It must remain consistent with the selected master
                Business Objective and must not introduce unsupported functionality.

                ============================================================
                PROJECT OBJECTIVE GENERATION RULE
                ============================================================

                For EACH selected Business Objective, generate one
                project-specific Project Objective.

                Generate it using:

                Business Objective
                + Description
                + Procedure in brief
                + Related Metrics
                + QPPO - Goal/KPI
                + Project Name
                + Requirement Summary
                + Project Scope
                + Components
                + Implementation Type
                + Priority
                + Existing Use Cases

                The Project Objective MUST:
                - be specific to the CURRENT PROJECT,
                - explain how the project will support the selected Business
                  Objective,
                - be meaningfully different from simply repeating the Business
                  Objective,
                - not copy the Description verbatim,
                - not copy Procedure in brief verbatim,
                - not introduce unsupported functionality.

                ============================================================
                SECTION 2.2 - PROJECT GOALS AS SET
                ============================================================

                Generate project goals based on the CURRENT PROJECT information
                and the selected Business Objectives.

                projectOverview.objectives remains a simple string array and must
                contain project-goal statements only.

                Do not put Business Objective objects inside
                projectOverview.objectives.

                The Business Objective table's projectGoal values and
                projectOverview.objectives must remain consistent with the
                supplied project context.

                ============================================================
                SECTION 11.1 CONSISTENCY
                ============================================================

                Section 11.1 MUST use the SAME SELECTED Business Objectives from
                projectOverview.businessObjectives.

                Do NOT select a different set of Business Objectives in Section 11.1.

                For each selected Business Objective, maintain consistency for:
                Business Objective
                -> Metric Identified
                -> Organizational Goal
                -> Project Goal

                ============================================================

                SECTION 2 REFERENCE FILE

                ============================================================

                An approved Excel reference file named "project objective.xlsx"
                is attached to this Gemini request through the Gemini Files API.

                Use the attached Excel file as the authoritative REFERENCE/TEMPLATE
                for Section 2 - Project Objectives & Goals.

                Use it to understand and preserve:

                - Business Objective structure
                - Project Objective structure
                - Description and procedure pattern
                - Related Metrics
                - QPPO / Goal / KPI structure
                - Tracking and monitoring interval
                - Guiding Note pattern
                - terminology
                - level of detail

                IMPORTANT:

                The Excel file is provided as reference data, not as current
                project-specific data. Use the approved values for the selected
                Business Objectives, metrics and organizational goals. Generate
                the Project Objective specifically for the CURRENT PROJECT.

                Do not copy unrelated project-specific values from the reference.

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

                IMPORTANT SCHEDULE DATA RULE:

                Do not invent or guess Schedule Target or Status values.

                The application will populate the final Schedule Target and Status
                from the actual Work Schedule after AI generation.

                Therefore:
                - target must not be treated as a source of authoritative schedule data.
                - status must not be treated as a source of authoritative schedule data.
                - Do not invent completion states, dates or target outcomes that conflict
                  with the Work Schedule.

                If exact calendar dates are unavailable, do NOT invent them.

                Use "To Be Confirmed" or "Based on approved project schedule"
                where appropriate for non-authoritative schedule information.

                ============================================================
                SECTION 11 - METRICATION PLAN
                ============================================================

                The Metrication Plan MUST follow this exact structure:

                11.0 Metrication Plan
                11.1 Project Goals / Organization Goals
                11.2 Goals for Critical Processes / Sub-processes
                11.3 Other Metrics for the Project and Corresponding Goals
                11.4 Metrics Data Capturing

                Do NOT merge these four parts.
                Do NOT use PmpItemDto for these metrication records.

                11.1 PROJECT GOALS / ORGANIZATION GOALS

                Populate metricationPlan.projectGoals with 3-5 meaningful
                project-specific mappings. Each object MUST contain exactly:

                {
                  "serialNumber": 1,
                  "businessObjective": "...",
                  "metricIdentified": "...",
                  "organizationalGoal": "...",
                  "projectGoal": "...",
                  "respectivePpm": "..."
                }

                serialNumber MUST be an integer (1, 2, 3...).
                businessObjective = relevant business objective.
                metricIdentified = metric used to measure it.
                organizationalGoal = relevant organization-level goal.
                projectGoal = goal established for this project.
                respectivePpm = Project Performance Measure / monitoring method.

                11.2 GOALS FOR CRITICAL PROCESSES / SUB-PROCESSES

                Populate metricationPlan.criticalProcessMetrics with 4-6 entries.
                Each object MUST contain exactly:

                {
                  "serialNumber": 1,
                  "projectGoal": "...",
                  "relevantCriticalProcesses": "...",
                  "metricsForCriticalProcess": "...",
                  "upperSpecificationLimit": "...",
                  "mean": "...",
                  "lowerSpecificationLimit": "...",
                  "periodicityOfAnalysisAndReview": "..."
                }

                The three specification fields represent USL, Mean and LSL.
                Do not invent unsupported numerical thresholds; use
                "To Be Confirmed" when an exact value is unavailable.

                11.3 OTHER METRICS FOR THE PROJECT AND CORRESPONDING GOALS

                Populate metricationPlan.otherMetrics with 4-6 entries.
                Each object MUST contain exactly:

                {
                  "serialNumber": 1,
                  "metricName": "...",
                  "organizationalGoal": "...",
                  "projectGoal": "...",
                  "periodicityOfAnalysisAndReview": "...",
                  "reasonsForDeviation": "..."
                }

                11.4 METRICS DATA CAPTURING

                Populate metricationPlan.dataCapturing with 5-8 entries.
                Each object MUST contain exactly:

                {
                  "serialNumber": 1,
                  "inputData": "...",
                  "source": "...",
                  "whenToCapture": "...",
                  "remarks": "..."
                }

                Use project-supported sources such as approved schedules,
                requirements, defect records, test records and review records
                where applicable. Do not invent specific tools or repositories.

                All four metrication arrays MUST be present and MUST NOT be null.
                Keep the content project-specific and consistent with the supplied
                requirements and existing use cases.

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
                      "introduction": "",
                      "projectScope": "",
                      "businessObjectives": [],
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
                      "projectGoals": [],
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

                21. projectOverview.introduction MUST be present.

                22. projectOverview.introduction MUST be a non-empty STRING.

                23. Do not create fields outside the specified JSON shape.

                22. Every PmpItemDto object contains exactly six fields.

                23. No PmpItemDto array contains strings instead of objects.

                24. No simple string array contains objects.

                25. Do not generate projectGoals.

                26. Do not generate processGoals.

                27. Do not generate tailoredProcesses.

                28. Do not generate decisionAnalysis.

                29. All required PMP headings and subsections are represented by the
                    existing DTO structure and must appear exactly once in the final document.

                30. Do not create a second Methodology section.

                31. Do not create a second Schedule section.

                32. Section 10 Schedule Target and Status must ultimately be taken from
                    the application's Work Schedule data, not invented by the AI.

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

                METRICATION PLAN TYPE CHECK

                metricationPlan.projectGoals MUST contain objects with exactly:
                serialNumber, businessObjective, metricIdentified, organizationalGoal,
                projectGoal, respectivePpm.

                metricationPlan.criticalProcessMetrics MUST contain objects with exactly:
                serialNumber, projectGoal, relevantCriticalProcesses, metricsForCriticalProcess,
                upperSpecificationLimit, mean, lowerSpecificationLimit,
                periodicityOfAnalysisAndReview.

                metricationPlan.otherMetrics MUST contain objects with exactly:
                serialNumber, metricName, organizationalGoal, projectGoal,
                periodicityOfAnalysisAndReview, reasonsForDeviation.

                metricationPlan.dataCapturing MUST contain objects with exactly:
                serialNumber, inputData, source, whenToCapture, remarks.

                serialNumber MUST be an INTEGER in all metrication objects.
                Do not return strings or PmpItemDto objects in metrication arrays.
                Do not return null for any metrication array.

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
                
                ------------------------------------------------------------
                DETAILED DELIVERABLES AND MILESTONES
                ------------------------------------------------------------

                Do not return empty arrays for detailedDeliverables or
                milestones when the project information is sufficient to
                derive them.

                ============================================================
                PROJECT DELIVERABLES TO CUSTOMER
                ============================================================

                Populate projectOverview.detailedDeliverables with meaningful
                project-specific customer deliverables.

                Every detailedDeliverables object MUST contain EXACTLY these
                six fields, matching DeliverableDto:

                {
                  "serialNumber": 1,
                  "itemDescription": "...",
                  "deliveryDate": "...",
                  "deliveryLocation": "...",
                  "quantity": "...",
                  "remarks": "..."
                }

                Rules:

                - serialNumber MUST be an integer and sequential: 1, 2, 3...
                - itemDescription MUST describe an actual customer-facing
                  deliverable relevant to the current project.
                - deliveryDate MUST use a supported project date when one is
                  available; otherwise use "To Be Confirmed".
                - deliveryLocation MUST use a project-supported location when
                  known; otherwise use "To Be Confirmed".
                - quantity MUST use a meaningful supported quantity; use "1"
                  when the deliverable is a single project artifact/system.
                - remarks MUST explain what is included in the deliverable.
                - Do NOT use null values.
                - Do NOT use empty strings.
                - Do NOT add fields.
                - Do NOT rename fields.
                - Do NOT use PmpItemDto for detailedDeliverables.
                - Do NOT invent unsupported exact dates or quantities.
                - Use "To Be Confirmed" when an exact value is unavailable.

                Generate 4-8 meaningful deliverables when the supplied project
                information supports that level of detail. Do not create
                artificial deliverables merely to reach a count.

                ============================================================
                PROJECT MILESTONES
                ============================================================

                Populate projectOverview.milestones with the major project
                milestones supported by the supplied project information.

                Every milestone object MUST contain EXACTLY these five fields,
                matching MilestoneDto:

                {
                  "phase": "...",
                  "milestone": "...",
                  "description": "...",
                  "targetDate": "...",
                  "deliverable": "..."
                }

                Rules:

                - phase MUST identify the relevant project phase.
                - milestone MUST identify the milestone.
                - description MUST explain the milestone.
                - targetDate MUST use a supported project date when available;
                  otherwise use "To Be Confirmed".
                - deliverable MUST identify the corresponding deliverable or
                  outcome.
                - Do NOT add serialNumber.
                - Do NOT add acceptanceCriteria.
                - Do NOT add any other fields.
                - Do NOT use PmpItemDto for milestones.
                - Do NOT invent unsupported dates.
                - Do NOT use null or empty values.

                ============================================================
                FINAL STRUCTURAL VALIDATION
                ============================================================

                Before returning the final JSON, validate every
                projectOverview.businessObjectives object. Each selected object
                must contain non-null, non-empty values for:

                serialNumber, businessObjective, projectObjective,
                metricIdentified, organizationalGoal and projectGoal.

                Also validate every projectOverview.detailedDeliverables object
                against DeliverableDto exactly:

                serialNumber, itemDescription, deliveryDate, deliveryLocation,
                quantity and remarks.

                Also validate every projectOverview.milestones object against
                MilestoneDto exactly:

                phase, milestone, description, targetDate and deliverable.

                Validate that all metricationPlan arrays use their dedicated
                custom DTO structures and are not returned as PmpItemDto objects.

                Do not add fields that are not represented by the Java DTOs.
                Do not omit mandatory DTO fields.

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