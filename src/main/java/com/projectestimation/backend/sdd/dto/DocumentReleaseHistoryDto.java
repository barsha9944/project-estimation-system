package com.projectestimation.backend.sdd.dto;

import java.util.List;

public record DocumentReleaseHistoryDto(
        List<DocumentReleaseHistoryEntryDto> entries
) {}