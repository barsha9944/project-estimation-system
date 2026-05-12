package com.projectestimation.backend.auth.dto;

import java.time.LocalDateTime;

public record RegisterResponse(
        Long userId,
        String fullName,
        String email,
        LocalDateTime createdAt
) {
}
