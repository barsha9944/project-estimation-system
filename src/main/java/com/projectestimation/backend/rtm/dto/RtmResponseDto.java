package com.projectestimation.backend.rtm.dto;

import java.util.List;

public record RtmResponseDto(
        Long opportunityId,
        String opportunityName,
        String clientName,
        List<RtmRowDto> rows
) {
}
