package com.projectestimation.backend.sdd.dto;

import java.util.List;

public record ApplicationComponentDetailsDto(
        String subComponentNumber,
        String subComponentName,
        String description,
        String primaryActor,
        String secondaryActor,
        String precondition,
        List<String> basicFlow,
        List<String> businessRules,
        String postCondition,
        String uiDesign,
        List<DesignDetailDto> designDetails,
        String sequenceDiagram
) {}