package com.projectestimation.backend.psr.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.stereotype.Component;

import com.projectestimation.backend.psr.dto.PsrActivityDto;

@Component
public class PsrDocxParser {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ENGLISH);

    public record ParsedPsrData(
            Integer version,
            LocalDate reportDate,
            LocalDate periodStart,
            LocalDate periodEnd,
            List<PsrActivityDto> activitiesPerformed,
            List<PsrActivityDto> nextWeekPlannedActivities,
            String riskStatus,
            String reconstructedMarkdown
    ) {}

    public ParsedPsrData parse(byte[] docxBytes) throws IOException {
        try (InputStream is = new ByteArrayInputStream(docxBytes)) {
            return parse(is);
        }
    }

    public ParsedPsrData parse(InputStream inputStream) throws IOException {
        try (XWPFDocument doc = new XWPFDocument(inputStream)) {
            Integer version = null;
            LocalDate reportDate = null;
            LocalDate periodStart = null;
            LocalDate periodEnd = null;

            StringBuilder mdBuilder = new StringBuilder();

            // 1. Read Paragraphs (Header information)
            List<XWPFParagraph> paragraphs = doc.getParagraphs();
            for (int i = 0; i < paragraphs.size(); i++) {
                String text = paragraphs.get(i).getText().trim();
                if (text.isBlank()) continue;

                mdBuilder.append(text).append("\n\n");

                if (text.contains("Version") && version == null) {
                    try {
                        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(?i)Version\\s*([0-9]+)").matcher(text);
                        if (m.find()) {
                            version = Integer.parseInt(m.group(1));
                        }
                    } catch (Exception ignored) {}
                }

                if (text.startsWith("Period of Reporting:") && i + 1 < paragraphs.size()) {
                    String dateLine = paragraphs.get(i + 1).getText().trim();
                    parsePeriodDates(dateLine, periodStart, periodEnd);
                } else if (text.contains("–") || text.contains("-")) {
                    String[] parts = text.split("[–-]");
                    if (parts.length == 2 && periodStart == null) {
                        try {
                            periodStart = LocalDate.parse(parts[0].trim());
                            periodEnd = LocalDate.parse(parts[1].trim());
                        } catch (Exception ignored) {}
                    }
                }

                if (text.startsWith("Date:") && reportDate == null) {
                    String dateStr = text.substring("Date:".length()).trim();
                    if (dateStr.isBlank() && i + 1 < paragraphs.size()) {
                        dateStr = paragraphs.get(i + 1).getText().trim();
                    }
                    try {
                        reportDate = LocalDate.parse(dateStr, DATE_FORMAT);
                    } catch (Exception ignored) {}
                }
            }

            // 2. Read Tables
            List<PsrActivityDto> activitiesPerformed = new ArrayList<>();
            List<PsrActivityDto> nextWeekPlannedActivities = new ArrayList<>();
            String riskStatus = null;

            List<XWPFTable> tables = doc.getTables();
            for (int tIdx = 0; tIdx < tables.size(); tIdx++) {
                XWPFTable table = tables.get(tIdx);
                List<XWPFTableRow> rows = table.getRows();
                if (rows.isEmpty()) continue;

                // Inspect header to detect table type
                String headerText = rows.get(0).getCell(0) != null ? rows.get(0).getCell(0).getText().toLowerCase() : "";

                if (tIdx == 0 || headerText.contains("task") || headerText.contains("activity")) {
                    // Check if it has 7 columns (Activities during the period) or 4 columns (Next 15 days planned)
                    int colCount = rows.get(0).getTableCells().size();
                    if (colCount >= 5 && activitiesPerformed.isEmpty()) {
                        // Table: Activities During the Period
                        // | Project Task | Completed | In progressing | Pending | % | Expected planned date | Actual finished date |
                        for (int r = 1; r < rows.size(); r++) {
                            XWPFTableRow row = rows.get(r);
                            List<XWPFTableCell> cells = row.getTableCells();
                            if (cells.isEmpty()) continue;

                            String taskName = getCellText(cells, 0);
                            if (taskName.isBlank()) continue;

                            String completedCheck = getCellText(cells, 1);
                            String inProgressCheck = getCellText(cells, 2);
                            String pendingCheck = getCellText(cells, 3);
                            String progressStr = getCellText(cells, 4);
                            String plannedDateStr = getCellText(cells, 5);
                            String actualDateStr = getCellText(cells, 6);

                            String status = "Pending";
                            int progress = 0;

                            if (isMarked(completedCheck)) {
                                status = "Completed";
                                progress = 100;
                            } else if (isMarked(inProgressCheck)) {
                                status = "In Progress";
                                progress = 50;
                            } else if (isMarked(pendingCheck)) {
                                status = "Pending";
                                progress = 0;
                            }

                            try {
                                if (!progressStr.isBlank()) {
                                    String clean = progressStr.replaceAll("[^0-9]", "");
                                    if (!clean.isBlank()) {
                                        progress = Integer.parseInt(clean);
                                        if (progress >= 100) status = "Completed";
                                        else if (progress > 0) status = "In Progress";
                                    }
                                }
                            } catch (Exception ignored) {}

                            PsrActivityDto dto = new PsrActivityDto();
                            dto.setActivityName(taskName);
                            dto.setTaskName(taskName);
                            dto.setStatus(status);
                            dto.setProgress(progress);
                            dto.setPlannedEndDate(plannedDateStr != null && !plannedDateStr.isBlank() ? plannedDateStr.trim() : null);
                            dto.setActualEndDate(actualDateStr != null && !actualDateStr.isBlank() ? actualDateStr.trim() : null);
                            activitiesPerformed.add(dto);
                        }
                    } else if (colCount <= 5 && nextWeekPlannedActivities.isEmpty()) {
                        // Table: Next 15 Days Planned Activities
                        // | Project Task | Duration (working days) | Planned date | Actual Finished date |
                        for (int r = 1; r < rows.size(); r++) {
                            XWPFTableRow row = rows.get(r);
                            List<XWPFTableCell> cells = row.getTableCells();
                            if (cells.isEmpty()) continue;

                            String taskName = getCellText(cells, 0);
                            if (taskName.isBlank()) continue;

                            String durationStr = getCellText(cells, 1);
                            String plannedDateStr = getCellText(cells, 2);
                            String actualDateStr = getCellText(cells, 3);

                            int duration = 1;
                            try {
                                if (!durationStr.isBlank()) {
                                    duration = Integer.parseInt(durationStr.replaceAll("[^0-9]", ""));
                                }
                            } catch (Exception ignored) {}

                            PsrActivityDto dto = new PsrActivityDto();
                            dto.setActivityName(taskName);
                            dto.setTaskName(taskName);
                            dto.setDuration(duration);
                            dto.setPlannedEndDate(plannedDateStr != null && !plannedDateStr.isBlank() ? plannedDateStr.trim() : null);
                            dto.setActualEndDate(actualDateStr != null && !actualDateStr.isBlank() ? actualDateStr.trim() : null);
                            dto.setStatus("Pending");
                            dto.setProgress(0);
                            nextWeekPlannedActivities.add(dto);
                        }
                    }
                } else if (headerText.contains("risk") || headerText.contains("item") || (tIdx == 2 && riskStatus == null)) {
                    // Risk table
                    StringBuilder riskBuilder = new StringBuilder();
                    for (XWPFTableRow row : rows) {
                        for (int c = 0; c < row.getTableCells().size(); c++) {
                            riskBuilder.append(row.getCell(c).getText().trim()).append(" | ");
                        }
                        riskBuilder.append("\n");
                    }
                    riskStatus = riskBuilder.toString();
                }
            }

            return new ParsedPsrData(
                    version,
                    reportDate != null ? reportDate : LocalDate.now(),
                    periodStart,
                    periodEnd,
                    activitiesPerformed,
                    nextWeekPlannedActivities,
                    riskStatus,
                    mdBuilder.toString()
            );
        }
    }

    private void parsePeriodDates(String line, LocalDate start, LocalDate end) {
        if (line == null || line.isBlank()) return;
        String[] parts = line.split("[–-]");
        if (parts.length == 2) {
            try {
                start = LocalDate.parse(parts[0].trim());
                end = LocalDate.parse(parts[1].trim());
            } catch (Exception ignored) {}
        }
    }

    private String getCellText(List<XWPFTableCell> cells, int index) {
        if (cells == null || index < 0 || index >= cells.size()) return "";
        XWPFTableCell cell = cells.get(index);
        return cell != null ? cell.getText().trim() : "";
    }

    private boolean isMarked(String text) {
        if (text == null) return false;
        String t = text.trim().toLowerCase();
        return t.contains("✓") || t.contains("v") || t.contains("x") || t.contains("yes") || t.contains("true") || t.equals("1");
    }

    private LocalDate parseDateSafe(String str) {
        if (str == null || str.isBlank()) return null;
        try {
            return LocalDate.parse(str.trim(), DATE_FORMAT);
        } catch (Exception e) {
            try {
                return LocalDate.parse(str.trim());
            } catch (Exception ignored) {}
        }
        return null;
    }
}
