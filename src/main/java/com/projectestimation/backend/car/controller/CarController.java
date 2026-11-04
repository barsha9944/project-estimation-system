
package com.projectestimation.backend.car.controller;

import com.projectestimation.backend.car.dto.CarCreateRequest;
import com.projectestimation.backend.car.dto.CarResponseDto;
import com.projectestimation.backend.car.service.CarExcelService;
import com.projectestimation.backend.car.service.CarRootCauseAnalysisService;
import com.projectestimation.backend.car.service.CarService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/opportunities/{opportunityId}/car")
@RequiredArgsConstructor
public class CarController {

    private final CarService carService;
    private final CarRootCauseAnalysisService rootCauseAnalysisService;
    private final CarExcelService excelService;

    /**
     * API 1: Create CAR and generate AI analysis.
     */
    @PostMapping
    public ResponseEntity<CarResponseDto> createCar(
            @PathVariable Long opportunityId,
            @Valid @RequestBody CarCreateRequest request) {

        request.setOpportunityId(opportunityId);

        CarResponseDto createdCar = carService.createCar(request);

        rootCauseAnalysisService.generateAnalysis(createdCar.getId());

        return ResponseEntity.ok(
                carService.getCar(createdCar.getId())
        );
    }

    /**
     * API 2: Retrieve all CAR records for an opportunity.
     */
    @GetMapping
    public ResponseEntity<List<CarResponseDto>> getAllCars(
            @PathVariable Long opportunityId) {

        return ResponseEntity.ok(
                carService.getCarsByOpportunity(opportunityId)
        );
    }

    /**
     * API 3: Download CAR Excel report.
     */
    @GetMapping("/{carId}/download")
    public ResponseEntity<byte[]> downloadCarReport(
            @PathVariable Long opportunityId,
            @PathVariable Long carId) {

        CarResponseDto car = carService.getCar(carId);

        if (!car.getOpportunityId().equals(opportunityId)) {
            return ResponseEntity.notFound().build();
        }

        byte[] excelFile = excelService.generateCarReport(carId);

        String filename = "CAR_Report_" + carId + ".xlsx";

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + "\""
                )
                .contentType(
                        MediaType.parseMediaType(
                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                        )
                )
                .body(excelFile);
    }
}
