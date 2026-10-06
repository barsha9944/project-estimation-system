package com.projectestimation.backend.sdd.dto;

public record CirculationDetailsEntryDto(
        Integer copyNumber,
        String designationOfCopyHolder,
        String locationOfCopy
) {}