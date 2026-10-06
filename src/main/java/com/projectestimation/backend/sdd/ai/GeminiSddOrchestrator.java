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

                Generate a complete Software Design Document for the project using
                the provided Software Requirements Specification (SRS) as the
                primary and authoritative input.

                IMPORTANT:
                The SDD MUST follow the exact structure of the supplied SDD
                reference document. Do not add, remove, merge, rename, or reorder
                sections or subsections.

                The required SDD structure is:

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
                  3.1 Specific Description of Component 1: <Module Name 1>
                    3.1.1 <Sub Component>
                    3.1.2 <Sub Component>
                    etc.
                  3.2 Specific Description of the Component 2: <Module Name 2>
                    ...
                  3.3 Etc.

                Each detailed component/sub-component must preserve:
                - Description
                - Primary Actor
                - Secondary Actor
                - Precondition
                - Basic flow
                - Business Rules
                - Post condition
                - UI Design
                - Design Details

                Design Details must preserve these columns:
                - SL. No.
                - Request Jsp
                - Response Jsp
                - Controller
                - DAO
                - Entity Name

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

                STRICT GENERATION RULES:

                1. Return ONLY valid JSON.
                2. Do NOT return Markdown outside string values.
                3. Do NOT wrap the JSON inside ```json or any code block.
                4. Do NOT add explanations before or after the JSON.
                5. Use ONLY information supported by the supplied SRS.
                6. Do not invent project-specific facts, technologies, modules,
                   actors, database tables, controllers, DAOs, entities, or
                   infrastructure that are not supported by the SRS.
                7. If the SRS does not provide enough information for a field,
                   use "Not specified".
                8. The SDD must describe the design derived from the SRS.
                9. Do not copy unrelated project-specific content from the
                   reference SDD.
                10. The reference document defines the STRUCTURE only.
                11. Keep all sections, subsections, and nested subsections present.
                12. Generate project-specific content from the SRS.
                13. Application components and detailed component descriptions
                    must be derived from the functional requirements in the SRS.
                14. The Requirements Traceability Matrix must trace SRS
                    requirements to the corresponding SDD design components.
                15. Preserve the distinction between parent application components
                    and their detailed sub-components.
                16. Do not omit 2.2.1.1, 2.2.1.2, 2.2.1.3,
                    2.2.2.1, 2.2.2.2, 2.2.2.3,
                    2.2.3.1, 2.2.3.2, and 2.2.3.3.
                17. Do not omit any 3.x.x detailed component fields.
                18. Use formal professional language suitable for a Software
                    Design Document.
                19. Keep the generated content consistent with the SRS.
                20. Do not generate source code.

                SRS INPUT:
                %s
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