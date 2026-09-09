package com.projectestimation.backend.pmp.dto;

import java.util.List;

public record ScheduleDto(

        List<PmpItemDto> scheduleItems

) {
}