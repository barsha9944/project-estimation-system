package com.projectestimation.backend.opportunity.dto;

import java.util.List;

public record ProjectTeamResponse(
        Long id,
        Long opportunityId,
        String projectManager,
        String teamLead,
        List<String> developers,
        String tester,
        String databaseDevelopers,
        String admin,
        String hr
) {
}