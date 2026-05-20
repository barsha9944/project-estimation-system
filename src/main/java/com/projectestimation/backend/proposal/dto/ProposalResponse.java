package com.projectestimation.backend.proposal.dto;

import java.time.LocalDateTime;

public record ProposalResponse(
        Long proposalId,
        Long opportunityId,
        Integer version,
        String title,
        String proposalContent,
        boolean generatedByAI,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        String downloadUrl
) {
}
