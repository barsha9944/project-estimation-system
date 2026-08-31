package com.projectestimation.backend.testcase.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import com.projectestimation.backend.testcase.model.TestCase;
import com.projectestimation.backend.testcase.model.TestCaseStep;

@Service
public class TestCaseExcelService {

    public byte[] generateExcel(List<TestCase> testCases) throws IOException {

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Test Cases");

            // Header row
            Row headerRow = sheet.createRow(0);

            String[] headers = {
                    "Req ID",
                    "Test Case ID",
                    "Test Case Name",
                    "Test Case Description",
                    "Test Data",
                    "Step Number",
                    "Step Description",
                    "Expected Result"
            };

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
            }

            int rowNumber = 1;

            for (TestCase testCase : testCases) {

                List<TestCaseStep> steps = testCase.getSteps();

                if (steps == null || steps.isEmpty()) {

                    Row row = sheet.createRow(rowNumber++);

                    row.createCell(0).setCellValue(
                            safeString(testCase.getReqId()));

                    row.createCell(1).setCellValue(
                            safeString(testCase.getTestCaseId()));

                    row.createCell(2).setCellValue(
                            safeString(testCase.getTestCaseName()));

                    row.createCell(3).setCellValue(
                            safeString(testCase.getTestCaseDescription()));

                    row.createCell(4).setCellValue(
                            safeString(testCase.getTestData()));

                    row.createCell(5).setCellValue("");

                    row.createCell(6).setCellValue("");

                    row.createCell(7).setCellValue("");

                    continue;
                }

                for (TestCaseStep step : steps) {

                    Row row = sheet.createRow(rowNumber++);

                    row.createCell(0).setCellValue(
                            safeString(testCase.getReqId()));

                    row.createCell(1).setCellValue(
                            safeString(testCase.getTestCaseId()));

                    row.createCell(2).setCellValue(
                            safeString(testCase.getTestCaseName()));

                    row.createCell(3).setCellValue(
                            safeString(testCase.getTestCaseDescription()));

                    row.createCell(4).setCellValue(
                            safeString(testCase.getTestData()));

                    if (step.getStepNumber() != null) {
                        row.createCell(5).setCellValue(
                                step.getStepNumber());
                    } else {
                        row.createCell(5).setCellValue("");
                    }

                    row.createCell(6).setCellValue(
                            safeString(step.getStepDescription()));

                    row.createCell(7).setCellValue(
                            safeString(step.getExpectedResult()));
                }
            }

            // Adjust column widths
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(outputStream);

            return outputStream.toByteArray();
        }
    }

    private String safeString(String value) {
        return value == null ? "" : value;
    }
}