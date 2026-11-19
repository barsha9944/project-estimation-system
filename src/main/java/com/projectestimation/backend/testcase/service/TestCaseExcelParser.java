package com.projectestimation.backend.testcase.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.projectestimation.backend.testcase.dto.TestCaseDto;
import com.projectestimation.backend.testcase.dto.TestCaseGenerationResponse;
import com.projectestimation.backend.testcase.dto.TestScenarioDto;
import com.projectestimation.backend.testcase.dto.TestStepDto;

@Component
public class TestCaseExcelParser {

    private static final Logger log = LoggerFactory.getLogger(TestCaseExcelParser.class);
    private final DataFormatter dataFormatter = new DataFormatter();

    public TestCaseGenerationResponse parse(Path filePath) throws IOException {
        try (InputStream is = Files.newInputStream(filePath)) {
            return parse(is);
        }
    }

    public TestCaseGenerationResponse parse(byte[] bytes) throws IOException {
        try (InputStream is = new ByteArrayInputStream(bytes)) {
            return parse(is);
        }
    }

    public TestCaseGenerationResponse parse(InputStream is) throws IOException {
        try (Workbook workbook = WorkbookFactory.create(is)) {
            Sheet sheet = findTestCasesSheet(workbook);
            if (sheet == null) {
                log.warn("No sheet found in test cases workbook");
                return new TestCaseGenerationResponse(List.of());
            }

            int headerRowIndex = findHeaderRowIndex(sheet);
            Map<String, Integer> colMap = buildColumnMap(sheet.getRow(headerRowIndex));

            Map<String, TestCaseHolder> testCasesMap = new LinkedHashMap<>();
            TestCaseHolder currentTestCase = null;
            ScenarioHolder currentScenario = null;

            int lastRow = sheet.getLastRowNum();
            for (int r = headerRowIndex + 1; r <= lastRow; r++) {
                Row row = sheet.getRow(r);
                if (row == null || isRowEmpty(row)) {
                    continue;
                }

                String reqId = getCellValue(row, colMap.getOrDefault("reqId", 0));
                String tcId = getCellValue(row, colMap.getOrDefault("testCaseId", 1));
                String tcName = getCellValue(row, colMap.getOrDefault("testCaseName", 2));
                String tcDesc = getCellValue(row, colMap.getOrDefault("testCaseDescription", 3));
                String testData = getCellValue(row, colMap.getOrDefault("testData", 4));

                String scId = getCellValue(row, colMap.getOrDefault("scenarioId", 5));
                String scName = getCellValue(row, colMap.getOrDefault("scenarioName", 6));
                String scType = getCellValue(row, colMap.getOrDefault("scenarioType", 7));
                String testCond = getCellValue(row, colMap.getOrDefault("testCondition", 8));

                String stepNumStr = getCellValue(row, colMap.getOrDefault("stepNumber", 9));
                String stepDesc = getCellValue(row, colMap.getOrDefault("stepDescription", 10));
                String expRes = getCellValue(row, colMap.getOrDefault("expectedResult", 11));
                String actRes = getCellValue(row, colMap.getOrDefault("actualResult", 12));
                String testStatus = getCellValue(row, colMap.getOrDefault("testStatus", 13));
                String passFail = getCellValue(row, colMap.getOrDefault("passFail", 14));
                String defectId = getCellValue(row, colMap.getOrDefault("defectId", 15));
                String severity = getCellValue(row, colMap.getOrDefault("severity", 16));
                String defectType = getCellValue(row, colMap.getOrDefault("defectType", 17));
                String rootCause = getCellValue(row, colMap.getOrDefault("rootCause", 18));
                String phaseIntro = getCellValue(row, colMap.getOrDefault("phaseIntroduced", 19));

                if (!tcId.isBlank()) {
                    currentTestCase = testCasesMap.computeIfAbsent(tcId, id -> {
                        TestCaseHolder holder = new TestCaseHolder();
                        holder.testCaseId = id;
                        holder.reqId = reqId;
                        holder.testCaseName = tcName;
                        holder.testCaseDescription = tcDesc;
                        holder.testData = testData;
                        return holder;
                    });
                    if (!reqId.isBlank()) currentTestCase.reqId = reqId;
                    if (!tcName.isBlank()) currentTestCase.testCaseName = tcName;
                    if (!tcDesc.isBlank()) currentTestCase.testCaseDescription = tcDesc;
                    if (!testData.isBlank()) currentTestCase.testData = testData;
                    currentScenario = null;
                }

                if (currentTestCase == null) {
                    continue;
                }

                if (!scId.isBlank()) {
                    currentScenario = currentTestCase.scenariosMap.computeIfAbsent(scId, id -> {
                        ScenarioHolder sHolder = new ScenarioHolder();
                        sHolder.scenarioId = id;
                        sHolder.scenarioName = scName;
                        sHolder.scenarioType = scType;
                        sHolder.testCondition = testCond;
                        return sHolder;
                    });
                    if (!scName.isBlank()) currentScenario.scenarioName = scName;
                    if (!scType.isBlank()) currentScenario.scenarioType = scType;
                    if (!testCond.isBlank()) currentScenario.testCondition = testCond;
                }

                boolean hasStepInfo = !stepNumStr.isBlank() || !stepDesc.isBlank() || !expRes.isBlank()
                        || !actRes.isBlank() || !testStatus.isBlank() || !passFail.isBlank() || !defectId.isBlank();

                if (hasStepInfo) {
                    if (currentScenario == null) {
                        String defId = "SC-001";
                        currentScenario = currentTestCase.scenariosMap.computeIfAbsent(defId, id -> {
                            ScenarioHolder sHolder = new ScenarioHolder();
                            sHolder.scenarioId = id;
                            sHolder.scenarioName = "Scenario 1";
                            return sHolder;
                        });
                    }

                    Integer stepNum = null;
                    try {
                        if (!stepNumStr.isBlank()) {
                            stepNum = (int) Double.parseDouble(stepNumStr);
                        }
                    } catch (NumberFormatException ignored) {
                    }
                    if (stepNum == null) {
                        stepNum = currentScenario.steps.size() + 1;
                    }

                    TestStepDto stepDto = new TestStepDto(
                            stepNum,
                            stepDesc,
                            expRes,
                            actRes,
                            testStatus,
                            passFail,
                            defectId,
                            severity,
                            defectType,
                            rootCause,
                            phaseIntro
                    );
                    currentScenario.steps.add(stepDto);
                }
            }

            List<TestCaseDto> dtos = new ArrayList<>();
            for (TestCaseHolder tcHolder : testCasesMap.values()) {
                List<TestScenarioDto> scenarioDtos = new ArrayList<>();
                for (ScenarioHolder scHolder : tcHolder.scenariosMap.values()) {
                    scenarioDtos.add(new TestScenarioDto(
                            scHolder.scenarioId,
                            scHolder.scenarioName,
                            scHolder.scenarioType,
                            scHolder.testCondition,
                            scHolder.steps
                    ));
                }
                dtos.add(new TestCaseDto(
                        tcHolder.reqId,
                        tcHolder.testCaseId,
                        tcHolder.testCaseName,
                        scenarioDtos,
                        tcHolder.testCaseDescription,
                        tcHolder.testData
                ));
            }

            return new TestCaseGenerationResponse(dtos);
        }
    }

