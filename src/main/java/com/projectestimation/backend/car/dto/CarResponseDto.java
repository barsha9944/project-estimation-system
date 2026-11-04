
package com.projectestimation.backend.car.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CarResponseDto {

    private Long id;

    private Long opportunityId;

    private String opportunityName;

    private String carType;

    private String triggerForCar;

    private String problemStatement;

    private String originatorName;

    private String performanceMonitoringDetails;

    private String causalAnalysisTechnique;

    private String rootCauseSummary;

    private String fiveWhysDetails;

    private String fishboneAnalysis;

    private String paretoAnalysis;

    private String histogramAnalysis;

    private String actionProposedSummary;

    private String lessonsLearned;

    private String status;

    private List<CarActionDto> actions;

    private List<CarEffectivenessDto> effectivenessReviews;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
