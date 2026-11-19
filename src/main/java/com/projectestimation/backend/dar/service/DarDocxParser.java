package com.projectestimation.backend.dar.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.projectestimation.backend.dar.dto.DarAgendaItemDto;
import com.projectestimation.backend.dar.dto.DarCriterionScoreDto;
import com.projectestimation.backend.dar.dto.DarDecisionAlternativeDto;
import com.projectestimation.backend.dar.dto.DarDiscussionNoteDto;

@Component
public class DarDocxParser {

    private static final Logger log = LoggerFactory.getLogger(DarDocxParser.class);

    public static class ParsedDarData {
        public String meetingName;
        public LocalDate meetingDate;
        public String projectName;
        public String projectPhase;
        public List<String> invitees = new ArrayList<>();
        public List<DarAgendaItemDto> agenda = new ArrayList<>();
        public List<DarDiscussionNoteDto> discussionNotes = new ArrayList<>();
        public List<String> evaluationCriteria = new ArrayList<>();
        public List<DarDecisionAlternativeDto> alternatives = new ArrayList<>();
        public String recommendedAlternativeId;
        public String recommendationReason;
    }

    public ParsedDarData parse(Path path) throws IOException {
        try (InputStream is = Files.newInputStream(path)) {
            return parse(is);
        }
    }

    public ParsedDarData parse(byte[] bytes) throws IOException {
        try (InputStream is = new ByteArrayInputStream(bytes)) {
            return parse(is);
        }
    }

    public ParsedDarData parse(InputStream inputStream) throws IOException {
        ParsedDarData data = new ParsedDarData();

        try (XWPFDocument doc = new XWPFDocument(inputStream)) {
            List<XWPFTable> tables = doc.getTables();

            XWPFTable detailsTable = null;
            XWPFTable agendaTable = null;
            XWPFTable discussionTable = null;
            XWPFTable decisionMatrixTable = null;
            XWPFTable criteriaTable = null;

            for (XWPFTable table : tables) {
                if (table.getRows().isEmpty()) continue;
                String firstRowText = getRowText(table.getRow(0)).toLowerCase();

                if (detailsTable == null && (firstRowText.contains("meeting name") || firstRowText.contains("project phase") || firstRowText.contains("invitees"))) {
                    detailsTable = table;
                } else if (agendaTable == null && (firstRowText.contains("item") && firstRowText.contains("action item"))) {
                    agendaTable = table;
                } else if (discussionTable == null && (firstRowText.contains("topic") && firstRowText.contains("discussion"))) {
                    discussionTable = table;
                } else if (decisionMatrixTable == null && (firstRowText.contains("solution") && firstRowText.contains("total score"))) {
                    decisionMatrixTable = table;
                } else if (criteriaTable == null && (firstRowText.contains("criteria") && firstRowText.contains("weight") && !firstRowText.contains("solution"))) {
                    criteriaTable = table;
                }
            }

            // Fallback to table indices if header matching missed any table
            if (detailsTable == null && tables.size() > 0) detailsTable = tables.get(0);
            if (agendaTable == null && tables.size() > 1) agendaTable = tables.get(1);
            if (discussionTable == null && tables.size() > 2) discussionTable = tables.get(2);
            if (decisionMatrixTable == null && tables.size() > 3) decisionMatrixTable = tables.get(3);
            if (criteriaTable == null && tables.size() > 4) criteriaTable = tables.get(4);

            parseDetailsTable(detailsTable, data);
            parseAgendaTable(agendaTable, data);
            parseDiscussionTable(discussionTable, data);

            List<Integer> criteriaWeights = new ArrayList<>();
            parseCriteriaTable(criteriaTable, data, criteriaWeights);
            parseDecisionMatrixTable(decisionMatrixTable, data, criteriaWeights);

            parseParagraphs(doc, data);

        } catch (Exception ex) {
            log.error("Error parsing DAR docx: {}", ex.getMessage(), ex);
            throw new IOException("Failed to parse DAR docx", ex);
        }

        return data;
    }

