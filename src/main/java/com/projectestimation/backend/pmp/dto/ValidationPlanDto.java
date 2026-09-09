package com.projectestimation.backend.pmp.dto;

import java.util.List;

public record ValidationPlanDto(

        String name,

        String description,

        String responsible,

        String timing,

        String target,

        String status,

        List<PmpItemDto> activities

) {
}