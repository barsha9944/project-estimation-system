package com.projectestimation.backend.psr.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.projectestimation.backend.opportunity.model.Opportunity;
import com.projectestimation.backend.opportunity.repository.OpportunityRepository;
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
    private final PsrPeriodCalculator psrPeriodCalculator;
    private final GeminiPsrOrchestrator geminiPsrOrchestrator;
    private final OpportunityRepository opportunityRepository;
    private final PsrDocxConverter psrDocxConverter;
    private final ObjectMapper objectMapper;

    /*
     * ================================================================
     * GENERATE ALL PSRs
     * ================================================================
     *
     * Used when PSRs need to be generated for the complete schedule.
     *
     * If a PSR already exists for a period, the SAME database record
     * is updated instead of creating another PSR.
     */
    @Transactional
    public List<PsrResponse> generateAllPsrs(
            Long opportunityId
    ) {

        Opportunity opportunity =
                opportunityRepository
                        .findByIdForPsrSynchronization(opportunityId)
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "Opportunity not found: "
                                                + opportunityId
                                )
                        );

        var schedule =
                psrScheduleDataService
                        .getScheduleForPsr(opportunityId);

        List<PsrPeriod> periods =
                psrPeriodCalculator.calculatePeriods(
                        schedule.getProjectStartDate(),
                        schedule.getProjectEndDate()
                );

        if (periods.isEmpty()) {

            throw new IllegalStateException(
                    "Project schedule does not contain any working days"
            );
        }

        List<PsrResponse> responses =
                new ArrayList<>();

        for (PsrPeriod period : periods) {

            responses.add(
                    generateOrUpdatePsr(
                            opportunity,
                            opportunityId,
                            period
                    )
            );
        }

        return responses;
    }

    /*
     * ================================================================
     * SYNCHRONIZE PSRs AFTER PROJECT SCHEDULE SAVE
     * ================================================================
     *
     * The Project Schedule is the source of truth.
     *
     * Only affected periods are regenerated.
     *
     * Existing PSRs are updated IN PLACE.
     *
     * Missing PSRs are created.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<PsrResponse> synchronizePsrs(
            Long opportunityId,
            Set<Integer> affectedVersions
    ) {

        Opportunity opportunity =
                opportunityRepository
                        .findByIdForPsrSynchronization(opportunityId)
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "Opportunity not found: "
                                                + opportunityId
                                )
                        );

        var schedule =
                psrScheduleDataService
                        .getScheduleForPsr(opportunityId);

        List<PsrPeriod> periods =
                psrPeriodCalculator.calculatePeriods(
                        schedule.getProjectStartDate(),
                        schedule.getProjectEndDate()
                );

        if (periods.isEmpty()) {

            throw new IllegalStateException(
                    "Project schedule does not contain any working days"
            );
        }

        /*
         * If this is the first PSR synchronization,
         * generate ALL PSRs for the complete schedule.
         */
        List<ProjectStatusReport> existingReports =
                projectStatusReportRepository
                        .findByOpportunityIdOrderByVersionAsc(
                                opportunityId
                        );

        if (existingReports.isEmpty()) {

            List<PsrResponse> responses =
                    new ArrayList<>();

            for (PsrPeriod period : periods) {

                responses.add(
                        generateOrUpdatePsr(
                                opportunity,
                                opportunityId,
                                period
                        )
                );
            }

            return responses;
        }

        List<PsrResponse> responses =
                new ArrayList<>();

        /*
         * ============================================================
         * UPDATE EXISTING AFFECTED PSRs
         * ============================================================
         */
        if (affectedVersions != null
                && !affectedVersions.isEmpty()) {

            for (PsrPeriod period : periods) {

                if (affectedVersions.contains(
                        period.version()
                )) {

                    responses.add(
                            generateOrUpdatePsr(
                                    opportunity,
                                    opportunityId,
                                    period
                            )
                    );
                }
            }
        }

        /*
         * ============================================================
         * CREATE MISSING PSRs
         * ============================================================
         *
         * This is important when the schedule becomes longer and
         * therefore creates additional reporting periods.
         */
        for (PsrPeriod period : periods) {

            boolean exists =
                    existingReports
                            .stream()
                            .anyMatch(report ->
                                    report.getVersion() != null
                                            && report.getVersion()
                                                    .equals(
                                                            period.version()
                                                    )
                            );

            if (!exists) {

                responses.add(
                        generateOrUpdatePsr(
                                opportunity,
                                opportunityId,
                                period
                        )
                );
            }
        }

        return responses;
    }

    /*
     * ================================================================
     * GENERATE OR UPDATE ONE PSR
     * ================================================================
     *
     * CRITICAL:
     *
     * PSR identity is:
     *
     *     opportunityId + version
     *
     * If the PSR already exists, we reuse the SAME entity.
     *
     * Therefore:
     *
     *     existing ID 46 → remains ID 46
     *
     * We only replace its content.
     */
    private PsrResponse generateOrUpdatePsr(
            Opportunity opportunity,
            Long opportunityId,
            PsrPeriod period
    ) {

        ProjectStatusReport report =
                projectStatusReportRepository
                        .findByOpportunityIdAndVersion(
                                opportunityId,
                                period.version()
                        )
                        .orElse(null);

        boolean isNew =
                report == null;

        /*
         * ============================================================
         * CREATE ONLY WHEN THIS PERIOD DOES NOT EXIST
         * ============================================================
         */
        if (isNew) {

            report =
                    new ProjectStatusReport();

            report.setOpportunity(
                    opportunity
            );

            report.setVersion(
                    period.version()
            );
        }

        /*
         * ============================================================
         * BUILD CONTENT FROM CURRENT PROJECT SCHEDULE
         * ============================================================
         */
        PsrContentDto content =
                psrScheduleDataService.buildPsrContent(
                        opportunityId,
                        period
                );

        /*
         * ============================================================
         * GENERATE NEW MARKDOWN
         * ============================================================
         *
         * Gemini receives the CURRENT schedule data.
         *
         * This replaces the old markdown when updating an existing
         * PSR.
         */
        AiPsrResult aiResult =
                geminiPsrOrchestrator.generate(
                        opportunity.getOpportunityName(),
                        content,
                        period.version()
                );

        String markdown =
                aiResult.markdownContent();

        /*
         * ============================================================
         * REGENERATE DOCX
         * ============================================================
         *
         * Same PSR period → same base filename.
         *
         * The existing DOCX is replaced.
         */
        String baseFileName =
                buildFileName(period);

        PsrDocxConverter.ConversionResult conversion =
                psrDocxConverter.convertMarkdownToDocx(
                        markdown,
                        baseFileName,
                        opportunityId
                );

        /*
         * ============================================================
         * UPDATE THE EXISTING PSR ENTITY
         * ============================================================
         */
        applyReportData(
                report,
                content,
                markdown,
                baseFileName,
                conversion
        );

        /*
         * Report date represents when this PSR was generated/refreshed.
         */
        report.setReportDate(
                LocalDate.now()
        );

        report.setGeneratedAt(
                LocalDateTime.now()
        );

        /*
         * ============================================================
         * SAVE
         * ============================================================
         *
         * Existing entity → UPDATE
         * New entity      → INSERT
         */
        ProjectStatusReport savedReport =
                projectStatusReportRepository.save(
                        report
                );

        return toResponse(
                savedReport,
                isNew
                        ? "GENERATED"
                        : "UPDATED"
        );
    }

    /*
     * ================================================================
     * APPLY PSR DATA
     * ================================================================
     */
    private void applyReportData(
            ProjectStatusReport report,
            PsrContentDto content,
            String markdown,
            String baseFileName,
            PsrDocxConverter.ConversionResult conversion
    ) {

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

        /*
         * Do NOT set:
         *
         * - startBreakdownId
         * - associatedBreakdownIds
         *
         * Those are legacy fields and are no longer used for PSR
         * generation or identity.
         */

        report.setFileName(
                baseFileName + ".docx"
        );

        report.setFileLocation(
                conversion.generatedDocPath()
        );

        /*
         * THIS IS THE IMPORTANT UPDATE:
         *
         * Existing markdownContent is replaced by the newly generated
         * Markdown.
         */
        report.setMarkdownContent(
                markdown
        );

        report.setRiskStatus(
                content.getRiskStatus()
        );

        report.setTrainingOfProjectTeamMembers(
                content.getTrainingOfProjectTeamMembers()
        );

        report.setIssuesManagementAttention(
                content.getIssuesManagementAttention()
        );
    }

    /*
     * ================================================================
     * FILE NAME
     * ================================================================
     */
    private String buildFileName(
            PsrPeriod period
    ) {

        return "PSR_"
                + period.version()
                + "_"
                + period.startDate()
                + "_"
                + period.endDate();
    }

    /*
     * ================================================================
     * RESPONSE
     * ================================================================
     */
    private PsrResponse toResponse(
            ProjectStatusReport report,
            String status
    ) {

        return new PsrResponse(
                report.getId(),
                report.getFileName(),
                report.getFileLocation(),
                report.getGeneratedAt(),
                status,
                report.getMarkdownContent()
        );
    }

    /*
     * ================================================================
     * BUILD PSR CONTENT
     * ================================================================
     */
    @Transactional(readOnly = true)
    public PsrContentDto buildPsrContent(
            Long opportunityId,
            PsrPeriod period
    ) {

        return psrScheduleDataService.buildPsrContent(
                opportunityId,
                period
        );
    }
}