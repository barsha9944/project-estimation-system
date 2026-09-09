package com.projectestimation.backend.pmp.dto;

public record CustomerInterfaceDto(

        String name,

        String designation,

        String phoneNumber,

        String faxNumber,

        String email,

        String skypeId

) {
}