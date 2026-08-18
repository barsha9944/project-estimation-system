package com.projectestimation.backend.projectmetrics.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.projectestimation.backend.projectmetrics.dto.ProjectMetricsResponse;
import com.projectestimation.backend.projectmetrics.service.ProjectMetricsService;

@RestController
@RequestMapping("/api/v1/project-metrics")
public class ProjectMetricsController {

    private final ProjectMetricsService projectMetricsService;

    public ProjectMetricsController(
            ProjectMetricsService projectMetricsService) {

        this.projectMetricsService = projectMetricsService;
    }

    @PostMapping("/{opportunityId}/calculate")
    public ResponseEntity<ProjectMetricsResponse> calculateMetrics(
            @PathVariable Long opportunityId) {

        ProjectMetricsResponse response =
                projectMetricsService.calculateMetrics(opportunityId);

        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/{opportunityId}")
    public ResponseEntity<ProjectMetricsResponse> getMetrics(
            @PathVariable Long opportunityId) {

        ProjectMetricsResponse response =
                projectMetricsService.getMetrics(opportunityId);

        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/{opportunityId}/download")
    public ResponseEntity<byte[]> downloadMetrics(
            @PathVariable Long opportunityId) {

        byte[] excel =
                projectMetricsService.downloadMetrics(
                        opportunityId
                );

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"Project_Metrics.xlsx\""
                )
                .contentType(
                        MediaType.parseMediaType(
                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                        )
                )
                .body(excel);
    }
}