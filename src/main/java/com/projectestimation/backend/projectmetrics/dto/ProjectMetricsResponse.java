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
    
    private List<SprintMetricsResponse> sprints;

    // getters & setters
}