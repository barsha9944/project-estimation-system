package com.projectestimation.backend.proposal.controller;

import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.projectestimation.backend.auth.model.User;
import com.projectestimation.backend.common.enums.ProposalType;
import com.projectestimation.backend.common.response.ApiResponse;
import com.projectestimation.backend.proposal.dto.ProposalDetailWithCountDto;
import com.projectestimation.backend.proposal.dto.ProposalResponse;
import com.projectestimation.backend.proposal.service.ProposalService;
import com.projectestimation.backend.proposal.dto.OpportunityProposalDto;

@RestController
@RequestMapping("/api/v1/opportunities/{opportunityId}/proposal")
public class OpportunityProposalController {

    private final ProposalService proposalService;

    public OpportunityProposalController(ProposalService proposalService) {
        this.proposalService = proposalService;
    }

//    @PostMapping("/generate")
//    public ResponseEntity<ApiResponse<ProposalResponse>> generate(
//            @PathVariable Long opportunityId,
//            @AuthenticationPrincipal User user
//    ) {
//        ProposalResponse response = proposalService.generateForOpportunity(opportunityId, user);
//        return ResponseEntity.status(HttpStatus.CREATED)
//                .body(ApiResponse.success("Proposal generated successfully", response));
//    }
    
    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<ProposalResponse>> generate(
            @PathVariable Long opportunityId,

            @RequestParam ProposalType type,

            @AuthenticationPrincipal User user
    ) {

        ProposalResponse response =
                proposalService.generateForOpportunity(
                        opportunityId,
                        type,
                        user
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(
                        ApiResponse.success(
                                "Proposal generated successfully",
                                response
                        )
                );
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
    
    
    
    @GetMapping("/getAllProposals")
    public ResponseEntity<ApiResponse<List<OpportunityProposalDto>>> getAllProposals() {

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Proposals fetched successfully",
                        proposalService.getAllProposals()
                )
        );
    }
    @GetMapping("/{proposalId}")
    public ResponseEntity<ApiResponse<ProposalResponse>> getProposal(
            @PathVariable Long proposalId
    ) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Proposal fetched successfully",
                        proposalService.getProposal(proposalId)
                )
        );
    }
    
    @GetMapping("/{proposalId}/download")
    public ResponseEntity<byte[]> downloadProposal(
            @PathVariable Long proposalId
    ) {

        return proposalService.downloadByProposalId(
                proposalId
        );
    }
    
    
    @GetMapping("/{proposalId}/images/{fileName:.+}")
    public ResponseEntity<Resource> getProposalImage(
            @PathVariable Long proposalId,
            @PathVariable String fileName
    ) {

        return proposalService.getProposalImage(
                proposalId,
                fileName
        );
    }
}
