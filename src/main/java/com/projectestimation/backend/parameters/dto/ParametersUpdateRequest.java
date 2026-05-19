package com.projectestimation.backend.parameters.dto;

import com.projectestimation.backend.parameters.model.ComplexityLevel;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ParametersUpdateRequest(
        @NotNull(message = "Complexity is required") ComplexityLevel complexity,
        @NotNull(message = "Risk factor is required")
        @DecimalMin(value = "0.1", message = "Risk factor must be >= 0.1") Double riskFactor,
        @NotNull(message = "Productivity factor is required")
        @DecimalMin(value = "0.1", message = "Productivity factor must be >= 0.1") Double productivityFactor,
        @NotNull(message = "Hourly rate is required")
        @DecimalMin(value = "0.0", message = "Hourly rate must be >= 0") Double hourlyRate,
        @NotNull(message = "Team size is required")
        @Min(value = 1, message = "Team size must be at least 1") Integer teamSize
) {
}
