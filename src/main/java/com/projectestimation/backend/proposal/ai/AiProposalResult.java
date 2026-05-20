package com.projectestimation.backend.proposal.ai;

public record AiProposalResult(
        String executiveSummary,
        String projectOverview,
        String scopeOfWork,
        String technologyStack,
        String componentsAndFeatures,
        String deliveryApproach,
        String teamStructure,
        String timelineEstimate,
        String costEstimate,
        String risksAndAssumptions,
        String supportAndMaintenance,
        String conclusion,
        String proposalContent
) {
}
