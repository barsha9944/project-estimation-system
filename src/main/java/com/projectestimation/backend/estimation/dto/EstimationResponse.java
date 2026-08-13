package com.projectestimation.backend.estimation.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EstimationResponse {

    private List<EstimationActorResponse> actors;

    private List<EstimationUseCaseResponse> useCases;

    private List<EstimationTechnicalFactorResponse> technicalFactors;

    private List<EstimationEnvironmentalFactorResponse> environmentalFactors;

    private Integer actorWeight;

    private Integer uucp;

    private Double tcf;

    private Double ef;

    private Double ucp;

    private Double benchmarkProductivityRatio;

    private Double hoursOfEffort;
    
    private Boolean estimationCompleted;

    private Boolean proposalCompleted;

    private Boolean workScheduleCompleted;

    private Boolean summaryMetricsCompleted;

    private Boolean nonFunctionalCompleted;
}
