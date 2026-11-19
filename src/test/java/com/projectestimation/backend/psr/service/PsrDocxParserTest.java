package com.projectestimation.backend.psr.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
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
            // | Project Task | Completed | In progressing | Pending | % | Expected planned date | Actual finished date |
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

            // Table 2: Next 15 Days Planned Activities
            // | Project Task | Duration (working days) | Planned date | Actual Finished date |
            XWPFTable table2 = doc.createTable(2, 4);
            XWPFTableRow hRow2 = table2.getRow(0);
            hRow2.getCell(0).setText("Project Task");
            hRow2.getCell(1).setText("Duration (working days)");
            hRow2.getCell(2).setText("Planned date");
            hRow2.getCell(3).setText("Actual Finished date");

            XWPFTableRow dRow2 = table2.getRow(1);
            dRow2.getCell(0).setText("Sprint 1 Implementation");
            dRow2.getCell(1).setText("10");
            dRow2.getCell(2).setText("2026-03-25");
            dRow2.getCell(3).setText("");

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

        assertEquals(1, parsed.nextWeekPlannedActivities().size());
        assertEquals("Sprint 1 Implementation", parsed.nextWeekPlannedActivities().get(0).getActivityName());
        assertEquals(10, parsed.nextWeekPlannedActivities().get(0).getDuration());
        assertEquals("2026-03-25", parsed.nextWeekPlannedActivities().get(0).getPlannedEndDate());
    }
}
