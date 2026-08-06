package com.projectestimation.backend.projectmetrics.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OtherActivityMetricsResponse {

    private Double actualTotal;

    private Double actualProjectManagement;

    private Double actualSupportGroup;

    private Double actualOthers;

    private Double plannedTotal;

    private Double plannedProjectManagement;

    private Double plannedSupportGroup;

    private Double plannedOthers;

}