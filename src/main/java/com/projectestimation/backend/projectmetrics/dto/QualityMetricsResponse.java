package com.projectestimation.backend.projectmetrics.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class QualityMetricsResponse {

    private Double averagePreDeliveryDefectDensity;

    private Integer uatDefects;

    private Double postDeliveryDefectDensity;

    private Double overallDefectDensity;

    private Double plannedUatEffort;

    private Double actualUatEffort;

    private Double overallDefectRate;

    private Double defectRemovalEfficiency;

}
