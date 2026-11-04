
package com.projectestimation.backend.car.dto;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CarActionDto {

    private Long id;

    private String actionCategory;

    private String actionDescription;

    private String assignedTo;

    private String priority;

    private LocalDate targetCompletionDate;

    private String actionTaken;

    private LocalDate implementationDate;

    private String evidenceReference;

    private String status;
}
