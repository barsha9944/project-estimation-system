
package com.projectestimation.backend.pmp.review.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Supplier;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.TableRowAlign;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.projectestimation.backend.common.storage.OpportunityDocumentStorageService;
import com.projectestimation.backend.opportunity.model.Opportunity;
import com.projectestimation.backend.opportunity.model.ProjectTeam;
import com.projectestimation.backend.opportunity.repository.OpportunityRepository;
import com.projectestimation.backend.opportunity.repository.ProjectTeamRepository;
import com.projectestimation.backend.pmp.review.dto.PmpReviewResponseDto;
import com.projectestimation.backend.pmp.review.dto.PmpReviewStatusDto;
import com.projectestimation.backend.pmp.review.entity.PmpReview;
import com.projectestimation.backend.pmp.review.entity.PmpReviewDocument;
import com.projectestimation.backend.pmp.review.repository.PmpReviewDocumentRepository;
import com.projectestimation.backend.pmp.review.repository.PmpReviewRepository;
import com.projectestimation.backend.pmp.service.PmpDocumentStorageService;


@Service
public class PmpReviewService {

    public static final String REVIEW_NOTE = "REVIEW_NOTE";
    public static final String DEFECT_TRACKING = "DEFECT_TRACKING";
    public static final String FOLLOW_UP = "FOLLOW_UP";

    private final PmpReviewRepository reviewRepository;
    private final PmpReviewDocumentRepository documentRepository;
    private final PmpDocumentStorageService pmpDocumentStorageService;
    private final ProjectTeamRepository projectTeamRepository;
    private final OpportunityRepository opportunityRepository;
    private final ObjectMapper objectMapper;
    private final PmpReviewNoteGenerator reviewNoteGenerator;
    private final PmpDefectTrackingGenerator defectTrackingGenerator;
    private final PmpReviewFollowUpGenerator followUpGenerator;
    private final OpportunityDocumentStorageService documentStorageService;

    public PmpReviewService(
            PmpReviewRepository reviewRepository,
            PmpReviewDocumentRepository documentRepository,
            PmpDocumentStorageService pmpDocumentStorageService,
            ProjectTeamRepository projectTeamRepository,
            OpportunityRepository opportunityRepository,
            ObjectMapper objectMapper,
            PmpReviewNoteGenerator reviewNoteGenerator,
            PmpDefectTrackingGenerator defectTrackingGenerator,
            PmpReviewFollowUpGenerator followUpGenerator,
            OpportunityDocumentStorageService documentStorageService) {

        this.reviewRepository = reviewRepository;
        this.documentRepository = documentRepository;
        this.pmpDocumentStorageService = pmpDocumentStorageService;
        this.projectTeamRepository = projectTeamRepository;
        this.opportunityRepository = opportunityRepository;
        this.objectMapper = objectMapper;
        this.reviewNoteGenerator = reviewNoteGenerator;
        this.defectTrackingGenerator = defectTrackingGenerator;
        this.followUpGenerator = followUpGenerator;
        this.documentStorageService = documentStorageService;
    }

    // ---------------------------------------------------------
    // Schedule PMP review
    // ---------------------------------------------------------

    @Transactional
    public PmpReviewResponseDto schedule(
            Long opportunityId,
            LocalDateTime pmpGeneratedAt) {

        if (opportunityId == null || opportunityId <= 0) {
            throw new IllegalArgumentException(
                    "Opportunity ID must be a positive number.");
        }

        if (pmpGeneratedAt == null) {
            throw new IllegalArgumentException(
                    "PMP generation time cannot be null.");
        }

        PmpReview existing = reviewRepository
                .findByOpportunityId(opportunityId)
                .orElse(null);

        // Do not reset an existing schedule when called again.
        if (existing != null) {
            return toResponse(existing);
        }

        PmpReview review = new PmpReview();
        review.setOpportunityId(opportunityId);
        review.setPmpGeneratedAt(pmpGeneratedAt);

        // Temporary test delay. Restore plusHours(24) after testing.
        review.setReviewDocumentsDueAt(
                pmpGeneratedAt.plusMinutes(2));

        review.setFollowUpDueAt(null);
        review.setStatus("SCHEDULED");
        review.setLastError(null);

        return toResponse(reviewRepository.save(review));
    }

    // ---------------------------------------------------------
    // Review status
    // ---------------------------------------------------------

    @Transactional(readOnly = true)
    public PmpReviewStatusDto getStatus(Long opportunityId) {

        PmpReview review = findReview(opportunityId);

        List<PmpReviewStatusDto.DocumentStatusDto> documents =
                documentRepository.findByReview(review)
                        .stream()
                        .map(document ->
                                new PmpReviewStatusDto.DocumentStatusDto(
                                        document.getId(),
                                        document.getDocumentType(),
                                        document.getStatus(),
                                        document.getGeneratedAt(),
                                        document.getLastError()))
                        .toList();

        return new PmpReviewStatusDto(
                review.getId(),
                review.getOpportunityId(),
                review.getStatus(),
                review.getPmpGeneratedAt(),
                review.getReviewDocumentsDueAt(),
                review.getFollowUpDueAt(),
                documents);
    }

