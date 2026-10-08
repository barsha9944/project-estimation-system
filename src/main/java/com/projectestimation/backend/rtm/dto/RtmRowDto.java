package com.projectestimation.backend.rtm.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RtmRowDto {

    private String serialNo;
    private String requirementId;
    private String requirementName;
    private String requirementDescription;
    private String srsSubsection;
    private String sddSubsection;
    private String sourceCodeReference;
    private String unitTestCaseReference;
    private String systemTestCaseReference;
    private String acceptanceTestCaseReference;
    private String status;
}
