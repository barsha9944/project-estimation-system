package com.projectestimation.backend.opportunity.dto;

import java.time.LocalDate;
import java.util.List;

import com.projectestimation.backend.opportunity.model.ImplementationType;
import com.projectestimation.backend.opportunity.model.Priority;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record OpportunityCreateRequest(
		@NotNull(message = "Implementation type is required") ImplementationType implementationType,
		@NotNull(message = "Platforms are required") List<String> platforms,
		@NotNull(message = "Technology categories are required") List<String> technologyCategories,
		@NotNull(message = "Enterprise contexts are required") List<String> enterpriseContexts,
		@NotBlank(message = "Opportunity name is required") String opportunityName,
		@NotBlank(message = "Client name is required") String clientName, String requirementSummary,
		@NotNull(message = "Priority is required") Priority priority, LocalDate expectedDeliveryDate,
		@NotNull(message = "Components are required") List<String> components, FileStorageDto attachmendDta) {
}
