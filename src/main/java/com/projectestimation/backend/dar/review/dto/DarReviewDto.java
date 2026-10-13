package com.projectestimation.backend.dar.review.dto;

import java.util.List;

public record DarReviewDto(
        Long opportunityId,
        String issueTitle,
        String meetingDateTime,
        String place,
        String darEventTrigger,
        String decisionOwner,
        List<ParticipantDto> evaluationParticipants,
        List<ParticipantDto> decisionParticipants,
        List<EvaluationCriterionDto> evaluationCriteria,
        List<AlternativeDto> alternatives,
        String finalSolutionId,
        String justification,
        String preparedBy,
        String preparedDate
) {
    public record ParticipantDto(
            String name,
            String status
    ) {}

    public record EvaluationCriterionDto(
            String name,
            int weight
    ) {}

    public record AlternativeDto(
            String alternativeId,
            String solution,
            String evaluationMethod,
            List<Integer> scores,
            double totalScore
    ) {}
}
