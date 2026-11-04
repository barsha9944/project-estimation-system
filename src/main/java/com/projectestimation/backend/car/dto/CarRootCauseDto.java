
package com.projectestimation.backend.car.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CarRootCauseDto {

    private Long carId;

    private String causalAnalysisTechnique;

    private String rootCauseSummary;

    private String fiveWhysDetails;

    private String fishboneAnalysis;

    private String paretoAnalysis;

    private String histogramAnalysis;

    private String actionProposedSummary;

    private String lessonsLearned;
}
