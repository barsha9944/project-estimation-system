package com.projectestimation.backend.opportunity.dto;

import com.projectestimation.backend.opportunity.model.ImplementationType;
import com.projectestimation.backend.opportunity.model.OpportunityStatus;
import com.projectestimation.backend.opportunity.model.Priority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record OpportunityUpdateRequest(
        @NotNull(message = "Implementation type is required") ImplementationType implementationType,
        @NotNull(message = "Platforms are required") List<String> platforms,
        @NotNull(message = "Technology categories are required") List<String> technologyCategories,
        @NotNull(message = "Enterprise contexts are required") List<String> enterpriseContexts,
        @NotBlank(message = "Opportunity name is required") String opportunityName,
        @NotBlank(message = "Client name is required") String clientName,
        @NotBlank(message = "Requirement summary is required") @Size(max = 5000, message = "Requirement summary must not exceed 5000 characters") String requirementSummary,
        @NotNull(message = "Priority is required") Priority priority,
        LocalDate expectedDeliveryDate,
        @NotNull(message = "Components are required") List<String> components,
        OpportunityStatus status
) {
}
