package com.projectestimation.backend.estimation.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record EstimateCalculationRequest(
        @NotBlank(message = "Project name is required") String projectName,
        @NotBlank(message = "Requirement summary is required") String requirementSummary,
        @NotNull @Valid ParameterInput parameters
) {
    public record ParameterInput(
            @DecimalMin(value = "0.1", message = "Complexity factor must be >= 0.1") double complexityFactor,
            @DecimalMin(value = "0.1", message = "Risk factor must be >= 0.1") double riskFactor,
            @DecimalMin(value = "0.1", message = "Productivity factor must be >= 0.1") double productivityFactor,
            @DecimalMin(value = "0.0", message = "Hourly rate must be >= 0") double hourlyRate,
            @Min(value = 1, message = "Team size must be at least 1") int teamSize
    ) {
    }
}
