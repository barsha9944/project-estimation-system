package com.projectestimation.backend.review.dto;

import java.time.LocalDateTime;

import com.projectestimation.backend.review.model.DocumentReviewType;
import com.projectestimation.backend.review.model.ReviewStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentReviewStatusDto {

    private Long opportunityId;
    private String opportunityName;
    private DocumentReviewType documentType;
    private LocalDateTime originalGeneratedAt;

    // Cycle 1
    private LocalDateTime cycle1ScheduledAt;
    private LocalDateTime cycle1GeneratedAt;
    private ReviewStatus cycle1Status;
    private boolean cycle1Available;
    private String cycle1FileName;

    // Cycle 2
    private LocalDateTime cycle2ScheduledAt;
    private LocalDateTime cycle2GeneratedAt;
    private ReviewStatus cycle2Status;
    private boolean cycle2Available;
    private String cycle2FileName;

    // Shared Review Data Sheet
    private LocalDateTime dataSheetScheduledAt;
    private LocalDateTime dataSheetGeneratedAt;
    private ReviewStatus dataSheetStatus;
    private boolean dataSheetAvailable;
    private String dataSheetFileName;
}
