package com.projectestimation.backend.pmp.dto;

import java.util.List;

public record MetricationPlanDto(

        // 11.1 Project Goals / Organization Goals
        List<MetricGoalDto> projectGoals,

        // 11.2 Goals for Critical Processes / Sub-processes
        List<CriticalProcessMetricDto> criticalProcessMetrics,

        // 11.3 Other Metrics for the Project
        List<OtherMetricDto> otherMetrics,

        // 11.4 Metrics Data Capturing
        List<MetricDataCaptureDto> dataCapturing

) {

    public record MetricGoalDto(
            Integer serialNumber,
            String businessObjective,
            String metricIdentified,
            String organizationalGoal,
            String projectGoal,
            String respectivePpm
    ) {
    }

    public record CriticalProcessMetricDto(
            Integer serialNumber,
            String projectGoal,
            String relevantCriticalProcesses,
            String metricsForCriticalProcess,
            String upperSpecificationLimit,
            String mean,
            String lowerSpecificationLimit,
            String periodicityOfAnalysisAndReview
    ) {
    }

    public record OtherMetricDto(
            Integer serialNumber,
            String metricName,
            String organizationalGoal,
            String projectGoal,
            String periodicityOfAnalysisAndReview,
            String reasonsForDeviation
    ) {
    }

    public record MetricDataCaptureDto(
            Integer serialNumber,
            String inputData,
            String source,
            String whenToCapture,
            String remarks
    ) {
    }
}