package com.projectestimation.backend.common.sync;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.projectestimation.backend.car.model.CarAnalysis;
import com.projectestimation.backend.car.repository.CarAnalysisRepository;
import com.projectestimation.backend.car.service.CarService;
import com.projectestimation.backend.common.storage.OpportunityDocumentStorageService;
import com.projectestimation.backend.dar.model.Dar;
import com.projectestimation.backend.dar.repository.DarRepository;
import com.projectestimation.backend.dar.service.DarService;
import com.projectestimation.backend.estimation.model.EstimationAnalysis;
import com.projectestimation.backend.estimation.repository.EstimationAnalysisRepository;
import com.projectestimation.backend.estimation.service.CalculationService;
import com.projectestimation.backend.mom.model.Mom;
import com.projectestimation.backend.mom.repository.MomRepository;
import com.projectestimation.backend.mom.service.MomService;
import com.projectestimation.backend.pmp.model.Pmp;
import com.projectestimation.backend.pmp.repository.PmpRepository;
import com.projectestimation.backend.pmp.service.PmpService;
import com.projectestimation.backend.projectmetrics.model.ProjectMetrics;
import com.projectestimation.backend.projectmetrics.repository.ProjectMetricsRepository;
import com.projectestimation.backend.projectmetrics.service.ProjectMetricsService;
import com.projectestimation.backend.projectschedule.model.ProjectSchedule;
import com.projectestimation.backend.projectschedule.repository.ProjectScheduleRepository;
import com.projectestimation.backend.projectschedule.service.ProjectScheduleService;
import com.projectestimation.backend.proposal.model.Proposal;
import com.projectestimation.backend.proposal.repository.ProposalRepository;
import com.projectestimation.backend.proposal.service.ProposalService;
import com.projectestimation.backend.psr.model.ProjectStatusReport;
import com.projectestimation.backend.psr.repository.ProjectStatusReportRepository;
import com.projectestimation.backend.psr.service.PsrService;
import com.projectestimation.backend.rtm.model.Rtm;
import com.projectestimation.backend.rtm.repository.RtmRepository;
import com.projectestimation.backend.rtm.service.RtmService;
import com.projectestimation.backend.sdd.model.Sdd;
import com.projectestimation.backend.sdd.repository.SddRepository;
import com.projectestimation.backend.sdd.service.SddService;
import com.projectestimation.backend.srs.model.Srs;
import com.projectestimation.backend.srs.repository.SrsRepository;
import com.projectestimation.backend.srs.service.SrsService;
import com.projectestimation.backend.testcase.model.TestCase;
import com.projectestimation.backend.testcase.repository.TestCaseRepository;
import com.projectestimation.backend.testcase.service.TestCaseService;

@Service
public class DocumentSyncService {

    private static final Logger log = LoggerFactory.getLogger(DocumentSyncService.class);

    private static final Long SSE_TIMEOUT = 30 * 60 * 1000L; // 30 minutes

    private final Map<Long, List<SseEmitter>> opportunityEmitters = new ConcurrentHashMap<>();
    private final Map<String, String> lastScannedHashes = new ConcurrentHashMap<>();

    private final OpportunityDocumentStorageService documentStorageService;
    private final SrsRepository srsRepository;
    private final SrsService srsService;
    private final SddRepository sddRepository;
    private final SddService sddService;
    private final PmpRepository pmpRepository;
    private final PmpService pmpService;
    private final ProjectScheduleRepository projectScheduleRepository;
    private final ProjectScheduleService projectScheduleService;
    private final RtmRepository rtmRepository;
    private final RtmService rtmService;
    private final TestCaseRepository testCaseRepository;
    private final TestCaseService testCaseService;
    private final MomRepository momRepository;
    private final MomService momService;
    private final ProjectMetricsRepository projectMetricsRepository;
    private final ProjectMetricsService projectMetricsService;
    private final DarRepository darRepository;
    private final DarService darService;
    private final ProposalRepository proposalRepository;
    private final ProposalService proposalService;
    private final EstimationAnalysisRepository estimationAnalysisRepository;
    private final CalculationService calculationService;
    private final CarAnalysisRepository carAnalysisRepository;
    private final CarService carService;
    private final ProjectStatusReportRepository projectStatusReportRepository;
    private final PsrService psrService;

