package com.projectestimation.backend.dar.ai;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.projectestimation.backend.common.ai.GeminiClient;
import com.projectestimation.backend.dar.dto.DarResponseDto;

import java.util.List;

@Service
public class GeminiDarOrchestrator {

    private final GeminiClient geminiClient;
    private final ObjectMapper objectMapper;

    public GeminiDarOrchestrator(
            GeminiClient geminiClient,
            ObjectMapper objectMapper
    ) {
        this.geminiClient = geminiClient;
        this.objectMapper = objectMapper;
    }

    public DarResponseDto generate(
            Long opportunityId,
            String projectName,
            String srsContext,
            String sddContext,
            String scheduleContext,
            List<String> invitees
    ) {
        String prompt = """
                You are a senior software architect preparing a Decision
                Analysis and Resolution (DAR) for a software project.

                OPPORTUNITY ID:
                %d

                PROJECT NAME:
                %s

                SOFTWARE REQUIREMENTS SPECIFICATION (SRS):
                %s

                SOFTWARE DESIGN DOCUMENT (SDD):
                %s

                CURRENT PROJECT SCHEDULE:
                %s

                ELIGIBLE PROJECT TEAM INVITEES:
                %s

                REQUIREMENTS:

                1. Determine the current phase from actual schedule status,
                   task progress, task breakdowns and dates.

                2. The phase must be exactly one of:
                   "Requirement Analysis"
                   "Solution Design"

                3. If Requirement Analysis is incomplete, prioritize it.
                   If Requirement Analysis is complete and Solution Design
                   is incomplete, select Solution Design.

                4. If both phases are complete, do not invent an active phase.
                   Explain that no active phase is available.

                5. For Requirement Analysis, focus on requirements,
                   ambiguities, completeness, scope, risks and decisions.

                6. For Solution Design, use both SRS and SDD to focus on
                   architecture, design alternatives, interfaces,
                   technical risks and design decisions.

                7. Create a meaningful meeting name and phase justification.

                8. Generate exactly ONE agenda item covering the main decision
                   to be evaluated. Include a clear topic and a detailed,
                   project-specific description. Do not generate additional
                   agenda items.

                9. Generate at least FOUR distinct discussion notes. Prefer
                   4 to 6 notes when supported by the SRS and SDD.
                   Each note must contain a meaningful topic, detailed
                   discussion, and clear outcome. Cover different relevant
                   aspects such as architecture, scalability, security,
                   maintainability, integration, implementation complexity,
                   performance, cost, or risks as supported by the evidence.
                   Avoid duplicate or repetitive points. Do not fabricate
                   requirements, facts, or decisions.

                10. Generate at least FOUR distinct, project-specific
                    evaluation criteria. Prefer 5 to 6 criteria when supported
                    by the SRS and SDD. Criteria must be relevant to the
                    selected phase and decision being evaluated.

                11. Generate at least FOUR distinct decision alternatives.
                    Prefer 4 to 6 alternatives when supported by the SRS and
                    SDD. The alternatives must represent meaningfully different,
                    feasible approaches to the decision, not renamed duplicates.
                    Give every alternative a unique alternativeId such as A1,
                    A2, A3, and A4. The alternatives array must contain at least
                    four objects. Do not return only two alternatives.

                12. Every evaluation criterion weight must be an integer
                    between 1 and 5 inclusive.

                13. Every alternative's score for each criterion must be an
                    integer between 1 and 5 inclusive.

                14. Calculate weightedTotal for each alternative as:
                    sum(weight * score) / sum(weight).
                    Round to two decimal places.

                15. The same criterion must use the same weight across all
                    alternatives. Each alternative must score every criterion,
                    using the same criteria in the same order.

                16. Recommend one of the generated alternatives using the
                    calculated totals. Explain the reasoning and uncertainty.

                17. Do not claim the recommendation has already been approved.
                    It requires human review.

                18. Use only the invitees provided above. Do not invent names.

                19. Do not invent dates, schedule status, project facts,
                    decisions, evidence, or implementation details.

                20. If evidence is insufficient, explain the uncertainty
                    instead of fabricating facts. If fewer than four genuinely
                    distinct alternatives can be supported, still provide four
                    reasonable options and clearly state assumptions or
                    uncertainties rather than inventing project facts.

                RETURN VALID JSON ONLY with this structure:

                {
                  "projectPhase": "Requirement Analysis",
                  "phaseJustification": "...",
                  "meetingName": "...",
                  "agenda": [
                    {
                      "topic": "Main Decision Evaluation",
                      "description": "Evaluate the project-specific alternatives against the identified evaluation criteria.",
                      "durationMinutes": 15
                    }
                  ],
                  "discussionNotes": [
                    {
                      "topic": "Architecture and Scalability",
                      "discussion": "...",
                      "outcome": "..."
                    },
                    {
                      "topic": "Security and Compliance",
                      "discussion": "...",
                      "outcome": "..."
                    },
                    {
                      "topic": "Maintainability and Complexity",
                      "discussion": "...",
                      "outcome": "..."
                    },
                    {
                      "topic": "Integration and Implementation Risks",
                      "discussion": "...",
                      "outcome": "..."
                    }
                  ],
                  "evaluationCriteria": [
                    "Scalability",
                    "Security Compliance",
                    "Maintainability",
                    "Integration Complexity",
                    "Performance"
                  ],
                  "alternatives": [
                    {
                      "alternativeId": "A1",
                      "alternativeName": "...",
                      "description": "...",
                      "criterionScores": [
                        {
                          "criterionName": "...",
                          "weight": 3,
                          "score": 4,
                          "justification": "..."
                        }
                      ],
                      "weightedTotal": 4.0
                    }
                  ],
                  "recommendedAlternativeId": "A1",
                  "recommendationReason": "..."
                }

                Do not return meetingDate, meetingTime, opportunityId,
                invitees, documentName, id, createdAt or updatedAt.
                The backend controls those fields.

                If no active phase is available, return
                "projectPhase": "NO_ACTIVE_PHASE" and explain why.
                """.formatted(
                    opportunityId,
                    projectName,
                    srsContext,
                    sddContext,
                    scheduleContext,
                    String.join(", ", invitees)
                );

        try {
            String response =
                    geminiClient.generateJsonContent(prompt, 4096);

            JsonNode root = objectMapper.readTree(stripMarkdown(response));

            String phase = requiredText(root, "projectPhase");

            if ("NO_ACTIVE_PHASE".equals(phase)) {
                throw new IllegalStateException(
                        "Gemini determined that no active phase is available: "
                                + root.path("phaseJustification").asText()
                );
            }

            if (!"Requirement Analysis".equals(phase)
                    && !"Solution Design".equals(phase)) {
                throw new IllegalArgumentException(
                        "Unsupported DAR phase returned by Gemini: " + phase
                );
            }

            DarResponseDto result = objectMapper.treeToValue(
                    root,
                    DarResponseDto.class
            );

            validate(result);

            return result;

        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to generate DAR using Gemini: " + e.getMessage(),
                    e
            );
        }
    }

    private void validate(DarResponseDto result) {
        if (result.meetingName() == null
                || result.meetingName().isBlank()) {
            throw new IllegalArgumentException(
                    "Gemini returned an empty meeting name."
            );
        }

        if (result.agenda() == null || result.agenda().size() != 1) {
            throw new IllegalArgumentException(
                    "DAR must contain exactly one agenda item."
            );
        }

        if (result.discussionNotes() == null
                || result.discussionNotes().size() < 4) {
            throw new IllegalArgumentException(
                    "DAR must contain at least four discussion notes."
            );
        }

        if (result.evaluationCriteria() == null
                || result.evaluationCriteria().size() < 4) {
            throw new IllegalArgumentException(
                    "DAR must contain at least four evaluation criteria."
            );
        }

        int discussionCount = result.discussionNotes().size();
        int criteriaCount = result.evaluationCriteria().size();
        int requiredCriteria = Math.min(discussionCount, 6);

        if (criteriaCount < requiredCriteria) {
            throw new IllegalArgumentException(
                    "Decision Matrix must contain at least "
                            + requiredCriteria
                            + " criteria for "
                            + discussionCount
                            + " discussion notes."
            );
        }

        if (result.alternatives() == null
                || result.alternatives().size() < 4) {
            throw new IllegalArgumentException(
                    "DAR must contain at least four decision alternatives."
            );
        }

        List<String> criteria = result.evaluationCriteria();

        for (var alternative : result.alternatives()) {
            if (alternative.criterionScores() == null
                    || alternative.criterionScores().size() != criteria.size()) {
                throw new IllegalArgumentException(
                        "Every alternative must score every evaluation criterion."
                );
            }

            double weightedSum = 0;
            double totalWeight = 0;

            for (String criterion : criteria) {
                long matches = alternative.criterionScores().stream()
                        .filter(s -> criterion.equals(s.criterionName()))
                        .count();

                if (matches != 1) {
                    throw new IllegalArgumentException(
                            "Each alternative must have exactly one score for "
                                    + criterion
                    );
                }
            }

            for (var score : alternative.criterionScores()) {
                if (score.weight() == null
                        || score.weight() < 1
                        || score.weight() > 5) {
                    throw new IllegalArgumentException(
                            "Criterion weights must be between 1 and 5."
                    );
                }

                if (score.score() == null
                        || score.score() < 1
                        || score.score() > 5) {
                    throw new IllegalArgumentException(
                            "Criterion scores must be between 1 and 5."
                    );
                }

                weightedSum += score.weight() * score.score();
                totalWeight += score.weight();
            }

            if (totalWeight == 0) {
                throw new IllegalArgumentException(
                        "The decision matrix has no valid weights."
                );
            }

            double expectedTotal = Math.round(
                    (weightedSum / totalWeight) * 100.0
            ) / 100.0;

            if (alternative.weightedTotal() == null
                    || !Double.isFinite(alternative.weightedTotal())) {
                throw new IllegalArgumentException(
                        "Missing or invalid weighted total for alternative "
                                + alternative.alternativeId()
                );
            }
        }

        // Every criterion must use the same weight across alternatives.
        for (String criterion : criteria) {
            Integer expectedWeight = result.alternatives().get(0)
                    .criterionScores().stream()
                    .filter(s -> criterion.equals(s.criterionName()))
                    .findFirst()
                    .orElseThrow()
                    .weight();

            for (var alternative : result.alternatives()) {
                Integer actualWeight = alternative.criterionScores().stream()
                        .filter(s -> criterion.equals(s.criterionName()))
                        .findFirst()
                        .orElseThrow()
                        .weight();

                if (!expectedWeight.equals(actualWeight)) {
                    throw new IllegalArgumentException(
                            "Weight must be consistent for criterion "
                                    + criterion
                    );
                }
            }
        }

        boolean recommendationExists = result.alternatives().stream()
                .anyMatch(a -> a.alternativeId().equals(
                        result.recommendedAlternativeId()
                ));

        if (!recommendationExists) {
            throw new IllegalArgumentException(
                    "The recommended alternative is not present in the matrix."
            );
        }
    }

    private String requiredText(JsonNode node, String field) {
        JsonNode value = node.get(field);

        if (value == null || value.isNull() || value.asText().isBlank()) {
            throw new IllegalArgumentException(
                    "Gemini response is missing required field: " + field
            );
        }

        return value.asText().trim();
    }

    private String stripMarkdown(String response) {
        if (response == null || response.isBlank()) {
            throw new IllegalArgumentException(
                    "Gemini returned an empty response."
            );
        }

        String cleaned = response.trim();

        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7);
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3);
        }

        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3);
        }

        return cleaned.trim();
    }
}
