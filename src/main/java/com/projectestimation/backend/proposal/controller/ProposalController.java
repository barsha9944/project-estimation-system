package com.projectestimation.backend.proposal.controller;

import com.projectestimation.backend.auth.model.User;
import com.projectestimation.backend.common.response.ApiResponse;
import com.projectestimation.backend.proposal.dto.ProposalGenerateRequest;
import com.projectestimation.backend.proposal.dto.ProposalGenerateResponse;
import com.projectestimation.backend.proposal.service.ProposalService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

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
}
