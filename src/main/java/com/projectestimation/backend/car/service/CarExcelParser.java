package com.projectestimation.backend.car.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Component;

@Component
public class CarExcelParser {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ENGLISH);

    public record ParsedAction(
            int sequence,
            String actionDescription,
            String actionCategory,
            String priority,
            String assignedTo,
            LocalDate targetCompletionDate,
            String actionTaken,
            String status
    ) {}

    public record ParsedEffectiveness(
            String effectivenessResult,
            String actualOutcome,
            String supportingEvidence
    ) {}

    public record ParsedCarData(
            String triggerForCar,
            String problemStatement,
            String originatorName,
            String causalAnalysisTechnique,
            String fiveWhysDetails,
            List<ParsedAction> actions,
            List<ParsedEffectiveness> effectivenessReviews
    ) {}

    public ParsedCarData parse(byte[] bytes) throws IOException {
        try (InputStream is = new ByteArrayInputStream(bytes)) {
            return parse(is);
        }
    }

    public ParsedCarData parse(InputStream inputStream) throws IOException {
        try (Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet trackerSheet = workbook.getSheet("CAR Action Tracker");
            Sheet reportSheet = workbook.getSheet("CAR Report");

            if (trackerSheet == null && reportSheet == null) {
                // Try first sheet if named differently
                trackerSheet = workbook.getSheetAt(0);
            }

            String triggerForCar = null;
            String problemStatement = null;
            String originatorName = null;
            String causalAnalysisTechnique = null;

            List<ParsedAction> actions = new ArrayList<>();
            List<ParsedEffectiveness> effectivenessList = new ArrayList<>();

            // 1. Read CAR Action Tracker Sheet
            if (trackerSheet != null) {
                triggerForCar = getCellValue(trackerSheet, 3, 2);
                problemStatement = getCellValue(trackerSheet, 4, 2);
                originatorName = getCellValue(trackerSheet, 5, 2);
                causalAnalysisTechnique = getCellValue(trackerSheet, 6, 3);

                // Actions from row 11 onwards (rows 11 and 12 typically)
                int maxRows = Math.min(trackerSheet.getLastRowNum(), 30);
                for (int r = 11; r <= maxRows; r++) {
                    Row row = trackerSheet.getRow(r);
                    if (row == null) continue;

                    String desc = getCellString(row.getCell(1));
                    if (desc == null || desc.isBlank()) continue;

                    int seq = getCellInt(row.getCell(0), actions.size() + 1);
                    String assignedTo = getCellString(row.getCell(2));
                    LocalDate targetDate = getCellDate(row.getCell(3));
                    String actionTaken = getCellString(row.getCell(4));
                    String effectStr = getCellString(row.getCell(6));
                    String status = getCellString(row.getCell(8));

                    if (effectStr != null && !effectStr.isBlank()) {
                        String clean = effectStr.replace("CAR-level review: ", "").trim();
                        String[] parts = clean.split("\\|");
                        String effResult = parts.length > 0 ? parts[0].trim() : clean;
                        String outcome = parts.length > 1 ? parts[1].trim() : null;
                        String evidence = parts.length > 2 ? parts[2].trim() : null;
                        effectivenessList.add(new ParsedEffectiveness(effResult, outcome, evidence));
                    }

                    actions.add(new ParsedAction(
                            seq,
                            desc,
                            null, // will be complemented from report sheet if available
                            null, // will be complemented from report sheet if available
                            assignedTo,
                            targetDate,
                            actionTaken,
                            status != null && !status.isBlank() ? status : "OPEN"
                    ));
                }
            }

            // 2. Read CAR Report Sheet
            String fiveWhysDetails = null;
            if (reportSheet != null) {
                if (triggerForCar == null || triggerForCar.isBlank()) {
                    String rawTrigger = getCellValue(reportSheet, 4, 3);
                    if (rawTrigger != null && rawTrigger.startsWith("Trigger for CAR:")) {
                        triggerForCar = rawTrigger.substring("Trigger for CAR:".length()).trim();
                    } else {
                        triggerForCar = rawTrigger;
                    }
                }

                if (causalAnalysisTechnique == null || causalAnalysisTechnique.isBlank()) {
                    String rawTech = getCellValue(reportSheet, 8, 3);
                    if (rawTech != null && rawTech.startsWith("Root Cause Analysis -")) {
                        causalAnalysisTechnique = rawTech.substring("Root Cause Analysis -".length()).trim();
                    }
                }

                // Parse Why / Answer pairs from rows 9..18
                StringBuilder whysBuilder = new StringBuilder();
                for (int pair = 0; pair < 5; pair++) {
                    int whyRow = 9 + (pair * 2);
                    int ansRow = whyRow + 1;
                    String q = getCellValue(reportSheet, whyRow, 5);
                    String a = getCellValue(reportSheet, ansRow, 5);
                    if ((q != null && !q.isBlank()) || (a != null && !a.isBlank())) {
                        if (whysBuilder.length() > 0) {
                            whysBuilder.append("\n");
                        }
                        whysBuilder.append((pair + 1)).append(". Why: ").append(q != null ? q.trim() : "")
                                .append(" Ans: ").append(a != null ? a.trim() : "");
                    }
                }
                if (whysBuilder.length() > 0) {
                    fiveWhysDetails = whysBuilder.toString();
                }

                // Check Action table rows (rows 24 and 25) for category and priority
                int[] actionRows = {24, 25};
                for (int i = 0; i < actionRows.length; i++) {
                    int rowIndex = actionRows[i];
                    Row row = reportSheet.getRow(rowIndex);
                    if (row == null) continue;

                    String desc = getCellString(row.getCell(4));
                    String category = getCellString(row.getCell(7));
                    String priority = getCellString(row.getCell(10));
                    String assignedTo = getCellString(row.getCell(11));
                    LocalDate targetDate = getCellDate(row.getCell(12));

                    if (desc != null && !desc.isBlank()) {
                        if (i < actions.size()) {
                            // Update category & priority of existing action from tracker
                            ParsedAction cur = actions.get(i);
                            actions.set(i, new ParsedAction(
                                    cur.sequence(),
                                    cur.actionDescription() != null && !cur.actionDescription().isBlank() ? cur.actionDescription() : desc,
                                    category != null ? category : cur.actionCategory(),
                                    priority != null ? priority : cur.priority(),
                                    cur.assignedTo() != null ? cur.assignedTo() : assignedTo,
                                    cur.targetCompletionDate() != null ? cur.targetCompletionDate() : targetDate,
                                    cur.actionTaken(),
                                    cur.status()
                            ));
                        } else {
                            actions.add(new ParsedAction(
                                    i + 1,
                                    desc,
                                    category,
                                    priority,
                                    assignedTo,
                                    targetDate,
                                    null,
                                    "OPEN"
                            ));
                        }
                    }
                }
            }

            return new ParsedCarData(
                    triggerForCar,
                    problemStatement,
                    originatorName,
                    causalAnalysisTechnique,
                    fiveWhysDetails,
                    actions,
                    effectivenessList
            );
        }
    }

    private String getCellValue(Sheet sheet, int rowIndex, int colIndex) {
        if (sheet == null) return null;
        Row row = sheet.getRow(rowIndex);
        if (row == null) return null;
        return getCellString(row.getCell(colIndex));
    }

    private String getCellString(Cell cell) {
        if (cell == null) return null;
        CellType type = cell.getCellType();
        if (type == CellType.FORMULA) {
            type = cell.getCachedFormulaResultType();
        }
        return switch (type) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    yield cell.getLocalDateTimeCellValue().toLocalDate().toString();
                } else {
                    double num = cell.getNumericCellValue();
                    if (num == Math.floor(num)) {
                        yield String.valueOf((long) num);
                    } else {
                        yield String.valueOf(num);
                    }
                }
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> null;
        };
    }

    private int getCellInt(Cell cell, int defaultValue) {
        if (cell == null) return defaultValue;
        try {
            if (cell.getCellType() == CellType.NUMERIC) {
                return (int) cell.getNumericCellValue();
            }
            String s = getCellString(cell);
            if (s != null && !s.isBlank()) {
                return Integer.parseInt(s.trim());
            }
        } catch (Exception ignored) {}
        return defaultValue;
    }

    private LocalDate getCellDate(Cell cell) {
        if (cell == null) return null;
        try {
            if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
                return cell.getLocalDateTimeCellValue().toLocalDate();
            }
            String str = getCellString(cell);
            if (str != null && !str.isBlank()) {
                return LocalDate.parse(str.trim(), DATE_FORMAT);
            }
        } catch (Exception ignored) {}
        return null;
    }
}
