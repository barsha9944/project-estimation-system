package com.projectestimation.backend.mom.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.projectestimation.backend.mom.dto.MomAgendaItemDto;
import com.projectestimation.backend.mom.dto.MomDto;

@Component
public class MomDocxParser {

    private static final Logger log = LoggerFactory.getLogger(MomDocxParser.class);

    public static class ParsedMomData {
        public String meetingName;
        public String projectName;
        public String clientName;
        public LocalDate meetingDate;
        public String meetingTime;
        public String meetingLocation;
        public List<String> invitees = new ArrayList<>();
        public String recordedBy;
        public String circulation;
        public List<MomAgendaItemDto> agenda = new ArrayList<>();
        public List<String> discussionNotes = new ArrayList<>();
    }

    public ParsedMomData parse(Path path) throws IOException {
        try (InputStream is = Files.newInputStream(path)) {
            return parse(is);
        }
    }

    public ParsedMomData parse(byte[] bytes) throws IOException {
        try (InputStream is = new ByteArrayInputStream(bytes)) {
            return parse(is);
        }
    }

    public ParsedMomData parse(InputStream inputStream) throws IOException {
        ParsedMomData data = new ParsedMomData();

        try (XWPFDocument doc = new XWPFDocument(inputStream)) {
            List<XWPFTable> tables = doc.getTables();

            if (!tables.isEmpty()) {
                parseDetailsTable(tables.get(0), data);
            }

            if (tables.size() > 1) {
                parseAgendaTable(tables.get(1), data);
            }

            parseDiscussionNotes(doc, data);
        } catch (Exception ex) {
            log.error("Error parsing MOM docx: {}", ex.getMessage(), ex);
            throw new IOException("Failed to parse MOM docx", ex);
        }

        return data;
    }

    private void parseDetailsTable(XWPFTable table, ParsedMomData data) {
        for (XWPFTableRow row : table.getRows()) {
            List<XWPFTableCell> cells = row.getTableCells();
            if (cells.size() < 2) continue;

            String label = cells.get(0).getText().trim().toLowerCase();
            String value = cells.get(1).getText().trim();

            if (label.contains("meeting name")) {
                data.meetingName = value;
            } else if (label.contains("project name")) {
                data.projectName = value;
            } else if (label.contains("client name")) {
                data.clientName = value;
            } else if (label.contains("meeting date")) {
                parseDateTime(value, data);
            } else if (label.contains("meeting location")) {
                data.meetingLocation = value;
            } else if (label.contains("invitees")) {
                if (!value.isBlank()) {
                    String[] parts = value.split(",");
                    for (String part : parts) {
                        String inv = part.trim();
                        if (!inv.isEmpty()) {
                            data.invitees.add(inv);
                        }
                    }
                }
            } else if (label.contains("recorded by")) {
                parseRecordedByCirculation(value, data);
            }
        }
    }

    private void parseDateTime(String value, ParsedMomData data) {
        if (value == null || value.isBlank()) return;
        if (value.contains("/")) {
            String[] parts = value.split("/", 2);
            String datePart = parts[0].trim();
            String timePart = parts[1].trim();
            try {
                data.meetingDate = LocalDate.parse(datePart);
            } catch (Exception ignored) {
            }
            data.meetingTime = timePart;
        } else {
            try {
                data.meetingDate = LocalDate.parse(value);
            } catch (Exception ignored) {
                data.meetingTime = value;
            }
        }
    }

    private void parseRecordedByCirculation(String value, ParsedMomData data) {
        if (value == null || value.isBlank()) return;
        if (value.contains("/")) {
            String[] parts = value.split("/", 2);
            data.recordedBy = parts[0].trim();
            data.circulation = parts[1].trim();
        } else {
            data.recordedBy = value.trim();
        }
    }

    private void parseAgendaTable(XWPFTable table, ParsedMomData data) {
        List<XWPFTableRow> rows = table.getRows();
        if (rows.size() <= 1) return;

        for (int i = 1; i < rows.size(); i++) {
            XWPFTableRow row = rows.get(i);
            List<XWPFTableCell> cells = row.getTableCells();
            if (cells.size() < 3) continue;

            String itemStr = cells.get(0).getText().trim();
            String actionItems = cells.get(1).getText().trim();
            String presenter = cells.get(2).getText().trim();

            if (actionItems.equalsIgnoreCase("No agenda items available.")) {
                continue;
            }

            Integer itemNum = null;
            try {
                itemNum = Integer.parseInt(itemStr);
            } catch (NumberFormatException ignored) {
                itemNum = i;
            }

            if (!actionItems.isEmpty() || !presenter.isEmpty()) {
                data.agenda.add(new MomAgendaItemDto(itemNum, actionItems, presenter));
            }
        }
    }

    private void parseDiscussionNotes(XWPFDocument doc, ParsedMomData data) {
        boolean inDiscussionNotes = false;

        for (IBodyElement element : doc.getBodyElements()) {
            if (element instanceof XWPFParagraph para) {
                String text = para.getText().trim();
                if (text.isEmpty()) continue;

                if (text.contains("Discussion Notes")) {
                    inDiscussionNotes = true;
                    continue;
                }

                if (inDiscussionNotes) {
                    if (text.equalsIgnoreCase("No discussion notes available.")) {
                        continue;
                    }
                    data.discussionNotes.add(text);
                }
            }
        }
    }
}
