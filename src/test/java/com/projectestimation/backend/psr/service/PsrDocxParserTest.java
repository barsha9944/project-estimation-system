package com.projectestimation.backend.psr.service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class PsrDocxParserTest {

    @Test
    public void testParsePsrDocx() throws IOException {
        byte[] docxBytes;
        try (XWPFDocument doc = new XWPFDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            // Paragraphs: Header
            XWPFParagraph p1 = doc.createParagraph();
            p1.createRun().setText("Project Status Report");

            XWPFParagraph p2 = doc.createParagraph();
            p2.createRun().setText("(BEAS/PM/Frm/01 Version 2.0)");

            XWPFParagraph p3 = doc.createParagraph();
            p3.createRun().setText("Project Name:\nProject Pegasus");

            XWPFParagraph p4 = doc.createParagraph();
            p4.createRun().setText("Period of Reporting:\n2026-03-01 – 2026-03-15");

            XWPFParagraph p5 = doc.createParagraph();
            p5.createRun().setText("Periodicity:\n15 days");

            XWPFParagraph p6 = doc.createParagraph();
            p6.createRun().setText("Date:\n2026-03-15");

            // Table 1: Activities during the Period
            XWPFTable table1 = doc.createTable(2, 7);
            XWPFTableRow hRow = table1.getRow(0);
            hRow.getCell(0).setText("Project Task");
            hRow.getCell(1).setText("Completed");
            hRow.getCell(2).setText("In progressing");
            hRow.getCell(3).setText("Pending");
            hRow.getCell(4).setText("%");
            hRow.getCell(5).setText("Expected planned date");
            hRow.getCell(6).setText("Actual finished date");

            XWPFTableRow dRow1 = table1.getRow(1);
            dRow1.getCell(0).setText("Architectural Blueprint");
            dRow1.getCell(1).setText("✓");
            dRow1.getCell(2).setText("");
            dRow1.getCell(3).setText("");
            dRow1.getCell(4).setText("100%");
            dRow1.getCell(5).setText("2026-03-10");
            dRow1.getCell(6).setText("2026-03-10");

            // Table 2: Next 15 Days Planned Activities (5 columns)
            XWPFTable table2 = doc.createTable(2, 5);
            XWPFTableRow hRow2 = table2.getRow(0);
            hRow2.getCell(0).setText("Sl. no.");
            hRow2.getCell(1).setText("Activity");
            hRow2.getCell(2).setText("Planned Duration (W/Days)");
            hRow2.getCell(3).setText("Planned Start");
            hRow2.getCell(4).setText("Planned Finish");

            XWPFTableRow dRow2 = table2.getRow(1);
            dRow2.getCell(0).setText("1");
            dRow2.getCell(1).setText("Sprint 1 Implementation");
            dRow2.getCell(2).setText("10");
            dRow2.getCell(3).setText("2026-03-16");
            dRow2.getCell(4).setText("2026-03-25");

            doc.write(out);
            docxBytes = out.toByteArray();
        }

        PsrDocxParser parser = new PsrDocxParser();
        PsrDocxParser.ParsedPsrData parsed = parser.parse(docxBytes);

        assertNotNull(parsed);
        assertEquals(2, parsed.version());
        assertEquals(1, parsed.activitiesPerformed().size());
        assertEquals("Architectural Blueprint", parsed.activitiesPerformed().get(0).getActivityName());
        assertEquals("Completed", parsed.activitiesPerformed().get(0).getStatus());
        assertEquals(100, parsed.activitiesPerformed().get(0).getProgress());
        assertEquals("2026-03-10", parsed.activitiesPerformed().get(0).getPlannedEndDate());
        assertEquals("2026-03-10", parsed.activitiesPerformed().get(0).getActualEndDate());
    }

    @Test
    public void testParseActualPandocPsrDocx() throws IOException {
        File file = new File("D:/proposal-storage/Project 4/Quality/PSR/PSR_2026-12-09.docx");
        if (!file.exists()) return;
        byte[] docxBytes = Files.readAllBytes(file.toPath());
        PsrDocxParser parser = new PsrDocxParser();
        PsrDocxParser.ParsedPsrData parsed = parser.parse(docxBytes);
        assertNotNull(parsed);
        assertEquals(1, parsed.version());
        assertEquals("Project 4", parsed.projectName());
        assertEquals(6, parsed.activitiesPerformed().size());
        assertEquals(7, parsed.nextWeekPlannedActivities().size());
        assertFalse(parsed.riskStatus().isBlank());

        String md = parsed.reconstructedMarkdown();
        assertNotNull(md);
        System.out.println("=== RECONSTRUCTED MARKDOWN OUTPUT ===");
        System.out.println(md);
        System.out.println("======================================");

        // Verification of Issue 2 fixes:
        assertFalse(md.contains("{width="), "Must not contain {width=...}");
        assertFalse(md.contains("<br>"), "Must not contain <br> tags");
        assertTrue(md.contains("## Activities during the Period"));
        assertTrue(md.contains("## Next 15 Days Planned Activities"));
        assertTrue(md.contains("## Risk Status"));
        assertTrue(md.contains("## Training of Project Team Members"));
        assertTrue(md.contains("## Issues Which Need Management Attention"));
        assertTrue(md.startsWith("Project Status Report (BEAS/PM/Frm/01 Version 1.0)"));
    }

    @Test
    public void testExternalModificationDetectionAndParsing() throws IOException {
        byte[] originalBytes;
        try (XWPFDocument doc = new XWPFDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            XWPFParagraph p1 = doc.createParagraph();
            p1.createRun().setText("Project Status Report (BEAS/PM/Frm/01 Version 1.0)");
            XWPFParagraph p2 = doc.createParagraph();
            p2.createRun().setText("Project Name: Initial Project");

            XWPFTable t1 = doc.createTable(2, 7);
            XWPFTableRow hRow = t1.getRow(0);
            hRow.getCell(0).setText("Project Task");
            hRow.getCell(1).setText("Completed");
            hRow.getCell(2).setText("In progressing");
            hRow.getCell(3).setText("Pending");
            hRow.getCell(4).setText("%");
            hRow.getCell(5).setText("Expected planned date");
            hRow.getCell(6).setText("Actual finished date");

            XWPFTableRow dRow1 = t1.getRow(1);
            dRow1.getCell(0).setText("Database Schema Design");
            dRow1.getCell(1).setText("");
            dRow1.getCell(2).setText("✓");
            dRow1.getCell(3).setText("");
            dRow1.getCell(4).setText("50%");
            dRow1.getCell(5).setText("2026-11-20");
            dRow1.getCell(6).setText("2026-11-20");

            doc.write(out);
            originalBytes = out.toByteArray();
        }

        PsrDocxParser parser = new PsrDocxParser();
        PsrDocxParser.ParsedPsrData initialParsed = parser.parse(originalBytes);
        assertEquals("Initial Project", initialParsed.projectName());
        assertEquals(1, initialParsed.activitiesPerformed().size());
        assertEquals("Database Schema Design", initialParsed.activitiesPerformed().get(0).getActivityName());
        assertEquals("In Progress", initialParsed.activitiesPerformed().get(0).getStatus());

        // Simulate external modification in Word: edit task name, change status to Completed (100%), add new task row
        byte[] modifiedBytes;
        try (XWPFDocument doc = new XWPFDocument(new java.io.ByteArrayInputStream(originalBytes));
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            XWPFTable t1 = doc.getTables().get(0);
            XWPFTableRow dRow1 = t1.getRow(1);
            dRow1.getCell(0).setText("Database Schema Design - FINALIZED");
            dRow1.getCell(1).setText("✓");
            dRow1.getCell(2).setText("");
            dRow1.getCell(4).setText("100%");

            XWPFTableRow dRow2 = t1.createRow();
            dRow2.getCell(0).setText("Security Audit");
            dRow2.getCell(3).setText("✓");
            dRow2.getCell(4).setText("0%");
            dRow2.getCell(5).setText("2026-11-28");
            dRow2.getCell(6).setText("2026-11-28");

            doc.write(out);
            modifiedBytes = out.toByteArray();
        }

        PsrDocxParser.ParsedPsrData modifiedParsed = parser.parse(modifiedBytes);
        assertEquals(2, modifiedParsed.activitiesPerformed().size());
        assertEquals("Database Schema Design - FINALIZED", modifiedParsed.activitiesPerformed().get(0).getActivityName());
        assertEquals("Completed", modifiedParsed.activitiesPerformed().get(0).getStatus());
        assertEquals(100, modifiedParsed.activitiesPerformed().get(0).getProgress());

        assertEquals("Security Audit", modifiedParsed.activitiesPerformed().get(1).getActivityName());
        assertEquals("Pending", modifiedParsed.activitiesPerformed().get(1).getStatus());
        assertEquals("2026-11-28", modifiedParsed.activitiesPerformed().get(1).getPlannedEndDate());

        String md = modifiedParsed.reconstructedMarkdown();
        assertTrue(md.contains("Database Schema Design - FINALIZED"));
        assertTrue(md.contains("Security Audit"));
        assertTrue(md.contains("2026-11-28"));
        assertFalse(md.contains("{width="));
        assertFalse(md.contains("<br>"));
    }
}
