package com.projectestimation.backend.projectmetrics.calculator;

import org.springframework.stereotype.Component;

import com.projectestimation.backend.common.exception.ResourceNotFoundException;
import com.projectestimation.backend.estimation.model.EstimationAnalysis;
import com.projectestimation.backend.estimation.repository.EstimationAnalysisRepository;
import com.projectestimation.backend.opportunity.model.Opportunity;
import com.projectestimation.backend.opportunity.repository.OpportunityRepository;
import com.projectestimation.backend.projectmetrics.dto.AnalysisMetricsResponse;
import com.projectestimation.backend.projectmetrics.dto.CodingMetricsResponse;
import com.projectestimation.backend.projectmetrics.dto.DesignMetricsResponse;
import com.projectestimation.backend.projectmetrics.dto.OtherActivityMetricsResponse;
import com.projectestimation.backend.projectmetrics.dto.ProjectMetricsResponse;
import com.projectestimation.backend.projectmetrics.dto.QualityMetricsResponse;
import com.projectestimation.backend.projectmetrics.dto.SitMetricsResponse;
import com.projectestimation.backend.projectmetrics.dto.SummaryMetricsResponse;
import com.projectestimation.backend.projectschedule.model.ProjectSchedule;
import com.projectestimation.backend.projectschedule.repository.ProjectScheduleRepository;
import com.projectestimation.backend.proposal.model.Proposal;
import com.projectestimation.backend.proposal.repository.ProposalRepository;

@Component
public class ProjectMetricsCalculator {

    private final OpportunityRepository opportunityRepository;

    private final EstimationAnalysisRepository estimationAnalysisRepository;

    private final ProposalRepository proposalRepository;

    private final ProjectScheduleRepository projectScheduleRepository;

    public ProjectMetricsCalculator(

            OpportunityRepository opportunityRepository,

            EstimationAnalysisRepository estimationAnalysisRepository,

            ProposalRepository proposalRepository,

            ProjectScheduleRepository projectScheduleRepository) {

        this.opportunityRepository = opportunityRepository;
        this.estimationAnalysisRepository = estimationAnalysisRepository;
        this.proposalRepository = proposalRepository;
        this.projectScheduleRepository = projectScheduleRepository;
    }

    public ProjectMetricsResponse calculate(
            Long opportunityId) {

    	Opportunity opportunity =
    	        opportunityRepository.findById(opportunityId)
    	        .orElseThrow(() ->
    	                new ResourceNotFoundException("Opportunity not found"));

    	EstimationAnalysis analysis =
    	        estimationAnalysisRepository
    	        .findByOpportunityId(opportunityId)
    	        .orElseThrow(() ->
    	                new ResourceNotFoundException("Estimation not found"));

    	Proposal proposal =
    	        proposalRepository
    	        .findFirstByOpportunity_IdOrderByVersionDesc(opportunityId)
    	        .orElseThrow(() ->
    	                new ResourceNotFoundException("Proposal not found"));

    	ProjectSchedule schedule =
    	        projectScheduleRepository
    	        .findByOpportunityId(opportunityId)
    	        .orElseThrow(() ->
    	                new ResourceNotFoundException("Project Schedule not found"));

    	SummaryMetricsResponse summary =
    	        new SummaryMetricsResponse();
    	
    	
    	summary.setProjectName(
    	        opportunity.getOpportunityName()
    	);

//    	summary.setReleaseNo(
//    	        proposal.getVersion()
//    	);
    	
    	// Barsha: need to come later
    	
    	summary.setTotalPlannedEffortWithoutPm(
    	        schedule.getEstimatedHours()
    	);

    	summary.setTotalPlannedEffort(
    	        schedule.getEstimatedHours()
    	);
    	
    	summary.setPlannedDuration(
    	        schedule.getDurationDays()
    	);
    	
    	
    	ProjectMetricsResponse response =
    	        new ProjectMetricsResponse();

    	response.setSummary(summary);
    
    	response.setSummary(new SummaryMetricsResponse());
    	response.setAnalysis(new AnalysisMetricsResponse());
    	response.setDesign(new DesignMetricsResponse());
    	response.setCoding(new CodingMetricsResponse());
    	response.setSit(new SitMetricsResponse());
    	response.setOtherActivity(new OtherActivityMetricsResponse());
    	response.setQuality(new QualityMetricsResponse());

    	return response;
    	   }

}