    private void parseDetailsTable(XWPFTable table, ParsedDarData data) {
        if (table == null) return;
        for (XWPFTableRow row : table.getRows()) {
            List<XWPFTableCell> cells = row.getTableCells();
            if (cells.size() < 2) continue;

            String label = cells.get(0).getText().trim().toLowerCase();
            String value = cells.get(1).getText().trim();

            if (label.contains("meeting name")) {
                data.meetingName = value;
            } else if (label.contains("meeting date")) {
                data.meetingDate = parseDate(value);
            } else if (label.contains("project phase")) {
                data.projectPhase = value;
            } else if (label.contains("project")) {
                data.projectName = value;
            } else if (label.contains("invitees")) {
                if (!value.isBlank()) {
                    String[] lines = value.split("[\\r\\n]+");
                    for (String line : lines) {
                        String trimmed = line.trim();
                        if (!trimmed.isBlank()) {
                            data.invitees.add(trimmed);
                        }
                    }
                    if (data.invitees.size() <= 1 && value.contains(",")) {
                        data.invitees.clear();
                        for (String part : value.split(",")) {
                            String trimmed = part.trim();
                            if (!trimmed.isBlank()) {
                                data.invitees.add(trimmed);
                            }
                        }
                    }
                }
            }
        }
    }

    private void parseAgendaTable(XWPFTable table, ParsedDarData data) {
        if (table == null) return;
        List<XWPFTableRow> rows = table.getRows();
        if (rows.size() <= 1) return;

        for (int i = 1; i < rows.size(); i++) {
            XWPFTableRow row = rows.get(i);
            List<XWPFTableCell> cells = row.getTableCells();
            if (cells.size() < 2) continue;

            String actionItemsText = cells.get(1).getText().trim();
            if (actionItemsText.isBlank()) continue;

            String topic;
            String description;
            int firstNewline = actionItemsText.indexOf('\n');
            if (firstNewline > 0) {
                topic = actionItemsText.substring(0, firstNewline).trim();
                description = actionItemsText.substring(firstNewline + 1).trim();
            } else {
                topic = actionItemsText;
                description = "";
            }

            data.agenda.add(new DarAgendaItemDto(topic, description, null));
        }
    }

    private void parseDiscussionTable(XWPFTable table, ParsedDarData data) {
        if (table == null) return;
        List<XWPFTableRow> rows = table.getRows();
        if (rows.size() <= 1) return;

        for (int i = 1; i < rows.size(); i++) {
            XWPFTableRow row = rows.get(i);
            List<XWPFTableCell> cells = row.getTableCells();
            if (cells.size() < 2) continue;

            String topic = cells.get(0).getText().trim();
            String content = cells.get(1).getText().trim();

            String discussion;
            String outcome;

            String lower = content.toLowerCase();
            if (lower.contains("outcome:")) {
                int outcomeIndex = lower.indexOf("outcome:");
                String discPart = content.substring(0, outcomeIndex).trim();
                String outPart = content.substring(outcomeIndex + "outcome:".length()).trim();

                if (discPart.toLowerCase().startsWith("discussion:")) {
                    discPart = discPart.substring("discussion:".length()).trim();
                }
                discussion = discPart;
                outcome = outPart;
            } else if (lower.startsWith("discussion:")) {
                discussion = content.substring("discussion:".length()).trim();
                outcome = "";
            } else {
                discussion = content;
                outcome = "";
            }

            data.discussionNotes.add(new DarDiscussionNoteDto(topic, discussion, outcome));
        }
    }

    private void parseCriteriaTable(XWPFTable table, ParsedDarData data, List<Integer> criteriaWeights) {
        if (table == null) return;
        List<XWPFTableRow> rows = table.getRows();
        if (rows.size() <= 1) return;

        for (int i = 1; i < rows.size(); i++) {
            XWPFTableRow row = rows.get(i);
            List<XWPFTableCell> cells = row.getTableCells();
            if (cells.size() < 2) continue;

            String criterionName = cells.get(1).getText().trim();
            if (criterionName.isBlank()) continue;
            data.evaluationCriteria.add(criterionName);

            Integer weight = null;
            if (cells.size() >= 3) {
                String weightStr = cells.get(2).getText().trim();
                try {
                    weight = (int) Math.round(Double.parseDouble(weightStr));
                } catch (Exception ignored) {
                }
            }
            criteriaWeights.add(weight);
        }
    }

