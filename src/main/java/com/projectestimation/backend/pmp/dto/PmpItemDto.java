package com.projectestimation.backend.pmp.dto;

public record PmpItemDto(

        String name,

        String description,

        String responsible,

        String timing,

        String target,

        String status

) {
}