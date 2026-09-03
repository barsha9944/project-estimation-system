package com.projectestimation.backend.projectschedule.dto;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TaskBreakdownResponse {

	private Long id;
	
	private String activityName;

	private Integer duration;

	private LocalDate plannedStartDate;

	private LocalDate plannedEndDate;

	private LocalDate actualStartDate;

	private LocalDate actualEndDate;

	private Long actualDuration;
	private String status;
	
	private Integer progress;
	
	private String psrFileName;
	
	private String psrFileLocation;
	
	private String psrMarkdown;
}