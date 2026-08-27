package com.projectestimation.backend.psr.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.projectestimation.backend.psr.dto.PsrResponse;
import com.projectestimation.backend.psr.service.PsrService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/opportunities/{opportunityId}/psr")
public class PsrController {

    private final PsrService psrService;

    @PostMapping("/generate")
    public ResponseEntity<PsrResponse> generatePsr(
            @PathVariable Long opportunityId,
            @RequestParam Long breakdownId
    ) {

        return ResponseEntity.ok(
                psrService.generatePsrIfRequired(
                        opportunityId,
                        breakdownId
                )
        );
    }
}