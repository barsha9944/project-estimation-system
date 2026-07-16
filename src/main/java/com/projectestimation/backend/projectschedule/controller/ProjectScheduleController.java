package com.projectestimation.backend.projectschedule.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.projectestimation.backend.auth.model.User;
import com.projectestimation.backend.common.response.ApiResponse;
import com.projectestimation.backend.projectschedule.dto.GenerateProjectScheduleRequest;
import com.projectestimation.backend.projectschedule.dto.ProjectScheduleResponse;
import com.projectestimation.backend.projectschedule.dto.RecalculateProjectScheduleRequest;
import com.projectestimation.backend.projectschedule.dto.SaveProjectScheduleRequest;
import com.projectestimation.backend.projectschedule.service.ProjectScheduleService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/opportunities/{opportunityId}/project-schedule")
public class ProjectScheduleController {

	
    private final ProjectScheduleService projectScheduleService;

    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<ProjectScheduleResponse>>
    generateProjectSchedule(

            @PathVariable Long opportunityId,

            @Valid
            @RequestBody
            GenerateProjectScheduleRequest request,

            @AuthenticationPrincipal
            User user

    ) {
    	
    	System.out.println("Controller entered");

        ProjectScheduleResponse response =
                projectScheduleService.generateProjectSchedule(
                        opportunityId,
                        request,
                        user
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Project schedule generated successfully.",
                        response
                )
        );

    }
    
    
    @PostMapping
    public ResponseEntity<ApiResponse<Void>> saveProjectSchedule(

            @PathVariable Long opportunityId,

            @Valid
            @RequestBody
            SaveProjectScheduleRequest request,

            @AuthenticationPrincipal
            User user

    ) {

        projectScheduleService.saveProjectSchedule(
                opportunityId,
                request,
                user
        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Project schedule saved successfully.",
                        null
                )
        );

    }
    
    @PostMapping("/download")
    public ResponseEntity<byte[]> downloadProjectSchedule(

            @PathVariable Long opportunityId,

            @RequestBody
            SaveProjectScheduleRequest request

    ) {

        return projectScheduleService.downloadProjectSchedule(
                opportunityId,
                request
        );

    }
    
    
    @GetMapping
    public ResponseEntity<ApiResponse<ProjectScheduleResponse>>
    getProjectSchedule(
            @PathVariable Long opportunityId
    ) {

    	System.out.print("get method");
    	
        ProjectScheduleResponse response =
                projectScheduleService.getProjectSchedule(
                        opportunityId
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Project schedule fetched successfully.",
                        response
                )
        );

    }
    
    
    @PostMapping("/recalculate")
    public ResponseEntity<ApiResponse<ProjectScheduleResponse>>
    recalculateProjectSchedule(

            @PathVariable Long opportunityId,

            @RequestBody RecalculateProjectScheduleRequest request

    ) {

        ProjectScheduleResponse response =
                projectScheduleService.recalculateProjectSchedule(
                        opportunityId,
                        request
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Project schedule recalculated successfully.",
                        response
                )
        );

    }

}