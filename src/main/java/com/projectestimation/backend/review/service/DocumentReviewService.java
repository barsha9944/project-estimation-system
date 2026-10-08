package com.projectestimation.backend.review.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.projectestimation.backend.common.exception.ResourceNotFoundException;
import com.projectestimation.backend.opportunity.model.Opportunity;
import com.projectestimation.backend.opportunity.model.ProjectTeam;
import com.projectestimation.backend.opportunity.repository.OpportunityRepository;
import com.projectestimation.backend.opportunity.repository.ProjectTeamRepository;
import com.projectestimation.backend.review.ai.GeminiReviewOrchestrator;
import com.projectestimation.backend.review.dto.DocumentReviewStatusDto;
import com.projectestimation.backend.review.dto.ReviewFindingDto;
import com.projectestimation.backend.review.dto.ReviewFindingsResponseDto;
import com.projectestimation.backend.review.model.DocumentReview;
import com.projectestimation.backend.review.model.DocumentReviewType;
import com.projectestimation.backend.review.model.ReviewStatus;
import com.projectestimation.backend.review.repository.DocumentReviewRepository;
import com.projectestimation.backend.sdd.dto.SddDto;
import com.projectestimation.backend.sdd.model.Sdd;
import com.projectestimation.backend.sdd.repository.SddRepository;
import com.projectestimation.backend.srs.dto.SrsDto;
import com.projectestimation.backend.srs.model.Srs;
import com.projectestimation.backend.srs.repository.SrsRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DocumentReviewService {

    private static final Logger log = LogManager.getLogger(DocumentReviewService.class);
    private static final String STORAGE_DIR = "uploads/reviews";

    private final DocumentReviewRepository documentReviewRepository;
    private final OpportunityRepository opportunityRepository;
    private final ProjectTeamRepository projectTeamRepository;
    private final ReviewNoteGenerator reviewNoteGenerator;
    private final ReviewDataSheetGenerator reviewDataSheetGenerator;
    private final GeminiReviewOrchestrator geminiReviewOrchestrator;
    private final SrsRepository srsRepository;
    private final SddRepository sddRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public DocumentReview initializeOrUpdateSchedule(
            Opportunity opportunity,
            DocumentReviewType documentType,
            LocalDateTime generationTimestamp
    ) {
        if (opportunity == null) {
            throw new IllegalArgumentException("Opportunity cannot be null when scheduling review");
        }

        LocalDateTime genTime = (generationTimestamp != null) ? generationTimestamp : LocalDateTime.now();

        DocumentReview review = documentReviewRepository
                .findByOpportunityIdAndDocumentType(opportunity.getId(), documentType)
                .orElseGet(() -> DocumentReview.builder()
                        .opportunity(opportunity)
                        .documentType(documentType)
                        .build());

        review.setOriginalGeneratedAt(genTime);

        // Schedule Cycle 1 for +1 day
        review.setCycle1ScheduledAt(genTime.plusDays(1));
        review.setCycle1Status(ReviewStatus.PENDING);
        review.setCycle1GeneratedAt(null);
        review.setCycle1FilePath(null);

        // Schedule Cycle 2 for +2 days
        review.setCycle2ScheduledAt(genTime.plusDays(2));
        review.setCycle2Status(ReviewStatus.PENDING);
        review.setCycle2GeneratedAt(null);
        review.setCycle2FilePath(null);

        // Shared Data Sheet is scheduled for Day 1
        review.setDataSheetScheduledAt(genTime.plusDays(1));
        review.setDataSheetStatus(ReviewStatus.PENDING);
        review.setDataSheetGeneratedAt(null);
        review.setDataSheetFilePath(null);

        review.setFindingsData(null);

        log.info("Initialized {} review lifecycle for opportunity {} ({}) with genTime: {}, Cycle1: {}, Cycle2: {}",
                documentType, opportunity.getId(), opportunity.getOpportunityName(),
                genTime, review.getCycle1ScheduledAt(), review.getCycle2ScheduledAt());

        return documentReviewRepository.save(review);
    }

    public List<DocumentReview> findDueCycle1Reviews(LocalDateTime now) {
        return documentReviewRepository.findByCycle1StatusAndCycle1ScheduledAtLessThanEqual(
                ReviewStatus.PENDING,
                now
        );
    }

    public List<DocumentReview> findDueCycle2Reviews(LocalDateTime now) {
        return documentReviewRepository.findByCycle2StatusAndCycle2ScheduledAtLessThanEqual(
                ReviewStatus.PENDING,
                now
        );
    }

    @Transactional
    public void processDueCycle1(Long reviewId) {
        DocumentReview review = documentReviewRepository.findById(reviewId).orElse(null);
        if (review == null) {
            log.warn("DocumentReview not found for id: {}", reviewId);
            return;
        }

        if (review.getCycle1Status() == ReviewStatus.COMPLETED) {
            log.info("Cycle 1 review already completed for review id {}", review.getId());
            return;
        }

        Opportunity opportunity = review.getOpportunity();
        DocumentReviewType docType = review.getDocumentType();
        String oppName = opportunity.getOpportunityName();
        String sanitizedOpp = sanitizeFileName(oppName);

        log.info("Processing due Cycle 1 review with Gemini for opportunity {} ({}) and docType {}",
                opportunity.getId(), oppName, docType);

        Optional<ProjectTeam> teamOpt = projectTeamRepository.findByOpportunityId(opportunity.getId());
        String teamLead = teamOpt.map(ProjectTeam::getTeamLead).orElse("Team Lead");
        String projectManager = teamOpt.map(ProjectTeam::getProjectManager).orElse("Manas Chattopadhay");

        String docPrefix = (docType == DocumentReviewType.SDD) ? "sdd" : "srs";
        String mainDocName = sanitizedOpp + "_" + docPrefix + ".docx";
        String review1FileName = sanitizedOpp + "_" + docPrefix + "_review1.docx";
        String dataSheetFileName = sanitizedOpp + "_" + docPrefix + "_review_data_sheet.xlsx";

        try {
            // 1. Invoke Gemini to inspect actual document and produce 3-5 real findings
            ReviewFindingsResponseDto findingsResponse;
            if (docType == DocumentReviewType.SRS) {
                Srs srs = srsRepository.findByOpportunityId(opportunity.getId())
                        .orElseThrow(() -> new IllegalStateException("Generated SRS data not found for opportunity ID " + opportunity.getId()));
                SrsDto srsDto = objectMapper.readValue(srs.getSrsData(), SrsDto.class);
                findingsResponse = geminiReviewOrchestrator.reviewSrsCycle1(srsDto, oppName);
            } else {
                Sdd sdd = sddRepository.findByOpportunityId(opportunity.getId())
                        .orElseThrow(() -> new IllegalStateException("Generated SDD data not found for opportunity ID " + opportunity.getId()));
                SddDto sddDto = objectMapper.readValue(sdd.getSddData(), SddDto.class);
                findingsResponse = geminiReviewOrchestrator.reviewSddCycle1(sddDto, oppName);
            }

            List<ReviewFindingDto> findings = (findingsResponse != null && findingsResponse.findings() != null)
                    ? findingsResponse.findings()
                    : new ArrayList<>();

            // 2. Generate and save Review Data Sheet XLSX
            byte[] dataSheetBytes = reviewDataSheetGenerator.generateReviewDataSheet(
                    docType,
                    oppName,
                    projectManager,
                    teamLead,
                    review.getOriginalGeneratedAt(),
                    review.getCycle1ScheduledAt(),
                    review.getCycle2ScheduledAt(),
                    mainDocName,
                    "1.0",
                    findings
            );
            Path dataSheetPath = saveFile(dataSheetFileName, dataSheetBytes);

            // 3. Generate and save Review Note 1 DOCX
            byte[] note1Bytes = reviewNoteGenerator.generateReviewNote(
                    docType,
                    1,
                    oppName,
                    teamLead,
                    review.getOriginalGeneratedAt(),
                    review.getCycle1ScheduledAt(),
                    review.getCycle2ScheduledAt(),
                    dataSheetFileName,
                    mainDocName,
                    "1.0",
                    false
            );
            Path note1Path = saveFile(review1FileName, note1Bytes);

            // Persist status and serialized findings for Cycle 2
            review.setFindingsData(objectMapper.writeValueAsString(findings));

            review.setDataSheetStatus(ReviewStatus.COMPLETED);
            review.setDataSheetGeneratedAt(LocalDateTime.now());
            review.setDataSheetFilePath(dataSheetPath.toAbsolutePath().toString());

            review.setCycle1Status(ReviewStatus.COMPLETED);
            review.setCycle1GeneratedAt(LocalDateTime.now());
            review.setCycle1FilePath(note1Path.toAbsolutePath().toString());

            documentReviewRepository.save(review);
            log.info("Successfully completed Cycle 1 review for opportunity {} ({}) with {} findings",
                    opportunity.getId(), oppName, findings.size());

        } catch (Exception e) {
            log.error("Failed to generate Cycle 1 review for opportunity {} ({}): {}",
                    opportunity.getId(), oppName, e.getMessage(), e);
            review.setCycle1Status(ReviewStatus.FAILED);
            documentReviewRepository.save(review);
        }
    }

    @Transactional
    public void processDueCycle2(Long reviewId) {
        DocumentReview review = documentReviewRepository.findById(reviewId).orElse(null);
        if (review == null) {
            log.warn("DocumentReview not found for id: {}", reviewId);
            return;
        }

        if (review.getCycle2Status() == ReviewStatus.COMPLETED) {
            log.info("Cycle 2 review already completed for review id {}", review.getId());
            return;
        }

        Opportunity opportunity = review.getOpportunity();
        DocumentReviewType docType = review.getDocumentType();
        String oppName = opportunity.getOpportunityName();
        String sanitizedOpp = sanitizeFileName(oppName);

        log.info("Processing due Cycle 2 review with Gemini for opportunity {} ({}) and docType {}",
                opportunity.getId(), oppName, docType);

        Optional<ProjectTeam> teamOpt = projectTeamRepository.findByOpportunityId(opportunity.getId());
        String teamLead = teamOpt.map(ProjectTeam::getTeamLead).orElse("Team Lead");
        String projectManager = teamOpt.map(ProjectTeam::getProjectManager).orElse("Manas Chattopadhay");

        String docPrefix = (docType == DocumentReviewType.SDD) ? "sdd" : "srs";
        String mainDocName = sanitizedOpp + "_" + docPrefix + ".docx";
        String review2FileName = sanitizedOpp + "_" + docPrefix + "_review2.docx";
        String dataSheetFileName = sanitizedOpp + "_" + docPrefix + "_review_data_sheet.xlsx";

        try {
            // 1. Load Cycle 1 findings
            List<ReviewFindingDto> cycle1Findings = new ArrayList<>();
            if (review.getFindingsData() != null && !review.getFindingsData().isBlank()) {
                cycle1Findings = objectMapper.readValue(
                        review.getFindingsData(),
                        new TypeReference<List<ReviewFindingDto>>() {}
                );
            }

            // 2. Fetch current document content and invoke Gemini re-evaluation
            String docJson;
            if (docType == DocumentReviewType.SRS) {
                Srs srs = srsRepository.findByOpportunityId(opportunity.getId())
                        .orElseThrow(() -> new IllegalStateException("Generated SRS data not found for opportunity ID " + opportunity.getId()));
                docJson = srs.getSrsData();
            } else {
                Sdd sdd = sddRepository.findByOpportunityId(opportunity.getId())
                        .orElseThrow(() -> new IllegalStateException("Generated SDD data not found for opportunity ID " + opportunity.getId()));
                docJson = sdd.getSddData();
            }

            ReviewFindingsResponseDto cycle2Response = geminiReviewOrchestrator.reviewCycle2(
                    docType,
                    docJson,
                    cycle1Findings,
                    oppName
            );

            List<ReviewFindingDto> updatedFindings = (cycle2Response != null && cycle2Response.findings() != null)
                    ? cycle2Response.findings()
                    : cycle1Findings;

            boolean isApproved = (cycle2Response != null && Boolean.TRUE.equals(cycle2Response.approved()));

            // 3. Update the SAME Review Data Sheet XLSX
            byte[] updatedDataSheetBytes = reviewDataSheetGenerator.generateReviewDataSheet(
                    docType,
                    oppName,
                    projectManager,
                    teamLead,
                    review.getOriginalGeneratedAt(),
                    review.getCycle1ScheduledAt(),
                    review.getCycle2ScheduledAt(),
                    mainDocName,
                    "1.0",
                    updatedFindings
            );
            Path dataSheetPath = saveFile(dataSheetFileName, updatedDataSheetBytes);

            // 4. Generate and save Review Note 2 DOCX
            byte[] note2Bytes = reviewNoteGenerator.generateReviewNote(
                    docType,
                    2,
                    oppName,
                    teamLead,
                    review.getOriginalGeneratedAt(),
                    review.getCycle1ScheduledAt(),
                    review.getCycle2ScheduledAt(),
                    dataSheetFileName,
                    mainDocName,
                    "1.0",
                    isApproved
            );
            Path note2Path = saveFile(review2FileName, note2Bytes);

            review.setFindingsData(objectMapper.writeValueAsString(updatedFindings));
            review.setDataSheetFilePath(dataSheetPath.toAbsolutePath().toString());

            review.setCycle2Status(ReviewStatus.COMPLETED);
            review.setCycle2GeneratedAt(LocalDateTime.now());
            review.setCycle2FilePath(note2Path.toAbsolutePath().toString());

            documentReviewRepository.save(review);
            log.info("Successfully completed Cycle 2 review for opportunity {} ({}) with approved={}",
                    opportunity.getId(), oppName, isApproved);

        } catch (Exception e) {
            log.error("Failed to generate Cycle 2 review for opportunity {} ({}): {}",
                    opportunity.getId(), oppName, e.getMessage(), e);
            review.setCycle2Status(ReviewStatus.FAILED);
            documentReviewRepository.save(review);
        }
    }

    public DocumentReviewStatusDto getReviewStatus(Long opportunityId, DocumentReviewType docType) {
        Opportunity opportunity = opportunityRepository.findById(opportunityId)
                .orElseThrow(() -> new ResourceNotFoundException("Opportunity not found with ID: " + opportunityId));

        DocumentReview review = documentReviewRepository
                .findByOpportunityIdAndDocumentType(opportunityId, docType)
                .orElse(null);

        if (review == null) {
            return null;
        }

        String sanitizedOpp = sanitizeFileName(opportunity.getOpportunityName());
        String docPrefix = (docType == DocumentReviewType.SDD) ? "sdd" : "srs";

        return DocumentReviewStatusDto.builder()
                .opportunityId(opportunityId)
                .opportunityName(opportunity.getOpportunityName())
                .documentType(docType)
                .originalGeneratedAt(review.getOriginalGeneratedAt())
                .cycle1ScheduledAt(review.getCycle1ScheduledAt())
                .cycle1GeneratedAt(review.getCycle1GeneratedAt())
                .cycle1Status(review.getCycle1Status())
                .cycle1Available(review.getCycle1Status() == ReviewStatus.COMPLETED)
                .cycle1FileName(sanitizedOpp + "_" + docPrefix + "_review1.docx")
                .cycle2ScheduledAt(review.getCycle2ScheduledAt())
                .cycle2GeneratedAt(review.getCycle2GeneratedAt())
                .cycle2Status(review.getCycle2Status())
                .cycle2Available(review.getCycle2Status() == ReviewStatus.COMPLETED)
                .cycle2FileName(sanitizedOpp + "_" + docPrefix + "_review2.docx")
                .dataSheetScheduledAt(review.getDataSheetScheduledAt())
                .dataSheetGeneratedAt(review.getDataSheetGeneratedAt())
                .dataSheetStatus(review.getDataSheetStatus())
                .dataSheetAvailable(review.getDataSheetStatus() == ReviewStatus.COMPLETED)
                .dataSheetFileName(sanitizedOpp + "_" + docPrefix + "_review_data_sheet.xlsx")
                .build();
    }

    public byte[] downloadCycle1Note(Long opportunityId, DocumentReviewType docType) throws IOException {
        DocumentReview review = getCompletedReview(opportunityId, docType);
        if (review.getCycle1Status() != ReviewStatus.COMPLETED || review.getCycle1FilePath() == null) {
            throw new ResourceNotFoundException("Review Note Cycle 1 is not yet generated. Scheduled for: " + review.getCycle1ScheduledAt());
        }

        return readFileBytes(review.getCycle1FilePath());
    }

    public byte[] downloadCycle2Note(Long opportunityId, DocumentReviewType docType) throws IOException {
        DocumentReview review = getCompletedReview(opportunityId, docType);
        if (review.getCycle2Status() != ReviewStatus.COMPLETED || review.getCycle2FilePath() == null) {
            throw new ResourceNotFoundException("Review Note Cycle 2 is not yet generated. Scheduled for: " + review.getCycle2ScheduledAt());
        }

        return readFileBytes(review.getCycle2FilePath());
    }

    public byte[] downloadDataSheet(Long opportunityId, DocumentReviewType docType) throws IOException {
        DocumentReview review = getCompletedReview(opportunityId, docType);
        if (review.getDataSheetStatus() != ReviewStatus.COMPLETED || review.getDataSheetFilePath() == null) {
            throw new ResourceNotFoundException("Review Data Sheet is not yet generated. Scheduled for: " + review.getDataSheetScheduledAt());
        }

        return readFileBytes(review.getDataSheetFilePath());
    }

    public String getReview1FileName(Long opportunityId, DocumentReviewType docType) {
        Opportunity opportunity = opportunityRepository.findById(opportunityId)
                .orElseThrow(() -> new ResourceNotFoundException("Opportunity not found with ID: " + opportunityId));
        String docPrefix = (docType == DocumentReviewType.SDD) ? "sdd" : "srs";
        return sanitizeFileName(opportunity.getOpportunityName()) + "_" + docPrefix + "_review1.docx";
    }

    public String getReview2FileName(Long opportunityId, DocumentReviewType docType) {
        Opportunity opportunity = opportunityRepository.findById(opportunityId)
                .orElseThrow(() -> new ResourceNotFoundException("Opportunity not found with ID: " + opportunityId));
        String docPrefix = (docType == DocumentReviewType.SDD) ? "sdd" : "srs";
        return sanitizeFileName(opportunity.getOpportunityName()) + "_" + docPrefix + "_review2.docx";
    }

    public String getDataSheetFileName(Long opportunityId, DocumentReviewType docType) {
        Opportunity opportunity = opportunityRepository.findById(opportunityId)
                .orElseThrow(() -> new ResourceNotFoundException("Opportunity not found with ID: " + opportunityId));
        String docPrefix = (docType == DocumentReviewType.SDD) ? "sdd" : "srs";
        return sanitizeFileName(opportunity.getOpportunityName()) + "_" + docPrefix + "_review_data_sheet.xlsx";
    }

    private DocumentReview getCompletedReview(Long opportunityId, DocumentReviewType docType) {
        return documentReviewRepository.findByOpportunityIdAndDocumentType(opportunityId, docType)
                .orElseThrow(() -> new ResourceNotFoundException(docType + " review schedule not found for opportunity ID: " + opportunityId));
    }

    private Path saveFile(String fileName, byte[] content) throws IOException {
        Path dirPath = Paths.get(STORAGE_DIR);
        if (!Files.exists(dirPath)) {
            Files.createDirectories(dirPath);
        }
        Path filePath = dirPath.resolve(fileName);
        Files.write(filePath, content);
        return filePath;
    }

    private byte[] readFileBytes(String filePathStr) throws IOException {
        Path path = Paths.get(filePathStr);
        if (!Files.exists(path)) {
            throw new ResourceNotFoundException("Generated review file could not be found on storage: " + filePathStr);
        }
        return Files.readAllBytes(path);
    }

    public static String sanitizeFileName(String input) {
        if (input == null || input.isBlank()) {
            return "Document";
        }
        return input.trim().replaceAll("[\\\\/:*?\"<>|]", "_");
    }
}
