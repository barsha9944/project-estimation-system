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
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import com.projectestimation.backend.testcase.model.TestCase;
import com.projectestimation.backend.testcase.model.TestCaseStep;

@Service
public class TestCaseExcelService {

    public byte[] generateExcel(List<TestCase> testCases) throws IOException {

        try (
                Workbook workbook = new XSSFWorkbook();
                ByteArrayOutputStream outputStream =
                        new ByteArrayOutputStream()
        ) {

            Sheet sheet = workbook.createSheet("Test Cases");

            // ============================================================
            // STYLES
            // ============================================================

            CellStyle titleStyle = createTitleStyle(workbook);
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle bodyStyle = createBodyStyle(workbook);
            CellStyle centerBodyStyle = createCenterBodyStyle(workbook);

            // ============================================================
            // TITLE
            // ============================================================

            Row titleRow = sheet.createRow(0);
            titleRow.setHeightInPoints(28);

            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("TEST CASES");
            titleCell.setCellStyle(titleStyle);

            // 18 columns => 0 to 17
            sheet.addMergedRegion(
                    new CellRangeAddress(
                            0,
                            0,
                            0,
                            17
                    )
            );

            // ============================================================
            // HEADER
            // ============================================================

            Row headerRow = sheet.createRow(1);
            headerRow.setHeightInPoints(32);

            String[] headers = {

                    "Req ID",

                    "Test Case ID",

                    "Test Case Condition",

                    "Test Case Scenario",

                    "Test Case Name",

                    "Test Case Description",

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

                Cell cell = headerRow.createCell(i);

                cell.setCellValue(headers[i]);

                cell.setCellStyle(headerStyle);
            }

            // ============================================================
            // DATA
            // ============================================================

            int rowNumber = 2;

            for (TestCase testCase : testCases) {

                List<TestCaseStep> steps = testCase.getSteps();

                // --------------------------------------------------------
                // TEST CASE WITHOUT STEPS
                // --------------------------------------------------------

                if (steps == null || steps.isEmpty()) {

                    Row row = sheet.createRow(rowNumber++);

                    writeTestCaseFields(
                            row,
                            testCase,
                            bodyStyle
                    );

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

                    continue;
                }

                // --------------------------------------------------------
                // TEST CASE WITH STEPS
                // --------------------------------------------------------

                for (TestCaseStep step : steps) {

                    Row row = sheet.createRow(rowNumber++);

                    // Test Case level fields
                    writeTestCaseFields(
                            row,
                            testCase,
                            bodyStyle
                    );

                    // ====================================================
                    // STEP LEVEL FIELDS
                    // ====================================================

                    // Step #
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

                    // Step Description
                    setCell(
                            row,
                            8,
                            step.getStepDescription(),
                            bodyStyle
                    );

                    // Expected Result
                    setCell(
                            row,
                            9,
                            step.getExpectedResult(),
                            bodyStyle
                    );

                    // Actual Result
                    setCell(
                            row,
                            10,
                            step.getActualResult(),
                            bodyStyle
                    );

                    // Test Status
                    setCell(
                            row,
                            11,
                            step.getTestStatus(),
                            centerBodyStyle
                    );

                    // PASS/FAIL
                    setCell(
                            row,
                            12,
                            step.getPassFail(),
                            centerBodyStyle
                    );

                    // Defect ID
                    setCell(
                            row,
                            13,
                            step.getDefectId(),
                            centerBodyStyle
                    );

                    // Severity
                    setCell(
                            row,
                            14,
                            step.getSeverity(),
                            centerBodyStyle
                    );

                    // Defect Type
                    setCell(
                            row,
                            15,
                            step.getDefectType(),
                            centerBodyStyle
                    );

                    // Root Cause
                    setCell(
                            row,
                            16,
                            step.getRootCause(),
                            bodyStyle
                    );

                    // Phase Introduced
                    setCell(
                            row,
                            17,
                            step.getPhaseIntroduced(),
                            centerBodyStyle
                    );
                }
            }

            // ============================================================
            // FILTER
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

            setColumnWidths(sheet);

            // ============================================================
            // ROW HEIGHTS / WRAPPING
            // ============================================================

            for (int i = 2; i < rowNumber; i++) {

                Row row = sheet.getRow(i);

                if (row != null) {

                    row.setHeightInPoints(45);

                    for (int j = 0; j < 18; j++) {

                        Cell cell = row.getCell(j);

                        if (cell != null) {

                            cell.getCellStyle()
                                    .setWrapText(true);
                        }
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

    // ================================================================
    // TEST CASE LEVEL FIELDS
    // ================================================================

    private void writeTestCaseFields(
            Row row,
            TestCase testCase,
            CellStyle bodyStyle
    ) {

        // ------------------------------------------------------------
        // 0 - Req ID
        // ------------------------------------------------------------

        setCell(
                row,
                0,
                testCase.getReqId(),
                bodyStyle
        );

        // ------------------------------------------------------------
        // 1 - Test Case ID
        // ------------------------------------------------------------

        setCell(
                row,
                1,
                testCase.getTestCaseId(),
                bodyStyle
        );

        // ------------------------------------------------------------
        // 2 - Test Case Condition
        // ------------------------------------------------------------

        setCell(
                row,
                2,
                testCase.getTestCondition(),
                bodyStyle
        );

        // ------------------------------------------------------------
        // 3 - Test Case Scenario
        // ------------------------------------------------------------

//        setCell(
//                row,
//                3,
//                testCase.getTestCaseScenario(),
//                bodyStyle
//        );

        // ------------------------------------------------------------
        // 4 - Test Case Name
        // ------------------------------------------------------------

        setCell(
                row,
                4,
                testCase.getTestCaseName(),
                bodyStyle
        );

        // ------------------------------------------------------------
        // 5 - Test Case Description
        // ------------------------------------------------------------

        setCell(
                row,
                5,
                testCase.getTestCaseDescription(),
                bodyStyle
        );

        // ------------------------------------------------------------
        // 6 - Test Data
        // ------------------------------------------------------------

        setCell(
                row,
                6,
                testCase.getTestData(),
                bodyStyle
        );
    }

    // ================================================================
    // SET CELL
    // ================================================================

    private void setCell(
            Row row,
            int column,
            String value,
            CellStyle style
    ) {

        Cell cell = row.createCell(column);

        cell.setCellValue(
                value == null ? "" : value
        );

        cell.setCellStyle(style);
    }

    // ================================================================
    // TITLE STYLE
    // ================================================================

    private CellStyle createTitleStyle(
            Workbook workbook
    ) {

        CellStyle style =
                workbook.createCellStyle();

        Font font =
                workbook.createFont();

        font.setBold(true);
        font.setFontHeightInPoints((short) 16);
        font.setColor(
                IndexedColors.WHITE.getIndex()
        );

        style.setFont(font);

        style.setFillForegroundColor(
                IndexedColors.DARK_BLUE.getIndex()
        );

        style.setFillPattern(
                FillPatternType.SOLID_FOREGROUND
        );

        style.setAlignment(
                HorizontalAlignment.CENTER
        );

        style.setVerticalAlignment(
                VerticalAlignment.CENTER
        );

        return style;
    }

    // ================================================================
    // HEADER STYLE
    // ================================================================

    private CellStyle createHeaderStyle(
            Workbook workbook
    ) {

        CellStyle style =
                workbook.createCellStyle();

        Font font =
                workbook.createFont();

        font.setBold(true);
        font.setFontHeightInPoints((short) 11);
        font.setColor(
                IndexedColors.WHITE.getIndex()
        );

        style.setFont(font);

        style.setFillForegroundColor(
                IndexedColors.BLUE.getIndex()
        );

        style.setFillPattern(
                FillPatternType.SOLID_FOREGROUND
        );

        style.setAlignment(
                HorizontalAlignment.CENTER
        );

        style.setVerticalAlignment(
                VerticalAlignment.CENTER
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

    // ================================================================
    // BODY STYLE
    // ================================================================

    private CellStyle createBodyStyle(
            Workbook workbook
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

    // ================================================================
    // CENTER BODY STYLE
    // ================================================================

    private CellStyle createCenterBodyStyle(
            Workbook workbook
    ) {

        CellStyle style =
                createBodyStyle(workbook);

        style.setAlignment(
                HorizontalAlignment.CENTER
        );

        style.setVerticalAlignment(
                VerticalAlignment.CENTER
        );

        return style;
    }

    // ================================================================
    // COLUMN WIDTHS
    // ================================================================

    private void setColumnWidths(
            Sheet sheet
    ) {

        // 0 - Req ID
        sheet.setColumnWidth(
                0,
                15 * 256
        );

        // 1 - Test Case ID
        sheet.setColumnWidth(
                1,
                18 * 256
        );

        // 2 - Test Case Condition
        sheet.setColumnWidth(
                2,
                32 * 256
        );

        // 3 - Test Case Scenario
        sheet.setColumnWidth(
                3,
                40 * 256
        );

        // 4 - Test Case Name
        sheet.setColumnWidth(
                4,
                35 * 256
        );

        // 5 - Test Case Description
        sheet.setColumnWidth(
                5,
                45 * 256
        );

        // 6 - Test Data
        sheet.setColumnWidth(
                6,
                32 * 256
        );

        // 7 - Step #
        sheet.setColumnWidth(
                7,
                10 * 256
        );

        // 8 - Step Description
        sheet.setColumnWidth(
                8,
                45 * 256
        );

        // 9 - Expected Result
        sheet.setColumnWidth(
                9,
                45 * 256
        );

        // 10 - Actual Result
        sheet.setColumnWidth(
                10,
                45 * 256
        );

        // 11 - Test Status
        sheet.setColumnWidth(
                11,
                18 * 256
        );

        // 12 - PASS/FAIL
        sheet.setColumnWidth(
                12,
                14 * 256
        );

        // 13 - Defect ID
        sheet.setColumnWidth(
                13,
                16 * 256
        );

        // 14 - Severity
        sheet.setColumnWidth(
                14,
                14 * 256
        );

        // 15 - Defect Type
        sheet.setColumnWidth(
                15,
                20 * 256
        );

        // 16 - Root Cause
        sheet.setColumnWidth(
                16,
                35 * 256
        );

        // 17 - Phase Introduced
        sheet.setColumnWidth(
                17,
                20 * 256
        );
    }
}