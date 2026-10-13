package com.projectestimation.backend.dar.ai;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.projectestimation.backend.common.ai.GeminiClient;
import com.projectestimation.backend.dar.dto.DarResponseDto;

import java.util.List;

@Service
public class GeminiDarOrchestrator {

    private static final List<String> DAR_DISCUSSION_TOPICS = List.of(
            "Identified Issue",
            "Evaluation Criterion",
            "Identification of Alternative Solutions",
            "Method Used for Identification of Alternative Solutions",
            "Evaluation Methods / Evaluation of Selected Alternatives",
            "Selection of Best Alternative"
    );

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

                9. Generate discussionNotes for EXACTLY these SIX fixed topics,
                   in this exact order. Copy each topic exactly as written;
                   do not rename, remove, reorder, or replace any topic:
                   - Identified Issue
                   - Evaluation Criterion
                   - Identification of Alternative Solutions
                   - Method Used for Identification of Alternative Solutions
                   - Evaluation Methods / Evaluation of Selected Alternatives
                   - Selection of Best Alternative

                   Gemini must generate the discussion and outcome content for
                   each fixed topic based on the project evidence supplied above.
                   Do not hardcode generic discussion or outcome text. Each
                   discussion and outcome must directly match its topic and be
                   consistent with the evaluation criteria, alternatives,
                   decision-matrix scores, and recommendation.

                   For Identified Issue, explain the project-specific problem
                   or decision being considered.
                   For Evaluation Criterion, explain the relevant factors used
                   to compare options, such as risk, effort, schedule, and cost
                   when supported by the project evidence.
                   For Identification of Alternative Solutions, the discussion
                   MUST be a numbered list, with every alternative on its own
                   separate line in this exact format:
                   1. Alternative Name — short project-specific description
                   2. Alternative Name — short project-specific description
                   Continue numbering for every alternative in the alternatives
                   array. Do not write the alternatives as a single paragraph.
                   The backend will also format this discussion as a numbered
                   list from the alternatives array to guarantee consistent output.
                   For Method Used for Identification of Alternative Solutions,
                   describe suitable identification methods such as brainstorming,
                   question and answer, open discussion, and analysis without
                   claiming a method occurred unless supported by context.
                   For Evaluation Methods / Evaluation of Selected Alternatives,
                   explain how the alternatives compare using the decision matrix.
                   For Selection of Best Alternative, explain the recommendation
                   using the calculated scores and relevant trade-offs.

                   Do not invent project facts, evidence, meeting events, or
                   decisions. State uncertainty where evidence is insufficient.

                10. Generate at least SIX distinct, project-specific evaluation
                    criteria because there are six fixed discussion topics.
                    Criteria must be relevant to the selected phase and decision,
                    and supported by the SRS and SDD. Use the same criteria in
                    the same order for every alternative.

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

                14. Calculate weightedTotal for each alternative as the weighted
                    score total: sum(weight * score) across all criteria. Round
                    to two decimal places. Do not return an arbitrary total;
                    the backend will recalculate it from the criterion scores.

