package com.projectestimation.backend.sdd.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.projectestimation.backend.sdd.service.SddService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/opportunities/{opportunityId}/sdd")
@RequiredArgsConstructor
public class SddController {

    private final SddService sddService;
}