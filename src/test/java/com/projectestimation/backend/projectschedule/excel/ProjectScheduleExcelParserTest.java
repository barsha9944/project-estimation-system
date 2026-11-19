package com.projectestimation.backend.projectschedule.excel;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import com.projectestimation.backend.projectschedule.dto.SaveProjectScheduleTaskRequest;

import static org.junit.jupiter.api.Assertions.*;

public class ProjectScheduleExcelParserTest {

    @Test
    public void testTaskDeduplicationAndBreakdownGrouping() throws IOException {
        byte[] excelBytes;
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Project Schedule");

            // Header row
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Seq");
            header.createCell(1).setCellValue("Task");
            header.createCell(2).setCellValue("Task Breakdown");
            header.createCell(3).setCellValue("Planned Start");
            header.createCell(4).setCellValue("Planned End");
            header.createCell(5).setCellValue("Working Days");
            header.createCell(6).setCellValue("Actual Start");
            header.createCell(7).setCellValue("Actual End");
            header.createCell(8).setCellValue("Actual Working Days");
            header.createCell(9).setCellValue("Predecessor");
            header.createCell(10).setCellValue("Status");

            // Row 1: Task 1 Breakdown 1
            Row r1 = sheet.createRow(1);
            r1.createCell(0).setCellValue(1);
            r1.createCell(1).setCellValue("Requirement Analysis");
            r1.createCell(2).setCellValue("Stakeholder Interview");
            r1.createCell(3).setCellValue("2026-03-01");
            r1.createCell(4).setCellValue("2026-03-05");
            r1.createCell(5).setCellValue(5);
            r1.createCell(10).setCellValue("COMPLETED");

            // Row 2: Same Task ("Requirement Analysis"), Breakdown 2
            Row r2 = sheet.createRow(2);
            r2.createCell(0).setCellValue(1);
            r2.createCell(1).setCellValue("Requirement Analysis");
            r2.createCell(2).setCellValue("Requirement Specification Document");
            r2.createCell(3).setCellValue("2026-03-06");
            r2.createCell(4).setCellValue("2026-03-10");
            r2.createCell(5).setCellValue(5);
            r2.createCell(10).setCellValue("IN_PROGRESS");

            // Row 3: Same Task ("Requirement Analysis ") with trailing whitespace or missing seq
            Row r3 = sheet.createRow(3);
            r3.createCell(1).setCellValue("Requirement Analysis ");
            r3.createCell(2).setCellValue("Requirement Review & Signoff");
            r3.createCell(3).setCellValue("2026-03-11");
            r3.createCell(4).setCellValue("2026-03-15");
            r3.createCell(5).setCellValue(5);
            r3.createCell(10).setCellValue("NOT_STARTED");

            // Row 4: Task 2: Architecture Design
            Row r4 = sheet.createRow(4);
            r4.createCell(0).setCellValue(2);
            r4.createCell(1).setCellValue("Architecture Design");
            r4.createCell(2).setCellValue("High Level Design");
            r4.createCell(3).setCellValue("2026-03-16");
            r4.createCell(4).setCellValue("2026-03-25");
            r4.createCell(5).setCellValue(8);
            r4.createCell(10).setCellValue("NOT_STARTED");

            workbook.write(out);
            excelBytes = out.toByteArray();
        }

        ProjectScheduleExcelParser parser = new ProjectScheduleExcelParser();
        List<SaveProjectScheduleTaskRequest> tasks = parser.parseTasks(excelBytes);

        // Assert exactly 2 tasks (Requirement Analysis and Architecture Design)
        assertNotNull(tasks);
        assertEquals(2, tasks.size(), "Duplicate task names in Excel must be merged into a single logical task");

        SaveProjectScheduleTaskRequest reqAnalysis = tasks.get(0);
        assertEquals("Requirement Analysis", reqAnalysis.getTaskName());
        assertEquals(1, reqAnalysis.getSequence());
        assertEquals(3, reqAnalysis.getTaskBreakdowns().size(), "All 3 breakdowns must belong to Requirement Analysis");
        assertEquals("Stakeholder Interview", reqAnalysis.getTaskBreakdowns().get(0).getActivityName());
        assertEquals("Requirement Specification Document", reqAnalysis.getTaskBreakdowns().get(1).getActivityName());
        assertEquals("Requirement Review & Signoff", reqAnalysis.getTaskBreakdowns().get(2).getActivityName());

        SaveProjectScheduleTaskRequest archDesign = tasks.get(1);
        assertEquals("Architecture Design", archDesign.getTaskName());
        assertEquals(2, archDesign.getSequence());
        assertEquals(1, archDesign.getTaskBreakdowns().size());
    }
}
