package com.projectestimation.backend.sdd.dto;

public record DocumentReleaseHistoryEntryDto(
        Integer serialNumber,
        String versionNumber,
        String releaseDate,
        String preparedBy,
        String reviewedAndApprovedBy,
        String reasonsForRelease
) {}