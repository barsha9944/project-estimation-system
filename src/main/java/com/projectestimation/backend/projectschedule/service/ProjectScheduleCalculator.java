package com.projectestimation.backend.projectschedule.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.projectestimation.backend.common.exception.BadRequestException;
import com.projectestimation.backend.projectschedule.dto.ProjectScheduleResponse;
import com.projectestimation.backend.projectschedule.dto.ProjectScheduleTaskResponse;
import com.projectestimation.backend.projectschedule.dto.RecalculateProjectScheduleRequest;
import com.projectestimation.backend.projectschedule.dto.SaveProjectScheduleTaskRequest;

@Component
public class ProjectScheduleCalculator {

	public ProjectScheduleResponse recalculate(RecalculateProjectScheduleRequest request) {

		return switch (request.getEditedField()) {

		case "plannedStartDate" -> recalculateFromPlannedStart(request);

		case "plannedEndDate" -> recalculateFromPlannedEnd(request);

		case "actualStartDate" -> recalculateFromActualStart(request);

		case "actualEndDate" -> recalculateFromActualEnd(request);

		default -> throw new BadRequestException("Unsupported edited field : " + request.getEditedField());

		};

	}

	private ProjectScheduleResponse recalculateFromPlannedStart(RecalculateProjectScheduleRequest request) {

		ProjectScheduleResponse response = new ProjectScheduleResponse();

		response.setProjectStartDate(request.getProjectStartDate());
		response.setTeamSize(request.getTeamSize());
		response.setWorkingDaysPerWeek(request.getWorkingDaysPerWeek());
		response.setWorkingHoursPerDays(request.getWorkingHoursPerDay());
		response.setBufferPercentage(request.getBufferPercentage());
		response.setEstimatedHours(request.getEstimatedHours());
		response.setCompletedTasks(request.getCompletedTasks());
		response.setCriticalTasks(request.getCriticalTasks());
		response.setTotalTasks(request.getTasks().size());

		List<ProjectScheduleTaskResponse> taskResponses = new ArrayList<>();

		int editedIndex = findEditedTaskIndex(request);

		LocalDate currentStart = null;

		for (int i = 0; i < request.getTasks().size(); i++) {

			SaveProjectScheduleTaskRequest taskRequest = request.getTasks().get(i);

			ProjectScheduleTaskResponse task = new ProjectScheduleTaskResponse();

			task.setId(taskRequest.getId());
			task.setSequence(taskRequest.getSequence());
			task.setTaskName(taskRequest.getTaskName());
			task.setDuration(taskRequest.getDuration());
			task.setStatus(taskRequest.getStatus());
			task.setPredecessor(taskRequest.getPredecessor());

//			task.setActualStartDate(taskRequest.getActualStartDate());
//			task.setActualEndDate(taskRequest.getActualEndDate());

			task.setActualStartDate(taskRequest.getActualStartDate() != null ? taskRequest.getActualStartDate()
					: taskRequest.getPlannedStartDate());

			task.setActualEndDate(taskRequest.getActualEndDate() != null ? taskRequest.getActualEndDate()
					: taskRequest.getPlannedEndDate());

			if (i < editedIndex) {

				task.setPlannedStartDate(taskRequest.getPlannedStartDate());

				task.setPlannedEndDate(taskRequest.getPlannedEndDate());

			} else {

				if (i == editedIndex) {

					currentStart = taskRequest.getPlannedStartDate();

				}

				task.setPlannedStartDate(currentStart);

				LocalDate plannedEnd = calculateWorkingEndDate(currentStart, taskRequest.getDuration());

				task.setPlannedEndDate(plannedEnd);

				currentStart = nextWorkingDay(plannedEnd);

			}

			taskResponses.add(task);

		}

		response.setTasks(taskResponses);

		if (!taskResponses.isEmpty()) {

			response.setDurationDays(

					(int) ChronoUnit.DAYS.between(

							taskResponses.get(0).getPlannedStartDate(),

							taskResponses.get(taskResponses.size() - 1).getPlannedEndDate()

					) + 1

			);

		}

		return response;

	}

	private ProjectScheduleResponse recalculateFromPlannedEnd(RecalculateProjectScheduleRequest request) {

		throw new UnsupportedOperationException("Recalculation from plannedEndDate is not supported yet.");

	}

