package com.projectestimation.backend.proposal.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.projectestimation.backend.auth.model.User;
import com.projectestimation.backend.common.response.ApiResponse;
import com.projectestimation.backend.proposal.dto.ProposalGenerateRequest;
import com.projectestimation.backend.proposal.dto.ProposalGenerateResponse;
import com.projectestimation.backend.proposal.dto.ProposalListResponse;
import com.projectestimation.backend.proposal.service.ProposalService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/proposals")
public class ProposalController {

    private final ProposalService proposalService;

    public ProposalController(ProposalService proposalService) {
        this.proposalService = proposalService;
    }

    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<ProposalGenerateResponse>> generate(
            @Valid @RequestBody ProposalGenerateRequest request,
            @AuthenticationPrincipal User user
    ) {
        ProposalGenerateResponse response = proposalService.generate(request, user);
        return ResponseEntity.ok(ApiResponse.success("Proposal generated successfully", response));
    }

    @GetMapping("/{proposalId}/download")
    public ResponseEntity<byte[]> download(@PathVariable Long proposalId) {
        return proposalService.download(proposalId);
    }
    
    @GetMapping("/getAllProposals")
    public ResponseEntity<ApiResponse<List<ProposalListResponse>>> getAllProposals() {

    	return ResponseEntity.ok(
    		    ApiResponse.success(
    		        "Proposals fetched successfully",
    		        proposalService.getAllProposals()
    		    )
    		);
    }
}
