package com.projectestimation.backend.pmp.dto;

import java.util.List;

public record ProjectManagementDto(

        String methodology,

        List<String> lifecyclePhases,

        List<PmpItemDto> organization,

        List<PmpItemDto> resources,

        List<PmpItemDto> estimation,

        List<PmpItemDto> schedule,

        List<PmpItemDto> communication,

        List<PmpItemDto> configurationManagement

) {
}