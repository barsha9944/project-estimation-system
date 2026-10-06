package com.projectestimation.backend.sdd.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.projectestimation.backend.sdd.dto.SddDto;
import com.projectestimation.backend.sdd.model.Sdd;
import com.projectestimation.backend.sdd.service.SddService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/opportunities/{opportunityId}/sdd")
@RequiredArgsConstructor
public class SddController {

    private final SddService sddService;
    
    @GetMapping
    public ResponseEntity<SddDto> getSdd(
            @PathVariable Long opportunityId) {

        SddDto sddDto = sddService.getGeneratedSdd(opportunityId);

        if (sddDto == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(sddDto);
    }
}