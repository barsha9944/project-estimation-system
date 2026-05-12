package com.projectestimation.backend.proposal.dto;

import java.time.LocalDateTime;

public record ProposalGenerateResponse(
        Long proposalId,
        String title,
        LocalDateTime generatedAt,
        String downloadUrl
) {
}
