
package com.projectestimation.backend.car.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CarCreateRequest {

	private Long opportunityId;

    @NotBlank
    private String carType;

    @NotBlank
    private String triggerForCar;

    @NotBlank
    private String problemStatement;

    private String originatorName;

    private String performanceMonitoringDetails;

    private String causalAnalysisTechnique;
}
