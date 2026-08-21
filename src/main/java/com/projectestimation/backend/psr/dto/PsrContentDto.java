package com.projectestimation.backend.psr.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PsrContentDto {

    private List<PsrActivityDto> activitiesPerformed;

    private List<PsrActivityDto> nextWeekPlannedActivities;

    private String riskStatus;

    private String trainingOfProjectTeamMembers;

    private String issuesManagementAttention;

    private String reportingStartDate;

    private String reportingEndDate;
    
    private String reportDate;
}