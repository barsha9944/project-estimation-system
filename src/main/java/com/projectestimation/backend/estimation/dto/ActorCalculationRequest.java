package com.projectestimation.backend.estimation.dto;

import java.util.List;

import lombok.Data;

@Data
public class ActorCalculationRequest {
	
	private Long opportunityId;

    private List<ActorDto> actors;
}