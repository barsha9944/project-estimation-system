package com.projectestimation.backend.pmp.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public record PmpItemDto(

        String name,

        String description,

        String responsible,

        String timing,

        String target,

        String status

) {

    @JsonCreator
    public PmpItemDto(
            @JsonProperty("name") String name,
            @JsonProperty("description") String description,
            @JsonProperty("responsible") String responsible,
            @JsonProperty("timing") String timing,
            @JsonProperty("target") String target,
            @JsonProperty("status") String status
    ) {
        this.name = name;
        this.description = description;
        this.responsible = responsible;
        this.timing = timing;
        this.target = target;
        this.status = status;
    }
}