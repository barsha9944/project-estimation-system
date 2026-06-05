package com.projectestimation.backend.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsResponse {

    private long totalOpportunities;

    private long newOpportunities;

    private long estimatedOpportunities;

    private long proposalGenerated;

    private long completedOpportunities;
}