    private Sheet findTestCasesSheet(Workbook workbook) {
        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            String sheetName = workbook.getSheetName(i);
            if (sheetName != null && sheetName.trim().equalsIgnoreCase("Test Cases")) {
                return workbook.getSheetAt(i);
            }
        }
        return workbook.getNumberOfSheets() > 0 ? workbook.getSheetAt(0) : null;
    }

    private int findHeaderRowIndex(Sheet sheet) {
        for (int r = 0; r <= Math.min(10, sheet.getLastRowNum()); r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;
            for (Cell cell : row) {
                String val = dataFormatter.formatCellValue(cell).trim().toLowerCase();
                if (val.contains("test case id") || val.contains("req id")) {
                    return r;
                }
            }
        }
        return 1;
    }

    private Map<String, Integer> buildColumnMap(Row headerRow) {
        Map<String, Integer> colMap = new LinkedHashMap<>();
        // Defaults
        colMap.put("reqId", 0);
        colMap.put("testCaseId", 1);
        colMap.put("testCaseName", 2);
        colMap.put("testCaseDescription", 3);
        colMap.put("testData", 4);
        colMap.put("scenarioId", 5);
        colMap.put("scenarioName", 6);
        colMap.put("scenarioType", 7);
        colMap.put("testCondition", 8);
        colMap.put("stepNumber", 9);
        colMap.put("stepDescription", 10);
        colMap.put("expectedResult", 11);
        colMap.put("actualResult", 12);
        colMap.put("testStatus", 13);
        colMap.put("passFail", 14);
        colMap.put("defectId", 15);
        colMap.put("severity", 16);
        colMap.put("defectType", 17);
        colMap.put("rootCause", 18);
        colMap.put("phaseIntroduced", 19);

        if (headerRow != null) {
            for (int c = 0; c < headerRow.getLastCellNum(); c++) {
                Cell cell = headerRow.getCell(c);
                if (cell == null) continue;
                String header = dataFormatter.formatCellValue(cell).trim().toLowerCase();
                if (header.contains("req id") || header.contains("requirement id")) colMap.put("reqId", c);
                else if (header.contains("test case id")) colMap.put("testCaseId", c);
                else if (header.contains("test case name")) colMap.put("testCaseName", c);
                else if (header.contains("test case desc") || header.contains("test case description")) colMap.put("testCaseDescription", c);
                else if (header.contains("test data")) colMap.put("testData", c);
                else if (header.contains("scenario id")) colMap.put("scenarioId", c);
                else if (header.contains("scenario name")) colMap.put("scenarioName", c);
                else if (header.contains("scenario type")) colMap.put("scenarioType", c);
                else if (header.contains("test condition")) colMap.put("testCondition", c);
                else if (header.contains("step #") || header.contains("step no") || header.contains("step number")) colMap.put("stepNumber", c);
                else if (header.contains("step desc") || header.contains("step description")) colMap.put("stepDescription", c);
                else if (header.contains("expected result")) colMap.put("expectedResult", c);
                else if (header.contains("actual result")) colMap.put("actualResult", c);
                else if (header.contains("test status")) colMap.put("testStatus", c);
                else if (header.contains("pass/fail") || header.contains("pass / fail") || header.contains("status")) colMap.put("passFail", c);
                else if (header.contains("defect id")) colMap.put("defectId", c);
                else if (header.contains("severity")) colMap.put("severity", c);
                else if (header.contains("defect type")) colMap.put("defectType", c);
                else if (header.contains("root cause")) colMap.put("rootCause", c);
                else if (header.contains("phase introduced")) colMap.put("phaseIntro", c);
            }
        }
        return colMap;
    }

    private String getCellValue(Row row, int colIndex) {
        if (colIndex < 0) return "";
        Cell cell = row.getCell(colIndex);
        if (cell == null) return "";
        return dataFormatter.formatCellValue(cell).trim();
    }

    private boolean isRowEmpty(Row row) {
        for (int c = 0; c < row.getLastCellNum(); c++) {
            Cell cell = row.getCell(c);
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                String val = dataFormatter.formatCellValue(cell).trim();
                if (!val.isEmpty()) return false;
            }
        }
        return true;
    }

    private static class TestCaseHolder {
        String reqId = "";
        String testCaseId = "";
        String testCaseName = "";
        String testCaseDescription = "";
        String testData = "";
        Map<String, ScenarioHolder> scenariosMap = new LinkedHashMap<>();
    }

    private static class ScenarioHolder {
        String scenarioId = "";
        String scenarioName = "";
        String scenarioType = "";
        String testCondition = "";
        List<TestStepDto> steps = new ArrayList<>();
    }
}
