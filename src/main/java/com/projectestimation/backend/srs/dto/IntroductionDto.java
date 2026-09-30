package com.projectestimation.backend.srs.dto;

import java.util.List;

public record IntroductionDto(
        String purpose,
        String backgroundOfDevelopment,
        String scopeOfTheSystem,
        List<String> assumptionsAndDependencies
) {
}