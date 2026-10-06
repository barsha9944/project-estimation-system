package com.projectestimation.backend.sdd.dto;

public record SystemPerspectiveDto(
        String systemContext,
        EnvironmentDto developmentTestingDeploymentEnvironment,
        String applicationComponents,
        String generalFeaturesOfProposedApplication
) {}