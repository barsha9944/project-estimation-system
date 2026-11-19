package com.projectestimation.backend.psr.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.stereotype.Component;

import com.projectestimation.backend.psr.dto.PsrActivityDto;

@Component
public class PsrDocxParser {

    private static final DateTimeFormatter ISO_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ENGLISH);

    public record ParsedPsrData(
            Integer version,
            LocalDate reportDate,
            LocalDate periodStart,
            LocalDate periodEnd,
            String projectName,
            String reportedBy,
            String projectCode,
            String periodicity,
            List<PsrActivityDto> activitiesPerformed,
            List<PsrActivityDto> nextWeekPlannedActivities,
            String riskStatus,
            String reconstructedMarkdown
    ) {
        // Backwards compatibility constructor
        public ParsedPsrData(
                Integer version,
                LocalDate reportDate,
                LocalDate periodStart,
                LocalDate periodEnd,
                List<PsrActivityDto> activitiesPerformed,
                List<PsrActivityDto> nextWeekPlannedActivities,
                String riskStatus,
                String reconstructedMarkdown
        ) {
            this(version, reportDate, periodStart, periodEnd, "", "", "", "15 days",
                    activitiesPerformed, nextWeekPlannedActivities, riskStatus, reconstructedMarkdown);
        }
    }

    public record RiskRow(
            String slNo,
            String riskIdentified,
            String probability,
            String impact,
            String mitigation,
            String status
    ) {}

    public ParsedPsrData parse(byte[] docxBytes) throws IOException {
        try (InputStream is = new ByteArrayInputStream(docxBytes)) {
            return parse(is);
        }
    }

    public ParsedPsrData parse(InputStream inputStream) throws IOException {
        try (XWPFDocument doc = new XWPFDocument(inputStream)) {
            Integer version = null;
            LocalDate reportDate = null;
            LocalDate periodStart = null;
            LocalDate periodEnd = null;

            String projectName = "";
            String reportedBy = "";
            String projectCode = "";
            String periodicity = "15 days";
            String rawPeriodString = "";
            String rawDateString = "";

            // 1. Read Paragraphs (Header information)
            List<XWPFParagraph> paragraphs = doc.getParagraphs();
            for (int i = 0; i < paragraphs.size(); i++) {
                String text = paragraphs.get(i).getText().trim();
                if (text.isBlank()) continue;

                // Stop header scan once body headings are encountered
                if (text.equalsIgnoreCase("Activities during the Period")
                        || text.equalsIgnoreCase("Next 15 Days Planned Activities")
                        || text.startsWith("##")) {
                    break;
                }

                if (text.contains("Version") && version == null) {
                    try {
                        Matcher m = Pattern.compile("(?i)Version\\s*([0-9]+(?:\\.[0-9]+)?)").matcher(text);
                        if (m.find()) {
                            double v = Double.parseDouble(m.group(1));
                            version = (int) Math.round(v);
                        }
                    } catch (Exception ignored) {}
                }

                if (text.startsWith("Reported by:")) {
                    String val = text.substring("Reported by:".length()).trim();
                    if (val.isBlank() && i + 1 < paragraphs.size()) {
                        String next = paragraphs.get(i + 1).getText().trim();
                        if (!isHeaderLabel(next)) val = next;
                    }
                    reportedBy = val;
                } else if (text.startsWith("Project Code:")) {
                    String val = text.substring("Project Code:".length()).trim();
                    if (val.isBlank() && i + 1 < paragraphs.size()) {
                        String next = paragraphs.get(i + 1).getText().trim();
                        if (!isHeaderLabel(next)) val = next;
                    }
                    projectCode = val;
                } else if (text.startsWith("Project Name:")) {
                    String val = text.substring("Project Name:".length()).trim();
                    if (val.isBlank() && i + 1 < paragraphs.size()) {
                        String next = paragraphs.get(i + 1).getText().trim();
                        if (!isHeaderLabel(next)) val = next;
                    }
                    projectName = val;
                } else if (text.startsWith("Period of Reporting:")) {
                    String val = text.substring("Period of Reporting:".length()).trim();
                    if (val.isBlank() && i + 1 < paragraphs.size()) {
                        String next = paragraphs.get(i + 1).getText().trim();
                        if (!isHeaderLabel(next)) val = next;
                    }
                    rawPeriodString = val;
                    parsePeriodDates(val, periodStart, periodEnd);
                } else if (text.startsWith("Periodicity:")) {
                    String val = text.substring("Periodicity:".length()).trim();
                    if (val.isBlank() && i + 1 < paragraphs.size()) {
                        String next = paragraphs.get(i + 1).getText().trim();
                        if (!isHeaderLabel(next)) val = next;
                    }
                    if (!val.isBlank()) periodicity = val;
                } else if (text.startsWith("Date:")) {
                    String val = text.substring("Date:".length()).trim();
                    if (val.isBlank() && i + 1 < paragraphs.size()) {
                        String next = paragraphs.get(i + 1).getText().trim();
                        if (!isHeaderLabel(next)) val = next;
                    }
                    rawDateString = val;
                    reportDate = parseDateSafe(val);
                }
            }

            if (version == null) version = 1;
            if (rawDateString.isBlank()) {
                rawDateString = reportDate != null ? reportDate.format(ISO_FORMAT) : LocalDate.now().format(ISO_FORMAT);
            }
            if (rawPeriodString.isBlank()) {
                rawPeriodString = (periodStart != null ? periodStart.toString() : "") + " – " + (periodEnd != null ? periodEnd.toString() : "");
            }

            // 2. Read Tables
            List<PsrActivityDto> activitiesPerformed = new ArrayList<>();
            List<PsrActivityDto> nextWeekPlannedActivities = new ArrayList<>();
            List<RiskRow> riskRows = new ArrayList<>();
            List<String> trainingRows = new ArrayList<>();

            List<XWPFTable> tables = doc.getTables();
            for (XWPFTable table : tables) {
                List<XWPFTableRow> rows = table.getRows();
                if (rows.isEmpty()) continue;

                // Inspect row 0 to classify table
                XWPFTableRow headerRow = rows.get(0);
                StringBuilder hSb = new StringBuilder();
                for (XWPFTableCell cell : headerRow.getTableCells()) {
                    hSb.append(" ").append(cell.getText().toLowerCase().trim());
                }
                String headerText = hSb.toString();

                if (headerText.contains("in progressing") || headerText.contains("expected planned date")
                        || (headerText.contains("task") && headerText.contains("completed"))) {
                    // Table 1: Activities During the Period
                    for (int r = 1; r < rows.size(); r++) {
                        XWPFTableRow row = rows.get(r);
                        List<XWPFTableCell> cells = row.getTableCells();
                        if (cells.isEmpty()) continue;

                        String taskName = getCellText(cells, 0);
                        if (taskName.isBlank()) continue;

                        String completedCheck = getCellText(cells, 1);
                        String inProgressCheck = getCellText(cells, 2);
                        String pendingCheck = getCellText(cells, 3);
                        String progressStr = getCellText(cells, 4);
                        String plannedDateStr = getCellText(cells, 5);
                        String actualDateStr = getCellText(cells, 6);

                        String status = "Pending";
                        int progress = 0;

                        if (isMarked(completedCheck)) {
                            status = "Completed";
                            progress = 100;
                        } else if (isMarked(inProgressCheck)) {
                            status = "In Progress";
                            progress = 50;
                        } else if (isMarked(pendingCheck)) {
                            status = "Pending";
                            progress = 0;
                        }

                        try {
                            if (!progressStr.isBlank()) {
                                String clean = progressStr.replaceAll("[^0-9]", "");
                                if (!clean.isBlank()) {
                                    progress = Integer.parseInt(clean);
                                    if (progress >= 100) status = "Completed";
                                    else if (progress > 0) status = "In Progress";
                                    else status = "Pending";
                                }
                            }
                        } catch (Exception ignored) {}

                        PsrActivityDto dto = new PsrActivityDto();
                        dto.setActivityName(taskName);
                        dto.setTaskName(taskName);
                        dto.setStatus(status);
                        dto.setProgress(progress);
                        dto.setPlannedEndDate(plannedDateStr.isBlank() ? null : plannedDateStr);
                        dto.setActualEndDate(actualDateStr.isBlank() ? null : actualDateStr);
                        activitiesPerformed.add(dto);
                    }
                } else if (headerText.contains("planned duration") || headerText.contains("planned start")
                        || (headerText.contains("sl") && headerText.contains("activity"))) {
                    // Table 2: Next 15 Days Planned Activities
                    for (int r = 1; r < rows.size(); r++) {
                        XWPFTableRow row = rows.get(r);
                        List<XWPFTableCell> cells = row.getTableCells();
                        if (cells.isEmpty()) continue;

                        int colCount = cells.size();
                        String taskName;
                        String durationStr;
                        String plannedStartStr = null;
                        String plannedEndStr;

                        if (colCount >= 5) {
                            // Col 0: Sl. no., Col 1: Activity, Col 2: Duration, Col 3: Start, Col 4: Finish
                            taskName = getCellText(cells, 1);
                            durationStr = getCellText(cells, 2);
                            plannedStartStr = getCellText(cells, 3);
                            plannedEndStr = getCellText(cells, 4);
                        } else {
                            // Col 0: Activity, Col 1: Duration, Col 2: Start, Col 3: Finish
                            taskName = getCellText(cells, 0);
                            durationStr = getCellText(cells, 1);
                            plannedStartStr = colCount > 2 ? getCellText(cells, 2) : null;
                            plannedEndStr = colCount > 3 ? getCellText(cells, 3) : null;
                        }

                        if (taskName.isBlank()) continue;

                        int duration = 1;
                        try {
                            if (!durationStr.isBlank()) {
                                duration = Integer.parseInt(durationStr.replaceAll("[^0-9]", ""));
                            }
                        } catch (Exception ignored) {}

                        PsrActivityDto dto = new PsrActivityDto();
                        dto.setActivityName(taskName);
                        dto.setTaskName(taskName);
                        dto.setDuration(duration);
                        dto.setPlannedStartDate(plannedStartStr != null && !plannedStartStr.isBlank() ? plannedStartStr : null);
                        dto.setPlannedEndDate(plannedEndStr != null && !plannedEndStr.isBlank() ? plannedEndStr : null);
                        dto.setStatus("Pending");
                        dto.setProgress(0);
                        nextWeekPlannedActivities.add(dto);
                    }
                } else if (headerText.contains("risk") || headerText.contains("mitigation")) {
                    // Table 3: Risk Status
                    for (int r = 1; r < rows.size(); r++) {
                        XWPFTableRow row = rows.get(r);
                        List<XWPFTableCell> cells = row.getTableCells();
                        if (cells.isEmpty()) continue;

                        String slNo = getCellText(cells, 0);
                        String risk = getCellText(cells, 1);
                        String prob = getCellText(cells, 2);
                        String impact = getCellText(cells, 3);
                        String mit = getCellText(cells, 4);
                        String status = getCellText(cells, 5);

                        if (!risk.isBlank()) {
                            riskRows.add(new RiskRow(
                                    slNo.isBlank() ? String.valueOf(r) : slNo,
                                    risk,
                                    prob.isBlank() ? "M" : prob,
                                    impact.isBlank() ? "M" : impact,
                                    mit.isBlank() ? "N/A" : mit,
                                    status.isBlank() ? "Open" : status
                            ));
                        }
                    }
                } else if (headerText.contains("training")) {
                    // Table 4: Training of Project Team Members
                    for (int r = 1; r < rows.size(); r++) {
                        XWPFTableRow row = rows.get(r);
                        List<XWPFTableCell> cells = row.getTableCells();
                        if (cells.isEmpty()) continue;
                        StringBuilder rowSb = new StringBuilder("| ");
                        boolean hasData = false;
                        for (int c = 0; c < 7; c++) {
                            String t = getCellText(cells, c);
                            if (!t.isBlank()) hasData = true;
                            rowSb.append(t).append(" | ");
                        }
                        if (hasData) {
                            trainingRows.add(rowSb.toString().trim());
                        }
                    }
                }
            }

            // 3. Build Reconstructed Markdown
            String reconstructedMarkdown = buildReconstructedMarkdown(
                    version,
                    reportedBy,
                    projectCode,
                    projectName,
                    rawPeriodString,
                    periodicity,
                    rawDateString,
                    activitiesPerformed,
                    nextWeekPlannedActivities,
                    riskRows,
                    trainingRows
            );

            // Reconstruct clean riskStatus string for database
            StringBuilder riskSb = new StringBuilder();
            for (RiskRow r : riskRows) {
                riskSb.append(r.slNo()).append(" | ")
                        .append(r.riskIdentified()).append(" | ")
                        .append(r.probability()).append(" | ")
                        .append(r.impact()).append(" | ")
                        .append(r.mitigation()).append(" | ")
                        .append(r.status()).append("\n");
            }

            return new ParsedPsrData(
                    version,
                    reportDate != null ? reportDate : LocalDate.now(),
                    periodStart,
                    periodEnd,
                    projectName,
                    reportedBy,
                    projectCode,
                    periodicity,
                    activitiesPerformed,
                    nextWeekPlannedActivities,
                    riskSb.toString().trim(),
                    reconstructedMarkdown
            );
        }
    }

    private String buildReconstructedMarkdown(
            Integer version,
            String reportedBy,
            String projectCode,
            String projectName,
            String periodString,
            String periodicity,
            String dateString,
            List<PsrActivityDto> activitiesPerformed,
            List<PsrActivityDto> nextWeekPlannedActivities,
            List<RiskRow> riskRows,
            List<String> trainingRows
    ) {
        StringBuilder sb = new StringBuilder();

        // 7 Header paragraphs formatted precisely for frontend CSS p:nth-of-type
        // Paragraph 1: Report Title
        sb.append("Project Status Report (BEAS/PM/Frm/01 Version ").append(version != null ? version : 1).append(".0)\n\n");

        // Paragraph 2: Reported by
        sb.append("Reported by:").append(reportedBy.isBlank() ? "" : " " + reportedBy).append("\n\n");

        // Paragraph 3: Project Code
        sb.append("Project Code:").append(projectCode.isBlank() ? "" : " " + projectCode).append("\n\n");

        // Paragraph 4: Project Name
        sb.append("Project Name: ").append(projectName.isBlank() ? "N/A" : projectName).append("\n\n");

        // Paragraph 5: Period of Reporting
        sb.append("Period of Reporting: ").append(periodString.isBlank() ? "N/A" : periodString).append("\n\n");

        // Paragraph 6: Periodicity
        sb.append("Periodicity: ").append(periodicity.isBlank() ? "15 days" : periodicity).append("\n\n");

        // Paragraph 7: Date
        sb.append("Date: ").append(dateString.isBlank() ? LocalDate.now().toString() : dateString).append("\n\n");

        // Section 1: Activities during the Period
        sb.append("## Activities during the Period\n\n");
        sb.append("| Project Task | Completed | In progressing | Pending | % | Expected planned date | Actual finished date |\n");
        sb.append("|---|---|---|---|---:|---|---|\n");
        if (activitiesPerformed.isEmpty()) {
            sb.append("| | | | | | | |\n");
        } else {
            for (PsrActivityDto act : activitiesPerformed) {
                String task = escapeMarkdown(act.getTaskName() != null ? act.getTaskName() : act.getActivityName());
                String status = act.getStatus() != null ? act.getStatus() : "Pending";
                int progress = act.getProgress() != null ? act.getProgress() : 0;
                String plannedDate = act.getPlannedEndDate() != null ? act.getPlannedEndDate() : "";
                String actualDate = act.getActualEndDate() != null ? act.getActualEndDate() : "";

                String comp = "Completed".equalsIgnoreCase(status) ? "✓" : "";
                String inProg = "In Progress".equalsIgnoreCase(status) ? "✓" : "";
                String pend = "Pending".equalsIgnoreCase(status) ? "✓" : "";

                sb.append("| ").append(task)
                        .append(" | ").append(comp)
                        .append(" | ").append(inProg)
                        .append(" | ").append(pend)
                        .append(" | ").append(progress)
                        .append(" | ").append(plannedDate)
                        .append(" | ").append(actualDate)
                        .append(" |\n");
            }
        }
        sb.append("\n");

        // Section 2: Next 15 Days Planned Activities
        sb.append("## Next 15 Days Planned Activities\n\n");
        sb.append("| Sl. no. | Activity | Planned Duration (W/Days) | Planned Start | Planned Finish |\n");
        sb.append("|---:|---|---|---|---|\n");
        if (nextWeekPlannedActivities.isEmpty()) {
            sb.append("| | | | | |\n");
        } else {
            int sl = 1;
            for (PsrActivityDto act : nextWeekPlannedActivities) {
                String name = escapeMarkdown(act.getActivityName() != null ? act.getActivityName() : act.getTaskName());
                int dur = act.getDuration() != null ? act.getDuration() : 1;
                String start = act.getPlannedStartDate() != null ? act.getPlannedStartDate() : "";
                String end = act.getPlannedEndDate() != null ? act.getPlannedEndDate() : "";

                sb.append("| ").append(sl++)
                        .append(" | ").append(name)
                        .append(" | ").append(dur)
                        .append(" | ").append(start)
                        .append(" | ").append(end)
                        .append(" |\n");
            }
        }
        sb.append("\n");

        // Section 3: Risk Status
        sb.append("## Risk Status\n\n");
        sb.append("| Sl.No. | Risk Identified | Probability of occurrence | Impact of Risk | Suggested Mitigation Strategy | Status |\n");
        sb.append("|---:|---|:---:|:---:|---|---|\n");
        if (riskRows.isEmpty()) {
            sb.append("| | | | | | |\n");
        } else {
            for (RiskRow r : riskRows) {
                sb.append("| ").append(r.slNo())
                        .append(" | ").append(escapeMarkdown(r.riskIdentified()))
                        .append(" | ").append(r.probability())
                        .append(" | ").append(r.impact())
                        .append(" | ").append(escapeMarkdown(r.mitigation()))
                        .append(" | ").append(r.status())
                        .append(" |\n");
            }
        }
        sb.append("\n");

        // Section 4: Training of Project Team Members
        sb.append("## Training of Project Team Members\n\n");
        sb.append("| Sl. No. | Training need Identified | Planned Training Hours | Actual Training Hours | Planned Nominations | Actual Nominations | Waiver obtained, if any |\n");
        sb.append("|---:|---|---:|---:|---:|---:|---| \n");
        if (trainingRows.isEmpty()) {
            sb.append("| | | | | | | |\n");
        } else {
            for (String tRow : trainingRows) {
                sb.append(tRow).append("\n");
            }
        }
        sb.append("\n");

        // Section 5: Issues Which Need Management Attention
        sb.append("## Issues Which Need Management Attention\n");

        return sb.toString();
    }

    private boolean isHeaderLabel(String text) {
        if (text == null) return false;
        String t = text.trim();
        return t.startsWith("Project Status Report")
                || t.startsWith("Reported by:")
                || t.startsWith("Project Code:")
                || t.startsWith("Project Name:")
                || t.startsWith("Period of Reporting:")
                || t.startsWith("Periodicity:")
                || t.startsWith("Date:");
    }

    private void parsePeriodDates(String line, LocalDate start, LocalDate end) {
        if (line == null || line.isBlank()) return;
        String[] parts = line.split("[–-]");
        if (parts.length == 2) {
            LocalDate s = parseDateSafe(parts[0].trim());
            LocalDate e = parseDateSafe(parts[1].trim());
            if (s != null) start = s;
            if (e != null) end = e;
        }
    }

    private String getCellText(List<XWPFTableCell> cells, int index) {
        if (cells == null || index < 0 || index >= cells.size()) return "";
        XWPFTableCell cell = cells.get(index);
        return cell != null ? cell.getText().trim() : "";
    }

    private boolean isMarked(String text) {
        if (text == null) return false;
        String t = text.trim().toLowerCase();
        return t.contains("✓") || t.contains("✔") || t.contains("☑") || t.contains("v") || t.contains("x") || t.contains("yes") || t.contains("true") || t.equals("1");
    }

    private LocalDate parseDateSafe(String str) {
        if (str == null || str.isBlank()) return null;
        String s = str.trim();
        try {
            return LocalDate.parse(s, ISO_FORMAT);
        } catch (Exception ignored) {}
        try {
            return LocalDate.parse(s, DateTimeFormatter.ofPattern("dd-MMMM-yyyy", Locale.ENGLISH));
        } catch (Exception ignored) {}
        try {
            return LocalDate.parse(s, DateTimeFormatter.ofPattern("dd-MMM-yyyy", Locale.ENGLISH));
        } catch (Exception ignored) {}
        try {
            return LocalDate.parse(s);
        } catch (Exception ignored) {}
        return null;
    }

    private String escapeMarkdown(String str) {
        if (str == null) return "";
        return str.replace("|", "\\|").replace("\n", " ").replace("\r", " ").trim();
    }
}