    private void parseDecisionMatrixTable(
            XWPFTable table,
            ParsedDarData data,
            List<Integer> criteriaWeights
    ) {
        if (table == null) return;
        List<XWPFTableRow> rows = table.getRows();
        if (rows.size() <= 1) return;

        XWPFTableRow headerRow = rows.get(0);
        List<XWPFTableCell> headerCells = headerRow.getTableCells();
        int totalCols = headerCells.size();
        int criterionCols = Math.max(0, totalCols - 3);

        for (int i = 1; i < rows.size(); i++) {
            XWPFTableRow row = rows.get(i);
            List<XWPFTableCell> cells = row.getTableCells();
            if (cells.isEmpty()) continue;

            String alternativeName = cells.get(0).getText().trim();
            if (alternativeName.isBlank()) continue;

            String alternativeId = "A" + i;
            String description = "";

            List<DarCriterionScoreDto> scores = new ArrayList<>();
            double calculatedWeightedTotal = 0.0;

            for (int c = 0; c < criterionCols; c++) {
                int colIndex = c + 2;
                String critName = (c < data.evaluationCriteria.size())
                        ? data.evaluationCriteria.get(c)
                        : "Criteria " + (c + 1);

                Integer weight = (c < criteriaWeights.size())
                        ? criteriaWeights.get(c)
                        : null;

                Integer score = null;
                if (colIndex < cells.size()) {
                    String scoreStr = cells.get(colIndex).getText().trim();
                    if (!scoreStr.isBlank()) {
                        try {
                            score = (int) Math.round(Double.parseDouble(scoreStr));
                        } catch (Exception ignored) {
                        }
                    }
                }

                if (weight != null && score != null) {
                    calculatedWeightedTotal += (weight * score);
                }

                scores.add(new DarCriterionScoreDto(critName, weight, score, null));
            }

            Double weightedTotal = null;
            int totalScoreCol = totalCols - 1;
            if (totalScoreCol < cells.size()) {
                String totalStr = cells.get(totalScoreCol).getText().trim();
                if (!totalStr.isBlank()) {
                    try {
                        weightedTotal = Double.parseDouble(totalStr);
                    } catch (Exception ignored) {
                    }
                }
            }
            if (weightedTotal == null && calculatedWeightedTotal > 0) {
                weightedTotal = calculatedWeightedTotal;
            }

            data.alternatives.add(new DarDecisionAlternativeDto(
                    alternativeId,
                    alternativeName,
                    description,
                    scores,
                    weightedTotal
            ));
        }
    }

    private void parseParagraphs(XWPFDocument doc, ParsedDarData data) {
        for (XWPFParagraph p : doc.getParagraphs()) {
            String text = p.getText().trim();
            if (text.isBlank()) continue;

            String lower = text.toLowerCase();
            if (lower.startsWith("recommended alternative:")) {
                data.recommendedAlternativeId = text.substring("recommended alternative:".length()).trim();
            } else if (lower.startsWith("recommendation reason:")) {
                data.recommendationReason = text.substring("recommendation reason:".length()).trim();
            }
        }
    }

    private String getRowText(XWPFTableRow row) {
        if (row == null) return "";
        StringBuilder sb = new StringBuilder();
        for (XWPFTableCell cell : row.getTableCells()) {
            sb.append(cell.getText()).append(" ");
        }
        return sb.toString();
    }

    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) return null;
        String clean = dateStr.trim();
        List<DateTimeFormatter> formatters = List.of(
                DateTimeFormatter.ISO_LOCAL_DATE,
                DateTimeFormatter.ofPattern("yyyy-MM-dd"),
                DateTimeFormatter.ofPattern("dd/MM/yyyy"),
                DateTimeFormatter.ofPattern("MM/dd/yyyy"),
                DateTimeFormatter.ofPattern("dd-MM-yyyy"),
                DateTimeFormatter.ofPattern("d/M/yyyy"),
                DateTimeFormatter.ofPattern("d-M-yyyy")
        );
        for (DateTimeFormatter dtf : formatters) {
            try {
                return LocalDate.parse(clean, dtf);
            } catch (Exception ignored) {
            }
        }
        return null;
    }
}
