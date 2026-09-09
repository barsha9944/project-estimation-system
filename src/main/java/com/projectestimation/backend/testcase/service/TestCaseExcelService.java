package com.projectestimation.backend.testcase.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import com.projectestimation.backend.testcase.model.TestCase;
import com.projectestimation.backend.testcase.model.TestCaseScenario;
import com.projectestimation.backend.testcase.model.TestCaseStep;

@Service
public class TestCaseExcelService {

    public byte[] generateExcel(List<TestCase> testCases)
            throws IOException {

        try (
                XSSFWorkbook workbook = new XSSFWorkbook();
                ByteArrayOutputStream outputStream =
                        new ByteArrayOutputStream()
        ) {

            Sheet sheet =
                    workbook.createSheet("Test Cases");

            // ============================================================
            // STYLES
            // ============================================================

            CellStyle headerStyle =
                    createHeaderStyle(workbook);

            CellStyle bodyStyle =
                    createBodyStyle(workbook);

            CellStyle centerBodyStyle =
                    createCenterBodyStyle(workbook);


            // ============================================================
            // TITLE
            // ============================================================

            Row titleRow =
                    sheet.createRow(0);

            Cell titleCell =
                    titleRow.createCell(0);

            titleCell.setCellValue("Test Cases");

            CellStyle titleStyle =
                    workbook.createCellStyle();

            Font titleFont =
                    workbook.createFont();

            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 16);

            titleStyle.setFont(titleFont);

            titleStyle.setAlignment(
                    HorizontalAlignment.CENTER
            );

            titleStyle.setVerticalAlignment(
                    VerticalAlignment.CENTER
            );

            titleCell.setCellStyle(titleStyle);

            sheet.addMergedRegion(
                    new CellRangeAddress(
                            0,
                            0,
                            0,
                            17
                    )
            );

            titleRow.setHeightInPoints(25);


            // ============================================================
            // HEADER
            // ============================================================

            Row headerRow =
                    sheet.createRow(1);

            String[] headers = {
                    "Req ID",
                    "Test Case ID",
                    "Test Case Name",
                    "Test Case Description",
                    "Test Condition",
                    "Test Case Scenario",
                    "Test Data",
                    "Step #",
                    "Step Description",
                    "Expected Result",
                    "Actual Result",
                    "Test Status",
                    "PASS/FAIL",
                    "Defect ID",
                    "Severity",
                    "Defect Type",
                    "Root Cause",
                    "Phase Introduced"
            };

            for (int i = 0; i < headers.length; i++) {

                Cell cell =
                        headerRow.createCell(i);

                cell.setCellValue(headers[i]);

                cell.setCellStyle(headerStyle);
            }


            // ============================================================
            // DATA
            // ============================================================

            int rowNumber = 2;

            if (testCases != null) {

                for (TestCase testCase : testCases) {

                    if (testCase == null) {
                        continue;
                    }

                    List<TestCaseScenario> scenarios =
                            testCase.getTestCaseScenario();


                    // ====================================================
                    // NO SCENARIOS
                    // ====================================================

                    if (scenarios == null
                            || scenarios.isEmpty()) {

                        Row row =
                                sheet.createRow(rowNumber++);

                        writeTestCaseFields(
                                row,
                                testCase,
                                bodyStyle
                        );

                        // Scenario column
                        setCell(
                                row,
                                5,
                                "",
                                bodyStyle
                        );

                        writeEmptyStepFields(
                                row,
                                bodyStyle,
                                centerBodyStyle
                        );

                        continue;
                    }


                    // ====================================================
                    // SCENARIOS
                    // ====================================================

                    for (TestCaseScenario scenario :
                            scenarios) {

                        if (scenario == null) {
                            continue;
                        }

                        List<TestCaseStep> steps =
                                scenario.getSteps();


                        // =================================================
                        // SCENARIO WITHOUT STEPS
                        // =================================================

                        if (steps == null
                                || steps.isEmpty()) {

                            Row row =
                                    sheet.createRow(rowNumber++);

                            writeTestCaseFields(
                                    row,
                                    testCase,
                                    bodyStyle
                            );

                            // Scenario
                            setCell(
                                    row,
                                    5,
                                    buildScenarioText(
                                            scenario
                                    ),
                                    bodyStyle
                            );

                            writeEmptyStepFields(
                                    row,
                                    bodyStyle,
                                    centerBodyStyle
                            );

                            continue;
                        }


                        // =================================================
                        // SCENARIO WITH STEPS
                        // =================================================

                        for (TestCaseStep step : steps) {

                            if (step == null) {
                                continue;
                            }

                            Row row =
                                    sheet.createRow(rowNumber++);


                            // =============================================
                            // TEST CASE FIELDS
                            // =============================================

                            writeTestCaseFields(
                                    row,
                                    testCase,
                                    bodyStyle
                            );


                            // =============================================
                            // TEST CASE SCENARIO - COLUMN 5
                            // =============================================

                            setCell(
                                    row,
                                    5,
                                    buildScenarioText(
                                            scenario
                                    ),
                                    bodyStyle
                            );


                            // =============================================
                            // STEP NUMBER - COLUMN 7
                            // =============================================

                            if (step.getStepNumber() != null) {

                                setCell(
                                        row,
                                        7,
                                        String.valueOf(
                                                step.getStepNumber()
                                        ),
                                        centerBodyStyle
                                );

                            } else {

                                setCell(
                                        row,
                                        7,
                                        "",
                                        centerBodyStyle
                                );
                            }


                            // =============================================
                            // STEP DESCRIPTION - COLUMN 8
                            // =============================================

                            setCell(
                                    row,
                                    8,
                                    step.getStepDescription(),
                                    bodyStyle
                            );


                            // =============================================
                            // EXPECTED RESULT - COLUMN 9
                            // =============================================

                            setCell(
                                    row,
                                    9,
                                    step.getExpectedResult(),
                                    bodyStyle
                            );


                            // =============================================
                            // ACTUAL RESULT - COLUMN 10
                            // =============================================

                            setCell(
                                    row,
                                    10,
                                    step.getActualResult(),
                                    bodyStyle
                            );


                            // =============================================
                            // TEST STATUS - COLUMN 11
                            // =============================================

                            setCell(
                                    row,
                                    11,
                                    step.getTestStatus(),
                                    centerBodyStyle
                            );


                            // =============================================
                            // PASS / FAIL - COLUMN 12
                            // =============================================

                            setCell(
                                    row,
                                    12,
                                    step.getPassFail(),
                                    centerBodyStyle
                            );


                            // =============================================
                            // DEFECT ID - COLUMN 13
                            // =============================================

                            setCell(
                                    row,
                                    13,
                                    step.getDefectId(),
                                    centerBodyStyle
                            );


                            // =============================================
                            // SEVERITY - COLUMN 14
                            // =============================================

                            setCell(
                                    row,
                                    14,
                                    step.getSeverity(),
                                    centerBodyStyle
                            );


                            // =============================================
                            // DEFECT TYPE - COLUMN 15
                            // =============================================

                            setCell(
                                    row,
                                    15,
                                    step.getDefectType(),
                                    centerBodyStyle
                            );


                            // =============================================
                            // ROOT CAUSE - COLUMN 16
                            // =============================================

                            setCell(
                                    row,
                                    16,
                                    step.getRootCause(),
                                    bodyStyle
                            );


                            // =============================================
                            // PHASE INTRODUCED - COLUMN 17
                            // =============================================

                            setCell(
                                    row,
                                    17,
                                    step.getPhaseIntroduced(),
                                    centerBodyStyle
                            );
                        }
                    }
                }
            }


            // ============================================================
            // AUTO FILTER
            // ============================================================

            if (rowNumber > 2) {

                sheet.setAutoFilter(
                        new CellRangeAddress(
                                1,
                                rowNumber - 1,
                                0,
                                17
                        )
                );
            }


            // ============================================================
            // FREEZE HEADER
            // ============================================================

            sheet.createFreezePane(
                    0,
                    2
            );


            // ============================================================
            // COLUMN WIDTHS
            // ============================================================

            sheet.setColumnWidth(
                    0,
                    15 * 256
            );

            sheet.setColumnWidth(
                    1,
                    18 * 256
            );

            sheet.setColumnWidth(
                    2,
                    32 * 256
            );

            sheet.setColumnWidth(
                    3,
                    45 * 256
            );

            sheet.setColumnWidth(
                    4,
                    28 * 256
            );

            sheet.setColumnWidth(
                    5,
                    45 * 256
            );

            sheet.setColumnWidth(
                    6,
                    35 * 256
            );

            sheet.setColumnWidth(
                    7,
                    10 * 256
            );

            sheet.setColumnWidth(
                    8,
                    45 * 256
            );

            sheet.setColumnWidth(
                    9,
                    45 * 256
            );

            sheet.setColumnWidth(
                    10,
                    40 * 256
            );

            sheet.setColumnWidth(
                    11,
                    18 * 256
            );

            sheet.setColumnWidth(
                    12,
                    15 * 256
            );

            sheet.setColumnWidth(
                    13,
                    18 * 256
            );

            sheet.setColumnWidth(
                    14,
                    15 * 256
            );

            sheet.setColumnWidth(
                    15,
                    20 * 256
            );

            sheet.setColumnWidth(
                    16,
                    40 * 256
            );

            sheet.setColumnWidth(
                    17,
                    22 * 256
            );


            // ============================================================
            // ROW HEIGHT + WRAPPING
            // ============================================================

            for (int i = 2; i < rowNumber; i++) {

                Row row =
                        sheet.getRow(i);

                if (row == null) {
                    continue;
                }

                row.setHeightInPoints(45);

                for (int j = 0; j < 18; j++) {

                    Cell cell =
                            row.getCell(j);

                    if (cell == null) {
                        continue;
                    }

                    CellStyle existingStyle =
                            cell.getCellStyle();

                    if (existingStyle != null) {
                        existingStyle.setWrapText(true);
                    }
                }
            }


            // ============================================================
            // WRITE EXCEL
            // ============================================================

            workbook.write(outputStream);

            return outputStream.toByteArray();
        }
    }


    // ====================================================================
    // TEST CASE FIELDS
    // ====================================================================

    private void writeTestCaseFields(
            Row row,
            TestCase testCase,
            CellStyle bodyStyle
    ) {

        // Column 0 - Req ID
        setCell(
                row,
                0,
                testCase.getReqId(),
                bodyStyle
        );


        // Column 1 - Test Case ID
        setCell(
                row,
                1,
                testCase.getTestCaseId(),
                bodyStyle
        );


        // Column 2 - Test Case Name
        setCell(
                row,
                2,
                testCase.getTestCaseName(),
                bodyStyle
        );


        // Column 3 - Test Case Description
        setCell(
                row,
                3,
                testCase.getTestCaseDescription(),
                bodyStyle
        );


        // Column 4 - Test Condition
        setCell(
                row,
                4,
                testCase.getTestCondition(),
                bodyStyle
        );


        // Column 5 = Test Case Scenario
        // Populated separately.


        // Column 6 - Test Data
        setCell(
                row,
                6,
                testCase.getTestData(),
                bodyStyle
        );
    }


    // ====================================================================
    // EMPTY STEP FIELDS
    // ====================================================================

    private void writeEmptyStepFields(
            Row row,
            CellStyle bodyStyle,
            CellStyle centerBodyStyle
    ) {

        // Step #
        setCell(
                row,
                7,
                "",
                centerBodyStyle
        );


        // Step Description
        setCell(
                row,
                8,
                "",
                bodyStyle
        );


        // Expected Result
        setCell(
                row,
                9,
                "",
                bodyStyle
        );


        // Actual Result
        setCell(
                row,
                10,
                "",
                bodyStyle
        );


        // Test Status
        setCell(
                row,
                11,
                "",
                centerBodyStyle
        );


        // PASS/FAIL
        setCell(
                row,
                12,
                "",
                centerBodyStyle
        );


        // Defect ID
        setCell(
                row,
                13,
                "",
                centerBodyStyle
        );


        // Severity
        setCell(
                row,
                14,
                "",
                centerBodyStyle
        );


        // Defect Type
        setCell(
                row,
                15,
                "",
                centerBodyStyle
        );


        // Root Cause
        setCell(
                row,
                16,
                "",
                bodyStyle
        );


        // Phase Introduced
        setCell(
                row,
                17,
                "",
                centerBodyStyle
        );
    }


    // ====================================================================
    // SCENARIO TEXT
    // ====================================================================

    private String buildScenarioText(
            TestCaseScenario scenario
    ) {

        if (scenario == null) {
            return "";
        }

        StringBuilder value =
                new StringBuilder();


        // Scenario ID
        if (scenario.getScenarioId() != null
                && !scenario.getScenarioId().isBlank()) {

            value.append(
                    scenario.getScenarioId()
            );
        }


        // Scenario Name
        if (scenario.getScenarioName() != null
                && !scenario.getScenarioName().isBlank()) {

            if (value.length() > 0) {
                value.append(" - ");
            }

            value.append(
                    scenario.getScenarioName()
            );
        }


        // Scenario Type
        if (scenario.getScenarioType() != null
                && !scenario.getScenarioType().isBlank()) {

            if (value.length() > 0) {
                value.append(" - ");
            }

            value.append(
                    scenario.getScenarioType()
            );
        }


        return value.toString();
    }


    // ====================================================================
    // SET CELL
    // ====================================================================

    private void setCell(
            Row row,
            int column,
            String value,
            CellStyle style
    ) {

        Cell cell =
                row.createCell(column);

        cell.setCellValue(
                value != null
                        ? value
                        : ""
        );

        cell.setCellStyle(style);
    }


    // ====================================================================
    // HEADER STYLE
    // ====================================================================

    private CellStyle createHeaderStyle(
            XSSFWorkbook workbook
    ) {

        CellStyle style =
                workbook.createCellStyle();

        Font font =
                workbook.createFont();

        font.setBold(true);

        // White text on blue header
        font.setColor(
                IndexedColors.WHITE.getIndex()
        );

        style.setFont(font);

        style.setAlignment(
                HorizontalAlignment.CENTER
        );

        style.setVerticalAlignment(
                VerticalAlignment.CENTER
        );

        style.setWrapText(true);


        // ================================================================
        // BLUE HEADER
        // ================================================================

        style.setFillForegroundColor(
                IndexedColors.DARK_BLUE.getIndex()
        );

        style.setFillPattern(
                FillPatternType.SOLID_FOREGROUND
        );


        // ================================================================
        // BORDERS
        // ================================================================

        style.setBorderTop(
                BorderStyle.THIN
        );

        style.setBorderBottom(
                BorderStyle.THIN
        );

        style.setBorderLeft(
                BorderStyle.THIN
        );

        style.setBorderRight(
                BorderStyle.THIN
        );


        return style;
    }


    // ====================================================================
    // BODY STYLE
    // ====================================================================

    private CellStyle createBodyStyle(
            XSSFWorkbook workbook
    ) {

        CellStyle style =
                workbook.createCellStyle();

        style.setAlignment(
                HorizontalAlignment.LEFT
        );

        style.setVerticalAlignment(
                VerticalAlignment.TOP
        );

        style.setWrapText(true);


        style.setBorderTop(
                BorderStyle.THIN
        );

        style.setBorderBottom(
                BorderStyle.THIN
        );

        style.setBorderLeft(
                BorderStyle.THIN
        );

        style.setBorderRight(
                BorderStyle.THIN
        );


        return style;
    }


    // ====================================================================
    // CENTER BODY STYLE
    // ====================================================================

    private CellStyle createCenterBodyStyle(
            XSSFWorkbook workbook
    ) {

        CellStyle style =
                workbook.createCellStyle();

        style.setAlignment(
                HorizontalAlignment.CENTER
        );

        style.setVerticalAlignment(
                VerticalAlignment.TOP
        );

        style.setWrapText(true);


        style.setBorderTop(
                BorderStyle.THIN
        );

        style.setBorderBottom(
                BorderStyle.THIN
        );

        style.setBorderLeft(
                BorderStyle.THIN
        );

        style.setBorderRight(
                BorderStyle.THIN
        );


        return style;
    }
}