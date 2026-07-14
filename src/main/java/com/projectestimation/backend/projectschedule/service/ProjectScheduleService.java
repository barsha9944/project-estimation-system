package com.projectestimation.backend.projectschedule.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import com.projectestimation.backend.projectschedule.dto.SaveProjectScheduleRequest;
import com.projectestimation.backend.projectschedule.dto.SaveProjectScheduleTaskRequest;
import com.projectestimation.backend.projectschedule.model.ProjectSchedule;
import com.projectestimation.backend.projectschedule.model.ProjectScheduleTask;
import com.projectestimation.backend.projectschedule.repository.ProjectScheduleRepository;
import com.projectestimation.backend.projectschedule.repository.ProjectScheduleTaskRepository;
import com.projectestimation.backend.proposal.model.Proposal;
import com.projectestimation.backend.proposal.repository.ProposalRepository;

import jakarta.transaction.Transactional;
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
	
	private final ProjectScheduleRepository projectScheduleRepository;

	private final ProjectScheduleTaskRepository projectScheduleTaskRepository;

	

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

    	        		opportunity,
    	        		
    	        		analysis,

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
    
    @Transactional
	public void saveProjectSchedule(
	
	        Long opportunityId,
	
	        SaveProjectScheduleRequest request,
	
	        User user
	
	) {
	
	    ProjectSchedule schedule =
	            projectScheduleRepository
	                    .findByOpportunityId(opportunityId)
	                    .orElse(null);
	
	    if (schedule == null) {
	
	        schedule = new ProjectSchedule();
	
	    }
	
	    Opportunity opportunity =
	            opportunityRepository
	                    .findById(opportunityId)
	                    .orElseThrow(() ->
	                            new RuntimeException("Opportunity not found.")
	                    );
	
	    EstimationAnalysis analysis =
	            estimationAnalysisRepository
	                    .findByOpportunityId(opportunityId)
	                    .orElseThrow(() ->
	                            new RuntimeException("Estimation analysis not found.")
	                    );
	
	    schedule.setOpportunity(opportunity);
	
	    schedule.setEstimationAnalysis(analysis);
	
	    schedule.setSavedBy(user);
	
	    schedule.setProjectStartDate(
	            request.getProjectStartDate()
	    );
	
	    schedule.setTeamSize(
	            request.getTeamSize()
	    );
	
	    schedule.setWorkingDaysPerWeek(
	            request.getWorkingDaysPerWeek()
	    );
	
	    schedule.setWorkingHoursPerDay(
	            request.getWorkingHoursPerDay()
	    );
	
	    schedule.setBufferPercentage(
	            request.getBufferPercentage()
	    );
	
	    schedule.setDurationDays(
	            request.getDurationDays()
	    );
	
	    schedule.setTotalTasks(
	            request.getTotalTasks()
	    );
	
	    schedule.setCompletedTasks(
	            request.getCompletedTasks()
	    );
	
	    schedule.setCriticalTasks(
	            request.getCriticalTasks()
	    );
	
	    schedule.setEstimatedHours(
	            request.getEstimatedHours()
	    );
	
	    schedule =
	            projectScheduleRepository.save(
	                    schedule
	            );
	    
	    List<ProjectScheduleTask> existingTasks =
	            schedule.getId() == null
	                    ? List.of()
	                    : projectScheduleTaskRepository.findByProjectScheduleId(
	                            schedule.getId()
	                    );

	    Map<Long, ProjectScheduleTask> existingTaskMap =
	            new HashMap<>();

	    for (ProjectScheduleTask task : existingTasks) {

	        existingTaskMap.put(
	                task.getId(),
	                task
	        );

	    }

	    List<ProjectScheduleTask> savedTasks = new java.util.ArrayList<>();

	    for (SaveProjectScheduleTaskRequest taskRequest : request.getTasks()) {

	        ProjectScheduleTask task;

	        if (taskRequest.getId() != null
	                && existingTaskMap.containsKey(taskRequest.getId())) {

	            task = existingTaskMap.get(
	                    taskRequest.getId()
	            );

	        } else {

	            task = new ProjectScheduleTask();

	            task.setProjectSchedule(schedule);

	        }

	        task.setSequence(
	                taskRequest.getSequence()
	        );

	        task.setTaskName(
	                taskRequest.getTaskName()
	        );

	        task.setDuration(
	                taskRequest.getDuration()
	        );

	        task.setPlannedStartDate(
	                taskRequest.getPlannedStartDate()
	        );

	        task.setPlannedEndDate(
	                taskRequest.getPlannedEndDate()
	        );

	        task.setActualStartDate(
	                taskRequest.getActualStartDate()
	        );

	        task.setActualEndDate(
	                taskRequest.getActualEndDate()
	        );

	        task.setPredecessor(
	                taskRequest.getPredecessor()
	        );

	        task.setStatus(
	                taskRequest.getStatus()
	        );

	        task =
	                projectScheduleTaskRepository.save(
	                        task
	                );

	        savedTasks.add(task);

	    }
	
	}

}