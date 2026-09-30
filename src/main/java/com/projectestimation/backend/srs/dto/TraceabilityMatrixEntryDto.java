package com.projectestimation.backend.srs.dto;

public record TraceabilityMatrixEntryDto(
        String requirementId,
        String requirementDescription,
        String source,
        String module,
        String relatedUseCase,
        String validationMethod
) {
}