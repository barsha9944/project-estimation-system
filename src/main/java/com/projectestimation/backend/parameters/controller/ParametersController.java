package com.projectestimation.backend.parameters.controller;

import com.projectestimation.backend.common.response.ApiResponse;
import com.projectestimation.backend.parameters.dto.ParametersCreateRequest;
import com.projectestimation.backend.parameters.dto.ParametersResponse;
import com.projectestimation.backend.parameters.dto.ParametersUpdateRequest;
import com.projectestimation.backend.parameters.service.ParametersService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/opportunities/{opportunityId}/parameters")
public class ParametersController {

    private final ParametersService parametersService;

    public ParametersController(ParametersService parametersService) {
        this.parametersService = parametersService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ParametersResponse>> create(
            @PathVariable Long opportunityId,
            @Valid @RequestBody ParametersCreateRequest request
    ) {
        ParametersResponse response = parametersService.createParameters(opportunityId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Parameters created successfully", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<ParametersResponse>> getByOpportunityId(
            @PathVariable Long opportunityId
    ) {
        ParametersResponse response = parametersService.getParametersByOpportunityId(opportunityId);
        return ResponseEntity.ok(ApiResponse.success("Parameters retrieved successfully", response));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<ParametersResponse>> update(
            @PathVariable Long opportunityId,
            @Valid @RequestBody ParametersUpdateRequest request
    ) {
        ParametersResponse response = parametersService.updateParameters(opportunityId, request);
        return ResponseEntity.ok(ApiResponse.success("Parameters updated successfully", response));
    }
}
