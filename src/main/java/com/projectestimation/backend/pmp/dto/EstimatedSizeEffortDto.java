package com.projectestimation.backend.pmp.dto;

import java.util.List;

public record EstimatedSizeEffortDto(

        List<PmpItemDto> sizeDetails,

        List<PmpItemDto> effortDetails

) {
}