package com.projectestimation.backend.srs.dto;

import java.util.List;

public record EnvironmentDto(
        List<EnvironmentItemDto> softwareNeeded,
        List<String> hardwareNeeded,
        List<String> networkNeeded
) {
}