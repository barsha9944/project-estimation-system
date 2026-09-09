package com.projectestimation.backend.pmp.dto;

import java.util.List;

public record OrganizationResourcesDto(

        List<PmpItemDto> hardwareNetworking,

        List<PmpItemDto> softwareTools,

        List<PmpItemDto> manpowerCompetency,

        List<PmpItemDto> projectTeam,

        List<PmpItemDto> trainingPlan

) {
}