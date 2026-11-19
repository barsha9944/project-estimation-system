package com.projectestimation.backend.car.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class CarExcelParserTest {

    @Test
    public void testParseCarExcelReport() throws IOException {
        byte[] excelBytes;
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            // Sheet 1: CAR Report
            Sheet reportSheet = workbook.createSheet("CAR Report");
            Row titleRow = reportSheet.createRow(0);
            titleRow.createCell(0).setCellValue("Project Alpha - CAR Report");

            Row triggerRow = reportSheet.createRow(4);
            triggerRow.createCell(3).setCellValue("Trigger for CAR: Milestone schedule variance > 10%");

            Row techRow = reportSheet.createRow(8);
            techRow.createCell(3).setCellValue("Root Cause Analysis - FIVE_WHYS");

            // Why 1 & Ans 1
            Row why1 = reportSheet.createRow(9);
            why1.createCell(4).setCellValue("Why 1");
            why1.createCell(5).setCellValue("Why was the phase delayed?");

            Row ans1 = reportSheet.createRow(10);
            ans1.createCell(4).setCellValue("Ans 1:");
            ans1.createCell(5).setCellValue("Because regression testing uncovered blocking bugs.");

            // Action row 24 in report
            Row actionRow = reportSheet.createRow(24);
            actionRow.createCell(3).setCellValue(1);
            actionRow.createCell(4).setCellValue("Implement automated regression suite");
            actionRow.createCell(7).setCellValue("PROCESS_IMPROVEMENT");
            actionRow.createCell(10).setCellValue("HIGH");
            actionRow.createCell(11).setCellValue("QA Lead");
            actionRow.createCell(12).setCellValue("2026-04-15");

            // Sheet 2: CAR Action Tracker
            Sheet trackerSheet = workbook.createSheet("CAR Action Tracker");
            Row tTrigger = trackerSheet.createRow(3);
            tTrigger.createCell(2).setCellValue("Milestone schedule variance > 10%");

            Row tProblem = trackerSheet.createRow(4);
            tProblem.createCell(2).setCellValue("Delivery phase exceeded estimated duration by 15 days.");

            Row tOriginator = trackerSheet.createRow(5);
            tOriginator.createCell(2).setCellValue("John Doe");

            Row tTech = trackerSheet.createRow(6);
            tTech.createCell(3).setCellValue("FIVE_WHYS");

            // Tracker action row 11
            Row tAction = trackerSheet.createRow(11);
            tAction.createCell(0).setCellValue(1);
            tAction.createCell(1).setCellValue("Implement automated regression suite");
            tAction.createCell(2).setCellValue("QA Lead");
            tAction.createCell(3).setCellValue("2026-04-15");
            tAction.createCell(4).setCellValue("Framework setup completed");
            tAction.createCell(6).setCellValue("CAR-level review: Effective | Reduced defect escape by 40% | Test report v1.2");
            tAction.createCell(8).setCellValue("CLOSED");

            workbook.write(out);
            excelBytes = out.toByteArray();
        }

        CarExcelParser parser = new CarExcelParser();
        CarExcelParser.ParsedCarData result = parser.parse(excelBytes);

        assertNotNull(result);
        assertEquals("Milestone schedule variance > 10%", result.triggerForCar());
        assertEquals("Delivery phase exceeded estimated duration by 15 days.", result.problemStatement());
        assertEquals("John Doe", result.originatorName());
        assertEquals("FIVE_WHYS", result.causalAnalysisTechnique());
        assertNotNull(result.fiveWhysDetails());
        assertTrue(result.fiveWhysDetails().contains("Why was the phase delayed?"));
        assertTrue(result.fiveWhysDetails().contains("Because regression testing uncovered blocking bugs."));

        assertEquals(1, result.actions().size());
        CarExcelParser.ParsedAction action = result.actions().get(0);
        assertEquals(1, action.sequence());
        assertEquals("Implement automated regression suite", action.actionDescription());
        assertEquals("PROCESS_IMPROVEMENT", action.actionCategory());
        assertEquals("HIGH", action.priority());
        assertEquals("QA Lead", action.assignedTo());
        assertEquals(LocalDate.of(2026, 4, 15), action.targetCompletionDate());
        assertEquals("Framework setup completed", action.actionTaken());
        assertEquals("CLOSED", action.status());

        assertEquals(1, result.effectivenessReviews().size());
        assertEquals("Effective", result.effectivenessReviews().get(0).effectivenessResult());
        assertEquals("Reduced defect escape by 40%", result.effectivenessReviews().get(0).actualOutcome());
    }
}
