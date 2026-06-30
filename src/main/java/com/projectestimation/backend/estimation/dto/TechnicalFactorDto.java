package com.projectestimation.backend.estimation.dto;

import lombok.Data;

@Data
public class TechnicalFactorDto {

    private String factorName;

    private Double multiplier;

    private Integer magnitude;

    private String description;
}