package com.projectestimation.backend.pmp.dto;

public record EnvironmentDto(

        PmpItemDto development,

        PmpItemDto testing,

        PmpItemDto operation

) {
}