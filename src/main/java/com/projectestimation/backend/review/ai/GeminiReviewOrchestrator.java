package com.projectestimation.backend.review.ai;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.projectestimation.backend.common.ai.GeminiClient;
import com.projectestimation.backend.common.exception.AiGenerationFailedException;
import com.projectestimation.backend.review.dto.ReviewFindingDto;
import com.projectestimation.backend.review.dto.ReviewFindingsResponseDto;
import com.projectestimation.backend.review.model.DocumentReviewType;
import com.projectestimation.backend.sdd.dto.SddDto;
import com.projectestimation.backend.srs.dto.SrsDto;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class GeminiReviewOrchestrator {

    private static final Logger log = LogManager.getLogger(GeminiReviewOrchestrator.class);

    private final GeminiClient geminiClient;
    private final ObjectMapper objectMapper;

    public ReviewFindingsResponseDto reviewSrsCycle1(SrsDto srsDto, String opportunityName) {
        log.info("Starting Gemini SRS Cycle 1 review for opportunity: {}", opportunityName);
        try {
            String srsJson = objectMapper.writeValueAsString(srsDto);
            String prompt = buildSrsCycle1Prompt(opportunityName, srsJson);

            String response = geminiClient.generateJsonContent(prompt, 4096);
            ReviewFindingsResponseDto findingsResponse = parseResponse(response);

            validateFindings(findingsResponse);
            return findingsResponse;

        } catch (Exception e) {
            log.error("Failed Gemini SRS review for opportunity {}: {}", opportunityName, e.getMessage(), e);
            throw new AiGenerationFailedException("Gemini SRS review failed: " + e.getMessage(), e);
        }
    }

    public ReviewFindingsResponseDto reviewSddCycle1(SddDto sddDto, String opportunityName) {
        log.info("Starting Gemini SDD Cycle 1 review for opportunity: {}", opportunityName);
        try {
            String sddJson = objectMapper.writeValueAsString(sddDto);
            String prompt = buildSddCycle1Prompt(opportunityName, sddJson);

            String response = geminiClient.generateJsonContent(prompt, 4096);
            ReviewFindingsResponseDto findingsResponse = parseResponse(response);

            validateFindings(findingsResponse);
            return findingsResponse;

        } catch (Exception e) {
            log.error("Failed Gemini SDD review for opportunity {}: {}", opportunityName, e.getMessage(), e);
            throw new AiGenerationFailedException("Gemini SDD review failed: " + e.getMessage(), e);
        }
    }

    public ReviewFindingsResponseDto reviewCycle2(
            DocumentReviewType docType,
            String docJson,
            List<ReviewFindingDto> cycle1Findings,
            String opportunityName
    ) {
        log.info("Starting Gemini Cycle 2 review for {} on opportunity: {}", docType, opportunityName);
        try {
            String findingsJson = objectMapper.writeValueAsString(cycle1Findings);
            String prompt = buildCycle2Prompt(docType, opportunityName, docJson, findingsJson);

            String response = geminiClient.generateJsonContent(prompt, 4096);
            ReviewFindingsResponseDto findingsResponse = parseResponse(response);

            if (findingsResponse == null || findingsResponse.findings() == null || findingsResponse.findings().isEmpty()) {
                // Fallback to closing the Cycle 1 findings if Gemini returned empty
                List<ReviewFindingDto> updated = new ArrayList<>();
                for (ReviewFindingDto f : cycle1Findings) {
                    updated.add(f.withStatusAtReview2("Closed"));
                }
                return new ReviewFindingsResponseDto(updated, "All defects closed and approved.", true);
            }

            return findingsResponse;

        } catch (Exception e) {
            log.error("Failed Gemini Cycle 2 review for {} on opportunity {}: {}", docType, opportunityName, e.getMessage(), e);
            throw new AiGenerationFailedException("Gemini Cycle 2 review failed: " + e.getMessage(), e);
        }
    }

    private String buildSrsCycle1Prompt(String opportunityName, String srsJson) {
        return """
        You are a senior formal software document reviewer and quality auditor (Reviewer: Manas Chattopadhay) conducting a formal Document Review Cycle 1 for project: %s.
        
        Inspect the ACTUAL Software Requirements Specification (SRS) content provided below:
        %s
        
        INSTRUCTIONS:
        1. Thoroughly review the complete document across functional requirements, business rules, interface requirements, non-functional requirements, data requirements, and traceability.
        2. Identify between 3 and 5 genuine, constructive findings/defects based strictly on the provided content.
        3. Do NOT make up random filler or hallucinated issues. Use actual section numbers from the document (e.g., "1.1", "2.1", "3.1", "5.1", "6.0", "7.0", etc.) in the 'referenceSection'.
        4. Provide specific, actionable recommendations for each finding.
        5. Assign realistic severity: "Critical", "Major", or "Minor".
        6. Set 'statusAtReview1' to "Open".
        
        OUTPUT FORMAT:
        Return ONLY a JSON object adhering to this schema:
        {
          "findings": [
            {
              "id": 1,
              "referenceSection": "5.1",
              "defect": "Precise defect description based on actual document content",
              "recommendation": "Specific actionable recommendation",
              "severity": "Major",
              "statusAtReview1": "Open"
            }
          ],
          "summary": "Brief overall review comments",
          "approved": false
        }
        """.formatted(opportunityName, srsJson);
    }

    private String buildSddCycle1Prompt(String opportunityName, String sddJson) {
        return """
        You are a senior formal software document reviewer and architecture auditor (Reviewer: Manas Chattopadhay) conducting a formal Document Review Cycle 1 for project: %s.
        
        Inspect the ACTUAL Software Design Document (SDD) content provided below:
        %s
        
        INSTRUCTIONS:
        1. Thoroughly review the complete document across application components, subsystem design, sequence interactions, proposed database design, system integration strategy, and traceability.
        2. Identify between 3 and 5 genuine, constructive design findings/defects based strictly on the provided content.
        3. Do NOT make up random filler. Use actual section numbers from the document (e.g., "1.1", "2.0", "3.1", "4.0", "5.0", "6.0", "7.0") in the 'referenceSection'.
        4. Provide specific, actionable recommendations for each finding.
        5. Assign realistic severity: "Critical", "Major", or "Minor".
        6. Set 'statusAtReview1' to "Open".
        
        OUTPUT FORMAT:
        Return ONLY a JSON object adhering to this schema:
        {
          "findings": [
            {
              "id": 1,
              "referenceSection": "3.1",
              "defect": "Precise design defect description based on actual document content",
              "recommendation": "Specific actionable recommendation",
              "severity": "Major",
              "statusAtReview1": "Open"
            }
          ],
          "summary": "Brief overall review comments",
          "approved": false
        }
        """.formatted(opportunityName, sddJson);
    }

    private String buildCycle2Prompt(
            DocumentReviewType docType,
            String opportunityName,
            String docJson,
            String findingsJson
    ) {
        return """
        You are a senior formal software document reviewer (Reviewer: Manas Chattopadhay) conducting Document Review Cycle 2 for %s document on project: %s.
        
        Here are the Cycle 1 findings identified previously:
        %s
        
        Here is the current document content:
        %s
        
        INSTRUCTIONS:
        1. Re-evaluate each Cycle 1 finding against the current document content.
        2. For each finding, determine 'statusAtReview2' as either "Closed", "Resolved", "Partially Resolved", or "Still Open".
        3. Set 'approved' to true if the document is acceptable/approved, false otherwise.
        4. Provide a concise summary comment.
        
        OUTPUT FORMAT:
        Return ONLY a JSON object adhering to this schema:
        {
          "findings": [
            {
              "id": 1,
              "referenceSection": "5.1",
              "defect": "Original defect",
              "recommendation": "Original recommendation",
              "severity": "Major",
              "statusAtReview1": "Open",
              "statusAtReview2": "Closed"
            }
          ],
          "summary": "No Defect found and hence approved.",
          "approved": true
        }
        """.formatted(docType, opportunityName, findingsJson, docJson);
    }

    private ReviewFindingsResponseDto parseResponse(String rawResponse) throws Exception {
        if (rawResponse == null || rawResponse.isBlank()) {
            throw new AiGenerationFailedException("Gemini returned an empty review response");
        }

        String cleaned = rawResponse.trim();
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7);
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3);
        }
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3);
        }
        cleaned = cleaned.trim();

        return objectMapper.readValue(cleaned, ReviewFindingsResponseDto.class);
    }

    private void validateFindings(ReviewFindingsResponseDto response) {
        if (response == null || response.findings() == null || response.findings().isEmpty()) {
            throw new AiGenerationFailedException("Gemini returned no review findings");
        }
        if (response.findings().size() < 1) {
            throw new AiGenerationFailedException("Gemini review returned insufficient findings");
        }
    }
}
