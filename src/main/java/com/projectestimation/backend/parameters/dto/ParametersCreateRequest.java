package com.projectestimation.backend.parameters.dto;

import com.projectestimation.backend.parameters.model.ComplexityLevel;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ParametersCreateRequest(
        @NotNull(message = "Complexity is required") ComplexityLevel complexity,
        @NotNull(message = "Risk factor is required")
        @DecimalMin(value = "0.1", message = "Risk factor must be >= 0.1") Double riskFactor,
        @NotNull(message = "Productivity factor is required")
        @DecimalMin(value = "0.1", message = "Productivity factor must be >= 0.1") Double productivityFactor,
        @NotNull(message = "Hourly rate is required")
        @DecimalMin(value = "0.0", message = "Hourly rate must be >= 0") Double hourlyRate,
        @NotBlank(message = "Currency is required")
        @Size(min = 3, max = 3, message = "Currency must be a 3-letter ISO code") String currency,
        @NotNull(message = "Team size is required")
        @Min(value = 1, message = "Team size must be at least 1") Integer teamSize
) {
}
