package com.projectestimation.backend.pmp.dto;

import java.util.List;

public record ProjectOverviewDto(

        String projectName,

        String projectDescription,

        String projectScope,

        List<String> objectives,

        List<String> deliverables,

        List<String> assumptions,

        List<String> constraints,

        List<String> acceptanceCriteria,

        CustomerInterfaceDto customerInterface,

        List<String> complianceRequirements,

        List<DeliverableDto> detailedDeliverables,

        List<MilestoneDto> milestones

) {
}