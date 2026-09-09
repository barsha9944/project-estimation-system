package com.projectestimation.backend.pmp.dto;

import java.util.List;

public record ConfigurationManagementPlanDto(

        List<PmpItemDto> configurationItems,

        String baselining,

        String releaseProcedure,

        String versionControl,

        String statusAccounting,

        String audit,

        String backup

) {
}