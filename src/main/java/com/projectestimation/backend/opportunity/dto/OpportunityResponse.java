package com.projectestimation.backend.opportunity.dto;

import com.projectestimation.backend.opportunity.model.ImplementationType;
import com.projectestimation.backend.opportunity.model.OpportunityStatus;
import com.projectestimation.backend.opportunity.model.Priority;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record OpportunityResponse(
        Long id,
        ImplementationType implementationType,
        List<String> platforms,
        List<String> technologyCategories,
        List<String> enterpriseContexts,
        String opportunityName,
        String clientName,
        String requirementSummary,
        Priority priority,
        LocalDate expectedDeliveryDate,
        List<String> components,
        OpportunityStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
