package com.projectestimation.backend.projectschedule.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.projectestimation.backend.auth.model.User;
import com.projectestimation.backend.estimation.model.EstimationActor;
import com.projectestimation.backend.estimation.model.EstimationAnalysis;
import com.projectestimation.backend.estimation.model.EstimationUseCase;
import com.projectestimation.backend.estimation.repository.EstimationActorRepository;
import com.projectestimation.backend.estimation.repository.EstimationAnalysisRepository;
import com.projectestimation.backend.estimation.repository.EstimationUseCaseRepository;
import com.projectestimation.backend.opportunity.model.Opportunity;
import com.projectestimation.backend.opportunity.repository.OpportunityRepository;
import com.projectestimation.backend.projectschedule.ai.AiProjectScheduleResult;
import com.projectestimation.backend.projectschedule.ai.GeminiProjectScheduleOrchestrator;
import com.projectestimation.backend.projectschedule.dto.GenerateProjectScheduleRequest;
import com.projectestimation.backend.projectschedule.dto.ProjectScheduleResponse;
import com.projectestimation.backend.proposal.model.Proposal;
import com.projectestimation.backend.proposal.repository.ProposalRepository;

import lombok.RequiredArgsConstructor;


@Service
@RequiredArgsConstructor
public class ProjectScheduleService {
	
	private final OpportunityRepository opportunityRepository;

	private final EstimationAnalysisRepository estimationAnalysisRepository;

	private final EstimationActorRepository estimationActorRepository;

	private final EstimationUseCaseRepository estimationUseCaseRepository;

	private final ProposalRepository proposalRepository;

	private final GeminiProjectScheduleOrchestrator orchestrator;

    public ProjectScheduleResponse generateProjectSchedule(

            Long opportunityId,

            GenerateProjectScheduleRequest request,

            User user

    ) {

    	Opportunity opportunity =
    	        opportunityRepository
    	                .findById(opportunityId)
    	                .orElseThrow(
    	                        () -> new RuntimeException(
    	                                "Opportunity not found."
    	                        )
    	                );
    	
    	EstimationAnalysis analysis =
    	        estimationAnalysisRepository
    	                .findByOpportunityId(opportunityId)
    	                .orElseThrow(
    	                        () -> new RuntimeException(
    	                                "Estimation Analysis not found."
    	                        )
    	                );
    	
    	Proposal proposal =
    	        proposalRepository
    	                .findFirstByOpportunity_IdOrderByVersionDesc(
    	                        opportunityId
    	                )
    	                .orElseThrow(
    	                        () -> new RuntimeException(
    	                                "Proposal not found."
    	                        )
    	                );
    	
    	List<EstimationActor> actors =
    	        estimationActorRepository
    	                .findByEstimationAnalysisId(
    	                        analysis.getId()
    	                );
    	
    	List<EstimationUseCase> useCases =
    	        estimationUseCaseRepository
    	                .findByEstimationAnalysisId(
    	                        analysis.getId()
    	                );
    	
    	String actorText =
    	        actors.stream()
    	                .map(actor ->
    	                        actor.getActorName()
    	                        + " ("
    	                        + actor.getActorType()
    	                        + ")"
    	                )
    	                .collect(Collectors.joining("\n"));
    	
    	String useCaseText =
    	        useCases.stream()
    	                .map(useCase ->
    	                        useCase.getUseCaseName()
    	                        + " ("
    	                        + useCase.getComplexity()
    	                        + ")"
    	                )
    	                .collect(Collectors.joining("\n"));
    	
    	AiProjectScheduleResult result =
    	        orchestrator.generate(

    	                proposal.getMarkdownContent(),

    	                actorText,

    	                useCaseText,

    	                request.getProjectStartDate().toString(),

    	                request.getTeamSize(),

    	                request.getWorkingDaysPerWeek(),

    	                request.getWorkingHoursPerDay(),

    	                request.getBufferPercentage()

    	        );

    	return result.schedule();

    }

}