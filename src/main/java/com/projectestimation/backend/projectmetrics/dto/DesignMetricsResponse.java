package com.projectestimation.backend.projectmetrics.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DesignMetricsResponse {

    private Integer plannedDuration;

    private Integer actualDuration;

    private Double scheduleVariance;

    private Double plannedEffort;

    private Double actualEffort;

    private Double productivity;

    private Double effortVariance;

    private Double effortInAnalysis;

    private Integer reviewDefects;

    private Double reviewEffort;

    private Double defectDensity;

    private Double defectDetectionRate;

    private Double defectRate;

}