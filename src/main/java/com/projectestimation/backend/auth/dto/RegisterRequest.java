package com.projectestimation.backend.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Full name is required") String fullName,
        @Email(message = "Email format is invalid") @NotBlank(message = "Email is required") String email,
        @Size(min = 8, message = "Password must be at least 8 characters") String password
) {
}
