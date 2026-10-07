package com.projectestimation.backend.mom.dto;

public record MomAgendaItemDto(
        Integer item,
        String actionItems,
        String presenter
) {
}