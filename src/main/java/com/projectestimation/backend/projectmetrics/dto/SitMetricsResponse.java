package com.projectestimation.backend.projectmetrics.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SitMetricsResponse {

    private Integer plannedDuration;

    private Integer actualDuration;

    private Double scheduleVariance;

    private Double plannedEffort;

    private Double actualEffort;

    private Double effortVariance;

    private Integer totalTestConditions;

    private Double testCaseWritingEffort;

    private Integer testCaseReviewDefects;

    private Double testCaseReviewEffort;

    private Double testExecutionEffort;

    private Double testCaseReviewDetectionRate;

    private Integer sitDefects;

    private Double sitEffort;

    private Double sitDetectionRate;

}