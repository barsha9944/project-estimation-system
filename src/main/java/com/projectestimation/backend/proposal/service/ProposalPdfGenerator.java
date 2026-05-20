package com.projectestimation.backend.proposal.service;

import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import com.projectestimation.backend.common.exception.ProposalFailedException;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;

@Component
public class ProposalPdfGenerator {

    public byte[] generate(String title, String proposalContent) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            Document document = new Document();
            PdfWriter.getInstance(document, outputStream);
            document.open();

            Font titleFont = new Font(Font.HELVETICA, 18, Font.BOLD);
            Font headingFont = new Font(Font.HELVETICA, 13, Font.BOLD);
            Font bodyFont = new Font(Font.HELVETICA, 11);

            document.add(new Paragraph(title, titleFont));
            document.add(new Paragraph(" "));

            for (String section : proposalContent.split("\n")) {
                String line = section.trim();
                if (line.isEmpty()) {
                    document.add(new Paragraph(" "));
                    continue;
                }

                if (line.equals(line.toUpperCase()) && line.length() < 80 && !line.contains(".")) {
                    document.add(new Paragraph(line, headingFont));
                } else {
                    document.add(new Paragraph(line, bodyFont));
                }
            }

            document.close();
            return outputStream.toByteArray();
        } catch (Exception ex) {
            throw new ProposalFailedException("Failed to generate proposal PDF", ex);
        }
    }
}
