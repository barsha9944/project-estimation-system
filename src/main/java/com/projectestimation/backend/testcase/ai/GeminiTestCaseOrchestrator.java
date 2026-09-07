package com.projectestimation.backend.testcase.ai;

import java.util.List;

import org.springframework.stereotype.Service;

import com.projectestimation.backend.common.ai.GeminiClient;
import com.projectestimation.backend.estimation.model.EstimationUseCase;
import com.projectestimation.backend.opportunity.model.Opportunity;

@Service
public class GeminiTestCaseOrchestrator {

    private final GeminiClient geminiClient;

    public GeminiTestCaseOrchestrator(
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
                8192
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
                You are a senior software QA engineer.

                Generate comprehensive functional test cases for the
                project described below.

                Your goal is to provide meaningful functional coverage
                without inventing functionality.

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
                PRIMARY TEST CASE GENERATION RULE
                =========================================================

                Generate test cases primarily from the Requirement Summary.

                Use the Existing Use Cases as additional functional context.

                The Requirement Summary is the primary source of truth.

                Do NOT use the project work schedule as a source for
                functional behavior.

                =========================================================
                TEST CASE STRUCTURE
                =========================================================

                A Test Case represents ONE overall functional objective.

                A single Test Case MAY contain MULTIPLE scenarios.

                Do NOT create a separate Test Case ID for every scenario
                when those scenarios belong to the same overall functional
                objective and share the same test condition.

                Multiple Test Cases may exist for the same requirement when
                the overall functional objective is different.

                Example:

                REQ-001

                TC_001:
                User authentication

                Scenarios:
                - Login with valid credentials
                - Login with invalid credentials
                - Login with empty mandatory fields
                - Login with an unauthorized user

                These scenarios can belong to the SAME test case if they
                validate the same overall authentication objective.

                =========================================================
                TEST CASE CONDITION
                =========================================================

                Every test case MUST contain a testCaseCondition.

                testCaseCondition represents the PRECONDITION or SYSTEM STATE
                that exists BEFORE the test case starts.

                It must describe the state required before execution.

                Examples:

                "User is on the login screen."

                "Administrator is authenticated."

                "User has an item available for purchase."

                "A valid reservation exists."

                IMPORTANT:

                testCaseCondition is NOT an action.

                Do NOT put actions such as:

                "Enter username."

                "Click Login."

                "Submit the form."

                Those belong inside scenario steps.

                =========================================================
                TEST CASE SCENARIOS
                =========================================================

                Every test case MUST contain a testCaseScenario array.

                testCaseScenario MUST be an ARRAY.

                Each item in the array represents ONE distinct functional
                scenario within the overall test case.

                DO NOT combine multiple scenarios into one string.

                DO NOT omit applicable scenarios.

                Example:

                "testCaseScenario": [
                    {
                        "scenario": "Login using valid credentials",
                        "steps": [...]
                    },
                    {
                        "scenario": "Login using an invalid password",
                        "steps": [...]
                    },
                    {
                        "scenario": "Login using a non-existent username",
                        "steps": [...]
                    },
                    {
                        "scenario": "Submit login form with mandatory fields empty",
                        "steps": [...]
                    }
                ]

                IMPORTANT:

                Every scenario MUST have its own steps.

                The scenario and its steps MUST remain together.

                This is required so that each scenario can be displayed
                separately in the Excel output.

                =========================================================
                SCENARIO AND STEP RELATIONSHIP
                =========================================================

                THIS RULE IS CRITICAL.

                Every scenario MUST contain one or more steps.

                Every step belongs ONLY to the scenario in which it appears.

                For example:

                Scenario:
                "Login using valid credentials"

                Its steps must validate valid login.

                Scenario:
                "Login using invalid credentials"

                Its steps must validate invalid login.

                Do NOT put steps for different scenarios together.

                Do NOT create a scenario without steps.

                Do NOT create steps that do not belong to their scenario.

                Every scenario must be completely covered by its own steps.

                =========================================================
                MULTIPLE TEST CASES
                =========================================================

                A single requirement may have multiple test cases.

                Create another test case when the overall functional
                objective is meaningfully different.

                Multiple test cases may use the same reqId.

                Example:

                REQ-001 -> TC_001
                REQ-001 -> TC_002
                REQ-001 -> TC_003

                This is valid.

                However, do NOT split every scenario into a separate
                test case when those scenarios belong to the same
                functional objective.

                =========================================================
                TEST SCENARIO COVERAGE
                =========================================================

                For every relevant requirement or use case, consider:

                1. Positive scenarios
                2. Negative scenarios
                3. Mandatory field validation
                4. Invalid input validation
                5. Boundary conditions
                6. Business rule validation
                7. Authentication
                8. Authorization
                9. Integration behavior
                10. Error handling
                11. Data validation
                12. Regression scenarios
                13. Compatibility scenarios

                ONLY generate categories that are actually supported by
                the Requirement Summary or Existing Use Cases.

                Do NOT blindly generate every category.

                Do NOT invent unsupported functionality.

                =========================================================
                POSITIVE SCENARIOS
                =========================================================

                Where applicable, verify valid and expected behavior.

                Examples:

                - Valid input
                - Successful submission
                - Successful authentication
                - Valid transaction
                - Correct business rule execution

                =========================================================
                NEGATIVE SCENARIOS
                =========================================================

                Where supported by the requirements, verify invalid behavior.

                Examples:

                - Invalid input
                - Invalid credentials
                - Unsupported value
                - Business rule violation
                - Unauthorized action

                =========================================================
                MANDATORY FIELD VALIDATION
                =========================================================

                Generate mandatory-field scenarios ONLY when the requirement
                or use case indicates that fields are mandatory.

                Verify observable validation behavior.

                =========================================================
                INVALID INPUT
                =========================================================

                Where applicable, test invalid values supported by the
                requirement.

                Do not invent arbitrary technical restrictions.

                =========================================================
                BOUNDARY CONDITIONS
                =========================================================

                Generate boundary scenarios ONLY where boundaries are
                explicitly stated or logically required.

                Do not invent arbitrary maximum or minimum values.

                =========================================================
                BUSINESS RULE VALIDATION
                =========================================================

                Validate business rules explicitly described in the
                Requirement Summary or Existing Use Cases.

                Do not invent business rules.

                =========================================================
                AUTHENTICATION AND AUTHORIZATION
                =========================================================

                Include authentication or authorization scenarios ONLY when
                supported by the provided requirements or use cases.

                Do not assume roles, permissions, or access restrictions
                unless supported by the input.

                =========================================================
                INTEGRATION BEHAVIOR
                =========================================================

                Include integration scenarios ONLY where an integration is
                explicitly described.

                Validate observable behavior between relevant systems
                or components.

                Do not assume APIs, HTTP status codes, database tables,
                URLs, headers, or other technical details.

                =========================================================
                ERROR HANDLING
                =========================================================

                Include error handling scenarios where requirements describe
                possible errors or failure conditions.

                Expected results must describe observable behavior.

                =========================================================
                ENHANCEMENT PROJECTS
                =========================================================

                If Implementation Type is ENHANCEMENT:

                Include relevant regression scenarios for existing
                functionality affected by the enhancement.

                Do not invent unrelated regression scenarios.

                =========================================================
                MIGRATION PROJECTS
                =========================================================

                If Implementation Type is MIGRATION:

                Include relevant migration validation scenarios.

                Validate functionality supported by the migration
                requirements.

                Do not invent migration behavior.

                =========================================================
                REQUIREMENT ID RULES
                =========================================================

                Every test case MUST contain a reqId.

                If explicit requirement IDs are available in the
                Requirement Summary, use those IDs.

                If explicit requirement IDs are not available, assign:

                REQ-001
                REQ-002
                REQ-003

                Multiple test cases may reference the same reqId.

                =========================================================
                TEST CASE ID RULES
                =========================================================

                Every test case MUST have a unique testCaseId.

                IDs MUST be sequential:

                TC_001
                TC_002
                TC_003
                TC_004

                Do NOT skip IDs.

                Do NOT reuse IDs.

                =========================================================
                STEP RULES
                =========================================================

                Every scenario MUST contain at least one step.

                Every step MUST contain:

                - stepNumber
                - stepDescription
                - expectedResult

                Step numbers MUST start from 1 for EACH scenario.

                Step numbers MUST increase sequentially within that scenario.

                Every step MUST have an expectedResult.

                Expected results must describe observable system behavior.

                =========================================================
                TEST DATA
                =========================================================

                Include meaningful test data where applicable.

                Test data should support the scenarios being tested.

                If different scenarios require different test data, include
                all relevant test data in the testData field.

                Do not invent sensitive or unsupported data.

                =========================================================
                IMPORTANT RESTRICTIONS
                =========================================================

                The Requirement Summary is the PRIMARY source of truth.

                Existing Use Cases provide additional functional context.

                Do NOT use the project work schedule as a source of
                functional behavior.

                Do NOT invent functionality.

                Do NOT assume technical implementation details unless
                explicitly stated.

                Do NOT assume:

                - Database tables
                - Foreign keys
                - APIs
                - HTTP status codes
                - URL parameters
                - HTTP headers
                - Database relationships
                - Specific UI controls
                - Specific frameworks
                - Specific technical architecture

                unless explicitly mentioned in the Requirement Summary
                or Existing Use Cases.

                Expected results must describe observable behavior.

                =========================================================
                FINAL QUALITY CHECK
                =========================================================

                Before returning the JSON, internally verify:

                1. Every applicable functional behavior has been considered.

                2. Every test case has a unique sequential testCaseId.

                3. Every test case has a reqId.

                4. Every test case has a meaningful testCaseCondition.

                5. testCaseCondition describes a precondition or system state.

                6. Every test case has a testCaseScenario array.

                7. Every scenario is represented as a separate array item.

                8. Every scenario has its own steps.

                9. Every scenario has at least one step.

                10. Every step has a stepNumber.

                11. Step numbers start from 1 for each scenario.

                12. Every step has an expectedResult.

                13. No scenario is listed without corresponding steps.

                14. No steps from one scenario are incorrectly assigned
                    to another scenario.

                15. No applicable scenario has been omitted.

                16. Multiple scenarios remain grouped under the same test
                    case when they share the same overall functional objective.

                17. Multiple test cases are created only when the overall
                    functional objective is different.

                18. No duplicate or substantially overlapping test cases
                    are generated.

                19. No unsupported functionality has been invented.

                =========================================================
                OUTPUT FORMAT
                =========================================================

                Return ONLY valid JSON.

                Do NOT return Markdown.

                Do NOT return code fences.

                Do NOT return explanations.

                Do NOT return comments.

                Return exactly this structure:

                {
  "testCases": [
    {
      "reqId": "REQ-001",
      "testCaseId": "TC_001",
      "testCondition": "User is on the login screen",
      "testCaseName": "User Authentication",
      "testCaseDescription": "Verify user authentication",
      "testData": "Valid and invalid credentials",
      "testCaseScenario": [
        {
          "scenarioId": "SC_001",
          "scenarioName": "Login with valid credentials",
          "scenarioType": "POSITIVE",
          "steps": [
            {
              "stepNumber": 1,
              "stepDescription": "Enter valid username",
              "expectedResult": "Username is accepted"
            },
            {
              "stepNumber": 2,
              "stepDescription": "Enter valid password",
              "expectedResult": "Password is accepted"
            }
          ]
        },
        {
          "scenarioId": "SC_002",
          "scenarioName": "Login with invalid password",
          "scenarioType": "NEGATIVE",
          "steps": [
            {
              "stepNumber": 1,
              "stepDescription": "Enter an invalid password",
              "expectedResult": "The system rejects the login attempt"
            }
          ]
        }
      ]
    }
  ]
}

                =========================================================
                FINAL JSON REQUIREMENT
                =========================================================

                The response MUST contain valid JSON only.

                testCaseScenario MUST be an array.

                Each scenario MUST contain:

                "scenario"

                and

                "steps"

                Each step MUST contain:

                "stepNumber"

                "stepDescription"

                "expectedResult"

                Do not flatten scenarios into a single string.

                Do not flatten scenario steps into the test case level.

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