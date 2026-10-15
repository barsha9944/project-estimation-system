package com.projectestimation.backend.dar.review.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.projectestimation.backend.common.exception.ResourceNotFoundException;
import com.projectestimation.backend.dar.model.Dar;
import com.projectestimation.backend.dar.repository.DarRepository;
import com.projectestimation.backend.dar.review.ai.GeminiDarReviewOrchestrator;
import com.projectestimation.backend.dar.review.dto.DarReviewDto;
import com.projectestimation.backend.dar.review.entity.DarReview;
import com.projectestimation.backend.dar.review.repository.DarReviewRepository;
import com.projectestimation.backend.dar.service.DarService;
import com.projectestimation.backend.opportunity.model.ProjectTeam;
import com.projectestimation.backend.opportunity.repository.ProjectTeamRepository;

import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class DarReviewService {

    private final DarReviewRepository reviewRepository;
    private final DarRepository darRepository;
    private final ProjectTeamRepository projectTeamRepository;
    private final GeminiDarReviewOrchestrator geminiOrchestrator;
    private final DarService darService;
    private final ObjectMapper objectMapper;
    private final boolean allowImmediateAccess;

    public DarReviewService(
            DarReviewRepository reviewRepository,
            DarRepository darRepository,
            ProjectTeamRepository projectTeamRepository,
            GeminiDarReviewOrchestrator geminiOrchestrator,
            DarService darService,
            ObjectMapper objectMapper,
            @Value("${dar.review.allow-immediate-access:false}")
            boolean allowImmediateAccess
    ) {
        this.reviewRepository = reviewRepository;
        this.darRepository = darRepository;
        this.projectTeamRepository = projectTeamRepository;
        this.geminiOrchestrator = geminiOrchestrator;
        this.darService = darService;
        this.objectMapper = objectMapper;
        this.allowImmediateAccess = allowImmediateAccess;
    }

    @Transactional
    public DarReviewDto generateReview(Long opportunityId) {
        Dar dar = getDarAndCheckEligibility(opportunityId);

        DarReviewDto generated = geminiOrchestrator.generateReview(
                opportunityId,
                darService.getDar(opportunityId)
        );

        ProjectTeam team = projectTeamRepository
                .findByOpportunityId(opportunityId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Project Team not found for opportunity " + opportunityId
                ));

        List<DarReviewDto.ParticipantDto> evaluationParticipants =
                buildEvaluationParticipants(team);

        List<DarReviewDto.ParticipantDto> decisionParticipants =
                buildDecisionParticipants(team);

        DarReviewDto review = new DarReviewDto(
                opportunityId,
                generated.issueTitle(),
                generated.meetingDateTime(),
                "Beas Office",
                generated.darEventTrigger(),
                team.getProjectManager(),
                evaluationParticipants,
                decisionParticipants,
                safeList(generated.evaluationCriteria()),
                safeList(generated.alternatives()),
                generated.finalSolutionId(),
                generated.justification(),
                generated.preparedBy(),
                generated.preparedDate()
        );

        DarReview saved = reviewRepository
                .findByOpportunityId(opportunityId)
                .orElseGet(DarReview::new);

        saved.setOpportunityId(opportunityId);
        saved.setReviewData(writeJson(review));

        if (saved.getCreatedAt() == null) {
            saved.setCreatedAt(LocalDateTime.now());
        }
        saved.setUpdatedAt(LocalDateTime.now());

        reviewRepository.save(saved);

        return review;
    }

    @Transactional(readOnly = true)
    public DarReviewDto getReview(Long opportunityId) {
        getDarAndCheckEligibility(opportunityId);

        DarReview saved = reviewRepository
                .findByOpportunityId(opportunityId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "DAR Review has not been generated for opportunity "
                                + opportunityId
                ));

        return readJson(saved.getReviewData());
    }

    @Transactional
    public DarReviewDto updateReview(
            Long opportunityId,
            DarReviewDto request
    ) {
        getDarAndCheckEligibility(opportunityId);

        if (request == null) {
            throw new IllegalArgumentException(
                    "DAR Review request body cannot be empty."
            );
        }

        if (!opportunityId.equals(request.opportunityId())) {
            throw new IllegalArgumentException(
                    "The opportunity ID in the request does not match the URL."
            );
        }

        DarReview saved = reviewRepository
                .findByOpportunityId(opportunityId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Generate the DAR Review before editing it."
                ));

        DarReviewDto existing = readJson(saved.getReviewData());

        ProjectTeam team = projectTeamRepository
                .findByOpportunityId(opportunityId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Project Team not found for opportunity " + opportunityId
                ));

        // Participant names and Decision Owner come from project-team data.
        DarReviewDto updated = new DarReviewDto(
                opportunityId,
                request.issueTitle(),
                request.meetingDateTime(),
                "Beas Office",
                request.darEventTrigger(),
                team.getProjectManager(),
                existing.evaluationParticipants(),
                existing.decisionParticipants(),
                safeList(request.evaluationCriteria()),
                safeList(request.alternatives()),
                request.finalSolutionId(),
                request.justification(),
                request.preparedBy(),
                request.preparedDate()
        );

        saved.setReviewData(writeJson(updated));
        saved.setUpdatedAt(LocalDateTime.now());
        reviewRepository.save(saved);

        return updated;
    }

    @Transactional(readOnly = true)
    public byte[] downloadReview(Long opportunityId) throws IOException {
        DarReviewDto review = getReview(opportunityId);

        try (
                XWPFDocument document = new XWPFDocument();
                ByteArrayOutputStream output = new ByteArrayOutputStream()
        ) {
            var section = document.getDocument().getBody().addNewSectPr();
            var pageSize = section.addNewPgSz();
            pageSize.setW(java.math.BigInteger.valueOf(12240));
            pageSize.setH(java.math.BigInteger.valueOf(15840));

            var margins = section.addNewPgMar();
            margins.setTop(java.math.BigInteger.valueOf(720));
            margins.setBottom(java.math.BigInteger.valueOf(720));
            margins.setLeft(java.math.BigInteger.valueOf(720));
            margins.setRight(java.math.BigInteger.valueOf(720));

            addLogo(document);
            addTitle(document, "Proceedings of DAR Meeting");
            addParagraph(document, "Reference: BEAS/DAR/SD/01");

            addHeading(document, "1. Worksheet");

            XWPFTable worksheet = document.createTable(5, 2);
            setTableBorders(worksheet);

            setCell(worksheet, 0, 0, "Issue Title");
            setCell(worksheet, 0, 1, safeText(review.issueTitle()));
            setCell(worksheet, 1, 0, "Date/Time");
            setCell(worksheet, 1, 1, safeText(review.meetingDateTime()));
            setCell(worksheet, 2, 0, "Place");
            setCell(worksheet, 2, 1, "Beas Office");
            setCell(worksheet, 3, 0, "DAR Event Trigger");
            setCell(worksheet, 3, 1, safeText(review.darEventTrigger()));
            setCell(worksheet, 4, 0, "Decision Owner");
            setCell(worksheet, 4, 1, safeText(review.decisionOwner()));
            styleTable(worksheet, false);

            addHeading(document, "2. Evaluation Participants");
            addParticipantsTable(document, review.evaluationParticipants());

            addHeading(document, "3. Decision Participants");
            addParticipantsTable(document, review.decisionParticipants());

            document.createParagraph().setPageBreak(true);

            addTitle(document, "Evaluation and Decision");
            addHeading(document, "4. Evaluation Criteria");

            XWPFTable criteria = document.createTable(1, 3);
            setCell(criteria, 0, 0, "No.");
            setCell(criteria, 0, 1, "Criteria");
            setCell(criteria, 0, 2, "Weight");

            int number = 1;
            for (DarReviewDto.EvaluationCriterionDto criterion :
                    safeList(review.evaluationCriteria())) {
                XWPFTableRow row = criteria.createRow();
                setCell(row.getCell(0), String.valueOf(number++));
                setCell(row.getCell(1), safeText(criterion.name()));
                setCell(row.getCell(2), String.valueOf(criterion.weight()));
            }
            setTableBorders(criteria);
            styleTable(criteria, true);

            addHeading(document, "5. Solution Evaluation");

            List<DarReviewDto.EvaluationCriterionDto> allCriteria =
                    safeList(review.evaluationCriteria());

            XWPFTable matrix = document.createTable(
                    1, allCriteria.size() + 3
            );

            setCell(matrix, 0, 0, "Solution");
            setCell(matrix, 0, 1, "Evaluation Method");

            for (int i = 0; i < allCriteria.size(); i++) {
                setCell(matrix, 0, i + 2, "Criteria " + (i + 1));
            }

            setCell(matrix, 0, allCriteria.size() + 2, "Total Score");

            for (DarReviewDto.AlternativeDto alternative :
                    safeList(review.alternatives())) {
                XWPFTableRow row = matrix.createRow();

                while (row.getTableCells().size() < allCriteria.size() + 3) {
                    row.addNewTableCell();
                }

                setCell(row.getCell(0), safeText(alternative.solution()));
                setCell(row.getCell(1),
                        safeText(alternative.evaluationMethod()));

                List<Integer> scores = safeList(alternative.scores());

                for (int i = 0; i < allCriteria.size(); i++) {
                    String score = i < scores.size() && scores.get(i) != null
                            ? scores.get(i).toString()
                            : "";
                    setCell(row.getCell(i + 2), score);
                }

                setCell(
                        row.getCell(allCriteria.size() + 2),
                        String.format(
                                java.util.Locale.ROOT,
                                "%.2f",
                                alternative.totalScore()
                        )
                );
            }

            setTableBorders(matrix);
            styleTable(matrix, true);

            addHeading(document, "6. Final Solution");
            addParagraph(document, safeText(review.finalSolutionId()));

            addHeading(document, "7. Justification for Solution");
            addParagraph(document, safeText(review.justification()));

//            document.createParagraph().setPageBreak(true);
//
//            addTitle(document, "Review Approval");
//            addHeading(document, "8. Prepared By and Date");
//
//            XWPFTable prepared = document.createTable(2, 2);
//            setCell(prepared, 0, 0, "Prepared By");
//            setCell(prepared, 0, 1, safeText(review.preparedBy()));
//            setCell(prepared, 1, 0, "Date");
//            setCell(prepared, 1, 1, safeText(review.preparedDate()));
//            setTableBorders(prepared);
//            styleTable(prepared, false);

            document.write(output);
            return output.toByteArray();
        }
    }

    private Dar getDarAndCheckEligibility(Long opportunityId) {
        Dar dar = darRepository.findByOpportunityId(opportunityId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Generate the original DAR before accessing its review."
                ));

        if (dar.getCreatedAt() == null) {
            throw new IllegalStateException(
                    "The original DAR has no creation timestamp."
            );
        }

        if (!allowImmediateAccess
                && LocalDateTime.now().isBefore(dar.getCreatedAt().plusHours(24))) {
            throw new IllegalStateException(
                    "DAR Review will be available 24 hours after the original DAR was generated."
            );
        }

        return dar;
    }

    private List<DarReviewDto.ParticipantDto> buildEvaluationParticipants(
            ProjectTeam team
    ) {
        Set<String> names = new LinkedHashSet<>();
        addName(names, team.getProjectManager());
        addName(names, team.getTeamLead());

        addName(names, team.getDevelopers());
        addName(names, team.getTester());
        addName(names, team.getDatabaseDevelopers());

        List<DarReviewDto.ParticipantDto> participants = new ArrayList<>();
        for (String name : names) {
            participants.add(new DarReviewDto.ParticipantDto(name, "P"));
        }
        return participants;
    }

    private List<DarReviewDto.ParticipantDto> buildDecisionParticipants(
            ProjectTeam team
    ) {
        Set<String> names = new LinkedHashSet<>();
        addName(names, team.getProjectManager());
        addName(names, team.getTeamLead());

        List<DarReviewDto.ParticipantDto> participants = new ArrayList<>();
        for (String name : names) {
            participants.add(new DarReviewDto.ParticipantDto(name, "P"));
        }
        return participants;
    }

    private void addName(Set<String> names, String name) {
        if (name != null && !name.isBlank()) {
            names.add(name.trim());
        }
    }

    private void addName(Set<String> names, List<String> values) {
        if (values != null) {
            values.stream()
                    .filter(value -> value != null && !value.isBlank())
                    .map(String::trim)
                    .forEach(names::add);
        }
    }

    private void addLogo(XWPFDocument document) throws IOException {
        XWPFParagraph paragraph = document.createParagraph();
        paragraph.setAlignment(ParagraphAlignment.RIGHT);

        try (InputStream stream = getClass().getClassLoader()
                .getResourceAsStream("psr/beas-logo.png")) {
            if (stream == null) {
                throw new IOException(
                        "BEAS logo not found at src/main/resources/psr/beas-logo.png"
                );
            }

            XWPFRun run = paragraph.createRun();
            try {
                run.addPicture(
                        stream,
                        XWPFDocument.PICTURE_TYPE_PNG,
                        "beas-logo.png",
                        Units.toEMU(158),
                        Units.toEMU(24)
                );
            } catch (org.apache.poi.openxml4j.exceptions.InvalidFormatException e) {
                throw new IOException("Could not add the BEAS logo.", e);
            }
        }
    }

    private void addTitle(XWPFDocument document, String text) {
        XWPFParagraph paragraph = document.createParagraph();
        paragraph.setSpacingAfter(160);

        XWPFRun run = paragraph.createRun();
        run.setBold(true);
        run.setFontFamily("Arial");
        run.setFontSize(20);
        run.setText(text);
    }

    private void addHeading(XWPFDocument document, String text) {
        XWPFParagraph paragraph = document.createParagraph();
        paragraph.setSpacingBefore(120);
        paragraph.setSpacingAfter(80);

        XWPFRun run = paragraph.createRun();
        run.setBold(true);
        run.setFontFamily("Times New Roman");
        run.setFontSize(15);
        run.setColor("C00000");
        run.setText(text);
    }

    private void addParagraph(XWPFDocument document, String text) {
        XWPFParagraph paragraph = document.createParagraph();
        paragraph.setSpacingAfter(60);

        XWPFRun run = paragraph.createRun();
        run.setFontFamily("Times New Roman");
        run.setFontSize(12);
        run.setText(text == null ? "" : text);
    }

    private void addParticipantsTable(
            XWPFDocument document,
            List<DarReviewDto.ParticipantDto> participants
    ) {
        // Place the status explanation above the table, as requested.
        addParagraph(
                document,
                "(Status: P = Active Participant, N = No Active Participation)"
        );

        XWPFTable table = document.createTable(1, 4);
        table.setWidth("100%");

        // Four-column layout: Name | Status | Name | Status.
        setCell(table, 0, 0, "Name");
        setCell(table, 0, 1, "Status");
        setCell(table, 0, 2, "Name");
        setCell(table, 0, 3, "Status");

        List<DarReviewDto.ParticipantDto> values = safeList(participants);

        // Display two participants in each row.
        for (int i = 0; i < values.size(); i += 2) {
            XWPFTableRow row = table.createRow();

            while (row.getTableCells().size() < 4) {
                row.addNewTableCell();
            }

            DarReviewDto.ParticipantDto first = values.get(i);
            setCell(row.getCell(0), safeText(first.name()));
            setCell(row.getCell(1), "P");

            if (i + 1 < values.size()) {
                DarReviewDto.ParticipantDto second = values.get(i + 1);
                setCell(row.getCell(2), safeText(second.name()));
                setCell(row.getCell(3), "P");
            } else {
                setCell(row.getCell(2), "");
                setCell(row.getCell(3), "");
            }
        }

        setTableBorders(table);
        styleTable(table, true);
    }

    private void setCell(
            XWPFTable table,
            int row,
            int column,
            String text
    ) {
        setCell(table.getRow(row).getCell(column), text);
    }

    private void setCell(XWPFTableCell cell, String text) {
        cell.setText("");
        XWPFParagraph paragraph = cell.getParagraphs().get(0);
        XWPFRun run = paragraph.createRun();
        run.setFontFamily("Times New Roman");
        run.setFontSize(11);
        run.setText(text == null ? "" : text);
    }

    private void setTableBorders(XWPFTable table) {
        var borders = table.getCTTbl().getTblPr().isSetTblBorders()
                ? table.getCTTbl().getTblPr().getTblBorders()
                : table.getCTTbl().getTblPr().addNewTblBorders();

        for (var edge : new org.openxmlformats.schemas.wordprocessingml.x2006.main.CTBorder[]{
                borders.isSetTop() ? borders.getTop() : borders.addNewTop(),
                borders.isSetLeft() ? borders.getLeft() : borders.addNewLeft(),
                borders.isSetBottom() ? borders.getBottom() : borders.addNewBottom(),
                borders.isSetRight() ? borders.getRight() : borders.addNewRight(),
                borders.isSetInsideH() ? borders.getInsideH() : borders.addNewInsideH(),
                borders.isSetInsideV() ? borders.getInsideV() : borders.addNewInsideV()
        }) {
            edge.setVal(
                    org.openxmlformats.schemas.wordprocessingml.x2006.main.STBorder.SINGLE
            );
            edge.setSz(java.math.BigInteger.valueOf(4));
            edge.setColor("808080");
        }
    }

    private void styleTable(XWPFTable table, boolean headerRow) {
        for (int i = 0; i < table.getRows().size(); i++) {
            XWPFTableRow row = table.getRow(i);

            for (XWPFTableCell cell : row.getTableCells()) {
                cell.setVerticalAlignment(XWPFTableCell.XWPFVertAlign.CENTER);

                // Add padding so the larger table text is easier to read.
                var tcPr = cell.getCTTc().getTcPr();
                if (tcPr == null) {
                    tcPr = cell.getCTTc().addNewTcPr();
                }

                var margins = tcPr.isSetTcMar()
                        ? tcPr.getTcMar()
                        : tcPr.addNewTcMar();

                var top = margins.isSetTop()
                        ? margins.getTop()
                        : margins.addNewTop();
                top.setW(java.math.BigInteger.valueOf(140));

                var bottom = margins.isSetBottom()
                        ? margins.getBottom()
                        : margins.addNewBottom();
                bottom.setW(java.math.BigInteger.valueOf(140));

                var left = margins.isSetStart()
                        ? margins.getStart()
                        : margins.addNewStart();
                left.setW(java.math.BigInteger.valueOf(160));

                var right = margins.isSetEnd()
                        ? margins.getEnd()
                        : margins.addNewEnd();
                right.setW(java.math.BigInteger.valueOf(160));

                if (headerRow && i == 0) {
                    var shading = tcPr.isSetShd()
                            ? tcPr.getShd()
                            : tcPr.addNewShd();
                    shading.setFill("E7E6E6");
                }

                for (XWPFParagraph paragraph : cell.getParagraphs()) {
                    for (XWPFRun run : paragraph.getRuns()) {
                        if (headerRow && i == 0) {
                            run.setBold(true);
                        }
                    }
                }
            }
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Could not serialize DAR Review data.", e
            );
        }
    }

    private DarReviewDto readJson(String json) {
        try {
            return objectMapper.readValue(json, DarReviewDto.class);
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Could not read saved DAR Review data.", e
            );
        }
    }

    private String safeText(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private <T> List<T> safeList(List<T> values) {
        return values == null ? List.of() : values;
    }
}