    @Transactional(readOnly = true)
    public PmpReview findReview(Long opportunityId) {
        return reviewRepository.findByOpportunityId(opportunityId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "PMP review not found for opportunity "
                                + opportunityId));
    }

    @Transactional(readOnly = true)
    public PmpReviewDocument getDocument(
            Long opportunityId,
            String documentType) {

        PmpReview review = findReview(opportunityId);

        if (FOLLOW_UP.equals(documentType)) {
            return documentRepository
                    .findByReviewAndDocumentTypeOrderByGeneratedAtAsc(
                            review, FOLLOW_UP)
                    .stream()
                    .filter(d -> "GENERATED".equals(d.getStatus()))
                    .max(java.util.Comparator.comparing(
                            PmpReviewDocument::getGeneratedAt,
                            java.util.Comparator.nullsFirst(
                                    java.util.Comparator.naturalOrder())))
                    .orElseThrow(() -> new IllegalArgumentException(
                            "PMP follow-up document not found for opportunity "
                                    + opportunityId));
        }

        return documentRepository
                .findByReviewAndDocumentType(review, documentType)
                .orElseThrow(() -> new IllegalArgumentException(
                        "PMP review document not found: " + documentType));
    }

    // ---------------------------------------------------------
    // Generate Review Note and Defect Tracking Sheet
    // ---------------------------------------------------------

    @Transactional
    public void generateReviewDocuments(Long opportunityId) {

        PmpReview review = findReview(opportunityId);

        if (LocalDateTime.now().isBefore(
                review.getReviewDocumentsDueAt())) {
            return;
        }

        if ("COMPLETED".equals(review.getStatus())) {
            return;
        }

        String pmpContent = getPmpContent(opportunityId);

        generateDocument(
                review,
                REVIEW_NOTE,
                () -> reviewNoteGenerator.generate(
                        opportunityId, pmpContent));

        generateDocument(
                review,
                DEFECT_TRACKING,
                () -> defectTrackingGenerator.generate(
                        opportunityId, pmpContent));

        updateReviewStatus(opportunityId);
    }

    // ---------------------------------------------------------
    // Generate Follow-up Note
    // ---------------------------------------------------------

    @Transactional
    public void generateFollowUp(Long opportunityId) {

        PmpReview review = findReview(opportunityId);

        if (review.getFollowUpDueAt() == null
                || LocalDateTime.now().isBefore(review.getFollowUpDueAt())) {
            return;
        }

        List<PmpReviewDocument> documents =
                documentRepository.findByReview(review);

        PmpReviewDocument note = findDocument(documents, REVIEW_NOTE);

        PmpReviewDocument latestDefects = documents.stream()
                .filter(d -> DEFECT_TRACKING.equals(d.getDocumentType()))
                .filter(d -> "GENERATED".equals(d.getStatus()))
                .max(java.util.Comparator.comparing(
                        PmpReviewDocument::getGeneratedAt,
                        java.util.Comparator.nullsFirst(
                                java.util.Comparator.naturalOrder())))
                .orElse(null);

        if (note == null || latestDefects == null
                || !"GENERATED".equals(note.getStatus())) {
            return;
        }

        // Avoid creating a duplicate follow-up for the same uploaded sheet.
        boolean followUpAlreadyGeneratedAfterLatestDefects =
                documents.stream()
                        .filter(d -> FOLLOW_UP.equals(d.getDocumentType()))
                        .filter(d -> "GENERATED".equals(d.getStatus()))
                        .anyMatch(f -> f.getGeneratedAt() != null
                                && latestDefects.getGeneratedAt() != null
                                && !f.getGeneratedAt().isBefore(
                                        latestDefects.getGeneratedAt()));

        if (followUpAlreadyGeneratedAfterLatestDefects) {
            return;
        }

        String pmpContent = getPmpContent(opportunityId);

        // Save each cycle as a separate document record.
        PmpReviewDocument followUp = new PmpReviewDocument();
        followUp.setReview(review);
        followUp.setDocumentType(FOLLOW_UP);
        followUp.setStatus("GENERATING");
        followUp.setAttempts(1);
        documentRepository.saveAndFlush(followUp);

        try {
            String content = followUpGenerator.generate(
                    opportunityId,
                    pmpContent,
                    note.getDocumentContent(),
                    latestDefects.getDocumentContent());

            if (content == null || content.isBlank()) {
                throw new IllegalStateException(
                        "Gemini returned empty follow-up content.");
            }

            LocalDateTime generatedAt = LocalDateTime.now();

            followUp.setDocumentContent(content);
            followUp.setGeneratedAt(generatedAt);
            followUp.setStatus("GENERATED");
            followUp.setLastError(null);

            saveGeneratedReviewFile(
                    opportunityId,
                    FOLLOW_UP,
                    content,
                    generatedAt
            );

        } catch (Exception ex) {
            followUp.setStatus("FAILED");
            followUp.setLastError(truncate(ex.getMessage()));
        }

        documentRepository.saveAndFlush(followUp);
        updateReviewStatus(opportunityId);
    }

