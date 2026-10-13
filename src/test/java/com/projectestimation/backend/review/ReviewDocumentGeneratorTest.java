package com.projectestimation.backend.review;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;

import com.projectestimation.backend.review.dto.ReviewFindingDto;
import com.projectestimation.backend.review.model.DocumentReviewType;
import com.projectestimation.backend.review.service.ReviewDataSheetGenerator;
import com.projectestimation.backend.review.service.ReviewNoteGenerator;

public class ReviewDocumentGeneratorTest {

    private final ReviewNoteGenerator reviewNoteGenerator = new ReviewNoteGenerator();
    private final ReviewDataSheetGenerator reviewDataSheetGenerator = new ReviewDataSheetGenerator();

    private final List<ReviewFindingDto> testFindings = List.of(
            new ReviewFindingDto(1, "5.1", "Module description lacks explicit validation rules for client inputs.", "Add regex pattern and field length validation table.", "Major", "Open", null),
            new ReviewFindingDto(2, "3.1", "External REST API timeout configurations not specified.", "Define connection and read timeouts under software interfaces.", "Minor", "Open", null),
            new ReviewFindingDto(3, "7.0", "Data retention policy for temporary calculation logs not mentioned.", "Specify 90-day retention schedule in section 7.0.", "Minor", "Open", null)
    );

    @Test
    void testGenerateSrsReviewNoteCycle1() throws IOException {
        LocalDateTime now = LocalDateTime.now();
        byte[] docBytes = reviewNoteGenerator.generateReviewNote(
                DocumentReviewType.SRS,
                1,
                "Opportunity 23",
                "Mousumi Mitra",
                now,
                now.plusDays(1),
                now.plusDays(2),
                "Opportunity 23_srs_review_data_sheet.xlsx",
                "Opportunity 23_srs.docx",
                "1.0",
                false
        );

        assertNotNull(docBytes);
        assertTrue(docBytes.length > 0);

        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(docBytes))) {
            assertNotNull(doc);
            assertTrue(doc.getParagraphs().size() > 5);
        }
    }

    @Test
    void testGenerateSrsReviewNoteCycle2() throws IOException {
        LocalDateTime now = LocalDateTime.now();
        byte[] docBytes = reviewNoteGenerator.generateReviewNote(
                DocumentReviewType.SRS,
                2,
                "Opportunity 23",
                "Mousumi Mitra",
                now,
                now.plusDays(1),
                now.plusDays(2),
                "Opportunity 23_srs_review_data_sheet.xlsx",
                "Opportunity 23_srs.docx",
                "1.0",
                true
        );

        assertNotNull(docBytes);
        assertTrue(docBytes.length > 0);

        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(docBytes))) {
            assertNotNull(doc);
            assertTrue(doc.getParagraphs().size() > 5);
        }
    }

    @Test
    void testGenerateReviewDataSheetWithFindings() throws IOException {
        LocalDateTime now = LocalDateTime.now();
        byte[] sheetBytes = reviewDataSheetGenerator.generateReviewDataSheet(
                DocumentReviewType.SRS,
                "Opportunity 23",
                "Manas Chattopadhay",
                "Mousumi Mitra",
                now,
                now.plusDays(1),
                now.plusDays(2),
                "Opportunity 23_srs.docx",
                "1.0",
                testFindings
        );

        assertNotNull(sheetBytes);
        assertTrue(sheetBytes.length > 0);

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(sheetBytes))) {
            assertNotNull(workbook);
            XSSFSheet sheet = workbook.getSheetAt(0);
            assertNotNull(sheet);

            // Row 12 is header, Row 13 is finding 1
            XSSFRow row1 = sheet.getRow(13);
            assertNotNull(row1);
            assertTrue(row1.getCell(5).getStringCellValue().contains("validation rules"));
        }
    }
}
