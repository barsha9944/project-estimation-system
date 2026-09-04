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

        String prompt = buildPrompt(opportunity, useCases);

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

        for (EstimationUseCase useCase : useCases) {

            useCaseContext
                    .append("- ")
                    .append(useCase.getUseCaseName())
                    .append(" | Complexity: ")
                    .append(useCase.getComplexity())
                    .append("\n");
        }

        return """
                You are a senior software QA engineer.

                Generate comprehensive functional test cases for the
                project described below.

                ========================================================
                PROJECT INFORMATION
                ========================================================

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


                ========================================================
                EXISTING USE CASES
                ========================================================

                %s


                ========================================================
                CORE TEST CASE STRUCTURE
                ========================================================

                A TEST CASE represents one functional area or business
                requirement.

                A TEST CASE can contain MULTIPLE SCENARIOS.

                IMPORTANT:

                Do NOT create a separate Test Case for every scenario.

                Instead, group related scenarios under the same Test Case
                whenever they validate the same functional requirement
                or functional area.

                Each scenario must be represented by one or more test steps
                inside the SAME Test Case.

                For example:

                Test Case:
                TC_001 - User Login Validation

                Steps/scenarios:

                Step 1:
                Valid username and valid password

                Step 2:
                Invalid username

                Step 3:
                Invalid password

                Step 4:
                Both username and password missing

                Step 5:
                Account locked after applicable failed attempts

                All of these belong to TC_001 when they are part of the
                same login functionality.

                DO NOT create:

                TC_001 - Valid login
                TC_002 - Invalid username
                TC_003 - Invalid password
                TC_004 - Missing username

                when these scenarios belong to the same functional area.

                Instead create one comprehensive Test Case with all
                applicable scenarios represented in its steps.


                ========================================================
                SCENARIO COVERAGE
                ========================================================

                For EACH relevant functional requirement or use case,
                identify ALL applicable scenarios.

                Consider:

                1. Positive scenarios
                2. Negative scenarios
                3. Mandatory field validation
                4. Missing field validation
                5. Invalid input validation
                6. Valid input validation
                7. Boundary conditions
                8. Business rule validation
                9. Authentication
                10. Authorization
                11. Integration behavior
                12. Error handling
                13. Data validation
                14. Regression scenarios for enhancement projects
                15. Migration validation for migration projects
                16. Compatibility scenarios where explicitly supported


                ========================================================
                SCENARIO COMPLETENESS
                ========================================================

                Do not stop after identifying only the happy path.

                For every functional area, carefully determine whether
                additional scenarios are applicable.

                If a requirement supports multiple different inputs,
                conditions, rules, outcomes, validations, or errors,
                include each applicable scenario.

                Every meaningful scenario must be represented.

                Do not omit negative scenarios simply because a positive
                scenario already exists.

                Do not omit validation scenarios.

                Do not omit business-rule scenarios.

                Do not omit error scenarios when the requirement supports
                error handling.

                However, do NOT invent scenarios that are unsupported by
                the Requirement Summary or Use Cases.


                ========================================================
                HOW TO REPRESENT SCENARIOS
                ========================================================

                Each scenario must be represented by one or more steps.

                Every step must contain:

                - stepNumber
                - stepDescription
                - expectedResult

                The stepDescription should clearly describe the scenario,
                action, input, condition, or validation being tested.

                The expectedResult must describe observable system behavior.

                Example:

                {
                  "stepNumber": 1,
                  "stepDescription":
                    "Enter a valid username and valid password and submit the login form.",
                  "expectedResult":
                    "The user is authenticated and successfully enters the application."
                }

                Another scenario under the SAME test case can be:

                {
                  "stepNumber": 2,
                  "stepDescription":
                    "Enter a valid username and an invalid password and submit the login form.",
                  "expectedResult":
                    "The system rejects the login attempt and displays the applicable authentication error."
                }

                Continue adding steps for every applicable scenario
                belonging to that Test Case.


                ========================================================
                REQUIREMENT RULES
                ========================================================

                - The Requirement Summary is the primary source of truth.

                - Existing Use Cases provide additional functional context.

                - Do NOT use the project work schedule as the source
                  for functional behavior.

                - Do NOT invent functionality.

                - Do NOT assume technical implementation details.

                - Do NOT assume database tables, foreign keys, APIs,
                  HTTP status codes, URL parameters, HTTP headers,
                  database relationships, or specific UI controls unless
                  explicitly stated.

                - Expected results must describe observable behavior.

                - Do not mention database implementation details unless
                  database validation is explicitly required.

                - Each Test Case must reference the functional requirement
                  or use case that it validates through reqId.

                - If explicit requirement IDs are available, use them.

                - If explicit requirement IDs are unavailable, assign
                  sequential IDs such as REQ-001, REQ-002, REQ-003.

                - Multiple Test Cases may reference the same REQ ID.

                - Multiple scenarios within the same functional area should
                  normally remain under the same Test Case.

                - Create a separate Test Case only when the functionality
                  being validated is sufficiently different to represent
                  a separate functional area.

                - Do not generate duplicate or substantially overlapping
                  Test Cases.

                - Every Test Case must have at least one step.

                - Every step must have an expected result.

                - Every Test Case must have a unique Test Case ID.

                - Test Case IDs must be sequential:

                  TC_001
                  TC_002
                  TC_003
                  TC_004

                - Do not skip Test Case IDs.

                - Do not reuse Test Case IDs.

                - Step numbers must start from 1 for each Test Case.

                - Step numbers must be sequential.

                - Test data should be specified where applicable.


                ========================================================
                IMPORTANT DISTINCTION
                ========================================================

                DO NOT interpret "multiple scenarios" as "multiple
                Test Cases".

                Instead:

                ONE FUNCTIONAL AREA
                       |
                       +-- Scenario 1
                       +-- Scenario 2
                       +-- Scenario 3
                       +-- Scenario 4
                       +-- Scenario 5

                should normally become:

                ONE TEST CASE
                       |
                       +-- Step 1
                       +-- Step 2
                       +-- Step 3
                       +-- Step 4
                       +-- Step 5

                Create multiple Test Cases only when the requirements
                contain genuinely different functional areas.


                ========================================================
                OUTPUT FORMAT
                ========================================================

                Return ONLY valid JSON.

                Return exactly this structure:

                {
                  "testCases": [
                    {
                      "reqId": "REQ-001",
                      "testCaseId": "TC_001",
                      "testCaseName": "User Login Validation",
                      "testCaseDescription":
                        "Validate the user login functionality across all applicable scenarios.",
                      "testData":
                        "Valid credentials, invalid credentials, empty credentials",
                      "steps": [
                        {
                          "stepNumber": 1,
                          "stepDescription":
                            "Enter valid username and valid password and submit login.",
                          "expectedResult":
                            "The user is successfully authenticated."
                        },
                        {
                          "stepNumber": 2,
                          "stepDescription":
                            "Enter a valid username and invalid password and submit login.",
                          "expectedResult":
                            "The login attempt is rejected and an appropriate error is displayed."
                        },
                        {
                          "stepNumber": 3,
                          "stepDescription":
                            "Submit the login without entering the mandatory username.",
                          "expectedResult":
                            "The system indicates that the username is required."
                        }
                      ]
                    }
                  ]
                }

                ========================================================
                FINAL VALIDATION BEFORE RETURNING JSON
                ========================================================

                Before returning the response:

                1. Review the complete Requirement Summary.
                2. Review all provided Use Cases.
                3. Identify every distinct functional area.
                4. Identify all applicable scenarios for each area.
                5. Group related scenarios under the same Test Case.
                6. Ensure no applicable scenario has been omitted.
                7. Ensure unrelated functionality is not incorrectly grouped.
                8. Ensure every Test Case has at least one step.
                9. Ensure every step has an expected result.
                10. Ensure Test Case IDs are sequential.
                11. Ensure there are no duplicate Test Cases.
                12. Return only valid JSON.

                Do not return Markdown.

                Do not return code fences.

                Do not return explanations before or after the JSON.

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