                15. The same criterion must use the same weight across all
                    alternatives. Each alternative must score every criterion,
                    using the same criteria in the same order. Assess alternatives
                    fairly and ensure their weighted totals are distinct whenever
                    the evidence supports meaningful differences. Never alter a
                    score solely to force a different total; justify each score.

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
                      "topic": "Identified Issue",
                      "discussion": "Project-specific discussion generated from the supplied SRS, SDD, and schedule.",
                      "outcome": "Evidence-based outcome for the identified issue."
                    },
                    {
                      "topic": "Evaluation Criterion",
                      "discussion": "Project-specific explanation of the criteria used to compare alternatives.",
                      "outcome": "Outcome that aligns with the evaluation criteria and their weights."
                    },
                    {
                      "topic": "Identification of Alternative Solutions",
                      "discussion": "Discussion of the alternatives included in the alternatives array.",
                      "outcome": "The alternatives to be compared in the decision matrix are identified."
                    },
                    {
                      "topic": "Method Used for Identification of Alternative Solutions",
                      "discussion": "Project-relevant methods used or proposed to identify alternatives, without inventing events.",
                      "outcome": "A clear, evidence-based outcome describing the identification approach."
                    },
                    {
                      "topic": "Evaluation Methods / Evaluation of Selected Alternatives",
                      "discussion": "Comparison of alternatives using the decision matrix, criteria, weights, and scores.",
                      "outcome": "A comparison outcome consistent with the calculated scores and trade-offs."
                    },
                    {
                      "topic": "Selection of Best Alternative",
                      "discussion": "Explanation of the recommended alternative based on the decision matrix.",
                      "outcome": "A recommendation consistent with recommendedAlternativeId and recommendationReason, subject to human review."
                    }
                  ],
                  "evaluationCriteria": [
                    "Scalability",
                    "Security Compliance",
                    "Maintainability",
                    "Integration Complexity",
                    "Performance",
                    "Implementation Effort"
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

            applyHardcodedDiscussionTopics(root);
            formatAlternativeSolutionsAsNumberedList(root);
            recalculateWeightedTotals(root);

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

    private void applyHardcodedDiscussionTopics(JsonNode root) {
        if (root == null || !root.isObject()) {
            throw new IllegalArgumentException(
                    "Gemini returned an invalid DAR JSON object."
            );
        }

        JsonNode notesNode = root.path("discussionNotes");
        if (!notesNode.isArray()
                || notesNode.size() != DAR_DISCUSSION_TOPICS.size()) {
            throw new IllegalArgumentException(
                    "DAR must contain exactly six discussion notes, one for each fixed topic."
            );
        }

        for (int i = 0; i < DAR_DISCUSSION_TOPICS.size(); i++) {
            JsonNode noteNode = notesNode.get(i);
            if (!(noteNode instanceof ObjectNode note)) {
                throw new IllegalArgumentException(
                        "Each DAR discussion note must be a JSON object."
                );
            }

            JsonNode discussion = note.get("discussion");
            JsonNode outcome = note.get("outcome");
            if (discussion == null || !discussion.isTextual()
                    || discussion.asText().isBlank()
                    || outcome == null || !outcome.isTextual()
                    || outcome.asText().isBlank()) {
                throw new IllegalArgumentException(
                        "Gemini must generate non-empty discussion and outcome text for topic: "
                                + DAR_DISCUSSION_TOPICS.get(i)
                );
            }

            // Topic names are backend-controlled; Gemini supplies the content.
            note.put("topic", DAR_DISCUSSION_TOPICS.get(i));
        }
    }


    private void formatAlternativeSolutionsAsNumberedList(JsonNode root) {
        JsonNode notesNode = root.path("discussionNotes");
        JsonNode alternativesNode = root.path("alternatives");

        if (!notesNode.isArray() || notesNode.size() != DAR_DISCUSSION_TOPICS.size()) {
            throw new IllegalArgumentException(
                    "DAR must contain exactly six discussion notes, one for each fixed topic."
            );
        }
        if (!alternativesNode.isArray() || alternativesNode.size() < 4) {
            throw new IllegalArgumentException(
                    "DAR must contain at least four decision alternatives."
            );
        }

        JsonNode noteNode = notesNode.get(2);
        if (!(noteNode instanceof ObjectNode note)) {
            throw new IllegalArgumentException(
                    "The Identification of Alternative Solutions note must be a JSON object."
            );
        }

        StringBuilder formatted = new StringBuilder();
        for (int i = 0; i < alternativesNode.size(); i++) {
            JsonNode alternative = alternativesNode.get(i);
            String name = alternative.path("alternativeName").asText("").trim();
            String description = alternative.path("description").asText("").trim();
            if (name.isBlank()) {
                throw new IllegalArgumentException(
                        "Every decision alternative must have a name."
                );
            }
            if (i > 0) {
                formatted.append("\n");
            }
            formatted.append(i + 1).append(". ").append(name);
            if (!description.isBlank()) {
                formatted.append(" — ").append(description);
            }
        }

        // Keep the alternatives as a numbered list instead of a paragraph.
        note.put("discussion", formatted.toString());
    }

    private void recalculateWeightedTotals(JsonNode root) {
        JsonNode alternativesNode = root.path("alternatives");
        if (!alternativesNode.isArray()) {
            throw new IllegalArgumentException(
                    "Gemini returned no decision alternatives."
            );
        }

        for (JsonNode alternativeNode : alternativesNode) {
            if (!(alternativeNode instanceof ObjectNode alternative)) {
                throw new IllegalArgumentException(
                        "Each decision alternative must be a JSON object."
                );
            }

            JsonNode scores = alternative.path("criterionScores");
            if (!scores.isArray() || scores.isEmpty()) {
                throw new IllegalArgumentException(
                        "Every alternative must contain criterion scores."
                );
            }

            double weightedSum = 0.0;
            for (JsonNode score : scores) {
                JsonNode weightNode = score.get("weight");
                JsonNode valueNode = score.get("score");
                if (weightNode == null || !weightNode.isNumber()
                        || valueNode == null || !valueNode.isNumber()) {
                    throw new IllegalArgumentException(
                            "Every criterion score must include numeric weight and score values."
                    );
                }
                weightedSum += weightNode.asDouble() * valueNode.asDouble();
            }

            double calculatedTotal = Math.round(weightedSum * 100.0) / 100.0;
            alternative.put("weightedTotal", calculatedTotal);
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

            double expectedTotal = Math.round(weightedSum * 100.0) / 100.0;

            if (alternative.weightedTotal() == null
                    || !Double.isFinite(alternative.weightedTotal())
                    || Math.abs(expectedTotal - alternative.weightedTotal()) > 0.001) {
                throw new IllegalArgumentException(
                        "Incorrect weighted total for alternative "
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

        // Do not emit duplicate total scores in the Decision Matrix.
        List<Double> totals = result.alternatives().stream()
                .map(a -> a.weightedTotal())
                .toList();
        if (totals.stream().distinct().count() != totals.size()) {
            throw new IllegalArgumentException(
                    "Decision alternatives have duplicate weighted totals. "
                            + "Regenerate the DAR with evidence-based, distinct scoring."
            );
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
