package com.projectestimation.backend.estimation.dto;

import java.util.List;

import lombok.Data;

@Data
public class UseCaseCalculationRequest {

	private Long opportunityId;
	
    private List<UseCaseDto> useCases;
}