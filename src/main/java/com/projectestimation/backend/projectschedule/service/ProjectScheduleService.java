package com.projectestimation.backend.projectschedule.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import com.projectestimation.backend.projectmetrics.model.ProjectMetrics;
import com.projectestimation.backend.projectmetrics.repository.ProjectMetricsRepository;
import com.projectestimation.backend.projectmetrics.repository.ProjectMetricsSprintRepository;
import com.projectestimation.backend.projectschedule.ai.AiProjectScheduleResult;
import com.projectestimation.backend.projectschedule.ai.GeminiProjectScheduleOrchestrator;
import com.projectestimation.backend.projectschedule.dto.GenerateProjectScheduleRequest;
import com.projectestimation.backend.projectschedule.dto.ProjectScheduleResponse;
import com.projectestimation.backend.projectschedule.dto.ProjectScheduleTaskResponse;
import com.projectestimation.backend.projectschedule.dto.RecalculateProjectScheduleRequest;
import com.projectestimation.backend.projectschedule.dto.SaveProjectScheduleRequest;
import com.projectestimation.backend.projectschedule.dto.SaveProjectScheduleTaskRequest;
import com.projectestimation.backend.projectschedule.dto.SaveTaskBreakdownRequest;
import com.projectestimation.backend.projectschedule.dto.TaskBreakdownResponse;
import com.projectestimation.backend.projectschedule.excel.ProjectScheduleExcelExporter;
import com.projectestimation.backend.projectschedule.model.ProjectSchedule;
import com.projectestimation.backend.projectschedule.model.ProjectScheduleTask;
import com.projectestimation.backend.projectschedule.model.ProjectScheduleTaskBreakdown;
import com.projectestimation.backend.projectschedule.repository.ProjectScheduleRepository;
import com.projectestimation.backend.projectschedule.repository.ProjectScheduleTaskBreakdownRepository;
import com.projectestimation.backend.projectschedule.repository.ProjectScheduleTaskRepository;
import com.projectestimation.backend.proposal.repository.ProposalRepository;
import com.projectestimation.backend.psr.dto.PsrActivityDto;
import com.projectestimation.backend.psr.model.ProjectStatusReport;
import com.projectestimation.backend.psr.repository.ProjectStatusReportRepository;
import com.projectestimation.backend.psr.dto.PsrResponse;
import com.projectestimation.backend.psr.model.ProjectStatusReport;
import com.projectestimation.backend.psr.repository.ProjectStatusReportRepository;

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

	private final ProjectMetricsRepository projectMetricsRepository;

	private final ProjectMetricsSprintRepository projectMetricsSprintRepository;

	private final ProjectStatusReportRepository projectStatusReportRepository;
	
	

	private final ProjectScheduleTaskBreakdownRepository projectScheduleTaskBreakdownRepository;

	private final ObjectMapper objectMapper;

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

		deleteExistingProjectMetrics(opportunityId);

		projectScheduleRepository.flush();

