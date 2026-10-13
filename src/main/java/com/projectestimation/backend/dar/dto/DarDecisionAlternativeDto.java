package com.projectestimation.backend.dar.dto;

import java.util.List;

public record DarDecisionAlternativeDto(
        String alternativeId,
        String alternativeName,
        String description,
        List<DarCriterionScoreDto> criterionScores,
        Double weightedTotal
) {
}