
package com.projectestimation.backend.car.dto;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CarEffectivenessDto {

    private Long id;

    private Long carId;

    private LocalDate reviewDate;

    private String evaluationPeriod;

    private String expectedOutcome;

    private String actualOutcome;

    private String beforeImplementationMetric;

    private String afterImplementationMetric;

    private String effectivenessResult;

    private String supportingEvidence;

    private String reviewedBy;

    private String additionalActionsRequired;
}
