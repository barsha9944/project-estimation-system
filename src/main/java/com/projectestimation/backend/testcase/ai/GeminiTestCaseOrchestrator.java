package com.projectestimation.backend.testcase.ai;

import java.util.List;

import org.springframework.stereotype.Service;

import com.projectestimation.backend.common.ai.GeminiClient;
import com.projectestimation.backend.estimation.model.EstimationUseCase;
import com.projectestimation.backend.opportunity.model.Opportunity;

@Service
public class GeminiTestCaseOrchestrator {

	private final GeminiClient geminiClient;

	public GeminiTestCaseOrchestrator(GeminiClient geminiClient) {
		this.geminiClient = geminiClient;
	}

	public String generate(Opportunity opportunity, List<EstimationUseCase> useCases) {

		String prompt = buildPrompt(opportunity, useCases);

		return geminiClient.generateJsonContent(prompt, 8192);
	}

	private String buildPrompt(Opportunity opportunity, List<EstimationUseCase> useCases) {

		StringBuilder useCaseContext = new StringBuilder();

		for (EstimationUseCase useCase : useCases) {
			useCaseContext.append("- ").append(useCase.getUseCaseName()).append(" | Complexity: ")
					.append(useCase.getComplexity()).append("\n");
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

								For each relevant requirement and use case, generate ALL applicable
								test scenarios needed to adequately validate the functionality.

								A single requirement or use case may have MULTIPLE test cases.

								Do NOT limit the output to one test case per requirement or use case.

								Generate separate test cases whenever the scenarios differ in purpose,
								input, condition, validation, or expected behavior.

								Consider the following scenario categories where applicable:

								1. Positive scenarios
								2. Negative scenarios
								3. Mandatory field validation
								4. Invalid input validation
								5. Boundary conditions
								6. Business rule validation
								7. Authentication and authorization
								8. Integration behavior
								9. Error handling
								10. Data validation
								11. Regression scenarios for affected existing functionality
								12. Compatibility scenarios where explicitly supported

								Only generate scenario categories that are relevant to the
								Requirement Summary or existing Use Cases.
								Do not artificially create scenarios that are not applicable.
								Only generate scenarios that are relevant to the functionality
								described in the Requirement Summary or Use Cases.
								Do not artificially create scenarios that are not applicable.


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

								- Do not generate duplicate or substantially overlapping test cases.

				- Multiple test cases may reference the same REQ ID.

				- Multiple test cases may reference the same use case when they validate
				  different scenarios.

				- Each test case must validate a distinct functional behavior,
				  condition, input, rule, or outcome.

				- Every test case must have a unique Test Case ID.

				- Test Case IDs must be sequential:
				  TC_001, TC_002, TC_003, TC_004...

				- Do not skip Test Case IDs.

				- Do not reuse a Test Case ID.

				- Generate enough test cases to provide meaningful functional coverage.
				  Do not stop after generating one test case for each requirement.

				  =========================================
				TEST CASE COVERAGE
				=========================================

				Review the complete Requirement Summary and existing Use Cases before
				generating the test cases.

				Identify distinct functional requirements and behaviors first.

				Then generate multiple test cases for each requirement or use case
				where necessary.

				For example, if one requirement supports:
				- valid input
				- invalid input
				- missing mandatory field
				- boundary value
				- authorization restriction

				these should be represented as separate test cases when applicable.

				Do not generate all categories blindly. Generate only scenarios that
				are supported by the provided requirements and use cases.

				The final test case collection should provide broad functional coverage
				without inventing unsupported functionality.


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

								                """.formatted(opportunity.getOpportunityName(),
				opportunity.getImplementationType(), opportunity.getPriority(), opportunity.getRequirementSummary(),
				opportunity.getComponents(), useCaseContext);
	}
}