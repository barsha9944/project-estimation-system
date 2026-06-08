package com.projectestimation.backend.proposal.dto;
import java.time.LocalDateTime;
public record ProposalListResponse(

        Long proposalId,
        Long opportunityId,
        String proposalName,
        String clientName,
        String proposalType,
        LocalDateTime createdAt

) {
}