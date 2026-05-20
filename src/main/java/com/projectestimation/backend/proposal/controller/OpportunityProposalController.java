package com.projectestimation.backend.proposal.controller;

import com.projectestimation.backend.auth.model.User;
import com.projectestimation.backend.common.response.ApiResponse;
import com.projectestimation.backend.proposal.dto.ProposalResponse;
import com.projectestimation.backend.proposal.service.ProposalService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/opportunities/{opportunityId}/proposal")
public class OpportunityProposalController {

    private final ProposalService proposalService;

    public OpportunityProposalController(ProposalService proposalService) {
        this.proposalService = proposalService;
    }

    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<ProposalResponse>> generate(
            @PathVariable Long opportunityId,
            @AuthenticationPrincipal User user
    ) {
        ProposalResponse response = proposalService.generateForOpportunity(opportunityId, user);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Proposal generated successfully", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<ProposalResponse>> getLatest(@PathVariable Long opportunityId) {
        ProposalResponse response = proposalService.getLatestProposalByOpportunityId(opportunityId);
        return ResponseEntity.ok(ApiResponse.success("Proposal retrieved successfully", response));
    }

    @GetMapping("/download")
    public ResponseEntity<byte[]> download(@PathVariable Long opportunityId) {
        return proposalService.downloadLatestByOpportunityId(opportunityId);
    }
}
