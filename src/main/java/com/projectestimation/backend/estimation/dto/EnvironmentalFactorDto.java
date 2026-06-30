package com.projectestimation.backend.estimation.dto;


import lombok.Data;

@Data
public class EnvironmentalFactorDto {

    private String factorName;

    private Double multiplier;

    private Integer magnitude;

    private String description;
}