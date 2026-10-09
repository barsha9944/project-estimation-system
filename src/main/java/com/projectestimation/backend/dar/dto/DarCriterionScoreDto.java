package com.projectestimation.backend.dar.dto;

public record DarCriterionScoreDto(
        String criterionName,
        Integer weight,
        Integer score,
        String justification
) {
}