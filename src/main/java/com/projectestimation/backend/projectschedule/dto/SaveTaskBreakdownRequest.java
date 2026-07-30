package com.projectestimation.backend.projectschedule.dto;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SaveTaskBreakdownRequest {

	private String activityName;

    private Integer duration;

    private LocalDate plannedStartDate;

    private LocalDate plannedEndDate;
    
    private LocalDate actualStartDate;

    private LocalDate actualEndDate;
}
