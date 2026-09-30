package com.projectestimation.backend.srs.dto;

import java.util.List;

public record OverallProductRequirementsDto(
        String productPerspective,
        List<String> productComponents
) {
}