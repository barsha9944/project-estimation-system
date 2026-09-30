package com.projectestimation.backend.srs.dto;

public record DocumentInformationDto(
        String documentTitle,
        String projectName,
        String clientName,
        String version,
        String date,
        String preparedBy,
        String reviewedBy,
        String approvedBy
) {
}