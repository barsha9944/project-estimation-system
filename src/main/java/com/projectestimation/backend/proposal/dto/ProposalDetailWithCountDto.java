package com.projectestimation.backend.proposal.dto;

import java.time.LocalDateTime;

import com.projectestimation.backend.common.enums.ProposalType;

public record ProposalDetailWithCountDto(

        Long proposalId,
        Long opportunityId,
        ProposalType proposalType,
        LocalDateTime createdAt,
        Long proposalCount

) {
}