@Transactional
public void uploadUpdatedDefectTracking(
        Long opportunityId,
        MultipartFile file) throws IOException {

    if (file == null || file.isEmpty()) {
        throw new IllegalArgumentException(
                "Please upload a non-empty defect tracking file.");
    }

    String filename = file.getOriginalFilename();

    if (filename == null
            || !filename.toLowerCase().endsWith(".xls")) {
        throw new IllegalArgumentException(
                "Only .xls defect tracking files are supported.");
    }

    PmpReview review = findReview(opportunityId);

    PmpReviewDocument originalDefects =
            documentRepository
                    .findByReviewAndDocumentType(
                            review, DEFECT_TRACKING)
                    .orElseThrow(() ->
                            new IllegalStateException(
                                    "Generate the initial defect tracking sheet first."));

    if (!"GENERATED".equals(originalDefects.getStatus())) {
        throw new IllegalStateException(
                "The initial defect tracking sheet is not ready.");
    }

    try (InputStream inputStream = file.getInputStream();
         HSSFWorkbook workbook = new HSSFWorkbook(inputStream)) {

        if (workbook.getNumberOfSheets() == 0) {
            throw new IllegalArgumentException(
                    "The uploaded Excel file has no worksheets.");
        }

        Sheet sheet = workbook.getSheetAt(0);

        if (sheet.getLastRowNum() < 13) {
            throw new IllegalArgumentException(
                    "The uploaded defect tracking sheet has no data rows.");
        }

        boolean hasDefectData = false;

        for (int rowIndex = 13;
             rowIndex <= sheet.getLastRowNum();
             rowIndex++) {

            Row row = sheet.getRow(rowIndex);

            if (row != null
                    && row.getCell(0) != null
                    && !row.getCell(0)
                            .toString().trim().isEmpty()) {
                hasDefectData = true;
                break;
            }
        }

        if (!hasDefectData) {
            throw new IllegalArgumentException(
                    "No defect records were found in the uploaded sheet.");
        }

        // Upload validation passed.
        // Persist the uploaded revision and schedule the next
        // review follow-up here.
    }
}


    // ---------------------------------------------------------
    // Retrieve saved PMP
    // ---------------------------------------------------------

