package com.projectestimation.backend.estimation.dto;

import java.util.List;

import lombok.Data;

@Data
public class EnvironmentalFactorCalculationRequest {

    private Long opportunityId;

    private List<EnvironmentalFactorDto> environmentalFactors;
}