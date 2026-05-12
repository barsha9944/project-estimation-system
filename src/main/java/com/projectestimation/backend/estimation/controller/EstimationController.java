package com.projectestimation.backend.estimation.controller;

import com.projectestimation.backend.auth.model.User;
import com.projectestimation.backend.common.response.ApiResponse;
import com.projectestimation.backend.estimation.dto.EstimateCalculationRequest;
import com.projectestimation.backend.estimation.dto.EstimateCalculationResponse;
import com.projectestimation.backend.estimation.service.EstimationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/estimates")
public class EstimationController {

    private final EstimationService estimationService;

    public EstimationController(EstimationService estimationService) {
        this.estimationService = estimationService;
    }

    @PostMapping("/calculate")
    public ResponseEntity<ApiResponse<EstimateCalculationResponse>> calculate(
            @Valid @RequestBody EstimateCalculationRequest request,
            @AuthenticationPrincipal User user
    ) {
        EstimateCalculationResponse response = estimationService.calculate(request, user);
        return ResponseEntity.ok(ApiResponse.success("Estimate calculated successfully", response));
    }
}
