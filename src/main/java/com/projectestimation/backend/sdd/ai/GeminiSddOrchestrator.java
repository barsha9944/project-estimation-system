package com.projectestimation.backend.sdd.ai;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.projectestimation.backend.common.ai.GeminiClient;
import com.projectestimation.backend.sdd.dto.SddDto;
import com.projectestimation.backend.srs.dto.SrsDto;

@Service
public class GeminiSddOrchestrator {

    private static final int MAX_OUTPUT_TOKENS = 16384;

    private final GeminiClient geminiClient;
    private final ObjectMapper objectMapper;

    public GeminiSddOrchestrator(
            GeminiClient geminiClient,
            ObjectMapper objectMapper) {

        this.geminiClient = geminiClient;
        this.objectMapper = objectMapper;
    }

    public SddDto generate(SrsDto srsDto) {

        try {
            String srsJson = objectMapper.writeValueAsString(srsDto);

            String prompt = buildPrompt(srsJson);

            String response = geminiClient.generateJsonContent(
                    prompt,
                    MAX_OUTPUT_TOKENS
            );

            return parseResponse(response);

        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to generate SDD from SRS: " + e.getMessage(),
                    e
            );
        }
    }

    private String buildPrompt(String srsJson) {

        return """
                You are an expert Software Design Document (SDD) analyst.

                Generate a complete Software Design Document for the project using ONLY
                the SRS JSON provided below as the source of truth.

                The SDD must follow the structure of the provided SDD reference document.
                Do not remove sections, subsections, or nested subsections from that
                structure.

                The SRS is the INPUT for generating this SDD.

                IMPORTANT RULES:

                1. Return ONLY valid JSON.

                2. Do NOT return Markdown outside string values.

                3. Do NOT wrap the JSON inside ```json or any code block.

                4. Do NOT add explanations before or after the JSON.

                5. Do NOT invent project facts that are not supported by the SRS.

                6. If a value cannot be determined from the SRS, use:
                   "Not specified"

                   Do not invent concrete implementation details.

                7. The SDD must describe the actual project represented by the SRS.
                   Do not copy project-specific names, modules, controllers, JSPs,
                   DAOs, entities, URLs, or technologies from the reference SDD.

                8. The reference SDD is used only for its document structure,
                   organization, terminology, and level of detail.

                9. Preserve the complete application-component hierarchy.

                   Every functional requirement in the SRS must be represented by
                   an appropriate SDD application component/sub-component.

                10. Do NOT omit functional requirements.

                    If the SRS contains 12 functional requirements, the generated SDD
                    must represent all 12 functional requirements.

                11. Multiple functional requirements may belong to the same parent
                    application component when they represent the same business module.

                12. Each application component must have an appropriate componentNumber
                    such as:

                    "3.1"
                    "3.2"
                    "3.3"

                13. Each sub-component/use case must have a corresponding
                    subComponentNumber such as:

                    "3.1.1"
                    "3.1.2"
                    "3.2.1"

                14. The following information must be derived from the SRS:

                    - Description
                    - Primary Actor
                    - Secondary Actor
                    - Precondition
                    - Basic Flow
                    - Business Rules
                    - Post condition
                    - UI Design

                15. Do NOT invent implementation artifacts.

                    Request Jsp
                    Response Jsp
                    Controller
                    DAO
                    Entity Name

                    must be "Not specified" unless the SRS explicitly provides
                    enough information to support them.

                16. Design Details must preserve the following structure:

                    SL. No.
                    Request Jsp
                    Response Jsp
                    Controller
                    DAO
                    Entity Name

                17. Generate formal, professional SDD language.

                18. Do not copy example content from the reference SDD.

                19. The generated SDD must be internally consistent.

                20. All diagrams must describe the actual project represented by
                    the SRS and must NOT describe the old reference project.

                ------------------------------------------------------------
                DIAGRAM REQUIREMENTS
                ------------------------------------------------------------

                21. Generate a sequence diagram for EVERY application
                    sub-component/use case.

                    The sequence diagram must be derived from the corresponding
                    SRS functional requirement.

                22. Store each sequence diagram in the field:

                    "sequenceDiagram"

                23. Every sequenceDiagram value must contain ONLY a valid
                    PlantUML definition.

                24. Every sequence diagram must start with:

                    @startuml

                    and end with:

                    @enduml

                25. Use logical actors and system components supported by the SRS.

                    For example, if the SRS identifies:

                    User
                    Buyer Portal
                    Supplier Portal
                    Backend REST API
                    PostgreSQL
                    Worker/Queue Services

                    those may be represented where appropriate.

                26. Do NOT invent controller class names, service class names,
                    DAO names, JSP names, or database table names.

                27. Sequence diagrams should represent the actual functional flow,
                    including:

                    actor interaction
                    system processing
                    relevant external interface
                    data persistence where supported
                    response/result

                28. Keep sequence diagrams readable.

                    Do not create unnecessarily large diagrams.

                29. Generate ONE overall project class diagram and store it in:

                    "classDiagram"

                30. The class diagram must also be valid PlantUML:

                    @startuml

                    ...

                    @enduml

                31. The class diagram must be derived from the SRS Data Requirements,
                    Product Components, Functional Requirements, and interfaces.

                32. Do NOT invent actual source-code POJO classes if the SRS does not
                    define them.

                33. When the SRS does not provide enough information for a concrete
                    class/entity structure, use logical domain entities and clearly
                    meaningful relationships derived from the SRS.

                34. Do NOT invent database table names.

                35. Generate the Section 6.2 integration-testing diagram and store it in:

                    "integrationTestingDiagram"

                36. This must also be valid PlantUML:

                    @startuml

                    ...

                    @enduml

                37. The integration-testing diagram must be derived from the actual
                    project components and functional areas in the SRS.

                38. Do not copy the old reference project's Recipient, Child,
                    Bar Graph, Upgrade User, or other project-specific integration
                    elements.

                39. If the SRS does not provide enough information for a specific
                    integration-test dependency, use a logical project-level
                    representation rather than inventing implementation details.

                ------------------------------------------------------------
                DOCUMENT STRUCTURE
                ------------------------------------------------------------

                Generate these root sections:

                A. Document Release History
                B. Circulation Details
                C. List of Amendments Made On The Previous Version No. _____

                1.0 Introduction
                  1.1 Purpose of this document
                  1.2 Scope of the Proposed System
                  1.3 Design Alternatives Considered
                  1.4 Make, Buy or Reuse Decisions Taken

                2.0 The System Perspective
                  2.1 The System Context
                  2.2 The Development, Testing, and Deployment Environment
                    2.2.1 Development Environment
                      2.2.1.1 Software Needed
                      2.2.1.2 Hardware Needed
                      2.2.1.3 Network needed
                    2.2.2 Testing Environment
                      2.2.2.1 Software Needed
                      2.2.2.2 Hardware Needed
                      2.2.2.3 Network needed
                    2.2.3 Deployment Environment
                      2.2.3.1 Software Needed
                      2.2.3.2 Hardware Needed
                      2.2.3.3 Network needed
                  2.3 The Application Components
                  2.4 General Features of the Proposed Application

                3.0 Specific Description of the Application Components

                4.0 Proposed Database Design

                5.0 Special Considerations
                  5.1 Design Rules/Criteria to Follow
                  5.2 Programming Rules to Follow
                  5.3 Error Handling Procedures
                  5.4 Special Security Provisions
                  5.5 Special Recovery Procedure
                  5.6 Audit tracing facility
                  5.7 System implementation procedures
                  5.8 Database administration procedure

                6.0 System Integration Strategy
                  6.1 Desired Strategy for Integration of Product Modules
                  6.2 Desired Sequence & Criteria for Integration Testing

                7.0 Requirements Traceability Matrix

                ------------------------------------------------------------
                EXACT JSON CONTRACT
                ------------------------------------------------------------

                Return JSON matching this logical structure exactly:

                {
                  "documentReleaseHistory": {
                    "entries": [
                      {
                        "serialNumber": 1,
                        "versionNumber": "1.0",
                        "releaseDate": "Not specified",
                        "preparedBy": "Not specified",
                        "reviewedAndApprovedBy": "Not specified",
                        "reasonsForRelease": "Initial release"
                      }
                    ]
                  },

                  "circulationDetails": {
                    "circulationInstructions": "Not specified",
                    "entries": [
                      {
                        "copyNumber": 1,
                        "designationOfCopyHolder": "Not specified",
                        "locationOfCopy": "Not specified"
                      }
                    ]
                  },

                  "amendments": [
                    {
                      "serialNumber": 1,
                      "sectionNoOrPageNo": "Not specified",
                      "descriptionOfAmendment": "Not specified",
                      "approvedBy": "Not specified",
                      "changeRequestNoAndDate": "Not specified"
                    }
                  ],

                  "introduction": {
                    "purposeOfDocument": "Not specified",

                    "scopeOfProposedSystem": [
                      {
                        "taskNo": 1,
                        "taskName": "Not specified",
                        "taskDetails": "Not specified"
                      }
                    ],

                    "designAlternativesConsidered": [
                      {
                        "designAlternative": "Not specified",
                        "description": "Not specified"
                      }
                    ],

                    "designAlternativesDecision": "Not specified",

                    "makeBuyOrReuseDecisionsTaken": "Not specified"
                  },

                  "systemPerspective": {
                    "systemContext": "Not specified",

                    "developmentTestingDeploymentEnvironment": {
                      "developmentEnvironment": {
                        "softwareNeeded": [
                          {
                            "technologyArea": "Not specified",
                            "productServiceOrStandard": "Not specified"
                          }
                        ],
                        "hardwareNeeded": "Not specified",
                        "networkNeeded": "Not specified"
                      },

                      "testingEnvironment": {
                        "softwareNeeded": [
                          {
                            "technologyArea": "Not specified",
                            "productServiceOrStandard": "Not specified"
                          }
                        ],
                        "hardwareNeeded": "Not specified",
                        "networkNeeded": "Not specified"
                      },

                      "deploymentEnvironment": {
                        "softwareNeeded": [
                          {
                            "technologyArea": "Not specified",
                            "productServiceOrStandard": "Not specified"
                          }
                        ],
                        "hardwareNeeded": "Not specified",
                        "networkNeeded": "Not specified"
                      }
                    },

                    "applicationComponents": "Not specified",

                    "generalFeaturesOfProposedApplication": "Not specified"
                  },

                  "applicationComponents": [
                    {
                      "componentNumber": "3.1",
                      "componentName": "Not specified",

                      "details": [
                        {
                          "subComponentNumber": "3.1.1",
                          "subComponentName": "Not specified",

                          "description": "Not specified",

                          "primaryActor": "Not specified",

                          "secondaryActor": "Not specified",

                          "precondition": "Not specified",

                          "basicFlow": [
                            "Not specified"
                          ],

                          "businessRules": [
                            "Not specified"
                          ],

                          "postCondition": "Not specified",

                          "uiDesign": "Not specified",

                          "designDetails": [
                            {
                              "serialNumber": 1,
                              "requestJsp": "Not specified",
                              "responseJsp": "Not specified",
                              "controller": "Not specified",
                              "dao": "Not specified",
                              "entityName": "Not specified"
                            }
                          ],

                          "sequenceDiagram": "@startuml\\n@enduml"
                        }
                      ]
                    }
                  ],

                  "classDiagram": "@startuml\\n@enduml",

                  "databaseDesign": {
                    "description": "Not specified"
                  },

                  "specialConsiderations": {
                    "designRulesCriteriaToFollow": "Not specified",
                    "programmingRulesToFollow": "Not specified",
                    "errorHandlingProcedures": "Not specified",
                    "specialSecurityProvisions": "Not specified",
                    "specialRecoveryProcedure": "Not specified",
                    "auditTracingFacility": "Not specified",
                    "systemImplementationProcedures": "Not specified",
                    "databaseAdministrationProcedure": "Not specified"
                  },

                  "systemIntegrationStrategy": {
                    "desiredStrategyForIntegrationOfProductModules": "Not specified",

                    "desiredSequenceAndCriteriaForIntegrationTesting": "Not specified",

                    "integrationTestingDiagram": "@startuml\\n@enduml"
                  },

                  "requirementsTraceabilityMatrix": [
                    {
                      "requirementId": "FR-001",
                      "requirementDescription": "Not specified",
                      "designComponent": "Not specified"
                    }
                  ]
                }

                ------------------------------------------------------------
                REQUIREMENTS TRACEABILITY
                ------------------------------------------------------------

                Every functional requirement in the SRS must appear in:

                1. applicationComponents
                2. requirementsTraceabilityMatrix

                Do not generate only one application component when the SRS contains
                multiple functional requirements.

                Preserve every requirementId from the SRS.

                The requirementsTraceabilityMatrix must contain one entry for every
                functional requirement.

                The designComponent should identify the corresponding SDD component
                and sub-component number.

                ------------------------------------------------------------
                DIAGRAM COMPLETENESS
                ------------------------------------------------------------

                For every generated application component detail:

                sequenceDiagram MUST NOT be omitted.

                For the overall SDD:

                classDiagram MUST NOT be omitted.

                For systemIntegrationStrategy:

                integrationTestingDiagram MUST NOT be omitted.

                If the SRS does not provide enough information to create a detailed
                diagram, generate a minimal valid PlantUML diagram using only the
                actors/components that can be supported by the SRS.

                Never use null for these diagram fields.

                ------------------------------------------------------------
                SOURCE SRS
                ------------------------------------------------------------

                SRS JSON:

                %s

                ------------------------------------------------------------

                Now generate the complete SDD JSON.

                Remember:

                - SRS is the only project-specific source of truth.
                - Reference SDD structure must be preserved.
                - Every functional requirement must be represented.
                - Every functional requirement must have a sequence diagram.
                - The project must have one class diagram.
                - Section 6.2 must have an integration-testing diagram.
                - Do not invent unsupported implementation details.
                - Return ONLY valid JSON.
                """.formatted(srsJson);
    }

    private SddDto parseResponse(String response) {

        try {
            String cleanedResponse = response == null
                    ? ""
                    : response.trim();

            if (cleanedResponse.startsWith("```json")) {
                cleanedResponse = cleanedResponse.substring(7).trim();
            } else if (cleanedResponse.startsWith("```")) {
                cleanedResponse = cleanedResponse.substring(3).trim();
            }

            if (cleanedResponse.endsWith("```")) {
                cleanedResponse = cleanedResponse
                        .substring(0, cleanedResponse.length() - 3)
                        .trim();
            }

            return objectMapper.readValue(
                    cleanedResponse,
                    SddDto.class
            );

        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to parse Gemini SDD response",
                    e
            );
        }
    }
}