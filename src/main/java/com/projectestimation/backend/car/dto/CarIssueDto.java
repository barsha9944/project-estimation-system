
package com.projectestimation.backend.car.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CarIssueDto {

    private Long opportunityId;

    private String carType;

    private String triggerForCar;

    private String problemStatement;

    private String originatorName;

    private String performanceMonitoringDetails;

    private String status;
}
