package com.projectestimation.backend.sdd.dto;

import java.util.List;

public record CirculationDetailsDto(
        String circulationInstructions,
        List<CirculationDetailsEntryDto> entries
) {}