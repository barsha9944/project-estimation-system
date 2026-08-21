package com.projectestimation.backend.psr.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PsrActivityDto {

    private Integer sequence;

    private String taskName;

    private String activityName;

    private String status;

    private Integer progress;

    private String plannedEndDate;

    private String actualEndDate;
}