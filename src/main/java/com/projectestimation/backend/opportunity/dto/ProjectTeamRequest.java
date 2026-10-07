package com.projectestimation.backend.opportunity.dto;

import java.util.List;

public record ProjectTeamRequest(
        String projectManager,
        String teamLead,
        List<String> developers,
        String tester,
        String databaseDevelopers,
        String admin,
        String hr
) {
}