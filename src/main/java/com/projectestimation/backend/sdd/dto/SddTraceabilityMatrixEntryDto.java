package com.projectestimation.backend.sdd.dto;

public record SddTraceabilityMatrixEntryDto(
        String requirementId,
        String requirementDescription,
        String designComponent
) {}