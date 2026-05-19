package com.projectestimation.backend.opportunity.controller;

import com.projectestimation.backend.common.response.ApiResponse;
import com.projectestimation.backend.opportunity.dto.*;
import com.projectestimation.backend.opportunity.service.OpportunityService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/opportunities")
public class OpportunityController {

    private final OpportunityService opportunityService;

    public OpportunityController(OpportunityService opportunityService) {
        this.opportunityService = opportunityService;
    }

    @PostMapping("/create")
    public ResponseEntity<ApiResponse<OpportunityResponse>> create(
            @Valid @RequestBody OpportunityCreateRequest request
    ) {
        OpportunityResponse response = opportunityService.createOpportunity(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Opportunity created successfully", response));
    }

    @GetMapping("/allopportunities")
    public ResponseEntity<ApiResponse<List<OpportunityListResponse>>> getAll() {
        List<OpportunityListResponse> opportunities = opportunityService.getAllOpportunities();
        return ResponseEntity.ok(ApiResponse.success("Opportunities retrieved successfully", opportunities));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OpportunityResponse>> getById(@PathVariable Long id) {
        OpportunityResponse response = opportunityService.getOpportunityById(id);
        return ResponseEntity.ok(ApiResponse.success("Opportunity retrieved successfully", response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<OpportunityResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody OpportunityUpdateRequest request
    ) {
        OpportunityResponse response = opportunityService.updateOpportunity(id, request);
        return ResponseEntity.ok(ApiResponse.success("Opportunity updated successfully", response));
    }
}
