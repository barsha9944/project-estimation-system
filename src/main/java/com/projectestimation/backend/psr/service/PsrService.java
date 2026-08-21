package com.projectestimation.backend.psr.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.projectestimation.backend.opportunity.model.Opportunity;
import com.projectestimation.backend.opportunity.repository.OpportunityRepository;
import com.projectestimation.backend.proposal.service.PandocDocxConverter;
import com.projectestimation.backend.psr.ai.AiPsrResult;
import com.projectestimation.backend.psr.ai.GeminiPsrOrchestrator;
import com.projectestimation.backend.psr.dto.PsrContentDto;
import com.projectestimation.backend.psr.dto.PsrResponse;
import com.projectestimation.backend.psr.model.ProjectStatusReport;
import com.projectestimation.backend.psr.repository.ProjectStatusReportRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PsrService {

    private final ProjectStatusReportRepository projectStatusReportRepository;

    private final PsrScheduleDataService psrScheduleDataService;

    private final GeminiPsrOrchestrator geminiPsrOrchestrator;

    private final OpportunityRepository opportunityRepository;

    private final PsrDocxConverter psrDocxConverter;

    private final ObjectMapper objectMapper;


    @Transactional(readOnly = true)
    public boolean hasRecentPsr(Long opportunityId) {

        return projectStatusReportRepository
                .findTopByOpportunityIdOrderByGeneratedAtDesc(
                        opportunityId
                )
                .map(psr ->
                        psr.getGeneratedAt() != null
                        && psr.getGeneratedAt().isAfter(
                                LocalDateTime.now().minusDays(15)
                        )
                )
                .orElse(false);
    }


    @Transactional(readOnly = true)
    public PsrContentDto buildPsrContent(Long opportunityId) {

        return psrScheduleDataService.buildPsrContent(
                opportunityId
        );
    }


    @Transactional
    public PsrResponse generatePsrIfRequired(Long opportunityId) {

        // ============================================
        // CHECK WHETHER PSR WAS GENERATED IN LAST 15 DAYS
        // ============================================

        var recentPsr =
                projectStatusReportRepository
                        .findTopByOpportunityIdOrderByGeneratedAtDesc(
                                opportunityId
                        );

        if (recentPsr.isPresent()
                && recentPsr.get().getGeneratedAt() != null
                && recentPsr.get().getGeneratedAt().isAfter(
                        LocalDateTime.now().minusDays(15)
                )) {

            ProjectStatusReport existingPsr =
                    recentPsr.get();

            return new PsrResponse(
                    existingPsr.getId(),
                    existingPsr.getFileName(),
                    existingPsr.getFileLocation(),
                    existingPsr.getGeneratedAt(),
                    "ALREADY_EXISTS"
            );
        }


        // ============================================
        // GET OPPORTUNITY
        // ============================================

        Opportunity opportunity =
                opportunityRepository.findById(opportunityId)
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "Opportunity not found: "
                                                + opportunityId
                                )
                        );


        // ============================================
        // BUILD PSR CONTENT FROM PROJECT SCHEDULE
        // ============================================

        PsrContentDto content =
                psrScheduleDataService.buildPsrContent(
                        opportunityId
                );


        // ============================================
        // GEMINI
        // ============================================

        AiPsrResult aiResult =
                geminiPsrOrchestrator.generate(
                        opportunity.getOpportunityName(),
                        content
                );


        // ============================================
        // FILE NAME
        // ============================================

        String baseFileName =
                		"PSR"
                        + "_"
                        + LocalDate.now();


        // ============================================
        // MARKDOWN → DOCX USING PANDOC
        // ============================================

        PsrDocxConverter.ConversionResult conversion =
                psrDocxConverter.convertMarkdownToDocx(
                        aiResult.markdownContent(),
                        baseFileName
                );


        // ============================================
        // CREATE DATABASE RECORD
        // ============================================

        ProjectStatusReport report =
                new ProjectStatusReport();

        report.setOpportunity(opportunity);

        report.setFileName(
                baseFileName + ".docx"
        );

        report.setFileLocation(
                conversion.generatedDocPath()
        );

        report.setGeneratedAt(
                LocalDateTime.now()
        );

        report.setReportDate(
                LocalDate.now()
        );


        // ============================================
        // SAVE PSR CONTENT
        // ============================================

        try {

            report.setActivitiesPerformed(
                    objectMapper.writeValueAsString(
                            content.getActivitiesPerformed()
                    )
            );

            report.setNextWeekPlannedActivities(
                    objectMapper.writeValueAsString(
                            content.getNextWeekPlannedActivities()
                    )
            );

        } catch (Exception ex) {

            throw new IllegalStateException(
                    "Failed to store PSR activity data",
                    ex
            );
        }


        report.setRiskStatus(
                content.getRiskStatus()
        );

        report.setTrainingOfProjectTeamMembers(
                content.getTrainingOfProjectTeamMembers()
        );

        report.setIssuesManagementAttention(
                content.getIssuesManagementAttention()
        );


        // ============================================
        // SAVE DATABASE RECORD
        // ============================================

        ProjectStatusReport savedReport =
                projectStatusReportRepository.save(
                        report
                );


        // ============================================
        // RETURN RESPONSE
        // ============================================

        return new PsrResponse(
                savedReport.getId(),
                savedReport.getFileName(),
                savedReport.getFileLocation(),
                savedReport.getGeneratedAt(),
                "GENERATED"
        );
    }
}