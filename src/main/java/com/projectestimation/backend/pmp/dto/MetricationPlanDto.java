package com.projectestimation.backend.pmp.dto;

import java.util.List;

public record MetricationPlanDto(

        List<PmpItemDto> criticalProcessMetrics,

        List<PmpItemDto> otherMetrics,

        List<PmpItemDto> dataCapturing

) {
}