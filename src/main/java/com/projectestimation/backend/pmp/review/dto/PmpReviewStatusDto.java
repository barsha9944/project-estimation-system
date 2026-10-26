package com.projectestimation.backend.pmp.review.dto;

import java.time.LocalDateTime;
import java.util.List;

public record PmpReviewStatusDto(
    Long reviewId,
    Long opportunityId,
    String status,
    LocalDateTime pmpGeneratedAt,
    LocalDateTime reviewDocumentsDueAt,
    LocalDateTime followUpDueAt,
    List<DocumentStatusDto> documents
) {
    public record DocumentStatusDto(
        Long id,
        String documentType,
        String status,
        LocalDateTime generatedAt,
        String lastError
    ) {}
}