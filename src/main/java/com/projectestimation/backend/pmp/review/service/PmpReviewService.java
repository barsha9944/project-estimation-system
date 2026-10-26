
package com.projectestimation.backend.pmp.review.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Supplier;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.projectestimation.backend.opportunity.model.ProjectTeam;
import com.projectestimation.backend.opportunity.repository.ProjectTeamRepository;
import com.projectestimation.backend.pmp.service.PmpService;
import com.projectestimation.backend.pmp.review.dto.PmpReviewResponseDto;
import com.projectestimation.backend.pmp.review.dto.PmpReviewStatusDto;
import com.projectestimation.backend.pmp.review.entity.PmpReview;
import com.projectestimation.backend.pmp.review.entity.PmpReviewDocument;
import com.projectestimation.backend.pmp.review.repository.PmpReviewRepository;
import com.projectestimation.backend.pmp.review.repository.PmpReviewDocumentRepository;

@Service
public class PmpReviewService {

    public static final String REVIEW_NOTE = "REVIEW_NOTE";
    public static final String DEFECT_TRACKING = "DEFECT_TRACKING";
    public static final String FOLLOW_UP = "FOLLOW_UP";

    private final PmpReviewRepository reviewRepository;
    private final PmpReviewDocumentRepository documentRepository;
    private final PmpService pmpService;
    private final ProjectTeamRepository projectTeamRepository;
    private final ObjectMapper objectMapper;
    private final PmpReviewNoteGenerator reviewNoteGenerator;
    private final PmpDefectTrackingGenerator defectTrackingGenerator;
    private final PmpReviewFollowUpGenerator followUpGenerator;

    public PmpReviewService(
            PmpReviewRepository reviewRepository,
            PmpReviewDocumentRepository documentRepository,
            PmpService pmpService,
            ProjectTeamRepository projectTeamRepository,
            ObjectMapper objectMapper,
            PmpReviewNoteGenerator reviewNoteGenerator,
            PmpDefectTrackingGenerator defectTrackingGenerator,
            PmpReviewFollowUpGenerator followUpGenerator) {

        this.reviewRepository = reviewRepository;
        this.documentRepository = documentRepository;
        this.pmpService = pmpService;
        this.projectTeamRepository = projectTeamRepository;
        this.objectMapper = objectMapper;
        this.reviewNoteGenerator = reviewNoteGenerator;
        this.defectTrackingGenerator = defectTrackingGenerator;
        this.followUpGenerator = followUpGenerator;
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

        return documentRepository
                .findByReviewAndDocumentType(review, documentType)
                .orElseThrow(() -> new IllegalArgumentException(
                        "PMP review document not found: "
                                + documentType));
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
                || LocalDateTime.now().isBefore(
                        review.getFollowUpDueAt())) {
            return;
        }

        PmpReviewDocument note;
        PmpReviewDocument defects;

        try {
            note = getDocument(opportunityId, REVIEW_NOTE);
            defects = getDocument(opportunityId, DEFECT_TRACKING);
        } catch (IllegalArgumentException ex) {
            return;
        }

        if (!"GENERATED".equals(note.getStatus())
                || !"GENERATED".equals(defects.getStatus())) {
            return;
        }

        PmpReviewDocument existingFollowUp = documentRepository
                .findByReviewAndDocumentType(review, FOLLOW_UP)
                .orElse(null);

        if (existingFollowUp != null
                && "GENERATED".equals(existingFollowUp.getStatus())) {
            updateReviewStatus(opportunityId);
            return;
        }

        String pmpContent = getPmpContent(opportunityId);

        generateDocument(
                review,
                FOLLOW_UP,
                () -> followUpGenerator.generate(
                        opportunityId,
                        pmpContent,
                        note.getDocumentContent(),
                        defects.getDocumentContent()));

        updateReviewStatus(opportunityId);
    }

    // ---------------------------------------------------------
    // Retrieve saved PMP
    // ---------------------------------------------------------

    private String getPmpContent(Long opportunityId) {
        try {
            return objectMapper.writeValueAsString(
                    pmpService.getPmp(opportunityId).pmp());
        } catch (Exception ex) {
            throw new IllegalStateException(
                    "Could not retrieve saved PMP for opportunity "
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

            document.setDocumentContent(generatedContent);
            document.setGeneratedAt(LocalDateTime.now());
            document.setStatus("GENERATED");
            document.setLastError(null);

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
        PmpReviewDocument defects = findDocument(documents, DEFECT_TRACKING);
        PmpReviewDocument followUp = findDocument(documents, FOLLOW_UP);

        if (followUp != null
                && "GENERATED".equals(followUp.getStatus())) {

            review.setStatus("COMPLETED");
            review.setLastError(null);

        } else if (note != null && defects != null
                && "GENERATED".equals(note.getStatus())
                && "GENERATED".equals(defects.getStatus())) {

            if (review.getFollowUpDueAt() == null) {
                LocalDateTime latest =
                        note.getGeneratedAt().isAfter(
                                defects.getGeneratedAt())
                                ? note.getGeneratedAt()
                                : defects.getGeneratedAt();

                // Temporary test delay. Restore plusHours(24) after testing.
                review.setFollowUpDueAt(latest.plusMinutes(2));
            }

            review.setStatus("WAITING_FOR_FOLLOW_UP");
            review.setLastError(null);

        } else if (documents.stream().anyMatch(
                document -> "FAILED".equals(document.getStatus()))) {

            review.setStatus("RETRY_PENDING");
            review.setLastError(
                    "One or more review documents failed. "
                            + "A retry is required.");

        } else {
            review.setStatus("SCHEDULED");
        }

        reviewRepository.save(review);
    }

    private PmpReviewDocument findDocument(
            List<PmpReviewDocument> documents,
            String type) {

        return documents.stream()
                .filter(document -> type.equals(document.getDocumentType()))
                .findFirst()
                .orElse(null);
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
            return generateXls(content);
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
            String cycle = root.path("cycleNumber").asText("1");

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
            titleRun.setText("Document Review Note");
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

            setCellText(findingsCell, "Comments:\n" + comments, false);

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
        paragraph.setSpacingBefore(50);
        paragraph.setSpacingAfter(50);

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
            String formatted = category.isBlank()
                    ? finding
                    : category + ": " + finding;

            formattedFindings.add(formatted);
        } else if (item.isValueNode()) {
            formattedFindings.add(item.asText());
        } else {
            formattedFindings.add(item.toPrettyString());
        }
    }

    return String.join("\n\n", formattedFindings);
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
        paragraph.setSpacingBefore(240);
        paragraph.setSpacingAfter(100);
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
        paragraph.setSpacingAfter(120);

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
        paragraph.setSpacingBefore(80);
        paragraph.setSpacingAfter(80);

        XWPFRun run = paragraph.createRun();
        run.setText(text == null ? "" : text);
        run.setFontFamily("Arial");
        run.setFontSize(11);
        run.setBold(bold);
    }

    // ---------------------------------------------------------
    // Create Excel file for the Defect Tracking Sheet
    // ---------------------------------------------------------

    private byte[] generateXls(String content) throws IOException {

        try (Workbook workbook = new HSSFWorkbook();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Defect Tracking");
            String[] lines = content.split("\\R", -1);

            for (int i = 0; i < lines.length; i++) {
                Row row = sheet.createRow(i);
                row.createCell(0).setCellValue(lines[i]);
            }

            sheet.setColumnWidth(0, 15000);
            workbook.write(output);
            return output.toByteArray();
        }
    }

    // ---------------------------------------------------------
    // Utility methods
    // ---------------------------------------------------------

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
}
