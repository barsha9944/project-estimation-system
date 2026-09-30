package com.projectestimation.backend.srs.dto;

public record ProposedSystemEnvironmentsDto(
        String operationalScenario,
        EnvironmentDto developmentEnvironment,
        EnvironmentDto testingEnvironment,
        EnvironmentDto deploymentEnvironment
) {
}