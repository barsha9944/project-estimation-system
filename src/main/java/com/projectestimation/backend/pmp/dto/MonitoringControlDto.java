package com.projectestimation.backend.pmp.dto;

import java.util.List;

public record MonitoringControlDto(

        List<PmpItemDto> monitoringMechanism,

        List<PmpItemDto> quantitativeMonitoring

) {
}