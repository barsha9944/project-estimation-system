package com.projectestimation.backend.estimation.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.projectestimation.backend.estimation.dto.ActorCalculationRequest;
import com.projectestimation.backend.estimation.dto.ActorCalculationResponse;
import com.projectestimation.backend.estimation.dto.UseCaseCalculationRequest;
import com.projectestimation.backend.estimation.dto.UseCaseCalculationResponse;
import com.projectestimation.backend.estimation.service.CalculationService;

@RestController
@RequestMapping("/api/v1/opportunities/calculate")
public class CalculationController {
	
	private final CalculationService calculationService;

    public CalculationController(CalculationService calculationService) {
        this.calculationService = calculationService;
    }
    
	@PostMapping("/calculate-actors")
	public ActorCalculationResponse calculateActors(
	        @RequestBody ActorCalculationRequest request
	) {
	    return calculationService.calculate(request);
	}
	
	@PostMapping("/calculate-usecases")
	public UseCaseCalculationResponse calculateUseCases(
	        @RequestBody UseCaseCalculationRequest request
	) {

	    return calculationService.calculate(
	            request
	    );
	}

}
