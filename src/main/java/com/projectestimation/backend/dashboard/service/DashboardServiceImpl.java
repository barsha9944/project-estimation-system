package com.projectestimation.backend.dashboard.service;

import com.projectestimation.backend.dashboard.dto.DashboardStatsResponse;
import com.projectestimation.backend.opportunity.model.OpportunityStatus;
import com.projectestimation.backend.opportunity.repository.OpportunityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final OpportunityRepository opportunityRepository;

    @Override
    public DashboardStatsResponse getDashboardStats() {

        return DashboardStatsResponse.builder()
                .totalOpportunities(opportunityRepository.count())
                .newOpportunities(
                        opportunityRepository.countByStatus(OpportunityStatus.NEW))
                .estimatedOpportunities(
                        opportunityRepository.countByStatus(OpportunityStatus.ESTIMATED))
                .proposalGenerated(
                        opportunityRepository.countByStatus(OpportunityStatus.PROPOSAL_GENERATED))
                .completedOpportunities(
                        opportunityRepository.countByStatus(OpportunityStatus.COMPLETED))
                .build();
    }
}