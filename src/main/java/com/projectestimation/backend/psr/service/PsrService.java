package com.projectestimation.backend.psr.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.projectestimation.backend.common.storage.OpportunityDocumentStorageService;
import com.projectestimation.backend.common.sync.DocumentChangeEvent;
import com.projectestimation.backend.common.sync.DocumentSyncService;
import com.projectestimation.backend.opportunity.model.Opportunity;
import com.projectestimation.backend.opportunity.repository.OpportunityRepository;
import com.projectestimation.backend.psr.ai.AiPsrResult;
import com.projectestimation.backend.psr.ai.GeminiPsrOrchestrator;
import com.projectestimation.backend.psr.dto.PsrContentDto;
import com.projectestimation.backend.psr.dto.PsrResponse;
import com.projectestimation.backend.psr.model.ProjectStatusReport;
import com.projectestimation.backend.psr.repository.ProjectStatusReportRepository;

@Service
public class PsrService {

    private static final Logger log = LoggerFactory.getLogger(PsrService.class);

    private final ProjectStatusReportRepository projectStatusReportRepository;
    private final PsrScheduleDataService psrScheduleDataService;
    private final PsrPeriodCalculator psrPeriodCalculator;
    private final GeminiPsrOrchestrator geminiPsrOrchestrator;
    private final OpportunityRepository opportunityRepository;
    private final PsrDocxConverter psrDocxConverter;
    private final ObjectMapper objectMapper;
    private final OpportunityDocumentStorageService documentStorageService;
    private final PsrDocxParser psrDocxParser;
    private final DocumentSyncService documentSyncService;

    public PsrService(
            ProjectStatusReportRepository projectStatusReportRepository,
            PsrScheduleDataService psrScheduleDataService,
            PsrPeriodCalculator psrPeriodCalculator,
            GeminiPsrOrchestrator geminiPsrOrchestrator,
            OpportunityRepository opportunityRepository,
            PsrDocxConverter psrDocxConverter,
            ObjectMapper objectMapper,
            OpportunityDocumentStorageService documentStorageService,
            PsrDocxParser psrDocxParser,
            @Lazy DocumentSyncService documentSyncService
    ) {
        this.projectStatusReportRepository = projectStatusReportRepository;
        this.psrScheduleDataService = psrScheduleDataService;
        this.psrPeriodCalculator = psrPeriodCalculator;
        this.geminiPsrOrchestrator = geminiPsrOrchestrator;
        this.opportunityRepository = opportunityRepository;
        this.psrDocxConverter = psrDocxConverter;
        this.objectMapper = objectMapper;
        this.documentStorageService = documentStorageService;
        this.psrDocxParser = psrDocxParser;
        this.documentSyncService = documentSyncService;
    }

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
                try {
                    responses.add(
                            generateOrUpdatePsr(
                                    opportunity,
                                    opportunityId,
                                    period
                            )
                    );
                } catch (Exception ex) {
                    log.error("Failed generating initial PSR for period {}: {}", period.version(), ex.getMessage());
                }
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
                    try {
                        responses.add(
                                generateOrUpdatePsr(
                                        opportunity,
                                        opportunityId,
                                        period
                                )
                        );
                    } catch (Exception ex) {
                        log.error("Failed updating affected PSR for period {}: {}", period.version(), ex.getMessage());
                    }
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
                try {
                    responses.add(
                            generateOrUpdatePsr(
                                    opportunity,
                                    opportunityId,
                                    period
                            )
                    );
                } catch (Exception ex) {
                    log.error("Failed creating missing PSR for period {}: {}", period.version(), ex.getMessage());
                }
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

        try {
            String relPath = conversion.generatedDocPath();
            report.setFilePath(relPath);
            report.setFileHash(documentStorageService.getFileChecksum(opportunityId, relPath));
            report.setFileLastModified(documentStorageService.getLastModified(opportunityId, relPath));
        } catch (Exception ex) {
            log.warn("Failed recording physical PSR file hash/timestamp: {}", ex.getMessage());
        }

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

        if (documentSyncService != null) {
            try {
                documentSyncService.publishEvent(
                        DocumentChangeEvent.of(
                                opportunityId,
                                "PSR",
                                savedReport.getFilePath() != null ? savedReport.getFilePath() : "Quality/PSR/" + savedReport.getFileName(),
                                isNew ? "CREATED" : "UPDATED",
                                savedReport.getFileHash(),
                                "PSR document " + (isNew ? "generated" : "updated") + " on disk"
                        )
                );
            } catch (Exception ex) {
                log.warn("Failed publishing PSR change event: {}", ex.getMessage());
            }
        }

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

