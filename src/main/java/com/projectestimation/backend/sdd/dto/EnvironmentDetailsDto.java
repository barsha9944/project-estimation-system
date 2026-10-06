package com.projectestimation.backend.sdd.dto;

import java.util.List;

public record EnvironmentDetailsDto(
        List<EnvironmentItemDto> softwareNeeded,
        String hardwareNeeded,
        String networkNeeded
) {}