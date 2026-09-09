package com.projectestimation.backend.pmp.dto;

public record PmpDto(

        ProjectOverviewDto projectOverview,

        ProjectManagementDto projectManagement,

        QualityManagementDto qualityManagement,

        RiskManagementDto riskManagement,

        PmpItemDto validationPlan

) {
}