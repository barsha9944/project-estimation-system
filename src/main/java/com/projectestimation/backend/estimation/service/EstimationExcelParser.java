package com.projectestimation.backend.estimation.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * High-fidelity Excel parser for the physical Estimation workbook.
 * Reads Actor, Use Cases, Technical Factors, Environmental Factors, and Final Calculation sheets.
 */
@Component
public class EstimationExcelParser {

    private static final Logger log = LoggerFactory.getLogger(EstimationExcelParser.class);

    public record ParsedActor(String name, String type) {}
    public record ParsedUseCase(String name, String complexity) {}
    public record ParsedFactor(String name, double magnitude) {}

    public static class ParsedEstimationWorkbook {
        private final List<ParsedActor> actors = new ArrayList<>();
        private final List<ParsedUseCase> useCases = new ArrayList<>();
        private final List<ParsedFactor> technicalFactors = new ArrayList<>();
        private final List<ParsedFactor> environmentalFactors = new ArrayList<>();
        private final Map<String, String> finalCalculations = new HashMap<>();

        public List<ParsedActor> getActors() { return actors; }
        public List<ParsedUseCase> getUseCases() { return useCases; }
        public List<ParsedFactor> getTechnicalFactors() { return technicalFactors; }
        public List<ParsedFactor> getEnvironmentalFactors() { return environmentalFactors; }
        public Map<String, String> getFinalCalculations() { return finalCalculations; }
    }

    public ParsedEstimationWorkbook parse(byte[] excelBytes) throws IOException {
        ParsedEstimationWorkbook result = new ParsedEstimationWorkbook();
        if (excelBytes == null || excelBytes.length == 0) {
            return result;
        }

        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(excelBytes))) {
            parseActorSheet(workbook.getSheet("Actor"), result);
            parseUseCaseSheet(workbook.getSheet("Use Cases"), result);
            parseFactorSheet(workbook.getSheet("Technical Factors"), result.getTechnicalFactors());
            parseFactorSheet(workbook.getSheet("Environmental Factors"), result.getEnvironmentalFactors());
            parseFinalCalculationSheet(workbook.getSheet("Final Calculation"), result.getFinalCalculations());
        }

        return result;
    }

    private void parseActorSheet(Sheet sheet, ParsedEstimationWorkbook result) {
        if (sheet == null) return;
        for (int r = 3; r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;
            String name = getCellString(row.getCell(0));
            if (name == null || name.isBlank() || name.equalsIgnoreCase("Actor Summary") || name.equalsIgnoreCase("Calculated AW")) {
                if (name != null && (name.equalsIgnoreCase("Actor Summary") || name.equalsIgnoreCase("Calculated AW"))) {
                    break;
                }
                continue;
            }
            String type = getCellString(row.getCell(1));
            if (type == null || type.isBlank()) {
                type = "SIMPLE";
            }
            result.getActors().add(new ParsedActor(name.trim(), type.trim().toUpperCase()));
        }
    }

    private void parseUseCaseSheet(Sheet sheet, ParsedEstimationWorkbook result) {
        if (sheet == null) return;
        for (int r = 3; r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;
            String name = getCellString(row.getCell(0));
            if (name == null || name.isBlank() || name.equalsIgnoreCase("Use Case Summary") || name.equalsIgnoreCase("Calculated UUCW") || name.equalsIgnoreCase("Calculated UUCP")) {
                if (name != null && (name.equalsIgnoreCase("Use Case Summary") || name.equalsIgnoreCase("Calculated UUCW") || name.equalsIgnoreCase("Calculated UUCP"))) {
                    break;
                }
                continue;
            }
            String complexity = getCellString(row.getCell(1));
            if (complexity == null || complexity.isBlank()) {
                complexity = "SIMPLE";
            }
            result.getUseCases().add(new ParsedUseCase(name.trim(), complexity.trim().toUpperCase()));
        }
    }

    private void parseFactorSheet(Sheet sheet, List<ParsedFactor> targetList) {
        if (sheet == null) return;
        for (int r = 3; r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;
            String factorName = getCellString(row.getCell(0));
            if (factorName == null || factorName.isBlank() || factorName.startsWith("Calculated")) {
                continue;
            }
            double magnitude = getCellDouble(row.getCell(2), 0.0);
            targetList.add(new ParsedFactor(factorName.trim(), magnitude));
        }
    }

    private void parseFinalCalculationSheet(Sheet sheet, Map<String, String> targetMap) {
        if (sheet == null) return;
        for (int r = 2; r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;
            String key = getCellString(row.getCell(0));
            String val = getCellString(row.getCell(1));
            if (key != null && !key.isBlank() && val != null) {
                targetMap.put(key.trim().toLowerCase(), val.trim());
            }
        }
    }

    private String getCellString(Cell cell) {
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> {
                double val = cell.getNumericCellValue();
                if (val == Math.floor(val) && !Double.isInfinite(val)) {
                    yield String.valueOf((long) val);
                }
                yield String.valueOf(val);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> {
                try {
                    yield cell.getStringCellValue();
                } catch (Exception e) {
                    yield String.valueOf(cell.getNumericCellValue());
                }
            }
            default -> null;
        };
    }

    private double getCellDouble(Cell cell, double defaultValue) {
        if (cell == null) return defaultValue;
        try {
            if (cell.getCellType() == CellType.NUMERIC) {
                return cell.getNumericCellValue();
            } else if (cell.getCellType() == CellType.STRING) {
                return Double.parseDouble(cell.getStringCellValue().trim());
            } else if (cell.getCellType() == CellType.FORMULA) {
                return cell.getNumericCellValue();
            }
        } catch (Exception e) {
            log.debug("Could not parse cell value as double: {}", e.getMessage());
        }
        return defaultValue;
    }
}
