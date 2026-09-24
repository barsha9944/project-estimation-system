package com.projectestimation.backend.pmp.dto;

public record BusinessObjectiveDto(
        Integer serialNumber,
        String businessObjective,
        String projectObjective,
        String relatedMetrics,
        String qppoGoalKpi
) {}