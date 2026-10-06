package com.projectestimation.backend.sdd.dto;

import java.util.List;

public record ApplicationComponentDto(
        String componentNumber,
        String componentName,
        String description,
        List<ApplicationComponentDetailsDto> details
) {}