private String getPmpContent(Long opportunityId) {

    try {
        String pmpContent =
                pmpDocumentStorageService.readLatestPmpText(opportunityId);

        if (pmpContent == null || pmpContent.isBlank()) {
            throw new IllegalStateException(
                    "PMP document contains no readable content for opportunity "
                            + opportunityId);
        }

        return pmpContent;

    } catch (IOException ex) {
        throw new IllegalStateException(
                "Could not read PMP DOCX for opportunity "
                        + opportunityId,
                ex);
    }
}

    // ---------------------------------------------------------
    // Generate and save a Gemini document
    // ---------------------------------------------------------

    private void generateDocument(
            PmpReview review,
            String documentType,
            Supplier<String> generator) {

        PmpReviewDocument document = documentRepository
                .findByReviewAndDocumentType(review, documentType)
                .orElseGet(() -> {
                    PmpReviewDocument created = new PmpReviewDocument();
                    created.setReview(review);
                    created.setDocumentType(documentType);
                    created.setStatus("PENDING");
                    return created;
                });

        if ("GENERATED".equals(document.getStatus())
                || "GENERATING".equals(document.getStatus())) {
            return;
        }

        document.setStatus("GENERATING");
        document.setAttempts(document.getAttempts() + 1);
        document.setLastError(null);
        documentRepository.saveAndFlush(document);

        try {
            String generatedContent = generator.get();

            if (generatedContent == null || generatedContent.isBlank()) {
                throw new IllegalStateException(
                        "Gemini returned empty content.");
            }

            LocalDateTime generatedAt = LocalDateTime.now();

            document.setDocumentContent(generatedContent);
            document.setGeneratedAt(generatedAt);
            document.setStatus("GENERATED");
            document.setLastError(null);

            saveGeneratedReviewFile(
                    review.getOpportunityId(),
                    documentType,
                    generatedContent,
                    generatedAt
            );

        } catch (Exception ex) {
            document.setStatus("FAILED");
            document.setLastError(truncate(ex.getMessage()));
        }

        documentRepository.saveAndFlush(document);
    }

    // ---------------------------------------------------------
    // Update workflow status and follow-up deadline
    // ---------------------------------------------------------

    @Transactional
    public void updateReviewStatus(Long opportunityId) {

        PmpReview review = findReview(opportunityId);

        List<PmpReviewDocument> documents =
                documentRepository.findByReview(review);

        PmpReviewDocument note = findDocument(documents, REVIEW_NOTE);

        boolean initialDefectsGenerated = documents.stream()
                .anyMatch(d -> DEFECT_TRACKING.equals(d.getDocumentType())
                        && "GENERATED".equals(d.getStatus()));

        boolean anyFailed = documents.stream()
                .anyMatch(d -> "FAILED".equals(d.getStatus()));

        if (anyFailed) {
            review.setStatus("RETRY_PENDING");
            review.setLastError(
                    "One or more PMP review documents failed. Retry required.");
            reviewRepository.save(review);
            return;
        }

        if (note == null || !"GENERATED".equals(note.getStatus())
                || !initialDefectsGenerated) {
            review.setStatus("SCHEDULED");
            reviewRepository.save(review);
            return;
        }

        // Do not complete merely because a follow-up document exists.
        // Completion must be decided from verified defect statuses.
        review.setStatus("WAITING_FOR_FOLLOW_UP");
        review.setLastError(null);

        if (review.getFollowUpDueAt() == null) {
            LocalDateTime latest = documents.stream()
                    .filter(d -> (REVIEW_NOTE.equals(d.getDocumentType())
                            || DEFECT_TRACKING.equals(d.getDocumentType()))
                            && "GENERATED".equals(d.getStatus())
                            && d.getGeneratedAt() != null)
                    .map(PmpReviewDocument::getGeneratedAt)
                    .max(LocalDateTime::compareTo)
                    .orElse(LocalDateTime.now());

            // Temporary testing interval. Change to plusHours(24) for production.
            review.setFollowUpDueAt(latest.plusMinutes(2));
        }

        reviewRepository.save(review);
    }

    // ---------------------------------------------------------
    // Export generated documents
    // ---------------------------------------------------------

    @Transactional(readOnly = true)
    public byte[] exportDocument(
            Long opportunityId,
            String documentType) throws IOException {

        PmpReviewDocument document = getDocument(opportunityId, documentType);

        if (!"GENERATED".equals(document.getStatus())) {
            throw new IllegalStateException(
                    "Document has not been generated: " + documentType);
        }

        String content = document.getDocumentContent();

        if (content == null || content.isBlank()) {
            throw new IllegalStateException(
                    "Generated document content is empty: " + documentType);
        }

        if (DEFECT_TRACKING.equals(documentType)) {
        	return generateXls(opportunityId, content);
        }

        if (REVIEW_NOTE.equals(documentType)
                || FOLLOW_UP.equals(documentType)) {
            return generateDocx(content);
        }

        throw new IllegalArgumentException(
                "Unsupported document type: " + documentType);
    }

    // ---------------------------------------------------------
    // Create Word document from Gemini JSON
    // ---------------------------------------------------------

    private byte[] generateDocx(String content) throws IOException {

        JsonNode root = parseGeminiJson(content);

        String opportunityIdText = root.path("opportunityId").asText("");

        if (opportunityIdText.isBlank()) {
            throw new IOException(
                    "Opportunity ID is missing from PMP review data.");
        }

        Long opportunityId;

        try {
            opportunityId = Long.valueOf(opportunityIdText);
        } catch (NumberFormatException ex) {
            throw new IOException(
                    "Invalid opportunity ID in PMP review data: "
                            + opportunityIdText,
                    ex);
        }

        ProjectTeam team = projectTeamRepository
                .findByOpportunityId(opportunityId)
                .orElseThrow(() -> new IOException(
                        "Project Team not found for opportunity "
                                + opportunityId));

        String teamLeadName = team.getTeamLead();
        String projectManagerName = team.getProjectManager();

        if (teamLeadName == null || teamLeadName.isBlank()) {
            teamLeadName = "Not assigned";
        }

        if (projectManagerName == null || projectManagerName.isBlank()) {
            projectManagerName = "Not assigned";
        }

        try (XWPFDocument document = new XWPFDocument();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {

            org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSectPr sectPr =
                    document.getDocument().getBody().isSetSectPr()
                            ? document.getDocument().getBody().getSectPr()
                            : document.getDocument().getBody().addNewSectPr();

            org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageMar margins =
                    sectPr.isSetPgMar()
                            ? sectPr.getPgMar()
                            : sectPr.addNewPgMar();

            margins.setTop(java.math.BigInteger.valueOf(720));
            margins.setBottom(java.math.BigInteger.valueOf(720));
            margins.setLeft(java.math.BigInteger.valueOf(900));
            margins.setRight(java.math.BigInteger.valueOf(900));

            JsonNode reviewer = root.path("reviewerInformation");

            String reviewerName = projectManagerName;
            String reviewDate = reviewer.path("reviewDate").asText("");
            String documentName = root.path("documentName")
                    .asText("Project Management Plan");
            String reviewNoteNo = root.path("reviewNoteNumber")
                    .asText("PMPReviewNote/ET/" + opportunityId);
            String ciNumber = root.path("ciIdentificationNumber")
                    .asText("ETI/ET/P/PMP/001");
            String version = root.path("versionNumber").asText("1.0");
            String cycle = root.path("cycleNumber").asText("");

            if (cycle.isBlank()) {
                cycle = "1";
            }

            String references = root.path("referenceDocuments").isArray()
                    ? joinJsonArray(root.path("referenceDocuments"))
                    : root.path("referenceDocuments").asText("");

            String reviewDueDate = root.path("reviewDueDate").asText("");
            String requestedBy = teamLeadName;
            String requestedDate =
                    root.path("reviewRequestedDate").asText(reviewDate);
            String comments = extractReviewComments(root);
            String nextAction = root.path("proposedNextAction").asText("");
            String effort = root.path("reviewEffort").asText("");
            String approval = root.path("approvalStatus").asText("");

            addBeasLogo(document);

            XWPFParagraph classification = document.createParagraph();
            classification.setAlignment(ParagraphAlignment.RIGHT);
            classification.setSpacingBefore(0);
            classification.setSpacingAfter(80);

            XWPFRun classificationRun = classification.createRun();
            classificationRun.setText("CL2  |  INTERNAL USE ONLY");
            classificationRun.setFontFamily("Arial");
            classificationRun.setFontSize(10);
            classificationRun.setBold(true);

            XWPFParagraph title = document.createParagraph();
            title.setAlignment(ParagraphAlignment.LEFT);
            title.setSpacingBefore(180);
            title.setSpacingAfter(40);

            XWPFRun titleRun = title.createRun();
            titleRun.setText(
                    root.path("documentTitle")
                            .asText("Document Review Note"));
            titleRun.setBold(true);
            titleRun.setFontFamily("Arial");
            titleRun.setFontSize(18);

            addBodyText(document, "(BEAS/DOC/Form/01 Version 1.0)");

            addFormLine(document, "Review Note No.:", reviewNoteNo);
            addFormLine(document,
                    "Name of the Document (including source code) to be reviewed:",
                    documentName);
            addFormLine(document, "CI Identification No. of Document:", ciNumber);
            addFormLine(document, "Version No.:", version);
            addFormLine(document, "Cycle # of Review:", cycle);
            addFormLine(document, "Reference Documents, if any:", references);
            addFormLine(document, "Reviewer’s Name:", reviewerName);
            addFormLine(document, "Project Manager:", projectManagerName);
            addFormLine(document, "Review to be completed by:", reviewDueDate);

            XWPFTable requestedTable = document.createTable(1, 2);
            requestedTable.setWidth("100%");

            setCellText(requestedTable.getRow(0).getCell(0),
                    "Review Requested By:  " + requestedBy + "\nSignature", true);
            setCellText(requestedTable.getRow(0).getCell(1),
                    "Date:  " + requestedDate, true);

            XWPFParagraph divider = document.createParagraph();
            divider.setSpacingBefore(80);
            divider.setSpacingAfter(80);

            XWPFRun dividerRun = divider.createRun();
            dividerRun.setText(
                    "________________________________________________________________________");
            dividerRun.setFontSize(8);

            addBodyText(document, "(To be filled in by the Reviewer(s))");
            addSectionHeading(document, "Reviewer’s Findings:");

            XWPFTable findingsTable = document.createTable(1, 1);
            findingsTable.setWidth("100%");
            findingsTable.setTableAlignment(TableRowAlign.CENTER);

            XWPFTableCell findingsCell =
                    findingsTable.getRow(0).getCell(0);
            findingsCell.setWidth("100%");
            findingsCell.setVerticalAlignment(
                    XWPFTableCell.XWPFVertAlign.TOP);

            setFindingsCellText(findingsCell, comments);

            findingsTable.createRow();

            XWPFTableCell blankFindingsCell =
                    findingsTable.getRow(1).getCell(0);
            blankFindingsCell.setWidth("100%");
            blankFindingsCell.setVerticalAlignment(
                    XWPFTableCell.XWPFVertAlign.TOP);

            setCellText(blankFindingsCell, "\n\n\n\n", false);

            addFormLine(document, "Next Action Proposed:",
                    (approval.equalsIgnoreCase("APPROVED")
                            ? "☐ Bring for further review   ☒ Approved"
                            : "☒ Bring for further review   ☐ Approved")
                            + (nextAction.isBlank() ? "" : "\n" + nextAction));

            addFormLine(document,
                    "Total effort Spent in Review (Person Hours):", effort);

            XWPFTable signatureTable = document.createTable(1, 2);
            signatureTable.setWidth("100%");

            setCellText(signatureTable.getRow(0).getCell(0),
                    "Reviewer’s Signature:  " + reviewerName, true);
            setCellText(signatureTable.getRow(0).getCell(1),
                    "Date:  " + reviewDate, true);

            XWPFParagraph footer = document.createParagraph();
            footer.setAlignment(ParagraphAlignment.RIGHT);
            footer.setSpacingBefore(300);

            XWPFRun footerRun = footer.createRun();
            footerRun.setText("Page 1 of 1");
            footerRun.setFontFamily("Arial");
            footerRun.setFontSize(8);

            document.write(output);
            return output.toByteArray();
        }
    }

    private void addBeasLogo(XWPFDocument document) throws IOException {
        XWPFParagraph paragraph = document.createParagraph();
        paragraph.setAlignment(ParagraphAlignment.CENTER);
        paragraph.setSpacingBefore(0);
        paragraph.setSpacingAfter(120);

        XWPFRun run = paragraph.createRun();

        try (InputStream logoStream = getClass().getClassLoader()
                .getResourceAsStream("psr/beas-logo.png")) {

            if (logoStream == null) {
                throw new IOException(
                        "BEAS logo not found: src/main/resources/psr/beas-logo.png");
            }

            try {
                run.addPicture(
                        logoStream,
                        XWPFDocument.PICTURE_TYPE_PNG,
                        "beas-logo.png",
                        Units.toEMU(180),
                        Units.toEMU(28));
            } catch (Exception ex) {
                throw new IOException(
                        "Failed to add BEAS logo to PMP review document", ex);
            }
        }
    }

    private void addFormLine(
            XWPFDocument document,
            String label,
            String value) {

        XWPFParagraph paragraph = document.createParagraph();
        paragraph.setSpacingBefore(100);
        paragraph.setSpacingAfter(100);
    
        
        XWPFRun labelRun = paragraph.createRun();
        labelRun.setText(label + " ");
        labelRun.setBold(true);
        labelRun.setFontFamily("Arial");
        labelRun.setFontSize(11);

        XWPFRun valueRun = paragraph.createRun();
        valueRun.setText(value == null || value.isBlank() ? " " : value);
        valueRun.setFontFamily("Arial");
        valueRun.setFontSize(11);
    }

    private String joinJsonArray(JsonNode array) {
        List<String> values = new java.util.ArrayList<>();

        array.forEach(value -> {
            String text = value.isValueNode()
                    ? value.asText()
                    : value.toString();

            if (text != null && !text.isBlank()) {
                values.add(text);
            }
        });

        return String.join(", ", values);
    }

    // ---------------------------------------------------------
    // Extract reviewer findings from Gemini JSON
    // ---------------------------------------------------------

    private String extractReviewComments(JsonNode root) {

    	String[] findingKeys = {
    	        "reviewerFindings",
    	        "findings",
    	        "reviewFindings",
    	        "reviewerComments",
    	        "reviewComments",
    	        "comments",
    	        "observations",
    	        "defects"
    	};
        for (String key : findingKeys) {
            JsonNode value = root.path(key);
            String extracted = extractFindingValue(value);

            if (extracted != null && !extracted.isBlank()) {
                return extracted;
            }
        }

        return "No reviewer findings provided.";
    }

    private String extractFindingValue(JsonNode value) {

        if (value == null || value.isMissingNode() || value.isNull()) {
            return null;
        }




if (value.isArray()) {
    List<String> formattedFindings = new java.util.ArrayList<>();

    int index = 1;

    for (JsonNode item : value) {
        String category = item.path("category").asText("");
        String finding = item.path("finding").asText("");

        if (finding.isBlank()) {
            finding = item.path("description").asText("");
        }

        if (finding.isBlank()) {
            finding = item.path("issue").asText("");
        }

        if (!category.isBlank() || !finding.isBlank()) {
            formattedFindings.add(
                    index++ + ". "
                    + (category.isBlank() ? "" : category + ": ")
                    + finding);
        }
    }

    return String.join("\n", formattedFindings);
}




        if (value.isObject()) {

            String[] nestedKeys = {
                    "comments",
                    "findings",
                    "reviewerFindings",
                    "reviewFindings",
                    "description",
                    "issue",
                    "observation",
                    "details",
                    "summary",
                    "text"
            };

            for (String key : nestedKeys) {
                JsonNode nested = value.path(key);
                String extracted = extractFindingValue(nested);

                if (extracted != null && !extracted.isBlank()) {
                    return extracted;
                }
            }

            return value.toPrettyString();
        }

        String text = value.asText("").trim();
        return text.isBlank() ? null : text;
    }

    private JsonNode parseGeminiJson(String content) throws IOException {

        String json = content.trim();

        // Handle Gemini responses wrapped in Markdown code fences.
        if (json.startsWith("```")) {
            int firstNewLine = json.indexOf('\n');

            if (firstNewLine >= 0) {
                json = json.substring(firstNewLine + 1);
            }

            int lastFence = json.lastIndexOf("```");

            if (lastFence >= 0) {
                json = json.substring(0, lastFence);
            }

            json = json.trim();
        }

        JsonNode root;

        try {
            root = objectMapper.readTree(json);
        } catch (Exception ex) {
            throw new IOException(
                    "Gemini returned content that is not valid JSON. "
                            + "Update the corresponding prompt to return "
                            + "valid JSON before exporting the DOCX.",
                    ex);
        }

        if (root == null || !root.isObject()) {
            throw new IOException(
                    "Gemini response must be a JSON object "
                            + "to create the formatted DOCX.");
        }

        return root;
    }

    private void addSectionHeading(
            XWPFDocument document,
            String text) {

        XWPFParagraph paragraph = document.createParagraph();
        paragraph.setSpacingBefore(320);
        paragraph.setSpacingAfter(180);
        paragraph.setKeepNext(true);

        XWPFRun run = paragraph.createRun();
        run.setText(text);
        run.setBold(true);
        run.setFontFamily("Arial");
        run.setFontSize(16);
    }

    private void addBodyText(
            XWPFDocument document,
            String text) {

        XWPFParagraph paragraph = document.createParagraph();
        paragraph.setSpacingAfter(160);

        XWPFRun run = paragraph.createRun();
        run.setText(text == null ? "" : text);
        run.setFontFamily("Arial");
        run.setFontSize(11);
    }

    private void addKeyValueTable(
            XWPFDocument document,
            String[][] values) {

        XWPFTable table = document.createTable();
        table.setTableAlignment(TableRowAlign.CENTER);

        for (int i = 0; i < values.length; i++) {
            XWPFTableRow row = (i == 0)
                    ? table.getRow(0)
                    : table.createRow();

            while (row.getTableCells().size() < 2) {
                row.addNewTableCell();
            }

            setCellText(row.getCell(0), values[i][0], true);
            setCellText(row.getCell(1), values[i][1], false);
        }
    }

		private void setFindingsCellText(
		        XWPFTableCell cell,
		        String comments) {
		
		    while (!cell.getParagraphs().isEmpty()) {
		        cell.removeParagraph(0);
		    }
		
		    XWPFParagraph heading = cell.addParagraph();
		    heading.setSpacingBefore(120);
		    heading.setSpacingAfter(120);
		    
		
		    XWPFRun headingRun = heading.createRun();
		    headingRun.setText("Comments:");
		    headingRun.setFontFamily("Arial");
		    headingRun.setFontSize(12);
		    headingRun.setBold(true);
		
		    if (comments == null || comments.isBlank()) {
		        return;
		    }
		
		    String[] findings = comments.split("\\R");
		
		    for (String finding : findings) {
		        if (finding.isBlank()) {
		            continue;
		        }
		
		        XWPFParagraph paragraph = cell.addParagraph();
		        paragraph.setSpacingBefore(100);
		        paragraph.setSpacingAfter(100);
		      
		        
		        XWPFRun run = paragraph.createRun();
		        run.setText(finding.trim());
		        run.setFontFamily("Arial");
		        run.setFontSize(12);
		    }
		}


    private void setCellText(
            XWPFTableCell cell,
            String text,
            boolean bold) {

        if (cell == null) {
            return;
        }

        if (cell.getParagraphs() != null
                && !cell.getParagraphs().isEmpty()) {
            cell.removeParagraph(0);
        }

        XWPFParagraph paragraph = cell.addParagraph();
        paragraph.setSpacingBefore(140);
        paragraph.setSpacingAfter(140);

        XWPFRun run = paragraph.createRun();
        run.setText(text == null ? "" : text);
        run.setFontFamily("Arial");
        run.setFontSize(11);
        run.setBold(bold);
    }

    // ---------------------------------------------------------
    // Create Excel file for the Defect Tracking Sheet
    // ---------------------------------------------------------



    private byte[] generateXls(Long opportunityId, String content)
            throws IOException {

    String json = content.trim();

    // Remove Markdown fences if Gemini returned them.
    if (json.startsWith("```")) {
        int firstNewLine = json.indexOf('\n');
        if (firstNewLine >= 0) {
            json = json.substring(firstNewLine + 1);
        }

        int lastFence = json.lastIndexOf("```");
        if (lastFence >= 0) {
            json = json.substring(0, lastFence);
        }

        json = json.trim();
    }

    // Accept either a direct JSON array or an object containing "defects".
    JsonNode root;
    try {
        root = objectMapper.readTree(json);
    } catch (Exception ex) {
        throw new IOException(
                "Gemini returned invalid defect tracking JSON.", ex);
    }

    JsonNode defects;

    if (root != null && root.isArray()) {
        defects = root;
    } else if (root != null && root.isObject()) {
        defects = root.path("defects");
    } else {
        throw new IOException(
                "Defect tracking JSON must be an array or an object "
                + "containing a defects array.");
    }

    if (!defects.isArray()) {
        throw new IOException(
                "Defect tracking JSON does not contain a defects array.");
    }

    String documentName = root.isObject()
            ? root.path("documentName").asText("Project Management Plan")
            : "Project Management Plan";

    String version = root.isObject()
            ? root.path("versionNumber").asText("Not provided")
            : "Not provided";

    // Always use the real project name from this opportunity, not Gemini
    // output or the sample project name stored in the Excel template.
    Opportunity opportunity = opportunityRepository.findById(opportunityId)
            .orElseThrow(() -> new IOException(
                    "Opportunity not found: " + opportunityId));
    String projectName = opportunity.getOpportunityName();
    if (projectName == null || projectName.isBlank()) {
        projectName = "";
    }

    try (InputStream templateStream = getClass().getClassLoader()
                .getResourceAsStream(
                        "pmp/ET_PMP_DefectTrackingSheet.xls");
         ByteArrayOutputStream output = new ByteArrayOutputStream()) {

        if (templateStream == null) {
            throw new IOException(
                    "PMP Defect Tracking template not found at "
                    + "src/main/resources/pmp/"
                    + "ET_PMP_DefectTrackingSheet.xls");
        }

        try (HSSFWorkbook workbook = new HSSFWorkbook(templateStream)) {

            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(12);
         // Fetch the current opportunity's Project Team.
            ProjectTeam team = projectTeamRepository
                    .findByOpportunityId(opportunityId)
                    .orElseThrow(() -> new IOException(
                            "Project Team not found for opportunity "
                                    + opportunityId));

            String projectManagerName = team.getProjectManager();

            if (projectManagerName == null || projectManagerName.isBlank()) {
                projectManagerName = "";
            }

            if (headerRow == null) {
                throw new IOException(
                        "Defect Tracking template header row is missing.");
            }

            // Update metadata in the template's original merged cells.
            // Apache POI row and column indexes are zero-based.

            // Excel B5: Project Name
            setCellValueIfPresent(
                    sheet.getRow(4), 1,
                    "Project Name: " + projectName);

            // Excel B6: Project Manager Name
            setCellValueIfPresent(
                    sheet.getRow(5), 1,
                    "Project Manager Name: " + projectManagerName);

            // Excel B7: Reviewer
            setCellValueIfPresent(
                    sheet.getRow(6), 1,
                    "Reviewer: " + projectManagerName);

            // Excel B8: Review Date — preserve the existing value.

            // Excel B9: Approver — remove the sample name.
            setCellValueIfPresent(
                    sheet.getRow(8), 1,
                    "Approver:");

            // Excel B10: Approval Date — preserve the existing value.

            // Update status headers only when those cells exist.
            if (headerRow.getCell(8) != null) {
                headerRow.getCell(8).setCellValue("Status at Review");
            }
            if (headerRow.getCell(9) != null) {
                headerRow.getCell(9).setCellValue("Status at Follow-up");
            }

            // Excel row 14 is index 13 in Apache POI.
            int firstDataRow = 13;
            int lastExistingRow = sheet.getLastRowNum();

            // Remove the template's sample defect values, keeping styles.
            for (int rowIndex = firstDataRow;
                    rowIndex <= lastExistingRow;
                    rowIndex++) {

                Row row = sheet.getRow(rowIndex);
                if (row == null) {
                    continue;
                }

                for (int column = 0; column < 10; column++) {
                    org.apache.poi.ss.usermodel.Cell cell =
                            row.getCell(column);
                    if (cell != null) {
                        cell.setCellValue("");
                    }
                }
            }

            Row styleSource = sheet.getRow(firstDataRow);

            for (int i = 0; i < defects.size(); i++) {
                JsonNode defect = defects.get(i);
                int rowIndex = firstDataRow + i;

                Row row = sheet.getRow(rowIndex);

                if (row == null) {
                    row = sheet.createRow(rowIndex);

                    if (styleSource != null) {
                        row.setHeight(styleSource.getHeight());
                    }

                    for (int column = 0; column < 10; column++) {
                        org.apache.poi.ss.usermodel.Cell cell =
                                row.createCell(column);

                        if (styleSource != null
                                && styleSource.getCell(column) != null) {
                            cell.setCellStyle(
                                    styleSource.getCell(column).getCellStyle());
                        }
                    }
                }

             // Generate sequential IDs: 1, 2, 3, 4, ...
                String defectId = String.valueOf(i + 1);

                String section = defect.path("pmpSection")
                        .asText("Not provided");

                String description = defect.path("defectDescription")
                        .asText("Not provided");

                // The generator uses "recommendedCorrectiveAction".
                // Keep "correctiveAction" as a fallback for older results.
                String recommendation = defect
                        .path("recommendedCorrectiveAction")
                        .asText("");

                if (recommendation.isBlank()) {
                    recommendation = defect.path("correctiveAction")
                            .asText("Not provided");
                }

                String severity = defect.path("severity")
                        .asText("Not provided");

                String status = defect.path("status").asText("Open");

                row.getCell(0).setCellValue(defectId);
                row.getCell(1).setCellValue("PMP");
                row.getCell(2).setCellValue(documentName);
                row.getCell(3).setCellValue(version);
                row.getCell(4).setCellValue(formatReferenceSection(section));
                row.getCell(5).setCellValue(description);
                row.getCell(6).setCellValue(recommendation);
                row.getCell(7).setCellValue(severity);
                row.getCell(8).setCellValue(status);
                row.getCell(9).setCellValue("Not provided");
            }

            workbook.write(output);
            return output.toByteArray();
        }
    }
}


    // ---------------------------------------------------------
    // Utility methods
    // ---------------------------------------------------------

    /**
     * Updates an existing template cell while preserving its formatting.
     */
    private void setCellValueIfPresent(
            Row row,
            int columnIndex,
            String value) {

        if (row == null) {
            return;
        }

        org.apache.poi.ss.usermodel.Cell cell = row.getCell(columnIndex);
        if (cell != null) {
            cell.setCellValue(value == null ? "" : value);
        }
    }
    private String formatReferenceSection(String section) {
        if (section == null || section.isBlank()) {
            return section;
        }

        String formatted = section.trim()
                .replaceAll("([a-z])([A-Z])", "$1 $2")
                .replaceAll("([A-Z])([A-Z][a-z])", "$1 $2");

        String[] words = formatted.split("\\s+");
        StringBuilder result = new StringBuilder();

        for (String word : words) {
            if (result.length() > 0) {
                result.append(" ");
            }

            result.append(word.substring(0, 1).toUpperCase(java.util.Locale.ROOT));
            result.append(word.substring(1));
        }

        return result.toString();
    }

    private String truncate(String message) {
        if (message == null || message.isBlank()) {
            return "Document generation failed.";
        }

        return message.substring(
                0, Math.min(message.length(), 1900));
    }

    private PmpReviewResponseDto toResponse(PmpReview review) {
        return new PmpReviewResponseDto(
                review.getId(),
                review.getOpportunityId(),
                review.getStatus(),
                review.getPmpGeneratedAt(),
                review.getReviewDocumentsDueAt(),
                review.getFollowUpDueAt(),
                review.getLastError());
    }

private PmpReviewDocument findDocument(
        List<PmpReviewDocument> documents,
        String documentType) {

    return documents.stream()
            .filter(d -> documentType.equals(d.getDocumentType()))
            .filter(d -> "GENERATED".equals(d.getStatus()))
            .max(java.util.Comparator.comparing(
                    PmpReviewDocument::getGeneratedAt,
                    java.util.Comparator.nullsFirst(
                            java.util.Comparator.naturalOrder())))
            .orElse(null);
}

private void saveGeneratedReviewFile(
        Long opportunityId,
        String documentType,
        String content,
        LocalDateTime generatedAt
) throws IOException {

    byte[] fileBytes;
    String fileName;

    if (DEFECT_TRACKING.equals(documentType)) {

        fileBytes = generateXls(opportunityId, content);

        fileName = "PMP_DEFECT_TRACKING_"
                + opportunityId
                + ".xls";

    } else {

        fileBytes = generateDocx(content);

        if (FOLLOW_UP.equals(documentType)) {

            String timestamp = generatedAt.format(
                    DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
            );

            fileName = "PMP_FOLLOW_UP_"
                    + opportunityId
                    + "_"
                    + timestamp
                    + ".docx";

        } else {

            fileName = "PMP_REVIEW_NOTE_"
                    + opportunityId
                    + ".docx";
        }
    }

    documentStorageService.saveFile(
            opportunityId,
            "Quality/Review",
            fileName,
            fileBytes
    );
}

}
