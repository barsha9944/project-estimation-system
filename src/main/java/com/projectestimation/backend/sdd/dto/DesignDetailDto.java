package com.projectestimation.backend.sdd.dto;

public record DesignDetailDto(
        Integer serialNumber,
        String requestJsp,
        String responseJsp,
        String controller,
        String dao,
        String entityName
) {}