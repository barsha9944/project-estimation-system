package com.projectestimation.backend.estimation.controller;

import com.projectestimation.backend.auth.model.User;
import com.projectestimation.backend.common.response.ApiResponse;
import com.projectestimation.backend.estimation.dto.EstimateCalculationResponse;
import com.projectestimation.backend.estimation.service.EstimationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/opportunities/{opportunityId}/estimate")
public class OpportunityEstimationController {

    private final EstimationService estimationService;

    public OpportunityEstimationController(EstimationService estimationService) {
        this.estimationService = estimationService;
    }

    @PostMapping("/calculate")
    public ResponseEntity<ApiResponse<EstimateCalculationResponse>> calculate(
            @PathVariable Long opportunityId,
            @AuthenticationPrincipal User user
    ) {
        EstimateCalculationResponse response = estimationService.calculateForOpportunity(opportunityId, user);
        return ResponseEntity.ok(ApiResponse.success("Estimate calculated successfully", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<EstimateCalculationResponse>> getLatest(
            @PathVariable Long opportunityId
    ) {
        EstimateCalculationResponse response = estimationService.getLatestEstimateByOpportunityId(opportunityId);
        return ResponseEntity.ok(ApiResponse.success("Estimate retrieved successfully", response));
    }
}
