package com.projectestimation.backend.proposal.dto;

import java.util.List;

public record OpportunityProposalDto(

        Long opportunityId,

        String opportunityName,

        String clientName,

        Long proposalCount,

        List<ProposalDetailWithCountDto> proposals

) {
}