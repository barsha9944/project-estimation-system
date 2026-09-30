package com.projectestimation.backend.pmp.dto;

import java.util.List;

public record ProjectManagementDto(

        List<String> projectLifeCyclePhases,

        List<PmpItemDto> softwareLifeCyclePhases,

        List<PmpItemDto> criticalProcesses,

        List<PmpItemDto> processGoals,

        List<PmpItemDto> tailoredProcesses,

        List<PmpItemDto> darProcess,

        List<String> qualityObjectives

) {
}