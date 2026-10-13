package com.projectestimation.backend.dar.review.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.projectestimation.backend.common.ai.GeminiClient;
import com.projectestimation.backend.dar.dto.DarResponseDto;
import com.projectestimation.backend.dar.review.dto.DarReviewDto;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class GeminiDarReviewOrchestrator {

    private final GeminiClient geminiClient;
    private final ObjectMapper objectMapper;

    public GeminiDarReviewOrchestrator(
            GeminiClient geminiClient,
            ObjectMapper objectMapper
    ) {
        this.geminiClient = geminiClient;
        this.objectMapper = objectMapper;
    }

    public DarReviewDto generateReview(
            Long opportunityId,
            DarResponseDto originalDar
    ) {
        try {
            String darJson = objectMapper.writeValueAsString(originalDar);

            String prompt = """
                    You are preparing a formal Decision Analysis and Resolution (DAR)
                    Review document based on an existing DAR meeting document.

                    Use the supplied original DAR as the primary source.
                    Do not invent facts that are not supported by the source.

                    Return ONLY valid JSON matching this structure:
                    {
                      "opportunityId": 1,
                      "issueTitle": "Issue title",
                      "meetingDateTime": "Meeting date",
                      "place": "Meeting place",
                      "darEventTrigger": "Event that triggered the DAR",
                      "decisionOwner": "Decision owner",
                      "evaluationParticipants": [
                        {"name": "Participant name", "status": "P"}
                      ],
                      "decisionParticipants": [
                        {"name": "Participant name", "status": "P"}
                      ],
                      "evaluationCriteria": [
                        {"name": "Criterion", "weight": 1}
                      ],
                      "alternatives": [
                        {
                          "alternativeId": "A1",
                          "solution": "Solution description",
                          "evaluationMethod": "Evaluation method",
                          "scores": [1, 2, 3, 4, 5],
                          "totalScore": 15
                        }
                      ],
                      "finalSolutionId": "A1",
                      "justification": "Reason for selecting the final solution",
                      "preparedBy": "",
                      "preparedDate": ""
                    }

                    Rules:
                    - opportunityId must be %d.
                    - Evaluation participants must include the Project Manager,
                      Team Lead, and all Team Members.
                    - Decision participants must include the Project Manager
                      and Team Lead only.
                    - Do not guess participant names. Use the participant names
                      provided separately by the application, if available.
                    - Participant status must be P or N. Use N if attendance
                      is not confirmed.
                    - Criterion weights must be integers from 1 to 5.
                    - Scores must correspond to the evaluation criteria.
                    - Keep the final solution consistent with the original DAR.
                    - Use an empty string for unavailable text fields.
                    - Return JSON only, without Markdown fences.

                    ORIGINAL DAR:
                    %s
                    """.formatted(opportunityId, darJson);

            String response = geminiClient.generateJsonContent(prompt, 5000);

            DarReviewDto generated =
                    objectMapper.readValue(response, DarReviewDto.class);

            return new DarReviewDto(
                    opportunityId,
                    generated.issueTitle(),
                    generated.meetingDateTime(),
                    generated.place(),
                    generated.darEventTrigger(),
                    generated.decisionOwner(),
                    safeList(generated.evaluationParticipants()),
                    safeList(generated.decisionParticipants()),
                    safeList(generated.evaluationCriteria()),
                    safeList(generated.alternatives()),
                    generated.finalSolutionId(),
                    generated.justification(),
                    generated.preparedBy(),
                    generated.preparedDate()
            );

        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to generate DAR Review using Gemini.", e
            );
        }
    }

    private <T> List<T> safeList(List<T> values) {
        return values == null ? List.of() : values;
    }
}
