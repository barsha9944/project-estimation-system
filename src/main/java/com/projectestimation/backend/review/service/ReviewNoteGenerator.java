package com.projectestimation.backend.review.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.apache.poi.util.Units;
import org.apache.poi.wp.usermodel.HeaderFooterType;
import org.apache.poi.xwpf.usermodel.BreakType;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFFooter;
import org.apache.poi.xwpf.usermodel.XWPFHeader;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTBorder;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPBdr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STBorder;
import org.springframework.stereotype.Component;

import com.projectestimation.backend.review.model.DocumentReviewType;

@Component
public class ReviewNoteGenerator {

    private static final String FIXED_REVIEWER_NAME = "Manas Chattopadhay";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public byte[] generateReviewNote(
            DocumentReviewType docType,
            int cycleNumber,
            String opportunityName,
            String teamLead,
            LocalDateTime originalGeneratedAt,
            LocalDateTime cycle1Date,
            LocalDateTime cycle2Date,
            String dataSheetFileName,
            String documentFileName,
            String docVersion,
            boolean isApproved
    ) throws IOException {

        try (XWPFDocument doc = new XWPFDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            String oppClean = (opportunityName != null && !opportunityName.isBlank())
                    ? opportunityName.trim()
                    : "Project";

            String oppCode = oppClean.replaceAll("[^a-zA-Z0-9]", "");
            if (oppCode.length() > 8) {
                oppCode = oppCode.substring(0, 8).toUpperCase();
            } else {
                oppCode = oppCode.toUpperCase();
            }

            String version = (docVersion != null && !docVersion.isBlank()) ? docVersion : "1.0";
            String requestedBy = (teamLead != null && !teamLead.isBlank()) ? teamLead.trim() : "Team Lead";

            String typePrefix = (docType == DocumentReviewType.SDD) ? "SDD" : "SRS";
            String noteSuffix = (cycleNumber == 2) ? "001A" : "001";
            String reviewNoteNo = typePrefix + "ReviewNote/ " + oppClean + "/" + noteSuffix;
            String ciNumber = "BEAS/" + oppCode + "/" + typePrefix + "/001";

            String requestDateStr = (originalGeneratedAt != null)
                    ? originalGeneratedAt.format(DATE_FORMATTER)
                    : LocalDateTime.now().format(DATE_FORMATTER);

            String cycle1DateStr = (cycle1Date != null)
                    ? cycle1Date.format(DATE_FORMATTER)
                    : requestDateStr;

            String cycle2DateStr = (cycle2Date != null)
                    ? cycle2Date.format(DATE_FORMATTER)
                    : cycle1DateStr;

            // Setup Header (Logo + Company Name + Divider Line)
            addBeasLogoBeforeTitle(doc);

            // Setup Footer (Page X of Y)
            addDocumentFooter(doc);

            // ================= PAGE 1 =================
            // Title
            XWPFParagraph titlePara = doc.createParagraph();
            titlePara.setAlignment(ParagraphAlignment.CENTER);
            titlePara.setSpacingBefore(100);
            titlePara.setSpacingAfter(40);

            XWPFRun titleRun = titlePara.createRun();
            titleRun.setText("Document Review Note");
            titleRun.setBold(true);
            titleRun.setFontFamily("Arial");
            titleRun.setFontSize(14);
            titleRun.addBreak();

            XWPFRun formCodeRun = titlePara.createRun();
            formCodeRun.setText("(BEAS/DOC/Form/01 Version 1.0)");
            formCodeRun.setFontFamily("Arial");
            formCodeRun.setFontSize(10);
            formCodeRun.setItalic(true);

            addSpacing(doc, 60);

            // Document Information Fields
            addLabeledField(doc, "Review Note No.:", reviewNoteNo);
            addLabeledField(doc, "Name of the Document to be reviewed:", documentFileName);
            addLabeledField(doc, "CI Identification No. of Document:", ciNumber);
            addLabeledField(doc, "Version No.:", version);
            addLabeledField(doc, "Cycle # of Review:", String.valueOf(cycleNumber));

            String refDocs = (cycleNumber == 1)
                    ? dataSheetFileName
                    : documentFileName + ", " + dataSheetFileName;
            addLabeledField(doc, "Reference Documents, if any:", refDocs);

            addLabeledField(doc, "Reviewer's Name:", FIXED_REVIEWER_NAME);

            String completionDateStr = (cycleNumber == 1) ? cycle1DateStr : cycle2DateStr;
            addLabeledField(doc, "Review to be completed by:", completionDateStr);

            // Review Requested By section
            XWPFParagraph reqPara = doc.createParagraph();
            reqPara.setSpacingBefore(40);
            reqPara.setSpacingAfter(20);

            XWPFRun reqLabelRun = reqPara.createRun();
            reqLabelRun.setText("Review Requested By:  ");
            reqLabelRun.setBold(true);
            reqLabelRun.setFontFamily("Arial");
            reqLabelRun.setFontSize(10);

            XWPFRun reqValRun = reqPara.createRun();
            reqValRun.setText(requestedBy + "                                        " + requestDateStr);
            reqValRun.setFontFamily("Arial");
            reqValRun.setFontSize(10);

            XWPFParagraph sigDatePara = doc.createParagraph();
            sigDatePara.setSpacingAfter(80);
            XWPFRun sigDateRun = sigDatePara.createRun();
            sigDateRun.setText("                                Signature                                                Date");
            sigDateRun.setFontFamily("Arial");
            sigDateRun.setFontSize(9);
            sigDateRun.setItalic(true);

            // Divider Section: (To be filled in by the Reviewer(s))
            XWPFParagraph reviewerSectionPara = doc.createParagraph();
            reviewerSectionPara.setSpacingBefore(60);
            reviewerSectionPara.setSpacingAfter(40);
            XWPFRun revSecRun = reviewerSectionPara.createRun();
            revSecRun.setText("(To be filled in by the Reviewer(s))");
            revSecRun.setBold(true);
            revSecRun.setItalic(true);
            revSecRun.setFontFamily("Arial");
            revSecRun.setFontSize(10);

            // Findings / Comments
            XWPFParagraph findingsPara = doc.createParagraph();
            findingsPara.setSpacingAfter(30);
            XWPFRun findingsTitleRun = findingsPara.createRun();
            findingsTitleRun.setText("Reviewer's Findings:");
            findingsTitleRun.setBold(true);
            findingsTitleRun.setFontFamily("Arial");
            findingsTitleRun.setFontSize(10);

            XWPFParagraph commentsPara = doc.createParagraph();
            commentsPara.setSpacingAfter(40);
            XWPFRun commLabel = commentsPara.createRun();
            commLabel.setText("Comments: ");
            commLabel.setBold(true);
            commLabel.setFontFamily("Arial");
            commLabel.setFontSize(10);

            // Large Bordered Comments Box
            String commentText = (cycleNumber == 1)
                    ? "See ," + dataSheetFileName
                    : (isApproved ? "No Defect found and hence approved." : "Defects re-evaluated. See ," + dataSheetFileName);
            addLargeBorderedCommentsBox(doc, commentText);

            // Next Action Proposed
            XWPFParagraph actionPara = doc.createParagraph();
            actionPara.setSpacingBefore(60);
            actionPara.setSpacingAfter(20);
            XWPFRun actLabel = actionPara.createRun();
            actLabel.setText("Next Action Proposed: ");
            actLabel.setBold(true);
            actLabel.setFontFamily("Arial");
            actLabel.setFontSize(10);

            XWPFRun actVal = actionPara.createRun();
            if (cycleNumber == 1) {
                actVal.setText("                  Bring for further review on: " + cycle2DateStr);
            } else {
                actVal.setText("                  Bring for further review on: NA");
            }
            actVal.setFontFamily("Arial");
            actVal.setFontSize(10);

            XWPFParagraph tickPara = doc.createParagraph();
            tickPara.setSpacingAfter(20);
            XWPFRun tickRun = tickPara.createRun();
            tickRun.setText("(Tick as appropriate)");
            tickRun.setFontFamily("Arial");
            tickRun.setFontSize(9);
            tickRun.setItalic(true);

            XWPFParagraph approvedPara = doc.createParagraph();
            approvedPara.setSpacingAfter(60);
            XWPFRun approvedRun = approvedPara.createRun();
            if (cycleNumber == 1) {
                approvedRun.setText("                                   [   ] Approved");
            } else {
                if (isApproved) {
                    approvedRun.setText("                                   [ X ] Approved");
                } else {
                    approvedRun.setText("                                   [   ] Approved");
                }
            }
            approvedRun.setBold(true);
            approvedRun.setFontFamily("Arial");
            approvedRun.setFontSize(10);

            // Total effort spent
            String effortHours = (cycleNumber == 1) ? "4" : "2";
            addLabeledField(doc, "Total effort Spent in Review (Person Hours):", effortHours);

            // Reviewer signature and review date
            String reviewDateStr = (cycleNumber == 1) ? cycle1DateStr : cycle2DateStr;
            addLabeledField(doc, "Reviewer's Signature:", FIXED_REVIEWER_NAME);

            // ================= PAGE BREAK TO PAGE 2 =================
            XWPFParagraph pageBreakPara = doc.createParagraph();
            XWPFRun pbRun = pageBreakPara.createRun();
            pbRun.addBreak(BreakType.PAGE);

            // ================= PAGE 2 =================
            // Date near the top of Page 2 matching reference sample
            XWPFParagraph page2DatePara = doc.createParagraph();
            page2DatePara.setSpacingBefore(100);
            page2DatePara.setSpacingAfter(40);
            XWPFRun p2DateLabel = page2DatePara.createRun();
            p2DateLabel.setText("Date:  ");
            p2DateLabel.setBold(true);
            p2DateLabel.setFontFamily("Arial");
            p2DateLabel.setFontSize(10);

            XWPFRun p2DateVal = page2DatePara.createRun();
            p2DateVal.setText(reviewDateStr);
            p2DateVal.setFontFamily("Arial");
            p2DateVal.setFontSize(10);

            doc.write(out);
            return out.toByteArray();
        }
    }

