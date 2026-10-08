package com.projectestimation.backend.mom.dto;

import java.time.LocalDate;

public record MomGenerateRequest(
        LocalDate projectStartDate,
        LocalDate projectEndDate
) {
}