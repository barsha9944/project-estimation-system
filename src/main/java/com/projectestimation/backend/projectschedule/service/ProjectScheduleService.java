package com.projectestimation.backend.projectschedule.service;

import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.projectestimation.backend.auth.model.User;
import com.projectestimation.backend.common.exception.ProjectScheduleFailedException;
import com.projectestimation.backend.common.exception.ResourceNotFoundException;
import com.projectestimation.backend.common.storage.OpportunityDocumentStorageService;
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
import com.projectestimation.backend.projectschedule.excel.ProjectScheduleExcelParser;
import com.projectestimation.backend.projectschedule.model.ProjectSchedule;
import com.projectestimation.backend.projectschedule.model.ProjectScheduleTask;
import com.projectestimation.backend.projectschedule.model.ProjectScheduleTaskBreakdown;
import com.projectestimation.backend.projectschedule.repository.ProjectScheduleRepository;
import com.projectestimation.backend.projectschedule.repository.ProjectScheduleTaskBreakdownRepository;
import com.projectestimation.backend.projectschedule.repository.ProjectScheduleTaskRepository;
import com.projectestimation.backend.proposal.repository.ProposalRepository;
import com.projectestimation.backend.psr.dto.PsrResponse;
import com.projectestimation.backend.psr.model.ProjectStatusReport;
import com.projectestimation.backend.psr.repository.ProjectStatusReportRepository;
import com.projectestimation.backend.psr.service.PsrPeriod;
import com.projectestimation.backend.psr.service.PsrPeriodCalculator;
import com.projectestimation.backend.psr.service.PsrService;

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

	private final PsrService psrService;

	private final PsrPeriodCalculator psrPeriodCalculator;
	
	private final OpportunityDocumentStorageService documentStorageService;
	private final ProjectScheduleExcelParser projectScheduleExcelParser;

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

		LocalDate projectStartDate = request.getProjectStartDate();
		LocalDate projectEndDate = request.getProjectEndDate();

		if (projectStartDate == null || projectEndDate == null) {
		    throw new IllegalArgumentException(
		            "Project start date and end date are required."
		    );
		}

		if (projectStartDate.isAfter(projectEndDate)) {
		    throw new IllegalArgumentException(
		            "Project start date cannot be after project end date."
		    );
		}

		if (isWeekend(projectStartDate) || isWeekend(projectEndDate)) {
		    throw new IllegalArgumentException(
		            "Project start date and end date must be working days."
		    );
		}

		int durationDays = 0;

		LocalDate currentDate = projectStartDate;

		while (!currentDate.isAfter(projectEndDate)) {
		    if (!isWeekend(currentDate)) {
		        durationDays++;
		    }
		    currentDate = currentDate.plusDays(1);
		}

		System.out.println("Fixed project duration: " + durationDays);

		AiProjectScheduleResult result;
		try {
			result = orchestrator.generate(opportunity, analysis, actorText, useCaseText,
					projectStartDate.toString(), projectEndDate.toString(), request.getTeamSize(), request.getWorkingDaysPerWeek(),
					request.getWorkingHoursPerDay(), request.getBufferPercentage(), durationDays,
					request.getEstimatedHours(), request.getSprintCount());
		} catch (Exception ex) {
			throw new ProjectScheduleFailedException("Failed to generate project schedule", ex);
		}


ProjectScheduleResponse response = result.schedule();

response.setDurationDays(durationDays);
response.setProjectStartDate(projectStartDate);
response.setProjectEndDate(projectEndDate);
response.setEstimatedHours(request.getEstimatedHours());

// Set the generation parameters in the response.
response.setBufferPercentage(request.getBufferPercentage());
response.setTeamSize(request.getTeamSize());
response.setWorkingDaysPerWeek(request.getWorkingDaysPerWeek());
response.setWorkingHoursPerDays(request.getWorkingHoursPerDay());

// Generate and save the Excel immediately.
try {

    SaveProjectScheduleRequest excelRequest =
            convertToSaveProjectScheduleRequest(response);

    byte[] excelBytes =
            projectScheduleExcelExporter.export(excelRequest, opportunityId);

    String fileName = opportunity.getOpportunityName()
            .replaceAll("[\\\\/:*?\"<>|]", "_")
            .replaceAll("\\s+", "_")
            .trim();

    if (fileName.isBlank()) {
        fileName = "Work_Schedule";
    }

    documentStorageService.saveFile(
            opportunityId,
            "Baseline/PMP",
            fileName + "_Project_Schedule.xlsx",
            excelBytes
    );

    log.info(
            "Generated Project Schedule saved for opportunity {}",
            opportunityId
    );

} catch (Exception e) {

    throw new ProjectScheduleFailedException(
            "Failed to save generated Project Schedule Excel.",
            e
    );
}

