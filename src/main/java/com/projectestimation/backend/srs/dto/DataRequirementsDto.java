package com.projectestimation.backend.srs.dto;

import java.util.List;

public record DataRequirementsDto(
        String dataStructuresAndRelationships,
        List<DataRequirementItemDto> inputs,
        List<DataRequirementItemDto> outputs,
        String interFunctionalDataDefinitions,
        String componentCrossReference
) {
}