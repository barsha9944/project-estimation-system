package com.projectestimation.backend.pmp.dto;

public record BusinessObjectiveDto(
        Integer serialNumber,
        String businessObjective,
        String metricIdentified,
        String organizationalGoal,
        String projectGoal
) {}