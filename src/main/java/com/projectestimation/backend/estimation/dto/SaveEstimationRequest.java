package com.projectestimation.backend.estimation.dto;

import java.math.BigDecimal;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SaveEstimationRequest {

    private Long opportunityId;

    private List<ActorDto> actors;

    private List<UseCaseDto> useCases;

    private List<TechnicalFactorDto> technicalFactors;

    private List<EnvironmentalFactorDto> environmentalFactors;

    private Integer actorWeight;

    private Integer uucp;

    private Double tcf;

    private Double ef;

    private Double ucp;

    private Double benchmarkProductivityRatio;

    private Double hoursOfEffort;
    
    private String currency;

    private BigDecimal hourlyRate;

    private BigDecimal projectPrice;

}