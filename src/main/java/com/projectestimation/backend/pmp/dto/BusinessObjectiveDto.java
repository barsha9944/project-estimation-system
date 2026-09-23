package com.projectestimation.backend.pmp.dto;

public record BusinessObjectiveDto(
        Integer serialNumber,
        String businessObjective,
        String description,
        String projectObjective,
        String relatedMetrics,
        String qppoGoalKpi
) {}