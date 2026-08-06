package com.projectestimation.backend.projectmetrics.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProjectMetricsResponse {

    private SummaryMetricsResponse summary;

    private AnalysisMetricsResponse analysis;

    private DesignMetricsResponse design;

    private CodingMetricsResponse coding;

    private SitMetricsResponse sit;

    private OtherActivityMetricsResponse otherActivity;

    private QualityMetricsResponse quality;

    // getters & setters
}