    public DocumentSyncService(
            OpportunityDocumentStorageService documentStorageService,
            SrsRepository srsRepository,
            @Lazy SrsService srsService,
            SddRepository sddRepository,
            @Lazy SddService sddService,
            PmpRepository pmpRepository,
            @Lazy PmpService pmpService,
            ProjectScheduleRepository projectScheduleRepository,
            @Lazy ProjectScheduleService projectScheduleService,
            RtmRepository rtmRepository,
            @Lazy RtmService rtmService,
            TestCaseRepository testCaseRepository,
            @Lazy TestCaseService testCaseService,
            MomRepository momRepository,
            @Lazy MomService momService,
            ProjectMetricsRepository projectMetricsRepository,
            @Lazy ProjectMetricsService projectMetricsService,
            DarRepository darRepository,
            @Lazy DarService darService,
            ProposalRepository proposalRepository,
            @Lazy ProposalService proposalService,
            EstimationAnalysisRepository estimationAnalysisRepository,
            @Lazy CalculationService calculationService,
            CarAnalysisRepository carAnalysisRepository,
            @Lazy CarService carService,
            ProjectStatusReportRepository projectStatusReportRepository,
            @Lazy PsrService psrService
    ) {
        this.documentStorageService = documentStorageService;
        this.srsRepository = srsRepository;
        this.srsService = srsService;
        this.sddRepository = sddRepository;
        this.sddService = sddService;
        this.pmpRepository = pmpRepository;
        this.pmpService = pmpService;
        this.projectScheduleRepository = projectScheduleRepository;
        this.projectScheduleService = projectScheduleService;
        this.rtmRepository = rtmRepository;
        this.rtmService = rtmService;
        this.testCaseRepository = testCaseRepository;
        this.testCaseService = testCaseService;
        this.momRepository = momRepository;
        this.momService = momService;
        this.projectMetricsRepository = projectMetricsRepository;
        this.projectMetricsService = projectMetricsService;
        this.darRepository = darRepository;
        this.darService = darService;
        this.proposalRepository = proposalRepository;
        this.proposalService = proposalService;
        this.estimationAnalysisRepository = estimationAnalysisRepository;
        this.calculationService = calculationService;
        this.carAnalysisRepository = carAnalysisRepository;
        this.carService = carService;
        this.projectStatusReportRepository = projectStatusReportRepository;
        this.psrService = psrService;
    }

