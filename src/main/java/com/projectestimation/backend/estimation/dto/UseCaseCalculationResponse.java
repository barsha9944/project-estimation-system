package com.projectestimation.backend.estimation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UseCaseCalculationResponse {

    private int simpleUseCases;

    private int averageUseCases;

    private int complexUseCases;

    private int uucp;
}
