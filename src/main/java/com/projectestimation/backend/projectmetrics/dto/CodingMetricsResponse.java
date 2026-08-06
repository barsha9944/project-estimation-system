package com.projectestimation.backend.projectmetrics.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CodingMetricsResponse {

    private Integer plannedDuration;

    private Integer actualDuration;

    private Double scheduleVariance;

    private Double plannedEffort;

    private Double actualEffort;

    private Double effortVariance;

    private Double codingEffort;

    private Integer codeReviewDefects;

    private Double codeReviewEffort;

    private Double defectDensity;

    private Double codeReviewDetectionRate;

    private Integer unitTestingDefects;

    private Double unitTestingEffort;

    private Double unitTestingDetectionRate;

    private Double defectRate;

    private Double productivity;

}