    public SseEmitter subscribe(Long opportunityId) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);
        List<SseEmitter> emitters = opportunityEmitters.computeIfAbsent(opportunityId, k -> new CopyOnWriteArrayList<>());
        emitters.add(emitter);

        emitter.onCompletion(() -> removeEmitter(opportunityId, emitter));
        emitter.onTimeout(() -> removeEmitter(opportunityId, emitter));
        emitter.onError(e -> removeEmitter(opportunityId, emitter));

        try {
            emitter.send(SseEmitter.event()
                    .name("CONNECTED")
                    .data(Map.of("message", "Subscribed to document updates for opportunity: " + opportunityId)));
        } catch (IOException e) {
            removeEmitter(opportunityId, emitter);
        }

        return emitter;
    }

    public void publishEvent(DocumentChangeEvent event) {
        if (event == null || event.opportunityId() == null) return;
        List<SseEmitter> emitters = opportunityEmitters.get(event.opportunityId());
        if (emitters == null || emitters.isEmpty()) return;

        List<SseEmitter> deadEmitters = new ArrayList<>();
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name("document-change")
                        .data(event));
            } catch (Exception e) {
                deadEmitters.add(emitter);
            }
        }

        if (!deadEmitters.isEmpty()) {
            emitters.removeAll(deadEmitters);
            if (emitters.isEmpty()) {
                opportunityEmitters.remove(event.opportunityId());
            }
        }
    }

    private void removeEmitter(Long opportunityId, SseEmitter emitter) {
        List<SseEmitter> emitters = opportunityEmitters.get(opportunityId);
        if (emitters != null) {
            emitters.remove(emitter);
            if (emitters.isEmpty()) {
                opportunityEmitters.remove(opportunityId);
            }
        }
    }

    @Scheduled(fixedDelay = 15000)
    public void scanActiveOpportunitiesForExternalChanges() {
        if (opportunityEmitters.isEmpty()) return;

        for (Long oppId : opportunityEmitters.keySet()) {
            try {
                List<DocumentChangeEvent> changes = checkForExternalChanges(oppId);
                for (DocumentChangeEvent change : changes) {
                    publishEvent(change);
                }
            } catch (Exception ex) {
                log.warn("Error scanning opportunity {} for file changes: {}", oppId, ex.getMessage());
            }
        }
    }

    public List<DocumentChangeEvent> checkForExternalChanges(Long opportunityId) {
        List<DocumentChangeEvent> detected = new ArrayList<>();

        // 1. Check SRS
        try {
            Srs srs = srsRepository.findByOpportunityId(opportunityId).orElse(null);
            if (srs != null && srs.getFilePath() != null && documentStorageService.exists(opportunityId, srs.getFilePath())) {
                String curHash = documentStorageService.getFileChecksum(opportunityId, srs.getFilePath());
                if (srs.getFileHash() == null || !srs.getFileHash().equals(curHash)) {
                    log.info("External change detected on SRS DOCX for opportunity: {}", opportunityId);
                    srsService.getGeneratedSrs(opportunityId);
                    detected.add(DocumentChangeEvent.of(opportunityId, "SRS", srs.getFilePath(), "EXTERNAL_MODIFIED", curHash, "SRS document updated on disk"));
                }
            }
        } catch (Exception e) {
            log.warn("Error checking SRS changes for opp {}: {}", opportunityId, e.getMessage());
        }

        // 2. Check SDD
        try {
            Sdd sdd = sddRepository.findByOpportunityId(opportunityId).orElse(null);
            if (sdd != null && sdd.getFilePath() != null && documentStorageService.exists(opportunityId, sdd.getFilePath())) {
                String curHash = documentStorageService.getFileChecksum(opportunityId, sdd.getFilePath());
                if (sdd.getFileHash() == null || !sdd.getFileHash().equals(curHash)) {
                    log.info("External change detected on SDD DOCX for opportunity: {}", opportunityId);
                    sddService.getGeneratedSdd(opportunityId);
                    detected.add(DocumentChangeEvent.of(opportunityId, "SDD", sdd.getFilePath(), "EXTERNAL_MODIFIED", curHash, "SDD document updated on disk"));
                }
            }
        } catch (Exception e) {
            log.warn("Error checking SDD changes for opp {}: {}", opportunityId, e.getMessage());
        }

        // 3. Check PMP
        try {
            Pmp pmp = pmpRepository.findByOpportunityId(opportunityId).orElse(null);
            if (pmp != null && pmp.getFilePath() != null && documentStorageService.exists(opportunityId, pmp.getFilePath())) {
                String curHash = documentStorageService.getFileChecksum(opportunityId, pmp.getFilePath());
                if (pmp.getFileHash() == null || !pmp.getFileHash().equals(curHash)) {
                    log.info("External change detected on PMP DOCX for opportunity: {}", opportunityId);
                    pmpService.getPmp(opportunityId);
                    detected.add(DocumentChangeEvent.of(opportunityId, "PMP", pmp.getFilePath(), "EXTERNAL_MODIFIED", curHash, "PMP document updated on disk"));
                }
            }
        } catch (Exception e) {
            log.warn("Error checking PMP changes for opp {}: {}", opportunityId, e.getMessage());
        }

        // 4. Check Project Schedule
        try {
            ProjectSchedule schedule = projectScheduleRepository.findByOpportunityId(opportunityId).orElse(null);
            if (schedule != null && schedule.getFilePath() != null && documentStorageService.exists(opportunityId, schedule.getFilePath())) {
                String curHash = documentStorageService.getFileChecksum(opportunityId, schedule.getFilePath());
                if (schedule.getFileHash() == null || !schedule.getFileHash().equals(curHash)) {
                    log.info("External change detected on Project Schedule XLSX for opportunity: {}", opportunityId);
                    projectScheduleService.getProjectSchedule(opportunityId);
                    detected.add(DocumentChangeEvent.of(opportunityId, "PROJECT_SCHEDULE", schedule.getFilePath(), "EXTERNAL_MODIFIED", curHash, "Project Schedule Excel updated on disk"));
                }
            }
        } catch (Exception e) {
            log.warn("Error checking Project Schedule changes for opp {}: {}", opportunityId, e.getMessage());
        }

        // 5. Check RTM
        try {
            Rtm rtm = rtmRepository.findByOpportunityId(opportunityId).orElse(null);
            if (rtm != null && rtm.getFilePath() != null && documentStorageService.exists(opportunityId, rtm.getFilePath())) {
                String curHash = documentStorageService.getFileChecksum(opportunityId, rtm.getFilePath());
                if (rtm.getFileHash() == null || !rtm.getFileHash().equals(curHash)) {
                    log.info("External change detected on RTM XLS for opportunity: {}", opportunityId);
                    rtmService.getRtmData(opportunityId);
                    detected.add(DocumentChangeEvent.of(opportunityId, "RTM", rtm.getFilePath(), "EXTERNAL_MODIFIED", curHash, "RTM document updated on disk"));
                }
            }
        } catch (Exception e) {
            log.warn("Error checking RTM changes for opp {}: {}", opportunityId, e.getMessage());
        }

        // 6. Check Test Cases
        try {
            List<TestCase> testCases = testCaseRepository.findByOpportunityId(opportunityId);
            if (!testCases.isEmpty() && testCases.get(0).getFilePath() != null) {
                String path = testCases.get(0).getFilePath();
                if (documentStorageService.exists(opportunityId, path)) {
                    String curHash = documentStorageService.getFileChecksum(opportunityId, path);
                    if (testCases.get(0).getFileHash() == null || !testCases.get(0).getFileHash().equals(curHash)) {
                        log.info("External change detected on Test Cases XLSX for opportunity: {}", opportunityId);
                        testCaseService.getTestCases(opportunityId);
                        detected.add(DocumentChangeEvent.of(opportunityId, "TEST_CASES", path, "EXTERNAL_MODIFIED", curHash, "Test Cases Excel updated on disk"));
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Error checking Test Case changes for opp {}: {}", opportunityId, e.getMessage());
        }

        // 7. Check MOM
        try {
            List<Mom> moms = momRepository.findByOpportunityIdOrderByMeetingSequenceAsc(opportunityId);
            for (Mom mom : moms) {
                String relPath = mom.getFilePath() != null ? mom.getFilePath() : "Quality/MOM/" + mom.getDocumentName();
                if (documentStorageService.exists(opportunityId, relPath)) {
                    String curHash = documentStorageService.getFileChecksum(opportunityId, relPath);
                    if (mom.getFileHash() == null || !mom.getFileHash().equals(curHash)) {
                        log.info("External change detected on MOM DOCX (id={}) for opportunity: {}", mom.getId(), opportunityId);
                        momService.getMomById(opportunityId, mom.getId());
                        detected.add(DocumentChangeEvent.of(opportunityId, "MOM", relPath, "EXTERNAL_MODIFIED", curHash, "MOM document updated on disk: " + mom.getDocumentName()));
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Error checking MOM changes for opp {}: {}", opportunityId, e.getMessage());
        }

        // 8. Check Project Metrics
        try {
            ProjectMetrics metrics = projectMetricsRepository.findByOpportunityId(opportunityId).orElse(null);
            String metricsFileName = projectMetricsService.getProjectMetricsFileName(opportunityId);
            String relPath = "Quality/" + metricsFileName;
            if (documentStorageService.exists(opportunityId, relPath)) {
                String curHash = documentStorageService.getFileChecksum(opportunityId, relPath);
                if (metrics == null || metrics.getFileHash() == null || !metrics.getFileHash().equals(curHash)) {
                    log.info("External change detected on Project Metrics XLSX for opportunity: {}", opportunityId);
                    projectMetricsService.getMetrics(opportunityId);
                    detected.add(DocumentChangeEvent.of(opportunityId, "PROJECT_METRICS", relPath, "EXTERNAL_MODIFIED", curHash, "Project Metrics Excel updated on disk"));
                }
            }
        } catch (Exception e) {
            log.warn("Error checking Project Metrics changes for opp {}: {}", opportunityId, e.getMessage());
        }

        // 9. Check DAR
        try {
            Dar dar = darRepository.findByOpportunityId(opportunityId).orElse(null);
            if (dar != null) {
                String relPath = dar.getFilePath() != null ? dar.getFilePath() : "Quality/DAR/" + dar.getDocumentName();
                if (documentStorageService.exists(opportunityId, relPath)) {
                    String curHash = documentStorageService.getFileChecksum(opportunityId, relPath);
                    if (dar.getFileHash() == null || !dar.getFileHash().equals(curHash)) {
                        log.info("External change detected on DAR DOCX for opportunity: {}", opportunityId);
                        darService.getDar(opportunityId);
                        detected.add(DocumentChangeEvent.of(opportunityId, "DAR", relPath, "EXTERNAL_MODIFIED", curHash, "DAR document updated on disk"));
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Error checking DAR changes for opp {}: {}", opportunityId, e.getMessage());
        }

        // 10. Check Proposal
        try {
            Proposal proposal = proposalRepository.findFirstByOpportunity_IdOrderByVersionDesc(opportunityId).orElse(null);
            if (proposal != null && proposal.getMarkdownFilePath() != null) {
                Path mdPath = Path.of(proposal.getMarkdownFilePath());
                if (Files.exists(mdPath)) {
                    String curHash = computeExternalFileHash(mdPath);
                    if (curHash != null && (proposal.getFileHash() == null || !proposal.getFileHash().equals(curHash))) {
                        log.info("External change detected on Proposal Markdown for opportunity: {}", opportunityId);
                        proposalService.syncFromMarkdownFile(opportunityId);
                        detected.add(DocumentChangeEvent.of(opportunityId, "PROPOSAL", proposal.getMarkdownFilePath(), "EXTERNAL_MODIFIED", curHash, "Proposal markdown document updated on disk"));
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Error checking Proposal changes for opp {}: {}", opportunityId, e.getMessage());
        }

        // 11. Check Estimation
        try {
            EstimationAnalysis analysis = estimationAnalysisRepository.findByOpportunityId(opportunityId).orElse(null);
            if (analysis != null) {
                String fileName = analysis.getFileName();
                if (fileName == null || fileName.isBlank()) {
                    String oppName = (analysis.getOpportunity() != null && analysis.getOpportunity().getOpportunityName() != null)
                            ? analysis.getOpportunity().getOpportunityName()
                            : "Opportunity_" + opportunityId;
                    fileName = oppName.replaceAll("[\\\\/:*?\"<>|\\s]+", "_").trim() + "_Estimate.xlsx";
                }
                String relPath = "Baseline/Estimation/" + fileName;
                if (documentStorageService.exists(opportunityId, relPath)) {
                    String curHash = documentStorageService.getFileChecksum(opportunityId, relPath);
                    if (analysis.getFileHash() == null || !analysis.getFileHash().equals(curHash)) {
                        log.info("External change detected on Estimation XLSX for opportunity: {}", opportunityId);
                        calculationService.syncFromExcelFile(opportunityId);
                        detected.add(DocumentChangeEvent.of(opportunityId, "ESTIMATION", relPath, "EXTERNAL_MODIFIED", curHash, "Estimation Excel workbook updated on disk"));
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Error checking Estimation changes for opp {}: {}", opportunityId, e.getMessage());
        }

        // 12. Check CAR
        try {
            List<CarAnalysis> cars = carAnalysisRepository.findByOpportunityIdOrderByCreatedAtDesc(opportunityId);
            for (CarAnalysis car : cars) {
                String fileName = "CAR_Report_" + car.getId() + ".xlsx";
                String relPath = car.getFilePath() != null ? car.getFilePath() : "Quality/CAR/" + fileName;
                if (documentStorageService.exists(opportunityId, relPath)) {
                    String curHash = documentStorageService.getFileChecksum(opportunityId, relPath);
                    if (car.getFileHash() == null || !car.getFileHash().equals(curHash)) {
                        log.info("External change detected on CAR XLSX (id={}) for opportunity: {}", car.getId(), opportunityId);
                        carService.syncCarFromPhysicalFileIfModified(car);
                        detected.add(DocumentChangeEvent.of(opportunityId, "CAR", relPath, "EXTERNAL_MODIFIED", curHash, "CAR Excel report updated on disk"));
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Error checking CAR changes for opp {}: {}", opportunityId, e.getMessage());
        }

        // 13. Check PSR
        try {
            List<ProjectStatusReport> psrs = projectStatusReportRepository.findByOpportunityIdOrderByVersionDesc(opportunityId);
            for (ProjectStatusReport psr : psrs) {
                String relPath = psr.getFilePath() != null ? psr.getFilePath() : (psr.getFileLocation() != null ? psr.getFileLocation() : (psr.getFileName() != null ? "Quality/PSR/" + psr.getFileName() : null));
                if (relPath != null && documentStorageService.exists(opportunityId, relPath)) {
                    String curHash = documentStorageService.getFileChecksum(opportunityId, relPath);
                    if (psr.getFileHash() == null || !psr.getFileHash().equals(curHash)) {
                        log.info("External change detected on PSR DOCX (id={}) for opportunity: {}", psr.getId(), opportunityId);
                        psrService.syncPsrFromPhysicalFileIfModified(psr);
                        detected.add(DocumentChangeEvent.of(opportunityId, "PSR", relPath, "EXTERNAL_MODIFIED", curHash, "PSR document updated on disk"));
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Error checking PSR changes for opp {}: {}", opportunityId, e.getMessage());
        }

        // 14. Check Review Documents (SRS Review Data Sheet, SDD Review Data Sheet, DAR Review)
        try {
            String srsSheet = "Baseline/SRS/Opportunity_" + opportunityId + "_Review_Data_Sheet.xls";
            if (documentStorageService.exists(opportunityId, srsSheet)) {
                String curHash = documentStorageService.getFileChecksum(opportunityId, srsSheet);
                String scanKey = "SRS_SHEET_" + opportunityId;
                String prevHash = lastScannedHashes.get(scanKey);
                if (prevHash == null) {
                    lastScannedHashes.put(scanKey, curHash);
                } else if (!prevHash.equals(curHash)) {
                    lastScannedHashes.put(scanKey, curHash);
                    log.info("External change detected on SRS Review Data Sheet for opportunity: {}", opportunityId);
                    detected.add(DocumentChangeEvent.of(opportunityId, "SRS", srsSheet, "EXTERNAL_MODIFIED", curHash, "SRS Review Data Sheet updated on disk"));
                }
            }

            String sddSheet = "Baseline/SDD/Opportunity_" + opportunityId + "_Review_Data_Sheet.xls";
            if (documentStorageService.exists(opportunityId, sddSheet)) {
                String curHash = documentStorageService.getFileChecksum(opportunityId, sddSheet);
                String scanKey = "SDD_SHEET_" + opportunityId;
                String prevHash = lastScannedHashes.get(scanKey);
                if (prevHash == null) {
                    lastScannedHashes.put(scanKey, curHash);
                } else if (!prevHash.equals(curHash)) {
                    lastScannedHashes.put(scanKey, curHash);
                    log.info("External change detected on SDD Review Data Sheet for opportunity: {}", opportunityId);
                    detected.add(DocumentChangeEvent.of(opportunityId, "SDD", sddSheet, "EXTERNAL_MODIFIED", curHash, "SDD Review Data Sheet updated on disk"));
                }
            }

            String darReviewPath = "Quality/DAR/Opportunity_" + opportunityId + "_DAR_Review.docx";
            if (documentStorageService.exists(opportunityId, darReviewPath)) {
                String curHash = documentStorageService.getFileChecksum(opportunityId, darReviewPath);
                String scanKey = "DAR_REVIEW_" + opportunityId;
                String prevHash = lastScannedHashes.get(scanKey);
                if (prevHash == null) {
                    lastScannedHashes.put(scanKey, curHash);
                } else if (!prevHash.equals(curHash)) {
                    lastScannedHashes.put(scanKey, curHash);
                    log.info("External change detected on DAR Review DOCX for opportunity: {}", opportunityId);
                    detected.add(DocumentChangeEvent.of(opportunityId, "DAR", darReviewPath, "EXTERNAL_MODIFIED", curHash, "DAR Review DOCX updated on disk"));
                }
            }
        } catch (Exception e) {
            log.warn("Error checking Review document changes for opp {}: {}", opportunityId, e.getMessage());
        }

        return detected;
    }

    private String computeExternalFileHash(Path path) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] fileBytes = Files.readAllBytes(path);
            byte[] hashBytes = digest.digest(fileBytes);
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }
}
