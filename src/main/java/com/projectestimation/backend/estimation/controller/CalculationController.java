package com.projectestimation.backend.estimation.controller;

import java.io.IOException;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.projectestimation.backend.estimation.dto.ActorCalculationRequest;
import com.projectestimation.backend.estimation.dto.ActorCalculationResponse;
import com.projectestimation.backend.estimation.dto.EnvironmentalFactorCalculationRequest;
import com.projectestimation.backend.estimation.dto.EnvironmentalFactorCalculationResponse;
import com.projectestimation.backend.estimation.dto.EstimationResponse;
import com.projectestimation.backend.estimation.dto.FinalCalculationRequest;
import com.projectestimation.backend.estimation.dto.FinalCalculationResponse;
import com.projectestimation.backend.estimation.dto.TechnicalFactorCalculationRequest;
import com.projectestimation.backend.estimation.dto.TechnicalFactorCalculationResponse;
import com.projectestimation.backend.estimation.dto.UseCaseCalculationRequest;
import com.projectestimation.backend.estimation.dto.UseCaseCalculationResponse;
import com.projectestimation.backend.estimation.service.CalculationService;
import com.projectestimation.backend.opportunity.dto.DownloadEstimateRequest;

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
	
	@PostMapping("/calculate-technical-factors")
	public TechnicalFactorCalculationResponse calculateTechnicalFactors(
	        @RequestBody TechnicalFactorCalculationRequest request
	) {
	    return calculationService.calculate(request);
	}
	
	@PostMapping("/calculate-environmental-factors")
	public EnvironmentalFactorCalculationResponse calculateEnvironmentalFactors(
	        @RequestBody EnvironmentalFactorCalculationRequest request
	) {

	    return calculationService.calculate(request);
	}
	
	@PostMapping("/calculate-final")
	public FinalCalculationResponse calculateFinal(
	        @RequestBody FinalCalculationRequest request
	) {
	    return calculationService.calculateFinal(request);
	}
	
	@PostMapping("/download-estimate")
	public ResponseEntity<byte[]> downloadEstimate(
	        @RequestBody DownloadEstimateRequest request)
	        throws IOException {

	    byte[] excel = calculationService.downloadEstimate(request);

//	    return ResponseEntity.ok()
//	            .header(
//	                    HttpHeaders.CONTENT_DISPOSITION,
//	                    "attachment; filename=Estimate.xlsx")
//	            .contentType(
//	                    MediaType.parseMediaType(
//	                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
//	            .body(excel);
	    
	    String fileName =
	            calculationService.getEstimateFileName(request);

	    return ResponseEntity.ok()
	            .header(
	                    HttpHeaders.CONTENT_DISPOSITION,
	                    "attachment; filename=\"" + fileName + "\"")
	            .contentType(
	                    MediaType.parseMediaType(
	                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
	            .body(excel);
	}

	
	@GetMapping("/{opportunityId}/estimation")
	public ResponseEntity<EstimationResponse> getEstimation(
	        @PathVariable Long opportunityId) {

	    return ResponseEntity.ok(
	            calculationService.getEstimation(opportunityId));
	}
}
