package com.projectestimation.backend.pmp.dto;

import java.util.List;

public record QualityControlPlanDto(

        List<PmpItemDto> standardsApplicable,

        List<PmpItemDto> productReviewTesting

) {
}