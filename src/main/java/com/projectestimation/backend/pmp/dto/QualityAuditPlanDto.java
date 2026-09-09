package com.projectestimation.backend.pmp.dto;

import java.util.List;

public record QualityAuditPlanDto(

        List<PmpItemDto> audits

) {
}