package com.projectestimation.backend.pmp.dto;

import java.util.List;

public record QualityManagementDto(

        List<PmpItemDto> qualityStandards,

        List<PmpItemDto> reviews,

        List<PmpItemDto> testing,

        List<PmpItemDto> metrics,

        List<PmpItemDto> qualityObjectives,

        List<PmpItemDto> audits,

        List<PmpItemDto> productReviews

) {
}