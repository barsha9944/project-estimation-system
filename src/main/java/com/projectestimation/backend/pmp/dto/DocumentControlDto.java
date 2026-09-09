package com.projectestimation.backend.pmp.dto;

import java.util.List;

public record DocumentControlDto(

        List<PmpItemDto> releaseHistory,

        List<PmpItemDto> circulationDetails,

        List<PmpItemDto> amendments

) {
}