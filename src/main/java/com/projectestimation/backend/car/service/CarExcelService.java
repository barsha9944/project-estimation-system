package com.projectestimation.backend.car.service;



import com.projectestimation.backend.car.model.CarAction;

import com.projectestimation.backend.car.model.CarAnalysis;

import com.projectestimation.backend.car.model.CarEffectiveness;

import lombok.RequiredArgsConstructor;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFSheet;

import org.springframework.core.io.ClassPathResource;

import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;



import java.io.ByteArrayOutputStream;

import java.io.InputStream;
import java.io.IOException;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

import java.time.LocalDate;

import java.time.LocalDateTime;

import java.time.format.DateTimeFormatter;

import java.util.ArrayList;

import java.util.List;

import java.util.Locale;

import java.util.regex.Matcher;

import java.util.regex.Pattern;



@Service

@RequiredArgsConstructor

public class CarExcelService {



   private static final String TEMPLATE_PATH = "CAR/CAR Report Mule III.xlsx";

   private static final DateTimeFormatter DATE_TIME_FORMAT =

           DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ENGLISH);



   private final CarService carService;



   @Transactional(readOnly = true)

   public byte[] generateCarReport(Long carId) {

       CarAnalysis car = carService.getCarEntity(carId);



       try {

           ClassPathResource resource = new ClassPathResource(TEMPLATE_PATH);



           try (InputStream inputStream = resource.getInputStream();

                Workbook workbook = WorkbookFactory.create(inputStream);

                ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {



               Sheet reportSheet = workbook.getSheet("CAR Report");

               Sheet trackerSheet = workbook.getSheet("CAR Action Tracker");



               if (reportSheet == null || trackerSheet == null) {

                   throw new IllegalStateException(

                           "Required CAR template worksheets are missing.");

               }



               populateReportSheet(workbook, reportSheet, car);

               populateTrackerSheet(trackerSheet, car);



               workbook.write(outputStream);

               return outputStream.toByteArray();

           }

       } catch (Exception ex) {

           throw new IllegalStateException(

                   "Failed to generate CAR Excel report: " + safe(ex.getMessage()), ex);

       }

   }



   // ---------------------------------------------------------------------

   // CAR REPORT

   // ---------------------------------------------------------------------



   private void populateReportSheet(Workbook workbook, Sheet sheet, CarAnalysis car) throws IOException {

       // Match the reference CAR Report sheet exactly:

       // title, development/release date row, phase heading, trigger,

       // five Why/Answer pairs and the two-row action proposal table.

       setCell(sheet, 0, 0,

               safe(car.getOpportunity() == null

                       ? null : car.getOpportunity().getOpportunityName())

                       + " - CAR Report");



       // The CAR entity does not contain development-start or release-target dates.

       // Clear the sample workbook dates instead of carrying another project's data.

       setCell(sheet, 1, 0, "");

       setCell(sheet, 1, 10, "");

       setCell(sheet, 2, 0,

               "Performance Monitoring & Quantitative Management");



       setCell(sheet, 4, 3,

               car.getTriggerForCar() == null ? "" : "Trigger for CAR: " + safe(car.getTriggerForCar()));



       setCell(sheet, 8, 3,

               "Root Cause Analysis - " + displayTechnique(car.getCausalAnalysisTechnique()));



       // Reference template has five Why/Answer pairs:

       // Excel rows 10-19, with question/answer text in column F.

       List<WhyAnswer> whyAnswers = parseWhyAnswers(car.getFiveWhysDetails());

       for (int pair = 0; pair < 5; pair++) {

           int whyRow = 9 + (pair * 2);

           int answerRow = whyRow + 1;

           WhyAnswer item = pair < whyAnswers.size()

                   ? whyAnswers.get(pair) : WhyAnswer.empty();



           setCell(sheet, whyRow, 4, "Why " + (pair + 1));

           setCell(sheet, answerRow, 4, "Ans " + (pair + 1) + ":");

           setCell(sheet, whyRow, 5, item.question());

           setCell(sheet, answerRow, 5, item.answer());



       }



       // The reference report does not have separate Root Cause, Lessons Learned,

       // Action Proposed, Monitoring Details or Fishbone sections. Clear any

       // non-reference content that may have been inserted by an earlier version.

       // Clear only the top-left cells of the two merged blank rows below

       // the five Why pairs. Do not write into non-anchor merged cells.

       setCell(sheet, 19, 5, "");

       setCell(sheet, 20, 5, "");



       // Reference action table: exactly two action rows (Excel rows 25 and 26).

       List<CarAction> actions = safeActions(car);

       int[] actionRows = {24, 25};



       for (int i = 0; i < actionRows.length; i++) {

           int rowIndex = actionRows[i];

           CarAction action = i < actions.size() ? actions.get(i) : null;

           Row row = getOrCreateRow(sheet, rowIndex);



           setCell(row, 3, action == null ? "" : String.valueOf(i + 1));

           setCell(row, 4, action == null ? "" : safe(action.getActionDescription()));

           setCell(row, 7, action == null ? "" : safe(action.getActionCategory()));

           setCell(row, 10, action == null ? "" : safe(action.getPriority()));

           setCell(row, 11, action == null ? "" : safe(action.getAssignedTo()));

           setCell(row, 12, action == null

                   ? "" : formatDate(action.getTargetCompletionDate()));



       }



       // Add the CAR-specific Fishbone diagram after the action table.
       // The template logo is retained; the diagram is added as a separate picture.
       addFishboneDiagram(workbook, sheet, car);

       // Preserve the reference template's original print settings and logo.
   }



   // ---------------------------------------------------------------------

   // CAR ACTION TRACKER

   // ---------------------------------------------------------------------



   private void populateTrackerSheet(Sheet sheet, CarAnalysis car) {

       // Match the reference CAR Action Tracker labels and cell positions.

       setCell(sheet, 3, 2, safe(car.getTriggerForCar()));

       setCell(sheet, 4, 2, safe(car.getProblemStatement()));

       setCell(sheet, 5, 2, safe(car.getOriginatorName()));

       setCell(sheet, 6, 3, displayTechnique(car.getCausalAnalysisTechnique()));



       LocalDateTime reviewDate = car.getUpdatedAt() != null

               ? car.getUpdatedAt() : car.getCreatedAt();

       setCell(sheet, 7, 2,

               reviewDate == null ? "" : reviewDate.toLocalDate().format(DATE_TIME_FORMAT));



       List<CarAction> actions = safeActions(car);



       // The reference tracker provides two action rows (12 and 13).

       // Keep the exact two-row format; do not insert additional rows.

       for (int i = 0; i < 2; i++) {

           int rowIndex = 11 + i;

           Row row = getOrCreateRow(sheet, rowIndex);

           CarAction action = i < actions.size() ? actions.get(i) : null;



           setCell(row, 0, action == null ? "" : String.valueOf(i + 1));

           setCell(row, 1, action == null ? "" : safe(action.getActionDescription()));

           setCell(row, 2, action == null ? "" : safe(action.getAssignedTo()));

           setCell(row, 3, action == null ? "" : formatDate(action.getTargetCompletionDate()));

           setCell(row, 4, action == null ? "" : safe(action.getActionTaken()));

           setCell(row, 6, action == null ? "" : effectivenessForActionRow(car, i));

           setCell(row, 8, action == null ? "" : trackerStatus(action.getStatus()));



       }



       // Keep the template's third preformatted row and print settings untouched.

   }



   /**

    * The current data model stores effectiveness at CAR level, not per action.

    * To avoid falsely assigning a review to a specific action, the first

    * tracker row is explicitly labelled as a CAR-level review.

    */

   private String effectivenessForActionRow(CarAnalysis car, int actionIndex) {

       if (actionIndex != 0 || car.getEffectivenessReviews() == null

               || car.getEffectivenessReviews().isEmpty()) {

           return "";

       }



       StringBuilder result = new StringBuilder("CAR-level review: ");

       CarEffectiveness review = car.getEffectivenessReviews().get(0);

       appendInline(result, safe(review.getEffectivenessResult()));

       appendInline(result, safe(review.getActualOutcome()));

       appendInline(result, safe(review.getSupportingEvidence()));

       return result.toString();

   }



   private void appendInline(StringBuilder builder, String value) {

       if (value != null && !value.isBlank()) {

           if (builder.length() > "CAR-level review: ".length()) {

               builder.append(" | ");

           }

           builder.append(value);

       }

   }



   // ---------------------------------------------------------------------

   // ---------------------------------------------------------------------
   // FISHBONE / CAUSE-AND-EFFECT DIAGRAM
   // ---------------------------------------------------------------------

   /** Adds a CAR-specific Fishbone diagram using the saved Five-Why answers first. */
   private void addFishboneDiagram(Workbook workbook, Sheet sheet, CarAnalysis car)
           throws IOException {

       String effect = safe(car.getRootCauseSummary());
       if (effect.isBlank()) effect = safe(car.getProblemStatement());
       if (effect.isBlank()) effect = safe(car.getTriggerForCar());
       if (effect.isBlank()) effect = "CAR outcome / problem";

       // The saved Five-Why answers are the primary source. This prevents a
       // generic AI response such as "Insufficient data" from becoming a branch.
       List<FishboneCause> causes = deriveFishboneCausesFromWhyAnswers(
               parseWhyAnswers(car.getFiveWhysDetails()));

       // Use the dedicated Fishbone field only when it contains meaningful causes.
       if (causes.isEmpty()) {
           causes = parseFishboneCauses(safe(car.getFishboneAnalysis()));
       }

       // Do not draw generic placeholder causes. If the record has no cause
       // information, omit the optional diagram rather than inventing data.
       if (causes.isEmpty()) {
           return;
       }

       removeExistingFishbonePictures(sheet);

       byte[] png = createFishboneImage(effect, causes);
       int pictureIndex = workbook.addPicture(png, Workbook.PICTURE_TYPE_PNG);

       Drawing<?> drawing = sheet.getDrawingPatriarch();
       if (drawing == null) {
           drawing = sheet.createDrawingPatriarch();
       }

       // Keep the diagram below the action table and inside the report width.
       ClientAnchor anchor = workbook.getCreationHelper().createClientAnchor();
       anchor.setCol1(1);   // Column B
       anchor.setRow1(28);  // Excel row 29
       anchor.setCol2(17);  // Before column R
       anchor.setRow2(49);  // Excel row 50
       anchor.setAnchorType(ClientAnchor.AnchorType.MOVE_AND_RESIZE);
       drawing.createPicture(anchor, pictureIndex);

       workbook.setPrintArea(workbook.getSheetIndex(sheet), "$A$1:$R$50");
   }

   /**
    * Removes only previously generated Fishbone pictures from the report.
    * The template logo is at the top of the sheet and is deliberately kept.
    */
   private void removeExistingFishbonePictures(Sheet sheet) {
       if (!(sheet instanceof XSSFSheet xssfSheet)) {
           return;
       }

       org.apache.poi.xssf.usermodel.XSSFDrawing xssfDrawing =
               xssfSheet.getDrawingPatriarch();
       if (xssfDrawing == null) {
           return;
       }

       var ctDrawing = xssfDrawing.getCTDrawing();

       ctDrawing.getTwoCellAnchorList().removeIf(anchor ->
               anchor.getPic() != null
                       && anchor.getFrom() != null
                       && (anchor.getFrom().getRow() >= 27
                           || (anchor.getFrom().getRow() > 2
                               && anchor.getFrom().getCol() >= 14)));

       ctDrawing.getOneCellAnchorList().removeIf(anchor ->
               anchor.getPic() != null
                       && anchor.getFrom() != null
                       && (anchor.getFrom().getRow() >= 27
                           || (anchor.getFrom().getRow() > 2
                               && anchor.getFrom().getCol() >= 14)));
   }

   /**
    * Converts the saved Why answers into Fishbone categories. The category is
    * inferred from words in the actual answer; the cause text itself is never
    * fabricated or replaced with generic placeholder text.
    */
   private List<FishboneCause> deriveFishboneCausesFromWhyAnswers(List<WhyAnswer> whyAnswers) {
       List<FishboneCause> causes = new ArrayList<>();
       if (whyAnswers == null || whyAnswers.isEmpty()) {
           return causes;
       }

       for (WhyAnswer pair : whyAnswers) {
           String detail = safe(pair.answer());
           if (detail.isBlank()) {
               detail = safe(pair.question());
           }
           detail = detail.replaceFirst("(?i)^because\\s+", "").trim();
           if (detail.isBlank() || isGenericFishboneText(detail)) {
               continue;
           }

           String lower = detail.toLowerCase(Locale.ROOT);
           String category;
           if (lower.contains("complexity") && lower.contains("resource")) {
               category = "COMPLEXITY & RESOURCES";
           } else if (lower.contains("risk assessment")) {
               category = "RISK ASSESSMENT";
           } else if (lower.contains("planning phase") || lower.contains("planned completion")
                   || lower.contains("schedule")) {
               category = "PLANNING";
           } else if (lower.contains("estimate") || lower.contains("estimation")
                   || lower.contains("estimated") || lower.contains("complexity")) {
               category = "ESTIMATION";
           } else if (lower.contains("resource") || lower.contains("staff")
                   || lower.contains("team") || lower.contains("skill")) {
               category = "RESOURCES";
           } else if (lower.contains("workflow") || lower.contains("process")
                   || lower.contains("procedure") || lower.contains("delay")) {
               category = "PROCESS";
           } else if (lower.contains("tool") || lower.contains("system")
                   || lower.contains("technology") || lower.contains("equipment")) {
               category = "TECHNOLOGY";
           } else {
               category = "CAUSE " + (causes.size() + 1);
           }

           // Keep distinct answers, but avoid duplicate category/detail entries.
           boolean duplicate = false;
           for (FishboneCause existing : causes) {
               if (existing.category().equalsIgnoreCase(category)
                       && existing.detail().equalsIgnoreCase(detail)) {
                   duplicate = true;
                   break;
               }
           }
           if (!duplicate) {
               causes.add(new FishboneCause(category, detail));
           }
           if (causes.size() == 5) {
               break;
           }
       }
       return causes;
   }

   /** Parses category-labelled Fishbone text, including multiple labels in one paragraph. */
   private List<FishboneCause> parseFishboneCauses(String value) {
       List<FishboneCause> causes = new ArrayList<>();
       if (value == null || value.isBlank()) {
           return causes;
       }

       String normalized = value.replace("\r", " ")
               .replace("\n", " ")
               .replaceAll("\\s+", " ")
               .trim();

       Pattern categoryPattern = Pattern.compile(
               "(?i)\\b(People|Process|Technology|Measurement|Materials?|Environment|"
               + "Management|Method|Machine|Equipment|Tools|Documentation|Communication|"
               + "Planning|Estimation|Resources|Risk Assessment)\\s*:");
       Matcher matcher = categoryPattern.matcher(normalized);
       List<String> categories = new ArrayList<>();
       List<Integer> starts = new ArrayList<>();
       List<Integer> ends = new ArrayList<>();

       while (matcher.find()) {
           categories.add(matcher.group(1).toUpperCase(Locale.ROOT));
           starts.add(matcher.start());
           ends.add(matcher.end());
       }

       for (int i = 0; i < categories.size(); i++) {
           int detailStart = ends.get(i);
           int detailEnd = (i + 1 < starts.size()) ? starts.get(i + 1) : normalized.length();
           String detail = normalized.substring(detailStart, detailEnd)
                   .replaceAll("^[,;:.\\s-]+", "")
                   .replaceAll("[,;:.\\s-]+$", "")
                   .trim();

           if (detail.isBlank() || isGenericFishboneText(detail)) {
               continue;
           }
           causes.add(new FishboneCause(categories.get(i), detail));
           if (causes.size() >= 5) {
               break;
           }
       }

       // Fallback for structured bullet/line responses without category labels.
       if (causes.isEmpty()) {
           String[] lines = normalized.split("[;|]");
           for (String raw : lines) {
               String detail = raw.trim().replaceFirst("^[•*\\-]+\\s*", "");
               if (detail.isBlank() || isGenericFishboneText(detail)) {
                   continue;
               }
               causes.add(new FishboneCause("CAUSE " + (causes.size() + 1), detail));
               if (causes.size() >= 5) {
                   break;
               }
           }
       }
       return causes;
   }

   private boolean isGenericFishboneText(String value) {
       if (value == null || value.isBlank()) {
           return true;
       }
       String normalized = value.trim().replaceAll("[.! ]+$", "");
       return normalized.matches("(?i)(insufficient data|no data|not available|n/?a|none|unknown)");
   }

   private byte[] createFishboneImage(String effect, List<FishboneCause> causes)
           throws IOException {
       final int width = 1800;
       final int height = 820;
       final int spineY = 410;
       final int headX = 1420;

       BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
       Graphics2D g = image.createGraphics();
       try {
           g.setColor(Color.WHITE);
           g.fillRect(0, 0, width, height);
           g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
           g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

           Color navy = new Color(31, 78, 121);
           Color slate = new Color(71, 85, 105);
           Color orange = new Color(234, 88, 12);
           Color red = new Color(220, 38, 38);
           Color pale = new Color(255, 247, 237);
           Color light = new Color(248, 250, 252);
           Color text = new Color(45, 55, 72);

           g.setFont(new Font("Arial", Font.BOLD, 32));
           g.setColor(new Color(36, 50, 71));
           g.drawString("FISHBONE / CAUSE-AND-EFFECT ANALYSIS", 55, 48);

           // Central spine and tail.
           g.setColor(slate);
           g.setStroke(new BasicStroke(8, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
           g.drawLine(75, spineY, headX, spineY);
           g.drawLine(75, spineY, 25, spineY - 30);
           g.drawLine(75, spineY, 25, spineY + 30);
           g.fillPolygon(new int[]{headX, headX + 34, headX},
                   new int[]{spineY - 17, spineY, spineY + 17}, 3);

           int[] joints = {250, 480, 710, 940, 1170};
           int count = Math.min(causes.size(), joints.length);

           for (int i = 0; i < count; i++) {
               FishboneCause cause = causes.get(i);
               boolean upper = (i % 2 == 0);
               int jointX = joints[i];
               int branchX = upper ? jointX - 95 : jointX + 95;
               int branchY = upper ? 235 : 610;

               g.setColor(navy);
               g.setStroke(new BasicStroke(6, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
               g.drawLine(jointX, spineY, branchX, branchY);

               int categoryX = upper ? Math.max(35, jointX - 220) : Math.min(1190, jointX - 5);
               int categoryY = upper ? 75 : 680;
               int categoryW = 220;
               int categoryH = 42;
               g.setColor(Color.WHITE);
               g.fillRoundRect(categoryX, categoryY, categoryW, categoryH, 8, 8);
               g.setColor(navy);
               g.setStroke(new BasicStroke(2));
               g.drawRoundRect(categoryX, categoryY, categoryW, categoryH, 8, 8);
               g.setFont(new Font("Arial", Font.BOLD, 19));
               drawWrapped(g, safe(cause.category()), categoryX + 10, categoryY + 27,
                       categoryW - 20, 20, 1, navy);

               int detailW = 245;
               int detailH = 105;
               int detailX = upper ? Math.max(25, jointX - 235) : Math.min(1200, jointX - 5);
               int detailY = upper ? 125 : 465;
               g.setColor(light);
               g.fillRoundRect(detailX, detailY, detailW, detailH, 9, 9);
               g.setColor(red);
               g.setStroke(new BasicStroke(2));
               g.drawRoundRect(detailX, detailY, detailW, detailH, 9, 9);
               g.setFont(new Font("Arial", Font.PLAIN, 17));
               drawWrapped(g, safe(cause.detail()), detailX + 10, detailY + 22,
                       detailW - 20, 20, 4, text);

               // Orange connector from the cause detail box to its diagonal bone.
               g.setColor(orange);
               g.setStroke(new BasicStroke(3));
               int sx = detailX + detailW / 2;
               int sy = detailY + detailH;
               int tx = branchX;
               int ty = upper ? branchY - 18 : branchY - 18;
               g.drawLine(sx, sy, tx, ty);
               drawArrowHead(g, sx, sy, tx, ty, orange);
           }

           // Effect box at the arrow head.
           int effectX = 1470;
           int effectY = 300;
           int effectW = 300;
           int effectH = 220;
           g.setColor(pale);
           g.fillRoundRect(effectX, effectY, effectW, effectH, 18, 18);
           g.setColor(orange);
           g.setStroke(new BasicStroke(4));
           g.drawRoundRect(effectX, effectY, effectW, effectH, 18, 18);
           g.setFont(new Font("Arial", Font.BOLD, 20));
           drawWrapped(g, effect, effectX + 18, effectY + 34,
                   effectW - 36, 27, 7, new Color(124, 45, 18));

       } finally {
           g.dispose();
       }

       try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
           ImageIO.write(image, "png", out);
           return out.toByteArray();
       }
   }

   private void drawArrowHead(Graphics2D g, int x1, int y1, int x2, int y2, Color color) {
       double angle = Math.atan2(y2 - y1, x2 - x1);
       int length = 14;
       int xA = (int) (x2 - length * Math.cos(angle - 0.45));
       int yA = (int) (y2 - length * Math.sin(angle - 0.45));
       int xB = (int) (x2 - length * Math.cos(angle + 0.45));
       int yB = (int) (y2 - length * Math.sin(angle + 0.45));
       g.setColor(color);
       g.fillPolygon(new int[]{x2, xA, xB}, new int[]{y2, yA, yB}, 3);
   }

   private void drawWrapped(Graphics2D g, String value, int x, int y,
                            int maxWidth, int lineHeight, int maxLines, Color color) {
       g.setColor(color);
       String[] words = safe(value).split("\\s+");
       StringBuilder line = new StringBuilder();
       int lineNumber = 0;
       for (String word : words) {
           String candidate = line.length() == 0 ? word : line + " " + word;
           if (g.getFontMetrics().stringWidth(candidate) > maxWidth && line.length() > 0) {
               g.drawString(line.toString(), x, y + lineNumber * lineHeight);
               lineNumber++;
               line.setLength(0);
               line.append(word);
               if (lineNumber >= maxLines) return;
           } else {
               if (line.length() > 0) line.append(' ');
               line.append(word);
           }
       }
       if (line.length() > 0 && lineNumber < maxLines) {
           g.drawString(line.toString(), x, y + lineNumber * lineHeight);
       }
   }

   private record FishboneCause(String category, String detail) { }


   // TRACKER ROW / EXCEL HELPERS

   // ---------------------------------------------------------------------



   private List<CarAction> safeActions(CarAnalysis car) {

       return car.getActions() == null ? List.of() : car.getActions();

   }



   /**

    * Accepts the common AI formats:

    * 1) "Why 1: question / Answer 1: answer" pairs,

    * 2) alternating question and answer lines,

    * 3) numbered plain-text lines.

    * It does not manufacture missing questions or answers.

    */

   private List<WhyAnswer> parseWhyAnswers(String value) {
        List<WhyAnswer> result = new ArrayList<>();
        if (value == null || value.isBlank()) {
            return result;
        }

        // Handles Gemini's inline format, for example:
        // 1. Why was the phase delayed? Because the planned date was exceeded.
        // 2. Why was the planned date exceeded? Because tasks took longer.
        String text = value.replace("\r", " ")
                .replace("\n", " ")
                .replaceAll("\\s+", " ")
                .trim();

        // Locate each numbered or labelled Why question. The question number
        // after the word Why is optional, so both "1. Why ..." and "Why 1: ..."
        // are supported.
        Pattern whyStartPattern = Pattern.compile(
                "(?i)(?:^|\\s)(?:\\d+\\s*[.)]\\s*)?Why\\s*\\d*\\s*[:.)-]?\\s*");
        Matcher matcher = whyStartPattern.matcher(text);

        List<Integer> contentStarts = new ArrayList<>();
        while (matcher.find() && contentStarts.size() < 5) {
            contentStarts.add(matcher.end());
        }

        // Re-scan to capture the start of the next Why section as the current
        // section's end. This avoids treating all five pairs as one paragraph.
        matcher.reset();
        List<Integer> markerStarts = new ArrayList<>();
        while (matcher.find() && markerStarts.size() < 5) {
            markerStarts.add(matcher.start());
        }

        for (int i = 0; i < contentStarts.size() && result.size() < 5; i++) {
            int sectionStart = contentStarts.get(i);
            int sectionEnd = (i + 1 < markerStarts.size())
                    ? markerStarts.get(i + 1) : text.length();
            String section = text.substring(sectionStart, sectionEnd).trim();
            if (section.isBlank()) {
                continue;
            }

            int questionEnd = section.indexOf('?');
            String question;
            String answer;
            if (questionEnd >= 0) {
                question = section.substring(0, questionEnd + 1).trim();
                answer = section.substring(questionEnd + 1).trim();
            } else {
                Matcher becauseMatcher = Pattern.compile("(?i)\\s+Because\\s+")
                        .matcher(section);
                if (becauseMatcher.find()) {
                    question = section.substring(0, becauseMatcher.start()).trim();
                    answer = section.substring(becauseMatcher.start()).trim();
                } else {
                    question = section;
                    answer = "";
                }
            }

            answer = answer.replaceFirst("(?i)^[-:;\\s]+", "").trim();
            result.add(new WhyAnswer(question, answer));
        }

        return result;
    }

    private record WhyAnswer(String question, String answer) {

       private static WhyAnswer empty() {

           return new WhyAnswer("", "");

       }

   }



   private Row getOrCreateRow(Sheet sheet, int rowIndex) {

       Row row = sheet.getRow(rowIndex);

       return row == null ? sheet.createRow(rowIndex) : row;

   }



   private Cell getOrCreateCell(Row row, int columnIndex) {

       Cell cell = row.getCell(columnIndex);

       return cell == null ? row.createCell(columnIndex) : cell;

   }



   private void setCell(Sheet sheet, int rowIndex, int columnIndex, String value) {

       setCell(getOrCreateRow(sheet, rowIndex), columnIndex, value);

   }



   private void setCell(Row row, int columnIndex, String value) {

       getOrCreateCell(row, columnIndex).setCellValue(value == null ? "" : value);

   }



   private String trackerStatus(String status) {

       if (status == null || status.isBlank()) {

           return "";

       }



       return switch (status.trim().toUpperCase(Locale.ROOT)) {

           case "COMPLETED" -> "Closed";

           case "IN_PROGRESS" -> "In Progress";

           case "OVERDUE" -> "Overdue";

           case "PENDING" -> "Open";

           default -> status;

       };

   }



   private String displayTechnique(String technique) {

       if (technique == null || technique.isBlank()) {

           return "";

       }

       return technique.trim().replace('_', ' ').replace('-', ' ');

   }



   private String formatDate(LocalDate date) {

       return date == null ? "" : date.format(DATE_TIME_FORMAT);

   }



   private String safe(Object value) {

       return value == null ? "" : value.toString().trim();

   }



}
