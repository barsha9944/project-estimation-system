package com.projectestimation.backend.projectmetrics.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SprintMetricsResponse {

    private Integer sprintNumber;

    private String taskName;

    // =========================
    // SPRINT SUMMARY
    // =========================

    private SummaryMetricsResponse summary;

    // =========================
    // SPRINT QUALITY / UAT
    // =========================

    private QualityMetricsResponse quality;

    // =========================
    // ANALYSIS
    // =========================

    private AnalysisMetricsResponse analysis;

    // =========================
    // DESIGN
    // =========================

    private DesignMetricsResponse design;

    // =========================
    // CODING
    // =========================

    private CodingMetricsResponse coding;

    // =========================
    // SIT
    // =========================

    private SitMetricsResponse sit;

    // =========================
    // OTHER ACTIVITY
    // =========================

    private OtherActivityMetricsResponse otherActivity;
}