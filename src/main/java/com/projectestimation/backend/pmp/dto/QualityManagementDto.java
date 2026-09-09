package com.projectestimation.backend.pmp.dto;

import java.util.List;

public record QualityManagementDto(

        List<PmpItemDto> qualityStandards,

        List<PmpItemDto> reviews,

        List<PmpItemDto> testing,

        List<PmpItemDto> metrics

) {
}