	private ProjectScheduleResponse recalculateFromActualStart(RecalculateProjectScheduleRequest request) {

		ProjectScheduleResponse response = new ProjectScheduleResponse();

		response.setProjectStartDate(request.getProjectStartDate());
		response.setTeamSize(request.getTeamSize());
		response.setWorkingDaysPerWeek(request.getWorkingDaysPerWeek());
		response.setWorkingHoursPerDays(request.getWorkingHoursPerDay());
		response.setBufferPercentage(request.getBufferPercentage());
		response.setEstimatedHours(request.getEstimatedHours());
		response.setCompletedTasks(request.getCompletedTasks());
		response.setCriticalTasks(request.getCriticalTasks());
		response.setTotalTasks(request.getTasks().size());

		List<ProjectScheduleTaskResponse> taskResponses = new ArrayList<>();

		int editedIndex = findEditedTaskIndex(request);

		LocalDate currentStart = null;

		for (int i = 0; i < request.getTasks().size(); i++) {

			SaveProjectScheduleTaskRequest taskRequest = request.getTasks().get(i);

			ProjectScheduleTaskResponse task = new ProjectScheduleTaskResponse();

			task.setId(taskRequest.getId());
			task.setSequence(taskRequest.getSequence());
			task.setTaskName(taskRequest.getTaskName());
			task.setDuration(taskRequest.getDuration());
			task.setStatus(taskRequest.getStatus());
			task.setPredecessor(taskRequest.getPredecessor());

//			task.setActualStartDate(taskRequest.getActualStartDate());
//			task.setActualEndDate(taskRequest.getActualEndDate());
			task.setActualStartDate(taskRequest.getActualStartDate() != null ? taskRequest.getActualStartDate()
					: taskRequest.getPlannedStartDate());

			task.setActualEndDate(taskRequest.getActualEndDate() != null ? taskRequest.getActualEndDate()
					: taskRequest.getPlannedEndDate());

			if (i < editedIndex) {

				task.setPlannedStartDate(taskRequest.getPlannedStartDate());
				task.setPlannedEndDate(taskRequest.getPlannedEndDate());

			} else {

				if (i == editedIndex) {

					currentStart = taskRequest.getActualStartDate();

				}

				task.setPlannedStartDate(currentStart);

				LocalDate plannedEnd = calculateWorkingEndDate(currentStart, taskRequest.getDuration());

				task.setPlannedEndDate(plannedEnd);

				currentStart = nextWorkingDay(plannedEnd);

			}

			taskResponses.add(task);

		}

		response.setTasks(taskResponses);

		if (!taskResponses.isEmpty()) {

			response.setDurationDays((int) ChronoUnit.DAYS.between(taskResponses.get(0).getPlannedStartDate(),
					taskResponses.get(taskResponses.size() - 1).getPlannedEndDate()) + 1);

		}

		return response;

	}

	private ProjectScheduleResponse recalculateFromActualEnd(RecalculateProjectScheduleRequest request) {

		ProjectScheduleResponse response = new ProjectScheduleResponse();

		response.setProjectStartDate(request.getProjectStartDate());
		response.setTeamSize(request.getTeamSize());
		response.setWorkingDaysPerWeek(request.getWorkingDaysPerWeek());
		response.setWorkingHoursPerDays(request.getWorkingHoursPerDay());
		response.setBufferPercentage(request.getBufferPercentage());
		response.setEstimatedHours(request.getEstimatedHours());
		response.setCompletedTasks(request.getCompletedTasks());
		response.setCriticalTasks(request.getCriticalTasks());
		response.setTotalTasks(request.getTasks().size());

		List<ProjectScheduleTaskResponse> taskResponses = new ArrayList<>();

		int editedIndex = findEditedTaskIndex(request);

		LocalDate currentStart = null;

		for (int i = 0; i < request.getTasks().size(); i++) {

			SaveProjectScheduleTaskRequest taskRequest = request.getTasks().get(i);

			ProjectScheduleTaskResponse task = new ProjectScheduleTaskResponse();

			task.setId(taskRequest.getId());
			task.setSequence(taskRequest.getSequence());
			task.setTaskName(taskRequest.getTaskName());
			task.setDuration(taskRequest.getDuration());
			task.setStatus(taskRequest.getStatus());
			task.setPredecessor(taskRequest.getPredecessor());

//	        task.setActualStartDate(taskRequest.getActualStartDate());
//	        task.setActualEndDate(taskRequest.getActualEndDate());

			task.setActualStartDate(taskRequest.getActualStartDate() != null ? taskRequest.getActualStartDate()
					: taskRequest.getPlannedStartDate());

			task.setActualEndDate(taskRequest.getActualEndDate() != null ? taskRequest.getActualEndDate()
					: taskRequest.getPlannedEndDate());

			if (i < editedIndex) {

				task.setPlannedStartDate(taskRequest.getPlannedStartDate());
				task.setPlannedEndDate(taskRequest.getPlannedEndDate());

			} else if (i == editedIndex) {

				task.setPlannedStartDate(taskRequest.getPlannedStartDate());

				LocalDate end = taskRequest.getActualEndDate();

				if (end == null) {
					end = taskRequest.getPlannedEndDate();
				}

				task.setPlannedEndDate(end);

				currentStart = nextWorkingDay(end);

			} else {

				task.setPlannedStartDate(currentStart);

				LocalDate plannedEnd = calculateWorkingEndDate(currentStart, taskRequest.getDuration());

				task.setPlannedEndDate(plannedEnd);

				currentStart = nextWorkingDay(plannedEnd);

			}

			taskResponses.add(task);

		}

		response.setTasks(taskResponses);

		if (!taskResponses.isEmpty()) {

			response.setDurationDays(

					(int) ChronoUnit.DAYS.between(

							taskResponses.get(0).getPlannedStartDate(),

							taskResponses.get(taskResponses.size() - 1).getPlannedEndDate()

					) + 1

			);

		}

		return response;

	}

	private LocalDate calculateWorkingEndDate(LocalDate start, Integer duration) {

		if (start == null || duration == null || duration <= 0) {
			return start;
		}

		LocalDate end = start;

		int remainingDays = duration - 1;

		while (remainingDays > 0) {

			end = end.plusDays(1);

			if (isWorkingDay(end)) {
				remainingDays--;
			}

		}

		return end;

	}

	private LocalDate nextWorkingDay(LocalDate date) {

		LocalDate next = date.plusDays(1);

		while (!isWorkingDay(next)) {

			next = next.plusDays(1);

		}

		return next;

	}

	private boolean isWorkingDay(LocalDate date) {

		return date.getDayOfWeek() != DayOfWeek.SATURDAY && date.getDayOfWeek() != DayOfWeek.SUNDAY;

	}

	private int findEditedTaskIndex(RecalculateProjectScheduleRequest request) {

		for (int i = 0; i < request.getTasks().size(); i++) {

			if (request.getTasks().get(i).getSequence().equals(request.getEditedSequence())) {

				return i;

			}

		}

		throw new BadRequestException("Edited task not found.");

	}

}
