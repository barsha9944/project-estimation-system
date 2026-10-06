package com.projectestimation.backend.sdd.dto;

public record AmendmentDto(
        Integer serialNumber,
        String sectionNoOrPageNo,
        String descriptionOfAmendment,
        String approvedBy,
        String changeRequestNoAndDate
) {}