package com.projectestimation.backend.sdd.dto;

public record SystemIntegrationStrategyDto(
        String desiredStrategyForIntegrationOfProductModules,
        String desiredSequenceAndCriteriaForIntegrationTesting
) {}