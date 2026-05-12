package com.projectestimation.backend.proposal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ProposalGenerateRequest(
        @NotNull Long estimateId,
        @NotBlank(message = "Proposal title is required") String proposalTitle,
        String notes
) {
}
