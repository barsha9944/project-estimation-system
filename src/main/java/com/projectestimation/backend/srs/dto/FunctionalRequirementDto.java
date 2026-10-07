package com.projectestimation.backend.srs.dto;

import java.util.List;

public record FunctionalRequirementDto(
        String requirementId,
        String sectionNumber,
        String module,
        String requirementName,
        String description,
        String primaryActor,
        String secondaryActor,
        List<String> preconditions,
        List<String> basicFlow,
        List<String> businessRules,
        List<BusinessRuleFieldDto> fields,
        List<String> postconditions,
        String uiDesign
) {
}