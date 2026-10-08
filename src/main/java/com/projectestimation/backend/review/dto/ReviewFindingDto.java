package com.projectestimation.backend.review.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ReviewFindingDto(
    Integer id,
    String referenceSection,
    String defect,
    String recommendation,
    String severity,
    String statusAtReview1,
    String statusAtReview2
) {
    public ReviewFindingDto withStatusAtReview2(String status2) {
        return new ReviewFindingDto(
            id(),
            referenceSection(),
            defect(),
            recommendation(),
            severity(),
            statusAtReview1(),
            status2
        );
    }
}