        report.setFilePath(
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

    @Transactional
    public List<PsrResponse> getPsrs(Long opportunityId) {
        List<ProjectStatusReport> reports = projectStatusReportRepository.findByOpportunityId(opportunityId);
        for (ProjectStatusReport report : reports) {
            syncPsrFromPhysicalFileIfModified(report);
        }
        return reports.stream()
                .map(r -> toResponse(r, "RETRIEVED"))
                .toList();
    }

    @Transactional
    public void syncPsrFromPhysicalFileIfModified(ProjectStatusReport psr) {
        if (psr == null || psr.getId() == null || psr.getOpportunity() == null) {
            return;
        }

        Long opportunityId = psr.getOpportunity().getId();
        String relPath = psr.getFilePath();
        if (relPath == null || relPath.isBlank()) {
            relPath = psr.getFileLocation();
        }
        if (relPath == null || relPath.isBlank()) {
            relPath = "Quality/PSR/" + psr.getFileName();
        }

        try {
            if (documentStorageService.exists(opportunityId, relPath)) {
                String currentHash = documentStorageService.getFileChecksum(opportunityId, relPath);
                if (psr.getFileHash() == null || !psr.getFileHash().equals(currentHash)) {
                    log.info("Detected physical PSR DOCX update for opp: {}, psr: {}, syncing from file", opportunityId, psr.getId());
                    byte[] docxBytes = documentStorageService.readFile(opportunityId, relPath);
                    PsrDocxParser.ParsedPsrData parsed = psrDocxParser.parse(docxBytes);
                    if (parsed != null) {
                        if (parsed.activitiesPerformed() != null && !parsed.activitiesPerformed().isEmpty()) {
                            psr.setActivitiesPerformed(objectMapper.writeValueAsString(parsed.activitiesPerformed()));
                        }
                        if (parsed.nextWeekPlannedActivities() != null && !parsed.nextWeekPlannedActivities().isEmpty()) {
                            psr.setNextWeekPlannedActivities(objectMapper.writeValueAsString(parsed.nextWeekPlannedActivities()));
                        }
                        if (parsed.riskStatus() != null && !parsed.riskStatus().isBlank()) {
                            psr.setRiskStatus(parsed.riskStatus());
                        }

                        // Check if a companion .md file was also modified or exists
                        String baseName = psr.getFileName() != null && psr.getFileName().endsWith(".docx")
                                ? psr.getFileName().substring(0, psr.getFileName().length() - 5)
                                : "PSR_" + (psr.getVersion() != null ? psr.getVersion() : "1");
                        String mdRelPath = "Quality/PSR/" + baseName + ".md";
                        if (documentStorageService.exists(opportunityId, mdRelPath)) {
                            byte[] mdBytes = documentStorageService.readFile(opportunityId, mdRelPath);
                            psr.setMarkdownContent(new String(mdBytes, java.nio.charset.StandardCharsets.UTF_8));
                        } else if (parsed.reconstructedMarkdown() != null && !parsed.reconstructedMarkdown().isBlank()) {
                            psr.setMarkdownContent(parsed.reconstructedMarkdown());
                        }
                    }

                    psr.setFilePath(relPath);
                    psr.setFileLocation(relPath);
                    psr.setFileHash(currentHash);
                    psr.setFileLastModified(documentStorageService.getLastModified(opportunityId, relPath));
                    projectStatusReportRepository.save(psr);
                }
            }
        } catch (Exception ex) {
            log.warn("Failed syncing PSR from physical file for psr {}: {}", psr.getId(), ex.getMessage());
        }
    }

    @Transactional
    public PsrResponse generateOrUpdateInitialPsr(Long opportunityId) {
        Opportunity opportunity = opportunityRepository
                .findByIdForPsrSynchronization(opportunityId)
                .orElse(null);
        if (opportunity == null) return null;

        var schedule = psrScheduleDataService.getScheduleForPsr(opportunityId);
        if (schedule == null || schedule.getProjectStartDate() == null || schedule.getProjectEndDate() == null) {
            return null;
        }

        List<PsrPeriod> periods = psrPeriodCalculator.calculatePeriods(
                schedule.getProjectStartDate(),
                schedule.getProjectEndDate()
        );

        if (periods.isEmpty()) {
            return null;
        }

        PsrPeriod targetPeriod = psrPeriodCalculator.findPeriodForDate(periods, LocalDate.now());
        if (targetPeriod == null) {
            targetPeriod = periods.get(0);
        }

        return generateOrUpdatePsr(opportunity, opportunityId, targetPeriod);
    }

    @Transactional
    public byte[] downloadPsr(Long opportunityId, Long psrId) throws java.io.IOException {
        ProjectStatusReport report = projectStatusReportRepository.findById(psrId)
                .orElseThrow(() -> new IllegalStateException("PSR not found: " + psrId));

        syncPsrFromPhysicalFileIfModified(report);

        String relPath = report.getFileLocation();
        if (relPath == null || relPath.isBlank()) {
            relPath = "Quality/PSR/" + report.getFileName();
        }

        if (documentStorageService.exists(opportunityId, relPath)) {
            return documentStorageService.readFile(opportunityId, relPath);
        }

        throw new java.io.FileNotFoundException("Physical PSR document not found: " + relPath);
    }

    @Transactional(readOnly = true)
    public String getPsrFileName(Long psrId) {
        return projectStatusReportRepository.findById(psrId)
                .map(ProjectStatusReport::getFileName)
                .orElse("Project_Status_Report.docx");
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