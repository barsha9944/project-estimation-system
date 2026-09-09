package com.projectestimation.backend.pmp.dto;

import java.util.List;

public record RiskManagementDto(

        List<PmpItemDto> risks,

        List<PmpItemDto> mitigationStrategies,

        List<PmpItemDto> contingencyPlans

) {
}