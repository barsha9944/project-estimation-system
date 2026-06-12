package com.projectestimation.backend.proposal.dto;

import java.util.List;

public record OpportunityProposalDto(

        Long opportunityId,

        Long proposalCount,

        List<ProposalDetailWithCountDto> proposals

) {
}