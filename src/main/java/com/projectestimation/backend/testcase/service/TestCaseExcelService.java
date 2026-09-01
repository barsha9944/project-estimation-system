package com.projectestimation.backend.testcase.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            Map<String, List<TestCase>> testCasesByPhase =
                    testCases.stream()
                            .collect(Collectors.groupingBy(
                                    testCase -> {
                                        String phase = testCase.getPhase();

                                        if (phase == null || phase.isBlank()) {
                                            return "Phase-I";
                                        }

                                        return phase;
                                    }
                            ));

            createPhaseSheet(
                    workbook,
                    "Phase-I",
                    testCasesByPhase.getOrDefault("Phase-I", List.of())
            );

            createPhaseSheet(
                    workbook,
                    "Phase-II",
                    testCasesByPhase.getOrDefault("Phase-II", List.of())
            );

            createPhaseSheet(
                    workbook,
                    "Phase-III",
                    testCasesByPhase.getOrDefault("Phase-III", List.of())
            );

            workbook.write(outputStream);

            return outputStream.toByteArray();
        }
    }

    private void createPhaseSheet(
            Workbook workbook,
            String phaseName,
            List<TestCase> testCases
    ) {

        Sheet sheet = workbook.createSheet(phaseName);

        CellStyle titleStyle = createTitleStyle(workbook);
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle bodyStyle = createBodyStyle(workbook);

        /*
         * Phase title
         */
        Row titleRow = sheet.createRow(0);
        titleRow.setHeightInPoints(28);

        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue(phaseName);
        titleCell.setCellStyle(titleStyle);

        /*
         * Merge phase title across all columns
         */
        sheet.addMergedRegion(
                new CellRangeAddress(0, 0, 0, 15)
        );

        /*
         * Headers
         */
        String[] headers = {
                "Req ID",
                "Test Case ID",
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

        Row headerRow = sheet.createRow(1);
        headerRow.setHeightInPoints(25);

        for (int i = 0; i < headers.length; i++) {

            Cell cell = headerRow.createCell(i);

            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        int rowNumber = 2;

        /*
         * Test cases
         */
        for (TestCase testCase : testCases) {

            List<TestCaseStep> steps = testCase.getSteps();

            /*
             * Test case without steps
             */
            if (steps == null || steps.isEmpty()) {

                Row row = sheet.createRow(rowNumber++);

                writeTestCaseFields(
                        row,
                        testCase,
                        bodyStyle
                );

                continue;
            }

            /*
             * Test case with steps
             */
            for (TestCaseStep step : steps) {

                Row row = sheet.createRow(rowNumber++);

                writeTestCaseFields(
                        row,
                        testCase,
                        bodyStyle
                );

                /*
                 * Step #
                 */
                if (step.getStepNumber() != null) {
                    setCell(
                            row,
                            5,
                            String.valueOf(step.getStepNumber()),
                            bodyStyle
                    );
                }

                /*
                 * Step Description
                 */
                setCell(
                        row,
                        6,
                        step.getStepDescription(),
                        bodyStyle
                );

                /*
                 * Expected Result
                 */
                setCell(
                        row,
                        7,
                        step.getExpectedResult(),
                        bodyStyle
                );

                /*
                 * Actual Result
                 */
                setCell(
                        row,
                        8,
                        step.getActualResult(),
                        bodyStyle
                );

                /*
                 * Test Status
                 */
                setCell(
                        row,
                        9,
                        step.getTestStatus(),
                        bodyStyle
                );

                /*
                 * PASS / FAIL
                 */
                setCell(
                        row,
                        10,
                        step.getPassFail(),
                        bodyStyle
                );

                /*
                 * Defect ID
                 */
                setCell(
                        row,
                        11,
                        step.getDefectId(),
                        bodyStyle
                );

                /*
                 * Severity
                 */
                setCell(
                        row,
                        12,
                        step.getSeverity(),
                        bodyStyle
                );

                /*
                 * Defect Type
                 */
                setCell(
                        row,
                        13,
                        step.getDefectType(),
                        bodyStyle
                );

                /*
                 * Root Cause
                 */
                setCell(
                        row,
                        14,
                        step.getRootCause(),
                        bodyStyle
                );

                /*
                 * Phase Introduced
                 */
                setCell(
                        row,
                        15,
                        step.getPhaseIntroduced(),
                        bodyStyle
                );
            }
        }

        /*
         * Freeze the header row
         */
        sheet.createFreezePane(0, 2);

        /*
         * Enable filtering
         */
        if (rowNumber > 2) {
            sheet.setAutoFilter(
                    new CellRangeAddress(
                            1,
                            rowNumber - 1,
                            0,
                            15
                    )
            );
        }

        /*
         * Column widths
         */
        setColumnWidths(sheet);
    }

    private void writeTestCaseFields(
            Row row,
            TestCase testCase,
            CellStyle bodyStyle
    ) {

        setCell(
                row,
                0,
                testCase.getReqId(),
                bodyStyle
        );

        setCell(
                row,
                1,
                testCase.getTestCaseId(),
                bodyStyle
        );

        setCell(
                row,
                2,
                testCase.getTestCaseName(),
                bodyStyle
        );

        setCell(
                row,
                3,
                testCase.getTestCaseDescription(),
                bodyStyle
        );

        setCell(
                row,
                4,
                testCase.getTestData(),
                bodyStyle
        );
    }

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

    private CellStyle createTitleStyle(Workbook workbook) {

        CellStyle style = workbook.createCellStyle();

        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 14);

        style.setFont(font);

        style.setAlignment(
                HorizontalAlignment.CENTER
        );

        style.setVerticalAlignment(
                VerticalAlignment.CENTER
        );

        style.setFillForegroundColor(
                IndexedColors.GREY_25_PERCENT.getIndex()
        );

        style.setFillPattern(
                FillPatternType.SOLID_FOREGROUND
        );

        style.setBorderBottom(BorderStyle.THIN);

        return style;
    }

    private CellStyle createHeaderStyle(Workbook workbook) {

        CellStyle style = workbook.createCellStyle();

        Font font = workbook.createFont();
        font.setBold(true);

        style.setFont(font);

        style.setAlignment(
                HorizontalAlignment.CENTER
        );

        style.setVerticalAlignment(
                VerticalAlignment.CENTER
        );

        style.setWrapText(true);

        style.setFillForegroundColor(
                IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex()
        );

        style.setFillPattern(
                FillPatternType.SOLID_FOREGROUND
        );

        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);

        return style;
    }

    private CellStyle createBodyStyle(Workbook workbook) {

        CellStyle style = workbook.createCellStyle();

        style.setVerticalAlignment(
                VerticalAlignment.TOP
        );

        style.setAlignment(
                HorizontalAlignment.LEFT
        );

        style.setWrapText(true);

        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);

        return style;
    }

    private void setColumnWidths(Sheet sheet) {

        sheet.setColumnWidth(0, 15 * 256);
        sheet.setColumnWidth(1, 16 * 256);
        sheet.setColumnWidth(2, 32 * 256);
        sheet.setColumnWidth(3, 45 * 256);
        sheet.setColumnWidth(4, 32 * 256);
        sheet.setColumnWidth(5, 10 * 256);
        sheet.setColumnWidth(6, 45 * 256);
        sheet.setColumnWidth(7, 45 * 256);
        sheet.setColumnWidth(8, 45 * 256);
        sheet.setColumnWidth(9, 18 * 256);
        sheet.setColumnWidth(10, 14 * 256);
        sheet.setColumnWidth(11, 16 * 256);
        sheet.setColumnWidth(12, 14 * 256);
        sheet.setColumnWidth(13, 20 * 256);
        sheet.setColumnWidth(14, 35 * 256);
        sheet.setColumnWidth(15, 20 * 256);
    }
}