    private void addDocumentHeader(XWPFDocument doc) {
    	XWPFHeader header = doc.createHeader(HeaderFooterType.DEFAULT);

    	XWPFParagraph headerPara = header.getParagraphArray(0);
    	if (headerPara == null) {
    	    headerPara = header.createParagraph();
    	}
    headerPara.setAlignment(ParagraphAlignment.LEFT);
    headerPara.setSpacingBefore(0);
    headerPara.setSpacingAfter(60);

    try (InputStream logoStream = getClass()
            .getClassLoader()
            .getResourceAsStream("psr/beas-logo.png")) {

        if (logoStream == null) {
            throw new IllegalStateException(
                    "BEAS logo not found: psr/beas-logo.png");
        }

        XWPFRun logoRun = headerPara.createRun();
        logoRun.addPicture(
                logoStream,
                XWPFDocument.PICTURE_TYPE_PNG,
                "beas-logo.png",
                Units.toEMU(110),
                Units.toEMU(17));

        XWPFRun textRun = headerPara.createRun();
        textRun.setText("   BEAS Consultancy and Services Pvt. Ltd.");
        textRun.setBold(true);
        textRun.setFontFamily("Arial");
        textRun.setFontSize(10);

    } catch (Exception e) {
        throw new IllegalStateException(
                "Failed to add BEAS logo to Review Note", e);
    }

    CTPPr ppr = headerPara.getCTP().getPPr();
    if (ppr == null) {
        ppr = headerPara.getCTP().addNewPPr();
    }

    CTPBdr bdr = ppr.isSetPBdr()
            ? ppr.getPBdr()
            : ppr.addNewPBdr();

    CTBorder bottomBorder = bdr.isSetBottom()
            ? bdr.getBottom()
            : bdr.addNewBottom();

    bottomBorder.setVal(STBorder.SINGLE);
    bottomBorder.setSz(BigInteger.valueOf(6));
    bottomBorder.setSpace(BigInteger.valueOf(4));
    bottomBorder.setColor("888888");
}

