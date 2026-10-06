package com.projectestimation.backend.sdd.dto;

public record EnvironmentDto(
        EnvironmentDetailsDto developmentEnvironment,
        EnvironmentDetailsDto testingEnvironment,
        EnvironmentDetailsDto deploymentEnvironment
) {}