return response;

	}

	@Transactional
	public void saveProjectSchedule(

			Long opportunityId,

			SaveProjectScheduleRequest request,

			User user

	) {

		ProjectSchedule schedule = projectScheduleRepository.findByOpportunityId(opportunityId).orElse(null);

		Set<Integer> affectedPsrVersions = determineAffectedPsrVersions(request, schedule);

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

		List<ProjectScheduleTask> existingTasks = schedule.getTasks();
		if (existingTasks == null) {
			existingTasks = new ArrayList<>();
			schedule.setTasks(existingTasks);
		}

		Map<Long, ProjectScheduleTask> existingById = existingTasks.stream()
				.filter(t -> t.getId() != null)
				.collect(Collectors.toMap(ProjectScheduleTask::getId, t -> t, (a, b) -> a));

		Map<String, ProjectScheduleTask> existingByName = existingTasks.stream()
				.filter(t -> t.getTaskName() != null && !t.getTaskName().isBlank())
				.collect(Collectors.toMap(t -> t.getTaskName().trim().toLowerCase(), t -> t, (a, b) -> a));

		Set<Long> savedTaskIds = new HashSet<>();
		Set<String> processedTaskNames = new HashSet<>();

		for (SaveProjectScheduleTaskRequest taskRequest : request.getTasks()) {
			if (taskRequest.getTaskName() == null || taskRequest.getTaskName().isBlank()) {
				continue;
			}

			String canonicalName = taskRequest.getTaskName().trim();
			String nameKey = canonicalName.toLowerCase();

			ProjectScheduleTask task = null;

			// 1. Try matching by ID first
			if (taskRequest.getId() != null) {
				task = existingById.get(taskRequest.getId());
				if (task == null) {
					task = projectScheduleTaskRepository.findById(taskRequest.getId()).orElse(null);
				}
			}

			// 2. If not matched by ID, try matching by canonical task name
			if (task == null) {
				task = existingByName.get(nameKey);
			}

			// 3. If duplicate task name appears again within the same request, merge into previously matched/created task
			if (processedTaskNames.contains(nameKey)) {
				log.warn("Duplicate task name '{}' detected in request. Merging into existing task.", canonicalName);
				if (task != null && taskRequest.getTaskBreakdowns() != null) {
					saveOrMergeBreakdowns(task, taskRequest.getTaskBreakdowns());
				}
				continue;
			}
			processedTaskNames.add(nameKey);

			if (task == null) {
				task = new ProjectScheduleTask();
				task.setProjectSchedule(schedule);
				existingTasks.add(task);
			}

			task.setSequence(taskRequest.getSequence());
			task.setSprintNumber(taskRequest.getSprintNumber());
			task.setTaskName(canonicalName);
			task.setDuration(taskRequest.getDuration());
			task.setPlannedStartDate(taskRequest.getPlannedStartDate());
			task.setPlannedEndDate(taskRequest.getPlannedEndDate());
			task.setActualStartDate(taskRequest.getActualStartDate() != null ? taskRequest.getActualStartDate()
					: taskRequest.getPlannedStartDate());
			task.setActualEndDate(taskRequest.getActualEndDate() != null ? taskRequest.getActualEndDate()
					: taskRequest.getPlannedEndDate());
			task.setPredecessor(taskRequest.getPredecessor());
			task.setStatus(taskRequest.getStatus());

			ProjectScheduleTask savedTask = projectScheduleTaskRepository.save(task);
			if (savedTask.getId() != null) {
				savedTaskIds.add(savedTask.getId());
				existingById.put(savedTask.getId(), savedTask);
				existingByName.put(nameKey, savedTask);
			}

			if (taskRequest.getTaskBreakdowns() != null) {
				saveOrMergeBreakdowns(savedTask, taskRequest.getTaskBreakdowns());
			}
		}

		// Clean up orphan tasks in DB that were deleted by user or duplicates from previous saves
		List<ProjectScheduleTask> orphanTasks = existingTasks.stream()
				.filter(t -> t.getId() != null && !savedTaskIds.contains(t.getId()))
				.toList();

		if (!orphanTasks.isEmpty()) {
			log.info("Removing {} orphan tasks for schedule {}", orphanTasks.size(), schedule.getId());
			for (ProjectScheduleTask orphan : orphanTasks) {
				if (orphan.getTaskBreakdowns() != null) {
					projectScheduleTaskBreakdownRepository.deleteAll(orphan.getTaskBreakdowns());
				}
			}
			projectScheduleTaskRepository.deleteAll(orphanTasks);
			existingTasks.removeAll(orphanTasks);
		}
		
		// Calculate completed parent tasks from their breakdown statuses.
		int completedTasks = (int) existingTasks.stream()
		        .filter(task -> {
		            List<ProjectScheduleTaskBreakdown> breakdowns =
		                    task.getTaskBreakdowns();

		            if (breakdowns != null && !breakdowns.isEmpty()) {
		                return breakdowns.stream()
		                        .allMatch(breakdown ->
		                                "Completed".equalsIgnoreCase(
		                                        breakdown.getStatus() == null
		                                                ? ""
		                                                : breakdown.getStatus().trim()
		                                )
		                        );
		            }

		            return "Completed".equalsIgnoreCase(
		                    task.getStatus() == null
		                            ? ""
		                            : task.getStatus().trim()
		            );
		        })
		        .count();

		schedule.setCompletedTasks(completedTasks);
		projectScheduleRepository.save(schedule);

		// Schedule persistence is the PSR trigger. The PSR service derives the
		// period from projectStartDate and refreshes its one report for that period.
		projectScheduleRepository.flush();

		try {
			byte[] excelBytes = projectScheduleExcelExporter.export(request, opportunityId);
			Opportunity opp = opportunityRepository.findById(opportunityId).orElse(null);
			if (opp != null) {
				String fileName = opp.getOpportunityName()
						.replaceAll("[\\\\/:*?\"<>|]", "_")
						.replaceAll("\\s+", "_")
						+ "_Project_Schedule.xlsx";
				String relativePath = documentStorageService.replaceFile(
						opportunityId,
						"Baseline/PMP",
						fileName,
						excelBytes
				);
				schedule.setFilePath(relativePath);
				schedule.setFileHash(documentStorageService.getFileChecksum(opportunityId, relativePath));
				schedule.setFileLastModified(documentStorageService.getLastModified(opportunityId, relativePath));
				projectScheduleRepository.save(schedule);
			}
		} catch (Exception ex) {
			log.warn("Failed saving physical Project Schedule Excel file: {}", ex.getMessage());
		}


	}

	public ResponseEntity<byte[]> downloadProjectSchedule(
        Long opportunityId,
        SaveProjectScheduleRequest request
) {

    Opportunity opportunity = opportunityRepository.findById(opportunityId)
            .orElseThrow(() -> new ResourceNotFoundException(
                    "Opportunity not found: " + opportunityId
            ));

    String fileName = opportunity.getOpportunityName()
            .replaceAll("[\\\\/:*?\"<>|]", "_")
            .replaceAll("\\s+", "_")
            + "_Project_Schedule.xlsx";

    String relativePath = "Baseline/PMP/" + fileName;

    try {
        if (documentStorageService.exists(opportunityId, relativePath)) {
            byte[] fileBytes = documentStorageService.readFile(opportunityId, relativePath);
            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=\"" + fileName + "\"")
                    .header("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                    .body(fileBytes);
        }
    } catch (Exception ex) {
        log.warn("Failed reading physical schedule file: {}", ex.getMessage());
    }

    byte[] excel = projectScheduleExcelExporter.export(request, opportunityId);
    try {
        documentStorageService.saveFile(opportunityId, "Baseline/PMP", fileName, excel);
    } catch (Exception ignored) {}

    return ResponseEntity.ok()
            .header(
                    "Content-Disposition",
                    "attachment; filename=\"" + fileName + "\""
            )
            .header(
                    "Content-Type",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            )
            .body(excel);
}

	@Transactional
	public ProjectScheduleResponse getProjectSchedule(Long opportunityId) {

		ProjectSchedule schedule = projectScheduleRepository.findByOpportunityIdWithTasks(opportunityId)
				.orElseThrow(() -> new ResourceNotFoundException("Project schedule not found."));

		// Check physical Excel file for external changes
		Opportunity opp = opportunityRepository.findById(opportunityId).orElse(null);
		if (opp != null) {
			String fileName = opp.getOpportunityName().replaceAll("[\\\\/:*?\"<>|]", "_").replaceAll("\\s+", "_") + "_Project_Schedule.xlsx";
			String relPath = "Baseline/PMP/" + fileName;
			if (documentStorageService.exists(opportunityId, relPath)) {
				try {
					String currentHash = documentStorageService.getFileChecksum(opportunityId, relPath);
					if (schedule.getFileHash() != null && !currentHash.equals(schedule.getFileHash())) {
						// File was changed externally! Synchronize tasks from physical Excel
						byte[] excelBytes = documentStorageService.readFile(opportunityId, relPath);
						List<SaveProjectScheduleTaskRequest> parsedTasks = projectScheduleExcelParser.parseTasks(excelBytes);
						if (!parsedTasks.isEmpty()) {
							syncTasksFromExcel(schedule, parsedTasks);
							schedule.setFileHash(currentHash);
							schedule.setFileLastModified(documentStorageService.getLastModified(opportunityId, relPath));
							projectScheduleRepository.save(schedule);
						}
					}
				} catch (Exception ex) {
					log.warn("Failed checking physical project schedule file for external changes: {}", ex.getMessage());
				}
			}
		}

		ProjectScheduleResponse response = new ProjectScheduleResponse();

		response.setDurationDays(schedule.getDurationDays());
		response.setTotalTasks(schedule.getTotalTasks());
		response.setCompletedTasks(
		        (int) schedule.getTasks().stream()
		                .filter(task -> {
		                    List<ProjectScheduleTaskBreakdown> breakdowns =
		                            task.getTaskBreakdowns();

		                    if (breakdowns != null && !breakdowns.isEmpty()) {
		                        return breakdowns.stream()
		                                .allMatch(breakdown ->
		                                        "Completed".equalsIgnoreCase(
		                                                breakdown.getStatus() == null
		                                                        ? ""
		                                                        : breakdown.getStatus().trim()
		                                        )
		                                );
		                    }

		                    return "Completed".equalsIgnoreCase(
		                            task.getStatus() == null
		                                    ? ""
		                                    : task.getStatus().trim()
		                    );
		                })
		                .count()
		);
		response.setCriticalTasks(schedule.getCriticalTasks());
		response.setEstimatedHours(schedule.getEstimatedHours());
		response.setBufferPercentage(schedule.getBufferPercentage());
		response.setProjectStartDate(schedule.getProjectStartDate());
		response.setProjectEndDate(schedule.getProjectEndDate());
		response.setWorkingDaysPerWeek(schedule.getWorkingDaysPerWeek());
		response.setWorkingHoursPerDays(schedule.getWorkingHoursPerDay());
		response.setTeamSize(schedule.getTeamSize());

		/*
		 * Calculate all PSR periods for this project.
		 */
		List<PsrPeriod> psrPeriods = psrPeriodCalculator.calculatePeriods(schedule.getProjectStartDate(),
				schedule.getProjectEndDate());

		/*
		 * Find the PSR belonging to the current 15-working-day period.
		 */
		PsrPeriod currentPeriod = psrPeriodCalculator.findPeriodForDate(psrPeriods, LocalDate.now());
		if (currentPeriod == null && !psrPeriods.isEmpty()) {
			if (schedule.getProjectStartDate() != null && LocalDate.now().isBefore(schedule.getProjectStartDate())) {
				currentPeriod = psrPeriods.get(0);
			} else if (schedule.getProjectEndDate() != null && LocalDate.now().isAfter(schedule.getProjectEndDate())) {
				currentPeriod = psrPeriods.get(psrPeriods.size() - 1);
			} else {
				currentPeriod = psrPeriods.get(0);
			}
		}

		if (currentPeriod != null) {

			ProjectStatusReport currentPsr = projectStatusReportRepository
					.findByOpportunityIdAndVersion(opportunityId, currentPeriod.version()).orElse(null);

			if (currentPsr == null) {
				// Fallback to earliest available generated PSR for this opportunity
				currentPsr = projectStatusReportRepository
						.findByOpportunityIdOrderByVersionAsc(opportunityId)
						.stream()
						.findFirst()
						.orElse(null);
			}

			// If still null on first save, generate initial PSR on-demand
			if (currentPsr != null) {
			    psrService.syncPsrFromPhysicalFileIfModified(currentPsr);

			    PsrResponse psrResponse = new PsrResponse(
			            currentPsr.getId(),
			            currentPsr.getFileName(),
			            currentPsr.getFileLocation(),
			            currentPsr.getGeneratedAt(),
			            "GENERATED",
			            currentPsr.getMarkdownContent()
			    );

			    response.setPsr(psrResponse);
			}
		}

		List<ProjectScheduleTask> sortedTasks = schedule.getTasks().stream()
				.sorted(
					    Comparator.comparing(
					        b -> b.getSequence() != null
					                ? b.getSequence()
					                : Integer.MAX_VALUE
					    )
					)
				.toList();

		List<ProjectScheduleTaskResponse> tasks = new ArrayList<>();

		for (ProjectScheduleTask task : sortedTasks) {

			ProjectScheduleTaskResponse taskResponse = mapTaskResponse(task);

//			List<ProjectScheduleTaskBreakdown> sortedBreakdowns = task.getTaskBreakdowns().stream()
//					.sorted(Comparator.comparing(b -> b.getId() != null ? b.getId() : Long.MAX_VALUE)).toList();

			List<ProjectScheduleTaskBreakdown> sortedBreakdowns = task.getTaskBreakdowns().stream()
			        .sorted(Comparator.comparing(
			                b -> b.getSequence() != null
			                        ? b.getSequence()
			                        : Integer.MAX_VALUE))
			        .toList();
			List<TaskBreakdownResponse> breakdownResponses = new ArrayList<>();

			for (ProjectScheduleTaskBreakdown breakdown : sortedBreakdowns) {

				TaskBreakdownResponse br = mapTaskBreakdownResponse(breakdown);

				/*
				 * Determine which PSR period this breakdown belongs to.
				 */
				LocalDate breakdownStart = breakdown.getPlannedStartDate();
				LocalDate breakdownEnd = breakdown.getPlannedEndDate();

				if (breakdownStart == null) {
				    breakdownStart = breakdown.getActualStartDate();
				}

				if (breakdownEnd == null) {
				    breakdownEnd = breakdown.getActualEndDate();
				}

				if (breakdownStart != null || breakdownEnd != null) {

				    LocalDate start =
				            breakdownStart != null ? breakdownStart : breakdownEnd;

				    LocalDate end =
				            breakdownEnd != null ? breakdownEnd : breakdownStart;

				    PsrPeriod breakdownPeriod = psrPeriods.stream()
				            .filter(period ->
				                    !end.isBefore(period.startDate())
				                            && !start.isAfter(period.endDate()))
				            .findFirst()
				            .orElse(null);

				    if (breakdownPeriod != null) {

				        ProjectStatusReport breakdownPsr =
				                projectStatusReportRepository
				                        .findByOpportunityIdAndVersion(
				                                opportunityId,
				                                breakdownPeriod.version()
				                        )
				                        .orElse(null);

				        if (breakdownPsr != null) {
				            psrService.syncPsrFromPhysicalFileIfModified(breakdownPsr);
				            br.setPsrFileName(breakdownPsr.getFileName());
				            br.setPsrFileLocation(breakdownPsr.getFileLocation());
				            br.setPsrMarkdown(breakdownPsr.getMarkdownContent());
				        }
				    }
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
		response.setSprintNumber(task.getSprintNumber());
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

	private TaskBreakdownResponse mapTaskBreakdownResponse(
        ProjectScheduleTaskBreakdown breakdown) {

    TaskBreakdownResponse response = new TaskBreakdownResponse();

    response.setId(breakdown.getId());
    response.setActivityName(breakdown.getActivityName());
    response.setDuration(breakdown.getDuration());
    response.setPlannedStartDate(breakdown.getPlannedStartDate());
    response.setPlannedEndDate(breakdown.getPlannedEndDate());
    response.setActualEndDate(breakdown.getActualEndDate());
    response.setActualStartDate(breakdown.getActualStartDate());

    response.setStatus(
            breakdown.getStatus() != null
                    ? breakdown.getStatus()
                    : "Not Started");

    response.setProgress(breakdown.getProgress());

    response.setSequence(breakdown.getSequence());

    response.setPredecessor(breakdown.getPredecessor());

    response.setActualDuration(
            ChronoUnit.DAYS.between(
                    breakdown.getActualStartDate(),
                    breakdown.getActualEndDate()) + 1);

    return response;
}

	public ProjectScheduleResponse recalculateProjectSchedule(

			Long opportunityId,

			RecalculateProjectScheduleRequest request) {

		return projectScheduleCalculator.recalculate(request);

	}

	private LocalDate calculateProjectEndDate(
        LocalDate startDate,
        int durationDays) {

    LocalDate currentDate =
            moveToNextWorkingDay(startDate);

    int completedWorkingDays = 1;

    while (completedWorkingDays < durationDays) {

        currentDate = currentDate.plusDays(1);

        if (!isWeekend(currentDate)) {
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

	private LocalDate moveToNextWorkingDay(LocalDate date) {

	    LocalDate currentDate = date;

	    while (isWeekend(currentDate)) {
	        currentDate = currentDate.plusDays(1);
	    }

	    return currentDate;
	}

	private boolean isWeekend(LocalDate date) {

	    DayOfWeek day = date.getDayOfWeek();

	    return day == DayOfWeek.SATURDAY
	            || day == DayOfWeek.SUNDAY;
	}
	private Set<Integer> determineAffectedPsrVersions(SaveProjectScheduleRequest request,
			ProjectSchedule existingSchedule) {

		Set<Integer> affectedVersions = new HashSet<>();

		/*
		 * ============================================================ OLD SCHEDULE
		 * ============================================================
		 *
		 * If a schedule already exists, collect the PSR periods represented by the OLD
		 * schedule.
		 *
		 * This is required when an activity is moved from one PSR period to another or
		 * removed from a period.
		 */
		if (existingSchedule != null && existingSchedule.getProjectStartDate() != null
				&& existingSchedule.getProjectEndDate() != null) {

			List<PsrPeriod> oldPeriods = psrPeriodCalculator.calculatePeriods(existingSchedule.getProjectStartDate(),
					existingSchedule.getProjectEndDate());

			/*
			 * Mark periods containing existing schedule activities.
			 */
			if (existingSchedule.getTasks() != null) {

				for (ProjectScheduleTask task : existingSchedule.getTasks()) {

					/*
					 * Task-level dates.
					 */
					addAffectedPeriodForDate(affectedVersions, oldPeriods, task.getPlannedStartDate());

					addAffectedPeriodForDate(affectedVersions, oldPeriods, task.getActualStartDate());

					/*
					 * Breakdown-level dates.
					 *
					 * We only inspect the already-existing in-memory breakdown collection here. We
					 * are NOT replacing it.
					 */
					if (task.getTaskBreakdowns() != null) {

						for (ProjectScheduleTaskBreakdown breakdown : task.getTaskBreakdowns()) {

							addAffectedPeriodForDates(affectedVersions, oldPeriods, breakdown.getPlannedStartDate(),
									breakdown.getPlannedEndDate(), breakdown.getActualStartDate(),
									breakdown.getActualEndDate());
						}
					}
				}
			}
		}

		/*
		 * ============================================================ NEW SCHEDULE
		 * ============================================================
		 *
		 * Now calculate the periods represented by the NEW request.
		 */
		List<PsrPeriod> newPeriods = psrPeriodCalculator.calculatePeriods(request.getProjectStartDate(),
				request.getProjectEndDate());

		/*
		 * If there are no tasks, the schedule-period change itself still needs
		 * synchronization.
		 *
		 * Mark all new periods.
		 */
		if (request.getTasks() == null) {

			for (PsrPeriod period : newPeriods) {

				affectedVersions.add(period.version());
			}

			return affectedVersions;
		}

		/*
		 * ============================================================ TASKS +
		 * BREAKDOWNS FROM NEW REQUEST
		 * ============================================================
		 */
		for (SaveProjectScheduleTaskRequest taskRequest : request.getTasks()) {

			/*
			 * -------------------------------------------------------- TASK-LEVEL DATES
			 * --------------------------------------------------------
			 */
			addAffectedPeriodForDate(affectedVersions, newPeriods, taskRequest.getPlannedStartDate());

			addAffectedPeriodForDate(affectedVersions, newPeriods, taskRequest.getActualStartDate());

			/*
			 * -------------------------------------------------------- BREAKDOWN-LEVEL
			 * DATES --------------------------------------------------------
			 */
			if (taskRequest.getTaskBreakdowns() == null) {
				continue;
			}

			for (SaveTaskBreakdownRequest breakdownRequest : taskRequest.getTaskBreakdowns()) {

				addAffectedPeriodForDates(affectedVersions, newPeriods, breakdownRequest.getPlannedStartDate(),
						breakdownRequest.getPlannedEndDate(), breakdownRequest.getActualStartDate(),
						breakdownRequest.getActualEndDate());
			}
		}

		// On initial save or if no specific task period was detected, ensure period 1 is included
		if (existingSchedule == null || affectedVersions.isEmpty()) {
			if (!newPeriods.isEmpty()) {
				affectedVersions.add(1);
			}
		}

		return affectedVersions;
	}

	private void addAffectedPeriodForDate(Set<Integer> affectedVersions, List<PsrPeriod> periods, LocalDate date) {

		if (date == null) {
			return;
		}

		PsrPeriod period = psrPeriodCalculator.findPeriodForDate(periods, date);

		if (period != null) {

			affectedVersions.add(period.version());
		}
	}

	private void addAffectedPeriodForDates(Set<Integer> affectedVersions, List<PsrPeriod> periods,
			LocalDate plannedStart, LocalDate plannedEnd, LocalDate actualStart, LocalDate actualEnd) {

		/*
		 * Prefer planned dates.
		 */
		LocalDate start = plannedStart != null ? plannedStart : actualStart;

		LocalDate end = plannedEnd != null ? plannedEnd : actualEnd;

		/*
		 * If only one date exists, that single date determines the PSR period.
		 */
		if (start == null && end == null) {
			return;
		}

		if (start == null) {
			start = end;
		}

		if (end == null) {
			end = start;
		}

		/*
		 * An activity can span multiple PSR periods.
		 *
		 * Mark every period it overlaps.
		 */
		for (PsrPeriod period : periods) {

			if (!end.isBefore(period.startDate()) && !start.isAfter(period.endDate())) {

				affectedVersions.add(period.version());
			}
		}
	}

	private boolean datesOverlap(LocalDate activityStart, LocalDate activityEnd, LocalDate periodStart,
			LocalDate periodEnd) {

		LocalDate start = activityStart != null ? activityStart : activityEnd;

		LocalDate end = activityEnd != null ? activityEnd : activityStart;

		if (start == null || end == null) {
			return false;
		}

		return !end.isBefore(periodStart) && !start.isAfter(periodEnd);
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

	

private SaveProjectScheduleRequest convertToSaveProjectScheduleRequest(
        ProjectScheduleResponse response) {

    SaveProjectScheduleRequest request =
            new SaveProjectScheduleRequest();

    request.setProjectStartDate(response.getProjectStartDate());
    request.setProjectEndDate(response.getProjectEndDate());

    request.setTeamSize(response.getTeamSize());
    request.setWorkingDaysPerWeek(response.getWorkingDaysPerWeek());
    request.setWorkingHoursPerDay(response.getWorkingHoursPerDays());
    request.setBufferPercentage(response.getBufferPercentage());

    request.setDurationDays(response.getDurationDays());
    request.setDuration(response.getDurationDays());

    request.setTotalTasks(response.getTotalTasks());
    request.setCompletedTasks(response.getCompletedTasks());
    request.setCriticalTasks(response.getCriticalTasks());
    request.setEstimatedHours(response.getEstimatedHours());

    List<SaveProjectScheduleTaskRequest> taskRequests =
            new ArrayList<>();

    if (response.getTasks() != null) {

        for (ProjectScheduleTaskResponse task : response.getTasks()) {

            SaveProjectScheduleTaskRequest taskRequest =
                    new SaveProjectScheduleTaskRequest();

            taskRequest.setSequence(task.getSequence());
            taskRequest.setSprintNumber(task.getSprintNumber());
            taskRequest.setTaskName(task.getTaskName());
            taskRequest.setDuration(task.getDuration());

            taskRequest.setPlannedStartDate(task.getPlannedStartDate());
            taskRequest.setPlannedEndDate(task.getPlannedEndDate());

            taskRequest.setActualStartDate(task.getActualStartDate());
            taskRequest.setActualEndDate(task.getActualEndDate());

            taskRequest.setPredecessor(task.getPredecessor());
            taskRequest.setStatus(task.getStatus());

            List<SaveTaskBreakdownRequest> breakdownRequests =
                    new ArrayList<>();

            if (task.getTaskBreakdowns() != null) {

                for (TaskBreakdownResponse breakdown :
                        task.getTaskBreakdowns()) {

                    SaveTaskBreakdownRequest breakdownRequest =
                            new SaveTaskBreakdownRequest();

                    breakdownRequest.setActivityName(
                            breakdown.getActivityName());

                    breakdownRequest.setDuration(
                            breakdown.getDuration());

                    breakdownRequest.setPlannedStartDate(
                            breakdown.getPlannedStartDate());

                    breakdownRequest.setPlannedEndDate(
                            breakdown.getPlannedEndDate());

                    breakdownRequest.setActualStartDate(
                            breakdown.getActualStartDate());

                    breakdownRequest.setActualEndDate(
                            breakdown.getActualEndDate());

                    breakdownRequest.setStatus(
                            breakdown.getStatus());

                    breakdownRequest.setProgress(
                            breakdown.getProgress());

                    breakdownRequest.setSequence(
                            breakdown.getSequence());

                    breakdownRequest.setPredecessor(
                            breakdown.getPredecessor());

                    breakdownRequests.add(breakdownRequest);
                }
            }

            taskRequest.setTaskBreakdowns(breakdownRequests);

            taskRequests.add(taskRequest);
        }
    }

    request.setTasks(taskRequests);

    return request;
}

	private void syncTasksFromExcel(ProjectSchedule schedule, List<SaveProjectScheduleTaskRequest> parsedTasks) {
		if (schedule.getTasks() == null || parsedTasks == null) return;
		Map<String, ProjectScheduleTask> taskByName = schedule.getTasks().stream()
				.collect(Collectors.toMap(t -> t.getTaskName().trim().toLowerCase(), t -> t, (a, b) -> a));

		for (SaveProjectScheduleTaskRequest pt : parsedTasks) {
			if (pt.getTaskName() == null) continue;
			ProjectScheduleTask existing = taskByName.get(pt.getTaskName().trim().toLowerCase());
			if (existing != null) {
				if (pt.getPlannedStartDate() != null) existing.setPlannedStartDate(pt.getPlannedStartDate());
				if (pt.getPlannedEndDate() != null) existing.setPlannedEndDate(pt.getPlannedEndDate());
				if (pt.getDuration() != null) existing.setDuration(pt.getDuration());
				if (pt.getActualStartDate() != null) existing.setActualStartDate(pt.getActualStartDate());
				if (pt.getActualEndDate() != null) existing.setActualEndDate(pt.getActualEndDate());
				if (pt.getStatus() != null) existing.setStatus(pt.getStatus());

				if (pt.getTaskBreakdowns() != null && existing.getTaskBreakdowns() != null) {
					Map<String, ProjectScheduleTaskBreakdown> breakdownByName = existing.getTaskBreakdowns().stream()
							.collect(Collectors.toMap(b -> b.getActivityName().trim().toLowerCase(), b -> b, (a, b) -> a));
					for (SaveTaskBreakdownRequest pb : pt.getTaskBreakdowns()) {
						if (pb.getActivityName() == null) continue;
						ProjectScheduleTaskBreakdown existingBd = breakdownByName.get(pb.getActivityName().trim().toLowerCase());
						if (existingBd != null) {
							if (pb.getPlannedStartDate() != null) existingBd.setPlannedStartDate(pb.getPlannedStartDate());
							if (pb.getPlannedEndDate() != null) existingBd.setPlannedEndDate(pb.getPlannedEndDate());
							if (pb.getDuration() != null) existingBd.setDuration(pb.getDuration());
							if (pb.getActualStartDate() != null) existingBd.setActualStartDate(pb.getActualStartDate());
							if (pb.getActualEndDate() != null) existingBd.setActualEndDate(pb.getActualEndDate());
							if (pb.getStatus() != null) existingBd.setStatus(pb.getStatus());
						}
					}
				}
			}
		}
	}

	private void saveOrMergeBreakdowns(ProjectScheduleTask savedTask, List<SaveTaskBreakdownRequest> breakdownRequests) {
		if (breakdownRequests == null) return;

		List<ProjectScheduleTaskBreakdown> existingBreakdowns = savedTask.getTaskBreakdowns();
		if (existingBreakdowns == null) {
			existingBreakdowns = new ArrayList<>();
			savedTask.setTaskBreakdowns(existingBreakdowns);
		}

		Map<Long, ProjectScheduleTaskBreakdown> existingBreakdownMap = existingBreakdowns.stream()
				.filter(b -> b.getId() != null)
				.collect(Collectors.toMap(ProjectScheduleTaskBreakdown::getId, b -> b, (a, b) -> a));

		Map<String, ProjectScheduleTaskBreakdown> existingByName = existingBreakdowns.stream()
				.filter(b -> b.getActivityName() != null && !b.getActivityName().isBlank())
				.collect(Collectors.toMap(b -> b.getActivityName().trim().toLowerCase(), b -> b, (a, b) -> a));

		List<Long> requestBreakdownIds = new ArrayList<>();

		for (SaveTaskBreakdownRequest breakdownRequest : breakdownRequests) {
			if (breakdownRequest.getActivityName() == null || breakdownRequest.getActivityName().isBlank()) continue;

			String cleanName = breakdownRequest.getActivityName().trim();
			ProjectScheduleTaskBreakdown breakdown = null;

			if (breakdownRequest.getId() != null) {
				breakdown = existingBreakdownMap.get(breakdownRequest.getId());
			}

			if (breakdown == null) {
				breakdown = existingByName.get(cleanName.toLowerCase());
			}

			if (breakdown == null) {
				breakdown = new ProjectScheduleTaskBreakdown();
				breakdown.setProjectScheduleTask(savedTask);
				existingBreakdowns.add(breakdown);
			}

			breakdown.setActivityName(cleanName);
			breakdown.setDuration(breakdownRequest.getDuration());
			breakdown.setPlannedStartDate(breakdownRequest.getPlannedStartDate());
			breakdown.setPlannedEndDate(breakdownRequest.getPlannedEndDate());
			breakdown.setStatus(breakdownRequest.getStatus());
			breakdown.setProgress(breakdownRequest.getProgress());
			breakdown.setSequence(breakdownRequest.getSequence());
			breakdown.setPredecessor(breakdownRequest.getPredecessor());
			breakdown.setActualStartDate(
					breakdownRequest.getActualStartDate() != null ? breakdownRequest.getActualStartDate()
							: breakdownRequest.getPlannedStartDate());
			breakdown.setActualEndDate(
					breakdownRequest.getActualEndDate() != null ? breakdownRequest.getActualEndDate()
							: breakdownRequest.getPlannedEndDate());

			ProjectScheduleTaskBreakdown savedBd = projectScheduleTaskBreakdownRepository.save(breakdown);
			if (savedBd.getId() != null) {
				requestBreakdownIds.add(savedBd.getId());
				existingBreakdownMap.put(savedBd.getId(), savedBd);
				existingByName.put(cleanName.toLowerCase(), savedBd);
			}
		}

		existingBreakdowns.removeIf(
				breakdown -> breakdown.getId() != null && !requestBreakdownIds.contains(breakdown.getId()));

		projectScheduleTaskRepository.save(savedTask);
	}

}
