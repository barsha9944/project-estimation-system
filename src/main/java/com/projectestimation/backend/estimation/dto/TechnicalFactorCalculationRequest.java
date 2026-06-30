package com.projectestimation.backend.estimation.dto;

import java.util.List;

import lombok.Data;

@Data
public class TechnicalFactorCalculationRequest {

    private Long opportunityId;

    private List<TechnicalFactorDto> technicalFactors;
}