package com.projectestimation.backend.estimation.dto;

import lombok.Data;

@Data
public class FinalCalculationRequest {

    private Long opportunityId;

    private Double benchmarkProductivityRatio;

}