    private void addDocumentFooter(XWPFDocument doc) {
        XWPFFooter footer = doc.createFooter(HeaderFooterType.DEFAULT);
        XWPFParagraph footerPara = footer.createParagraph();
        footerPara.setAlignment(ParagraphAlignment.RIGHT);

        XWPFRun run = footerPara.createRun();
        run.setText("Page ");
        run.setFontFamily("Arial");
        run.setFontSize(9);

        // Dynamic page numbering field
        footerPara.getCTP().addNewFldSimple().setInstr("PAGE");

        XWPFRun run2 = footerPara.createRun();
        run2.setText(" of ");
        run2.setFontFamily("Arial");
        run2.setFontSize(9);

        footerPara.getCTP().addNewFldSimple().setInstr("NUMPAGES");
    }

    private void addLargeBorderedCommentsBox(XWPFDocument doc, String text) {
        XWPFTable table = doc.createTable(1, 1);
        table.setWidth("100%");

        XWPFTableRow row = table.getRow(0);
        row.setHeight(1800); // Box height

        XWPFTableCell cell = row.getCell(0);
        cell.removeParagraph(0);

        XWPFParagraph p = cell.addParagraph();
        p.setSpacingBefore(60);
        p.setSpacingAfter(60);

        XWPFRun r = p.createRun();
        r.setText(text);
        r.setFontFamily("Arial");
        r.setFontSize(10);
    }

    private void addLabeledField(XWPFDocument doc, String label, String value) {
        XWPFParagraph p = doc.createParagraph();
        p.setSpacingAfter(25);

        XWPFRun labelRun = p.createRun();
        labelRun.setText(label + " ");
        labelRun.setBold(true);
        labelRun.setFontFamily("Arial");
        labelRun.setFontSize(10);

        XWPFRun valRun = p.createRun();
        valRun.setText((value != null) ? value : "");
        valRun.setFontFamily("Arial");
        valRun.setFontSize(10);
    }

    private void addSpacing(XWPFDocument doc, int space) {
        XWPFParagraph p = doc.createParagraph();
        p.setSpacingAfter(space);
    }
 
    private void addBeasLogoBeforeTitle(XWPFDocument doc) {
        XWPFParagraph logoPara = doc.createParagraph();
        logoPara.setAlignment(ParagraphAlignment.CENTER);
        logoPara.setSpacingBefore(0);
        logoPara.setSpacingAfter(10);

        try (InputStream logoStream = getClass()
                .getClassLoader()
                .getResourceAsStream("psr/beas-logo.png")) {

            if (logoStream == null) {
                throw new IllegalStateException(
                        "BEAS logo not found: psr/beas-logo.png");
            }

            XWPFRun logoRun = logoPara.createRun();
            logoRun.addPicture(
                    logoStream,
                    XWPFDocument.PICTURE_TYPE_PNG,
                    "beas-logo.png",
                    Units.toEMU(158),
                    Units.toEMU(24));

        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to add BEAS logo before title", e);
        }
    }
}
