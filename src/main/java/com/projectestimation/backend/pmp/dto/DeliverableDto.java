package com.projectestimation.backend.pmp.dto;

public record DeliverableDto(

        Integer serialNumber,

        String itemDescription,

        String deliveryDate,

        String deliveryLocation,

        String quantity,

        String remarks

) {
}