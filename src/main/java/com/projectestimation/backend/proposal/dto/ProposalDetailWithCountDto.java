package com.projectestimation.backend.proposal.dto;

import java.time.LocalDateTime;

import com.projectestimation.backend.common.enums.ProposalType;

public record ProposalDetailWithCountDto(

        Long proposalId,

        Long opportunityId,

        String opportunityName,

        String clientName,

        ProposalType proposalType,

        LocalDateTime createdAt,

        Long proposalCount

) {
}