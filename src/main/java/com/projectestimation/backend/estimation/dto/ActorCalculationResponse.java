package com.projectestimation.backend.estimation.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ActorCalculationResponse {

    private int simpleActors;

    private int averageActors;

    private int complexActors;

    private int actorWeight;
}