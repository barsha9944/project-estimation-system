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

                Generate functional test cases for the project described below.

                =========================================
                PROJECT INFORMATION
                =========================================

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

                =========================================
                EXISTING USE CASES
                =========================================

                %s

                =========================================
                TEST CASE GENERATION RULES
                =========================================

                Generate test cases based primarily on the Requirement Summary.

                Use the existing use cases as structured functional context.

                Do NOT use the project work schedule as the source for
                functional behavior.

              For each relevant use case, identify applicable test scenarios from:

1. Positive scenarios
2. Negative scenarios
3. Mandatory field validation
4. Invalid input
5. Boundary conditions
6. Business rule validation
7. Authentication and authorization
8. Integration behavior
9. Error handling

Only generate scenarios that are relevant to the functionality
described in the Requirement Summary or Use Cases.
Do not artificially create scenarios that are not applicable.

Only generate scenarios that are relevant to the functionality
described in the Requirement Summary or Use Cases.
Do not artificially create scenarios that are not applicable.

=========================================
PROJECT-SPECIFIC TESTING PHASES
=========================================

Analyze the Requirement Summary and Existing Use Cases and identify
the appropriate testing phases for THIS project.

The testing phases must be based on the actual requirements and
functionality of this project.

Do not assume that every project has the same number of phases.

Create only phases that are relevant to this project.

Assign every generated test case to the most appropriate testing phase.

Use a clear phase name such as:
"Phase 1 - Functional Testing"
"Phase 2 - Integration Testing"
"Phase 3 - User Acceptance Testing"

The phase must describe the testing stage represented by the test case.

Do not determine testing phases from the project work schedule,
project dates, task durations, or implementation schedule.

For ENHANCEMENT projects, include regression scenarios
where applicable.

                For ENHANCEMENT projects, include regression scenarios
                where applicable.

                For MIGRATION projects, include migration validation
                scenarios where applicable.

            

                IMPORTANT:
                
                IMPORTANT:

- The Requirement Summary is the primary source of truth.
- Existing Use Cases provide additional functional context.
- Do not use the project work schedule as a source for functional behavior.

- Do not invent functionality that is not supported by the
  Requirement Summary or existing Use Cases.

- Do not assume a specific technical implementation unless it is
  explicitly stated in the Requirement Summary or Use Cases.

- Do not assume database tables, foreign keys, APIs, HTTP status codes,
  URL parameters, HTTP headers, database relationships, or specific
  UI controls unless explicitly mentioned.

- Expected results must describe observable system behavior rather
  than internal implementation details.

- Do not mention database implementation details unless the requirement
  explicitly requires database validation.

- Each test case must reference the functional requirement or use case
  that it validates.

- If explicit requirement IDs are available in the Requirement Summary,
  use those IDs.

- If explicit requirement IDs are not available, assign sequential IDs
  such as REQ-001, REQ-002, etc.

- Multiple test cases may reference the same REQ ID when they validate
  different scenarios for the same requirement.

- Do not generate duplicate test cases.

- Every test case must have a unique Test Case ID.

- Every test case must contain at least one test step.

- Every test step must have an expected result.

- Test Case IDs must use the format TC_001, TC_002, TC_003...

- Step numbers must start from 1 for each test case.

- Test data should be specified where applicable.


                =========================================
                OUTPUT FORMAT
                =========================================

                Return ONLY valid JSON.

                Return exactly this structure:

                {
  "testCases": [
    {
      "reqId": "REQ-001",
      "testCaseId": "TC_001",
      "phase": "Phase 1 - Functional Testing",
      "testCaseName": "Test case name",
      "testCaseDescription": "Description",
      "testData": "Test data",
      "steps": [
                        {
                          "stepNumber": 1,
                          "stepDescription": "Step description",
                          "expectedResult": "Expected result"
                        }
                      ]
                    }
                  ]
                }

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