package com.projectestimation.backend.projectschedule.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.projectestimation.backend.auth.model.User;
import com.projectestimation.backend.common.exception.ProjectScheduleFailedException;
import com.projectestimation.backend.common.exception.ResourceNotFoundException;
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
import com.projectestimation.backend.projectschedule.controller.ProjectScheduleController;
import com.projectestimation.backend.projectschedule.dto.GenerateProjectScheduleRequest;
import com.projectestimation.backend.projectschedule.dto.ProjectScheduleResponse;
import com.projectestimation.backend.projectschedule.dto.ProjectScheduleTaskResponse;
import com.projectestimation.backend.projectschedule.dto.RecalculateProjectScheduleRequest;
import com.projectestimation.backend.projectschedule.dto.SaveProjectScheduleRequest;
import com.projectestimation.backend.projectschedule.dto.SaveProjectScheduleTaskRequest;
import com.projectestimation.backend.projectschedule.dto.TaskBreakdownResponse;
import com.projectestimation.backend.projectschedule.excel.ProjectScheduleExcelExporter;
import com.projectestimation.backend.projectschedule.model.ProjectSchedule;
import com.projectestimation.backend.projectschedule.model.ProjectScheduleTask;
import com.projectestimation.backend.projectschedule.model.ProjectScheduleTaskBreakdown;
import com.projectestimation.backend.projectschedule.repository.ProjectScheduleRepository;
import com.projectestimation.backend.projectschedule.repository.ProjectScheduleTaskRepository;
import com.projectestimation.backend.proposal.repository.ProposalRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProjectScheduleService {
	private static final Logger log = LogManager.getLogger(ProjectScheduleService.class);
	private final OpportunityRepository opportunityRepository;

	private final EstimationAnalysisRepository estimationAnalysisRepository;

	private final EstimationActorRepository estimationActorRepository;

	private final EstimationUseCaseRepository estimationUseCaseRepository;

	private final ProposalRepository proposalRepository;

	private final GeminiProjectScheduleOrchestrator orchestrator;

	private final ProjectScheduleRepository projectScheduleRepository;

	private final ProjectScheduleTaskRepository projectScheduleTaskRepository;

	private final ProjectScheduleExcelExporter projectScheduleExcelExporter;

	private final ProjectScheduleCalculator projectScheduleCalculator;

	public ProjectScheduleResponse generateProjectSchedule(

			Long opportunityId,

			GenerateProjectScheduleRequest request,

			User user) {
		Opportunity opportunity = opportunityRepository.findById(opportunityId)
				.orElseThrow(() -> new ResourceNotFoundException("Opportunity not found."));

		EstimationAnalysis analysis = estimationAnalysisRepository.findByOpportunityId(opportunityId)
				.orElseThrow(() -> new ResourceNotFoundException("Estimation Analysis not found."));

		List<EstimationActor> actors = estimationActorRepository.findByEstimationAnalysisId(analysis.getId());

		List<EstimationUseCase> useCases = estimationUseCaseRepository.findByEstimationAnalysisId(analysis.getId());

		String actorText = actors.stream().map(actor -> actor.getActorName() + " (" + actor.getActorType() + ")")
				.collect(Collectors.joining("\n"));

		String useCaseText = useCases.stream()
				.map(useCase -> useCase.getUseCaseName() + " (" + useCase.getComplexity() + ")")
				.collect(Collectors.joining("\n"));

		double hoursWithBuffer = request.getEstimatedHours() * (1 + request.getBufferPercentage() / 100.0);

		int durationDays = request.getDuration() > 0 ? request.getDuration()
				: (int) Math.ceil(hoursWithBuffer / (request.getTeamSize() * request.getWorkingHoursPerDay()));
		System.out.println("Duration days : " + durationDays);
		LocalDate projectEndDate = calculateProjectEndDate(request.getProjectStartDate(), durationDays,
				request.getWorkingDaysPerWeek());

		AiProjectScheduleResult result;
		try {
			result = orchestrator.generate(opportunity, analysis, actorText, useCaseText,
					request.getProjectStartDate().toString(), request.getTeamSize(), request.getWorkingDaysPerWeek(),
					request.getWorkingHoursPerDay(), request.getBufferPercentage(), durationDays,
					request.getEstimatedHours());
		} catch (Exception ex) {
			throw new ProjectScheduleFailedException("Failed to generate project schedule", ex);
		}

		ProjectScheduleResponse response = result.schedule();

		response.setDurationDays(durationDays);

		response.setProjectEndDate(projectEndDate);

		response.setEstimatedHours(request.getEstimatedHours());

		return response;
	}

	@Transactional
	public void saveProjectSchedule(

			Long opportunityId,

			SaveProjectScheduleRequest request,

			User user

	) {

		ProjectSchedule schedule = projectScheduleRepository.findByOpportunityId(opportunityId).orElse(null);

		if (schedule == null) {

			schedule = new ProjectSchedule();

		}

		Opportunity opportunity = opportunityRepository.findById(opportunityId)
				.orElseThrow(() -> new ResourceNotFoundException("Opportunity not found."));

		EstimationAnalysis analysis = estimationAnalysisRepository.findByOpportunityId(opportunityId)
				.orElseThrow(() -> new ResourceNotFoundException("Estimation analysis not found."));

		schedule.setOpportunity(opportunity);

		schedule.setEstimationAnalysis(analysis);

		schedule.setSavedBy(user);

		schedule.setProjectStartDate(request.getProjectStartDate());

		schedule.setProjectEndDate(request.getProjectEndDate());

		schedule.setTeamSize(request.getTeamSize());

		schedule.setWorkingDaysPerWeek(request.getWorkingDaysPerWeek());

		schedule.setWorkingHoursPerDay(request.getWorkingHoursPerDay());

		schedule.setBufferPercentage(request.getBufferPercentage());

		schedule.setDurationDays(request.getDurationDays());

		schedule.setTotalTasks(request.getTotalTasks());

		schedule.setCompletedTasks(request.getCompletedTasks());

		schedule.setCriticalTasks(request.getCriticalTasks());

		schedule.setEstimatedHours(request.getEstimatedHours());

		schedule = projectScheduleRepository.save(schedule);

		projectScheduleTaskRepository.deleteByProjectScheduleId(schedule.getId());

		for (SaveProjectScheduleTaskRequest taskRequest : request.getTasks()) {

			ProjectScheduleTask task = new ProjectScheduleTask();

			task.setProjectSchedule(schedule);

			task.setSequence(taskRequest.getSequence());

			task.setTaskName(taskRequest.getTaskName());

			task.setDuration(taskRequest.getDuration());

			task.setPlannedStartDate(taskRequest.getPlannedStartDate());

			task.setPlannedEndDate(taskRequest.getPlannedEndDate());

			// task.setActualStartDate(taskRequest.getActualStartDate());

			// task.setActualEndDate(taskRequest.getActualEndDate());
			task.setActualStartDate(taskRequest.getActualStartDate() != null ? taskRequest.getActualStartDate()
					: taskRequest.getPlannedStartDate());

			task.setActualEndDate(taskRequest.getActualEndDate() != null ? taskRequest.getActualEndDate()
					: taskRequest.getPlannedEndDate());

			task.setPredecessor(taskRequest.getPredecessor());

			task.setStatus(taskRequest.getStatus());

			ProjectScheduleTask savedTask = projectScheduleTaskRepository.save(task);

			if (taskRequest.getTaskBreakdowns() != null) {

				List<ProjectScheduleTaskBreakdown> breakdowns = taskRequest.getTaskBreakdowns().stream()
						.map(breakdownRequest -> {

							ProjectScheduleTaskBreakdown breakdown = new ProjectScheduleTaskBreakdown();

							breakdown.setProjectScheduleTask(savedTask);

							breakdown.setActivityName(breakdownRequest.getActivityName());

							breakdown.setDuration(breakdownRequest.getDuration());

							breakdown.setPlannedStartDate(breakdownRequest.getPlannedStartDate());

							breakdown.setPlannedEndDate(breakdownRequest.getPlannedEndDate());

							breakdown.setStatus(breakdownRequest.getStatus());

							breakdown.setActualStartDate(breakdownRequest.getActualStartDate() != null
									? breakdownRequest.getActualStartDate()
									: breakdownRequest.getPlannedStartDate());

							breakdown.setActualEndDate(
									breakdownRequest.getActualEndDate() != null ? breakdownRequest.getActualEndDate()
											: breakdownRequest.getPlannedEndDate());

							return breakdown;

						}).collect(Collectors.toCollection(ArrayList::new));

				savedTask.setTaskBreakdowns(breakdowns);

				projectScheduleTaskRepository.save(savedTask);
			}

		}

	}

	public ResponseEntity<byte[]> downloadProjectSchedule(

			Long opportunityId,

			SaveProjectScheduleRequest request

	) {

		Opportunity opportunity = opportunityRepository.findById(opportunityId)
				.orElseThrow(() -> new ResourceNotFoundException("Opportunity not found: " + opportunityId));

		String opportunityName = opportunity.getOpportunityName();

		String fileName = opportunityName.replaceAll("[\\\\/:*?\"<>|]", "_").replaceAll("\\s+", "_")
				+ "_Project_Schedule.xlsx";

		byte[] excel = projectScheduleExcelExporter.export(request);

		return ResponseEntity.ok().header("Content-Disposition", "attachment; filename=\"" + fileName + "\"")
				.header("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
				.body(excel);

	}

	@Transactional(readOnly = true)
	public ProjectScheduleResponse getProjectSchedule(Long opportunityId) {

		ProjectSchedule schedule = projectScheduleRepository.findByOpportunityIdWithTasks(opportunityId)
				.orElseThrow(() -> new ResourceNotFoundException("Project schedule not found."));

		ProjectScheduleResponse response = new ProjectScheduleResponse();

		response.setDurationDays(schedule.getDurationDays());

		response.setTotalTasks(schedule.getTotalTasks());

		response.setCompletedTasks(schedule.getCompletedTasks());

		response.setCriticalTasks(schedule.getCriticalTasks());

		response.setEstimatedHours(schedule.getEstimatedHours());

		response.setBufferPercentage(schedule.getBufferPercentage());

		response.setProjectStartDate(schedule.getProjectStartDate());

		response.setProjectEndDate(schedule.getProjectEndDate());

		response.setWorkingDaysPerWeek(schedule.getWorkingDaysPerWeek());

		response.setWorkingHoursPerDays(schedule.getWorkingHoursPerDay());

		response.setTeamSize(schedule.getTeamSize());

		List<ProjectScheduleTaskResponse> tasks = schedule.getTasks().stream().map(this::mapTaskResponse).toList();

		response.setTasks(tasks);

		return response;

	}

	private ProjectScheduleTaskResponse mapTaskResponse(ProjectScheduleTask task) {

		ProjectScheduleTaskResponse response = new ProjectScheduleTaskResponse();

		response.setSequence(task.getSequence());

		response.setTaskName(task.getTaskName());

		response.setDuration(task.getDuration());

		response.setPlannedStartDate(task.getPlannedStartDate());

		response.setPlannedEndDate(task.getPlannedEndDate());

		response.setActualStartDate(
				task.getActualStartDate() != null ? task.getActualStartDate() : task.getPlannedStartDate());

		response.setActualEndDate(task.getActualEndDate() != null ? task.getActualEndDate() : task.getPlannedEndDate());

		response.setStatus(task.getStatus());

		response.setPredecessor(task.getPredecessor());

		response.setTaskBreakdowns(task.getTaskBreakdowns().stream().map(this::mapTaskBreakdownResponse).toList());

		return response;
	}

	private TaskBreakdownResponse mapTaskBreakdownResponse(ProjectScheduleTaskBreakdown breakdown) {

		TaskBreakdownResponse response = new TaskBreakdownResponse();

		response.setActivityName(breakdown.getActivityName());

		response.setDuration(breakdown.getDuration());

		response.setPlannedStartDate(breakdown.getPlannedStartDate());

		response.setPlannedEndDate(breakdown.getPlannedEndDate());

		response.setActualEndDate(breakdown.getActualEndDate());

		response.setActualStartDate(breakdown.getActualStartDate());
		response.setStatus(breakdown.getStatus() != null ? breakdown.getStatus() : "Not Started");
//		log.info("Actual duration :: {}. Start date : {}, End date:{}",
//				ChronoUnit.DAYS.between(breakdown.getActualStartDate(), breakdown.getActualEndDate()),  breakdown.getActualStartDate(), breakdown.getActualEndDate());
		response.setActualDuration(
				ChronoUnit.DAYS.between(breakdown.getActualStartDate(), breakdown.getActualEndDate()));
		return response;
	}

	public ProjectScheduleResponse recalculateProjectSchedule(

			Long opportunityId,

			RecalculateProjectScheduleRequest request) {

		return projectScheduleCalculator.recalculate(request);

	}

	private LocalDate calculateProjectEndDate(LocalDate startDate, int durationDays, int workingDaysPerWeek) {

		LocalDate currentDate = startDate;

		int completedWorkingDays = 1;

		while (completedWorkingDays < durationDays) {

			currentDate = currentDate.plusDays(1);

			if (isWorkingDay(currentDate, workingDaysPerWeek)) {
				completedWorkingDays++;
			}
		}
		System.out.println("end date :: " + currentDate);

		return currentDate;
	}

	private boolean isWorkingDay(LocalDate date, int workingDaysPerWeek) {

		DayOfWeek day = date.getDayOfWeek();

		if (workingDaysPerWeek == 5) {
			return day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY;
		}

		return true;
	}
}