//		projectScheduleTaskRepository.deleteByProjectScheduleId(
//				schedule.getId());

		for (SaveProjectScheduleTaskRequest taskRequest : request.getTasks()) {

			ProjectScheduleTask task;

			if (taskRequest.getId() != null) {

				task = projectScheduleTaskRepository.findById(taskRequest.getId()).orElseThrow(
						() -> new ResourceNotFoundException("Project schedule task not found: " + taskRequest.getId()));

			} else {

				task = new ProjectScheduleTask();
				task.setProjectSchedule(schedule);
			}

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

    List<ProjectScheduleTaskBreakdown> existingBreakdowns =
            savedTask.getTaskBreakdowns();

    if (existingBreakdowns == null) {
        existingBreakdowns = new ArrayList<>();
        savedTask.setTaskBreakdowns(existingBreakdowns);
    }

    Map<Long, ProjectScheduleTaskBreakdown> existingBreakdownMap =
            existingBreakdowns.stream()
                    .filter(b -> b.getId() != null)
                    .collect(Collectors.toMap(
                            ProjectScheduleTaskBreakdown::getId,
                            b -> b
                    ));

    List<Long> requestBreakdownIds = new ArrayList<>();

    for (SaveTaskBreakdownRequest breakdownRequest :
            taskRequest.getTaskBreakdowns()) {

        ProjectScheduleTaskBreakdown breakdown;

        if (breakdownRequest.getId() != null) {

            breakdown = existingBreakdownMap.get(
                    breakdownRequest.getId()
            );

            if (breakdown == null) {
                throw new ResourceNotFoundException(
                        "Project schedule breakdown not found: "
                                + breakdownRequest.getId()
                );
            }

            requestBreakdownIds.add(
                    breakdownRequest.getId()
            );

        } else {

            breakdown = new ProjectScheduleTaskBreakdown();

            breakdown.setProjectScheduleTask(
                    savedTask
            );

            existingBreakdowns.add(breakdown);
        }

        breakdown.setActivityName(
                breakdownRequest.getActivityName()
        );

        breakdown.setDuration(
                breakdownRequest.getDuration()
        );

        breakdown.setPlannedStartDate(
                breakdownRequest.getPlannedStartDate()
        );

        breakdown.setPlannedEndDate(
                breakdownRequest.getPlannedEndDate()
        );

        breakdown.setStatus(
                breakdownRequest.getStatus()
        );

        breakdown.setProgress(
                breakdownRequest.getProgress()
        );

        breakdown.setActualStartDate(
                breakdownRequest.getActualStartDate() != null
                        ? breakdownRequest.getActualStartDate()
                        : breakdownRequest.getPlannedStartDate()
        );

        breakdown.setActualEndDate(
                breakdownRequest.getActualEndDate() != null
                        ? breakdownRequest.getActualEndDate()
                        : breakdownRequest.getPlannedEndDate()
        );
    }

    existingBreakdowns.removeIf(
            breakdown ->
                    breakdown.getId() != null
                    && !requestBreakdownIds.contains(
                            breakdown.getId()
                    )
    );

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
		
		ProjectStatusReport psr =
		        projectStatusReportRepository
		                .findTopByOpportunityIdOrderByGeneratedAtDesc(opportunityId)
		                .orElse(null);

		if (psr != null) {

		    PsrResponse psrResponse = new PsrResponse(
		            psr.getId(),
		            psr.getFileName(),
		            psr.getFileLocation(),
		            psr.getGeneratedAt(),
		            "GENERATED",
		            psr.getMarkdownContent()
		    );

		    response.setPsr(psrResponse);
		}

		List<ProjectStatusReport> psrReports = projectStatusReportRepository
				.findByOpportunityIdOrderByGeneratedAtAsc(opportunityId);

		Map<Long, ProjectStatusReport> breakdownPsrMap = new HashMap<>();

		for (ProjectStatusReport report : psrReports) {

			if (report.getAssociatedBreakdownIds() == null || report.getAssociatedBreakdownIds().isBlank()) {
				continue;
			}

			try {

				List<Long> breakdownIds = objectMapper.readValue(report.getAssociatedBreakdownIds(),
						new TypeReference<List<Long>>() {
						});

				for (Long breakdownId : breakdownIds) {
					breakdownPsrMap.put(breakdownId, report);
				}

			} catch (Exception ex) {

				log.error("Failed to parse associated breakdown IDs for PSR {}", report.getId(), ex);

				throw new IllegalStateException("Failed to parse PSR associated breakdown IDs", ex);
			}
		}
		List<ProjectScheduleTask> sortedTasks = schedule.getTasks().stream()
				.sorted(Comparator.comparing(t -> t.getSequence() != null ? t.getSequence() : Integer.MAX_VALUE))
				.toList();

		// 1. Build flat list of breakdowns and map (sequence + activityType) to flat
		// index
		Map<String, Integer> keyToFlatIndex = new HashMap<>();
		int flatIdx = 0;
		for (ProjectScheduleTask task : sortedTasks) {
			List<ProjectScheduleTaskBreakdown> sortedBreakdowns = task.getTaskBreakdowns().stream()
					.sorted(Comparator.comparing(b -> b.getId() != null ? b.getId() : Long.MAX_VALUE)).toList();
			for (ProjectScheduleTaskBreakdown breakdown : sortedBreakdowns) {
				keyToFlatIndex.put(buildPsrKey(task.getSequence(), breakdown.getActivityName()), flatIdx++);
			}
		}

		// 3. Apply PSR logic based on genPoint boundaries
		List<ProjectScheduleTaskResponse> tasks = new ArrayList<>();

		for (ProjectScheduleTask task : sortedTasks) {
			ProjectScheduleTaskResponse taskResponse = mapTaskResponse(task);

			List<ProjectScheduleTaskBreakdown> sortedBreakdowns = task.getTaskBreakdowns().stream()
					.sorted(Comparator.comparing(b -> b.getId() != null ? b.getId() : Long.MAX_VALUE)).toList();

			List<TaskBreakdownResponse> breakdownResponses = new ArrayList<>();
			for (ProjectScheduleTaskBreakdown breakdown : sortedBreakdowns) {

				TaskBreakdownResponse br = mapTaskBreakdownResponse(breakdown);

				ProjectStatusReport assignedReport = breakdownPsrMap.get(breakdown.getId());

				if (assignedReport != null) {

					br.setPsrFileName(assignedReport.getFileName());

					br.setPsrFileLocation(assignedReport.getFileLocation());

					br.setPsrMarkdown(assignedReport.getMarkdownContent());
				}

				breakdownResponses.add(br);
			}

			taskResponse.setTaskBreakdowns(breakdownResponses);
			tasks.add(taskResponse);
		}

		response.setTasks(tasks);

		return response;

	}

	private ProjectScheduleTaskResponse mapTaskResponse(ProjectScheduleTask task) {

		ProjectScheduleTaskResponse response = new ProjectScheduleTaskResponse();

		response.setId(task.getId());
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

		return response;
	}

	private TaskBreakdownResponse mapTaskBreakdownResponse(ProjectScheduleTaskBreakdown breakdown) {

		TaskBreakdownResponse response = new TaskBreakdownResponse();

		response.setId(breakdown.getId());
		response.setActivityName(breakdown.getActivityName());
		response.setDuration(breakdown.getDuration());
		response.setPlannedStartDate(breakdown.getPlannedStartDate());
		response.setPlannedEndDate(breakdown.getPlannedEndDate());
		response.setActualEndDate(breakdown.getActualEndDate());
		response.setActualStartDate(breakdown.getActualStartDate());
		response.setStatus(breakdown.getStatus() != null ? breakdown.getStatus() : "Not Started");
		response.setProgress(breakdown.getProgress());
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

	private void deleteExistingProjectMetrics(Long opportunityId) {

		ProjectMetrics metrics = projectMetricsRepository.findByOpportunityId(opportunityId).orElse(null);

		if (metrics == null) {
			return;
		}

		Long projectMetricsId = metrics.getId();

		// FIRST: delete child sprint rows
		projectMetricsSprintRepository.deleteByProjectMetricsId(projectMetricsId);

		// THEN: delete parent project metrics row
		projectMetricsRepository.deleteByOpportunityId(opportunityId);
	}

	private Map<String, PsrBreakdownInfo> buildBreakdownPsrMap(List<ProjectStatusReport> psrReports) {

		Map<String, PsrBreakdownInfo> psrMap = new HashMap<>();

		for (ProjectStatusReport report : psrReports) {

			addPsrActivitiesToMap(report, report.getActivitiesPerformed(), psrMap);

			addPsrActivitiesToMap(report, report.getNextWeekPlannedActivities(), psrMap);
		}

		return psrMap;
	}

	private void addPsrActivitiesToMap(ProjectStatusReport report, String activitiesJson,
			Map<String, PsrBreakdownInfo> psrMap) {

		if (activitiesJson == null || activitiesJson.isBlank()) {
			return;
		}

		try {

			List<PsrActivityDto> activities = objectMapper.readValue(activitiesJson,
					new TypeReference<List<PsrActivityDto>>() {
					});

			for (PsrActivityDto activity : activities) {

				String key = buildPsrKey(activity.getSequence(), activity.getActivityName());

				psrMap.put(key, new PsrBreakdownInfo(report.getFileName(), report.getMarkdownContent()));
			}

		} catch (Exception ex) {

			throw new IllegalStateException("Failed to parse PSR activity data", ex);
		}
	}

	private static class PsrBreakdownInfo {

		private final String fileName;
		private final String markdownContent;

		public PsrBreakdownInfo(String fileName, String markdownContent) {
			this.fileName = fileName;
			this.markdownContent = markdownContent;
		}

		public String getFileName() {
			return fileName;
		}

		public String getMarkdownContent() {
			return markdownContent;
		}
	}

	private String buildPsrKey(Integer sequence, String activityName) {

		return String.valueOf(sequence) + "|" + (activityName == null ? "" : activityName.trim().toLowerCase());
	}
}
