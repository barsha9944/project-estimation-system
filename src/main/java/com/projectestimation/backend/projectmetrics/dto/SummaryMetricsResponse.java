package com.projectestimation.backend.projectmetrics.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SummaryMetricsResponse {

    private String projectName;

    private String releaseNo;

    private Double originalSize;

    private Double actualSize;

    private Double sizeVariance;

    private Double totalPlannedEffortWithoutPm;

    private Double totalPlannedEffort;

    private Double totalActualEffortWithoutPm;

    private Double totalActualEffort;

    private Double effortVariance;

    private Integer plannedDuration;

    private Integer actualDuration;

    private Double scheduleVariance;

    private Double actualOverallProductivity;

    private Double reviewEffectiveness;

    private Double testingEffectiveness;

}