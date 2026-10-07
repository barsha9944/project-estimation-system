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
				   exactly ONE corresponding SDD application component.
				
				10. Do NOT omit, merge, combine, or group functional requirements.
				
				    If the SRS contains 12 functional requirements, the generated
				    SDD Section 3 must contain exactly 12 application components.
				
				    Each SRS functional requirement must map one-to-one to one
				    SDD application component.
				
				11. SDD Section 3 numbering MUST be derived directly from the
				    SRS functional-requirement sectionNumber.
				
				    Replace the leading "5" of the SRS sectionNumber with "3".
				
				    Examples:
				
				    SRS 5.1  -> SDD 3.1
				    SRS 5.2  -> SDD 3.2
				    SRS 5.3  -> SDD 3.3
				    SRS 5.10 -> SDD 3.10
				
				    Do NOT independently renumber the application components.
				
				12. The componentName MUST match the corresponding SRS
				    functional-requirement name.
				
				    Example:
				
				    SRS 5.1 User Authentication
				    -> SDD 3.1 User Authentication
				
				    SRS 5.2 Project Requirement Management
				    -> SDD 3.2 Project Requirement Management
				
				13. If one functional requirement contains multiple detailed
				    functionalities or use cases, keep those details inside
				    the SAME application component.
				
				    Do NOT create another parent application component for them.
				
				14. Every application-component detail must have a
				    subComponentNumber belonging to its parent component.
				
				    For component 3.1:
				
				    3.1.1
				    3.1.2
				    3.1.3
				
				    For component 3.2:
				
				    3.2.1
				    3.2.2
				    3.2.3
				
				    Never assign a sub-component number belonging to another
				    parent component.

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

21. Generate exactly ONE sequence diagram for EVERY
    application sub-component/use case.

    Each sequence diagram MUST be specific to the corresponding
    SRS functional requirement and sub-component.

    Do NOT generate a generic project-level sequence diagram.

22. Store each sequence diagram in the field:

    "sequenceDiagram"

23. Every sequenceDiagram value MUST contain ONLY valid PlantUML.

    It MUST start with:

    @startuml

    and MUST end with:

    @enduml

24. The sequence diagram MUST show the actual interaction flow
    required to perform the corresponding functional requirement.

    The diagram should show, where applicable:

    Actor
    UI / Frontend
    Controller / API
    Service / Business Logic
    Security / Authentication
    Repository / DAO
    Database
    External System

    Only include participants that are actually relevant to the
    corresponding functional requirement.

25. DO NOT use generic placeholder participants such as:

    Supplier
    Backend
    Database

    unless those exact concepts are actually supported by the SRS.

    In particular, do NOT generate the same generic:

    Actor -> Backend -> Database

    flow for multiple functional requirements.

26. The participant names and interactions MUST be derived from
    the actual SRS functional requirement.

    For example, if the requirement is:

    "User Login"

    the diagram should represent a login flow.

    If the requirement is:

    "Project Requirement Entry"

    the diagram should represent the requirement-entry flow.

    If the requirement is:

    "Parameter Management"

    the diagram should represent the parameter-management flow.

    If the requirement is:

    "Expected Output Generation"

    the diagram should represent the output-generation flow.

    Do NOT reuse a sequence diagram from another functional
    requirement.

27. The sequence diagram MUST show the complete logical flow
    of the specific use case.

    Where supported by the SRS, include:

    1. Actor initiates the operation.
    2. UI sends the request.
    3. Controller/API receives the request.
    4. Authentication/authorization is performed.
    5. Service/business logic processes the request.
    6. Repository/DAO accesses persistent data.
    7. Database returns the required data/result.
    8. Service prepares the result.
    9. Controller/API returns the response.
    10. UI displays the result to the actor.

    Do NOT force every step into every diagram.

    Include only the steps supported by the corresponding
    functional requirement and SRS.

28. Show important validation and alternate flows where they
    are supported by the SRS.

    Use PlantUML constructs such as:

    alt
    else
    opt
    loop

    only when they represent an actual functional or business
    condition.

29. Use PlantUML participant types appropriately.

    For example:

    actor "User" as User
    boundary "Login UI" as UI
    control "Authentication API" as API
    control "Authentication Service" as Service
    database "PostgreSQL" as DB

    However, these names are examples only.

    Use names supported by the SRS whenever available.

30. Do NOT invent concrete implementation details.

    Do NOT invent:

    - controller class names
    - service class names
    - repository class names
    - DAO class names
    - JSP names
    - database table names
    - API endpoint names
    - method names
    - queue names
    - external services

    unless they are explicitly supported by the SRS.

31. If the SRS does not specify an implementation class name,
    use a logical architectural component name instead.

    For example:

    "Requirement Controller"
    "Requirement Service"
    "Requirement Repository"

    instead of inventing:

    "RequirementControllerImpl"
    "RequirementServiceImpl"
    "RequirementRepositoryImpl"

32. The sequence diagram MUST contain meaningful message
    labels describing the actual operation.

    Do NOT use meaningless messages such as:

    request
    process
    response
    save
    success

    when a more specific operation can be derived from the SRS.

33. The sequence diagram title MUST identify the actual
    functional requirement/use case.

    Example:

    @startuml
    title Sequence Diagram - Project Requirement Entry

    ...

    @enduml

    The title MUST NOT use an unrelated business process.

34. Every sequence diagram MUST be different when the
    corresponding functional requirements have different flows.

    Do NOT copy the same sequence diagram into multiple
    application components.

35. The sequence diagram should follow this general structure
    where applicable:

    Actor
        |
        v
    UI / Frontend
        |
        v
    Controller / API
        |
        v
    Service / Business Logic
        |
        v
    Repository / DAO
        |
        v
    Database

    followed by the appropriate response path.

    This is a logical pattern only.

    The actual participants and messages MUST be determined
    from the SRS.

36. Do NOT create a sequence diagram merely to satisfy the
    presence of the field.

    The diagram must meaningfully describe how the specific
    functional requirement is executed.

37. Keep the generated diagrams readable.

    Prefer a small number of meaningful participants and clear
    interactions over a large diagram containing unnecessary
    components.

38. Never use project-specific content from the reference SDD.

    Do NOT use examples such as:

    Supplier
    Recipient
    Child
    Eulogy
    HomeController
    File Size
    Bar Graph
    Upgrade User

    unless the same concept is explicitly present in the
    current project's SRS.

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

        		IMPORTANT:

				The following JSON is ONLY a schema/example of the object structure.
				
				It is NOT an instruction to generate only one application component.
				
				The actual output MUST contain one application component for EVERY
				functional requirement present in the SRS.
				
				Therefore, if the SRS contains:
				
				5.1
				5.2
				5.3
				5.4
				5.5
				
				the output MUST contain:
				
				3.1
				3.2
				3.3
				3.4
				3.5
				
				The actual number of application components MUST be determined from
				the SRS functional requirements.
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

                Every functional requirement in the SRS must appear exactly once in:

				1. applicationComponents
				2. requirementsTraceabilityMatrix
				
				The number of entries in applicationComponents MUST equal the number
				of functional requirements in the SRS.
				
				Do not generate only one application component when the SRS contains
				multiple functional requirements.
				
				Preserve every requirementId from the SRS.
				
				Preserve every sectionNumber from the SRS.
				
				For every SRS functional requirement:
				
				SRS sectionNumber 5.x
				-> SDD componentNumber 3.x
				
				The requirementsTraceabilityMatrix must contain exactly one entry
				for every functional requirement.
				
				The designComponent must identify the corresponding SDD component
				number and, where applicable, the corresponding sub-component number.

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