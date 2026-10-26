package com.projectestimation.backend.pmp.review.dto;

import java.time.LocalDateTime;

public record PmpReviewResponseDto(
    Long id,
    Long opportunityId,
    String status,
    LocalDateTime pmpGeneratedAt,
    LocalDateTime reviewDocumentsDueAt,
    LocalDateTime followUpDueAt,
    String lastError
) {}