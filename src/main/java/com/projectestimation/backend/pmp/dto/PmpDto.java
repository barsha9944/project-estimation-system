package com.projectestimation.backend.pmp.dto;

public record PmpDto(

        ProjectOverviewDto projectOverview,

        ProjectManagementDto projectManagement,

        QualityManagementDto qualityManagement,

        RiskManagementDto riskManagement,

        ValidationPlanDto validationPlan,

        DocumentControlDto documentControl,

        EnvironmentDto environment,

        OrganizationResourcesDto organizationResources,

        MonitoringControlDto monitoringControl,

        InterGroupSupportDto interGroupSupport,

        EstimatedSizeEffortDto estimatedSizeEffort,

        ScheduleDto schedule,

        MetricationPlanDto metricationPlan,

        QualityControlPlanDto qualityControlPlan,

        QualityAuditPlanDto qualityAuditPlan,

        ConfigurationManagementPlanDto configurationManagementPlan

) {
}