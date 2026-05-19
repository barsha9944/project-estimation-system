package com.projectestimation.backend.opportunity.dto;

import com.projectestimation.backend.opportunity.model.ImplementationType;
import com.projectestimation.backend.opportunity.model.OpportunityStatus;
import com.projectestimation.backend.opportunity.model.Priority;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record OpportunityListResponse(
        Long id,
        String opportunityName,
        String clientName,
        ImplementationType implementationType,
        Priority priority,
        OpportunityStatus status,
        LocalDate expectedDeliveryDate,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
