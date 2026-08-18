package com.projectestimation.backend.projectmetrics.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProjectMetricsResponse {

	private SummaryMetricsResponse summary;

    private QualityMetricsResponse quality;

    // Overall / cumulative metrics
    private AnalysisMetricsResponse analysis;

    private DesignMetricsResponse design;

    private CodingMetricsResponse coding;

    private SitMetricsResponse sit;

    private OtherActivityMetricsResponse otherActivity;

    // Individual coding sprint metrics
    private List<SprintMetricsResponse> sprints;
}