package com.projectestimation.backend.mom.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.apache.poi.util.Units;
import org.apache.poi.wp.usermodel.HeaderFooterType;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.TableWidthType;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFHeader;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.projectestimation.backend.mom.dto.MomAgendaItemDto;
import com.projectestimation.backend.mom.dto.MomDto;
import com.projectestimation.backend.mom.dto.MomGenerateRequest;
import com.projectestimation.backend.mom.dto.MomGenerationResponse;
import com.projectestimation.backend.mom.model.Mom;
import com.projectestimation.backend.mom.repository.MomRepository;
import com.projectestimation.backend.momAI.GeminiMomOrchestrator;
import com.projectestimation.backend.opportunity.model.Opportunity;
import com.projectestimation.backend.opportunity.repository.OpportunityRepository;

@Service
public class MomService {

    private static final String KICKOFF = "KICKOFF";
    private static final String TEAM = "TEAM";
    private static final String CLIENT = "CLIENT";
    private static final String SENIOR_MANAGEMENT = "SENIOR_MANAGEMENT";

    private static final String RED = "C00000";
    private static final String BLACK = "000000";
    private static final String LIGHT_GRAY = "D9D9D9";

    private final OpportunityRepository opportunityRepository;
    private final MomRepository momRepository;
    private final GeminiMomOrchestrator geminiMomOrchestrator;
    private final ObjectMapper objectMapper;

    public MomService(
            OpportunityRepository opportunityRepository,
            MomRepository momRepository,
            GeminiMomOrchestrator geminiMomOrchestrator,
            ObjectMapper objectMapper
    ) {
        this.opportunityRepository = opportunityRepository;
        this.momRepository = momRepository;
        this.geminiMomOrchestrator = geminiMomOrchestrator;
        this.objectMapper = objectMapper;
    }

    // ============================================================
    // GENERATE ALL MOMS
    // ============================================================

    @Transactional
    public MomGenerationResponse generateAllMoms(
            Long opportunityId,
            MomGenerateRequest request
    ) {

        Opportunity opportunity =
                opportunityRepository.findById(opportunityId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Opportunity not found: "
                                                + opportunityId
                                )
                        );

        if (request == null
                || request.projectStartDate() == null
                || request.projectEndDate() == null) {

            throw new IllegalArgumentException(
                    "Project start date and project end date are required"
            );
        }

        if (request.projectEndDate()
                .isBefore(request.projectStartDate())) {

            throw new IllegalArgumentException(
                    "Project end date cannot be before project start date"
            );
        }

        /*
         * Prevent duplicate generation.
         */
        if (momRepository.existsByOpportunityId(
                opportunityId)) {

            List<MomDto> existing =
                    momRepository
                            .findByOpportunityIdOrderByMeetingSequenceAsc(
                                    opportunityId
                            )
                            .stream()
                            .map(this::toDto)
                            .toList();

            return new MomGenerationResponse(
                    opportunityId,
                    existing.size(),
                    existing
            );
        }

        String projectName =
                opportunity.getOpportunityName();

        String clientName =
                opportunity.getClientName();

        String projectInformation =
                buildProjectInformation(
                        opportunity
                );

        List<Mom> generatedMoms =
                new ArrayList<>();

        LocalDate meetingDate =
                request.projectStartDate();

        int sequence = 1;

        String previousMeetingInformation =
                "No previous meeting exists. "
                        + "This is the first meeting.";

        while (!meetingDate.isAfter(
                request.projectEndDate())) {

            String meetingType =
                    determineMeetingType(
                            sequence
                    );

            String geminiResponse =
                    geminiMomOrchestrator.generate(
                            projectName,
                            clientName,
                            meetingType,
                            sequence,
                            meetingDate,
                            projectInformation,
                            previousMeetingInformation
                    );

            Mom mom =
                    createMom(
                            opportunityId,
                            projectName,
                            clientName,
                            meetingType,
                            sequence,
                            meetingDate,
                            geminiResponse
                    );

            Mom saved =
                    momRepository.save(mom);

            generatedMoms.add(saved);

            previousMeetingInformation =
                    buildPreviousMeetingInformation(
                            saved
                    );

            meetingDate =
                    meetingDate.plusDays(7);

            sequence++;
        }

        List<MomDto> result =
                generatedMoms.stream()
                        .map(this::toDto)
                        .toList();

        return new MomGenerationResponse(
                opportunityId,
                result.size(),
                result
        );
    }

    // ============================================================
    // GET ALL MOMS
    // ============================================================

    @Transactional(readOnly = true)
    public List<MomDto> getAllMoms(
            Long opportunityId
    ) {

        return momRepository
                .findByOpportunityIdOrderByMeetingSequenceAsc(
                        opportunityId
                )
                .stream()
                .map(this::toDto)
                .toList();
    }

    // ============================================================
    // DOWNLOAD MOM
    // ============================================================

    public byte[] downloadMom(
            Long momId
    ) {

        Mom mom =
                momRepository.findById(momId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "MOM not found: "
                                                + momId
                                )
                        );

        return generateDocx(mom);
    }

    // ============================================================
    // MEETING SEQUENCE
    // ============================================================

    private String determineMeetingType(
            int sequence
    ) {

        if (sequence == 1) {
            return KICKOFF;
        }

        int position =
                (sequence - 2) % 4;

        return switch (position) {
            case 0, 1 -> TEAM;
            case 2 -> CLIENT;
            default -> SENIOR_MANAGEMENT;
        };
    }

    // ============================================================
    // CREATE MOM
    // ============================================================

    private Mom createMom(
            Long opportunityId,
            String projectName,
            String clientName,
            String meetingType,
            int sequence,
            LocalDate meetingDate,
            String geminiResponse
    ) {

        try {

            JsonNode root =
                    objectMapper.readTree(
                            geminiResponse
                    );

            Mom mom = new Mom();

            mom.setOpportunityId(
                    opportunityId
            );

            mom.setProjectName(
                    projectName
            );

            mom.setClientName(
                    clientName
            );

            mom.setMeetingType(
                    meetingType
            );

            mom.setMeetingSequence(
                    sequence
            );

            mom.setMeetingDate(
                    meetingDate
            );

            mom.setMeetingName(
                    root.path("meetingName")
                            .asText(
                                    "Project Meeting"
                            )
            );

            mom.setMeetingTime(
                    root.path("meetingTime")
                            .asText("")
            );

            mom.setMeetingLocation(
                    root.path("meetingLocation")
                            .asText("")
            );

            List<String> invitees =
                    objectMapper.convertValue(
                            root.path("invitees"),
                            new TypeReference<List<String>>() {}
                    );

            mom.setInvitees(
                    objectMapper.writeValueAsString(
                            invitees
                    )
            );

            mom.setRecordedBy(
                    root.path("recordedBy")
                            .asText("")
            );

            mom.setCirculation(
                    root.path("circulation")
                            .asText("")
            );

            mom.setAgenda(
                    objectMapper.writeValueAsString(
                            root.path("agenda")
                    )
            );

            mom.setDiscussionNotes(
                    objectMapper.writeValueAsString(
                            root.path("discussionNotes")
                    )
            );

            mom.setDocumentName(
                    buildDocumentName(
                            projectName,
                            meetingType,
                            sequence
                    )
            );

            mom.setCreatedAt(
                    LocalDateTime.now()
            );

            return mom;

        } catch (Exception ex) {

            throw new IllegalStateException(
                    "Failed to parse Gemini MOM response",
                    ex
            );
        }
    }

    // ============================================================
    // PROJECT INFORMATION
    // ============================================================

    private String buildProjectInformation(
            Opportunity opportunity
    ) {

        return """
                PROJECT INFORMATION

                Opportunity ID:
                %s

                Project Name:
                %s

                Client Name:
                %s

                Implementation Type:
                %s

                Requirement Summary:
                %s

                Priority:
                %s

                Expected Delivery Date:
                %s

                Project Status:
                %s

                Platforms:
                %s

                Technology Categories:
                %s

                Enterprise Contexts:
                %s

                Components:
                %s

                Project Created Date:
                %s

                Project Last Updated Date:
                %s
                """.formatted(
                opportunity.getId(),
                safe(opportunity.getOpportunityName()),
                safe(opportunity.getClientName()),
                opportunity.getImplementationType() != null
                        ? opportunity.getImplementationType().name()
                        : "Not Available",
                safe(opportunity.getRequirementSummary()),
                opportunity.getPriority() != null
                        ? opportunity.getPriority().name()
                        : "Not Available",
                opportunity.getExpectedDeliveryDate() != null
                        ? opportunity.getExpectedDeliveryDate().toString()
                        : "Not Available",
                opportunity.getStatus() != null
                        ? opportunity.getStatus().name()
                        : "Not Available",
                formatList(
                        opportunity.getPlatforms()
                ),
                formatList(
                        opportunity.getTechnologyCategories()
                ),
                formatList(
                        opportunity.getEnterpriseContexts()
                ),
                formatList(
                        opportunity.getComponents()
                ),
                opportunity.getCreatedAt() != null
                        ? opportunity.getCreatedAt().toString()
                        : "Not Available",
                opportunity.getUpdatedAt() != null
                        ? opportunity.getUpdatedAt().toString()
                        : "Not Available"
        );
    }

    // ============================================================
    // PREVIOUS MEETING INFORMATION
    // ============================================================

    private String buildPreviousMeetingInformation(
            Mom mom
    ) {

        return """
                Meeting Type:
                %s

                Meeting Sequence:
                %s

                Meeting Date:
                %s

                Meeting Name:
                %s

                Agenda:
                %s

                Discussion Notes:
                %s
                """.formatted(
                safe(mom.getMeetingType()),
                mom.getMeetingSequence(),
                mom.getMeetingDate(),
                safe(mom.getMeetingName()),
                safe(mom.getAgenda()),
                safe(mom.getDiscussionNotes())
        );
    }

    // ============================================================
    // DOCUMENT NAME
    // ============================================================

    private String buildDocumentName(
            String projectName,
            String meetingType,
            int sequence
    ) {

        String cleanProjectName =
                projectName == null
                        ? "Project"
                        : projectName
                                .replaceAll(
                                        "[^a-zA-Z0-9-_ ]",
                                        ""
                                )
                                .trim()
                                .replaceAll(
                                        "\\s+",
                                        "_"
                                );

        return cleanProjectName
                + "_MOM_"
                + meetingType
                + "_"
                + sequence
                + ".docx";
    }

    // ============================================================
    // FORMAT LIST
    // ============================================================

    private String formatList(
            List<String> values
    ) {

        if (values == null
                || values.isEmpty()) {

            return "Not Available";
        }

        return String.join(
                ", ",
                values
        );
    }

    // ============================================================
    // CONVERT TO DTO
    // ============================================================

    private MomDto toDto(
            Mom mom
    ) {

        try {

            List<String> invitees =
                    objectMapper.readValue(
                            mom.getInvitees(),
                            new TypeReference<List<String>>() {}
                    );

            List<MomAgendaItemDto> agenda =
                    objectMapper.readValue(
                            mom.getAgenda(),
                            new TypeReference<List<MomAgendaItemDto>>() {}
                    );

            List<String> discussionNotes =
                    objectMapper.readValue(
                            mom.getDiscussionNotes(),
                            new TypeReference<List<String>>() {}
                    );

            return new MomDto(
                    mom.getId(),
                    mom.getOpportunityId(),
                    mom.getProjectName(),
                    mom.getClientName(),
                    mom.getMeetingType(),
                    mom.getMeetingSequence(),
                    mom.getMeetingDate(),
                    mom.getMeetingName(),
                    mom.getMeetingTime(),
                    mom.getMeetingLocation(),
                    invitees,
                    mom.getRecordedBy(),
                    mom.getCirculation(),
                    agenda,
                    discussionNotes,
                    mom.getDocumentName(),
                    mom.getCreatedAt()
            );

        } catch (Exception ex) {

            throw new IllegalStateException(
                    "Failed to convert MOM",
                    ex
            );
        }
    }

    // ============================================================
    // DOCX GENERATION
    // ============================================================

    private byte[] generateDocx(
            Mom mom
    ) {

        try (
                XWPFDocument document =
                        new XWPFDocument();

                ByteArrayOutputStream output =
                        new ByteArrayOutputStream()
        ) {

            configureDocument(
                    document
            );

            /*
             * BEAS logo only.
             *
             * CL2 / INTERNAL USE ONLY
             * has intentionally been removed.
             */
            addHeader(
                    document
            );

            addTitle(
                    document,
                    "Meeting Minutes"
            );

            addHeading(
                    document,
                    "1.1. Meeting Details"
            );

            addDetailsTable(
                    document,
                    mom
            );

            addHeading(
                    document,
                    "1.2. Agenda"
            );

            addAgendaTable(
                    document,
                    mom
            );

            addHeading(
                    document,
                    "1.3. Discussion Notes"
            );

            addDiscussionNotes(
                    document,
                    mom
            );

            document.write(
                    output
            );

            return output.toByteArray();

        } catch (Exception ex) {

            ex.printStackTrace();

            throw new IllegalStateException(
                    "Failed to generate MOM document: "
                            + ex.getClass().getName()
                            + " - "
                            + ex.getMessage(),
                    ex
            );
        }
    }

    // ============================================================
    // DOCUMENT CONFIGURATION
    // ============================================================

    private void configureDocument(
            XWPFDocument document
    ) {

        var body =
                document.getDocument()
                        .getBody();

        var section =
                body.isSetSectPr()
                        ? body.getSectPr()
                        : body.addNewSectPr();

        var pageMargins =
                section.isSetPgMar()
                        ? section.getPgMar()
                        : section.addNewPgMar();

        pageMargins.setTop(
                BigInteger.valueOf(720)
        );

        pageMargins.setBottom(
                BigInteger.valueOf(720)
        );

        pageMargins.setLeft(
                BigInteger.valueOf(900)
        );

        pageMargins.setRight(
                BigInteger.valueOf(900)
        );
    }

    // ============================================================
    // HEADER
    // ============================================================

    private void addHeader(
            XWPFDocument document
    ) throws IOException {

        XWPFHeader header =
                document.createHeader(
                        HeaderFooterType.DEFAULT
                );

        XWPFTable headerTable =
                header.createTable(
                        1,
                        1
                );

        headerTable.setWidth(
                "100%"
        );

        headerTable.setWidthType(
                TableWidthType.PCT
        );

        removeTableBorders(
                headerTable
        );

        XWPFTableCell logoCell =
                headerTable
                        .getRow(0)
                        .getCell(0);

        logoCell.setVerticalAlignment(
                XWPFTableCell.XWPFVertAlign.CENTER
        );

        XWPFParagraph logoParagraph =
                logoCell.getParagraphArray(0);

        logoParagraph.setAlignment(
                ParagraphAlignment.RIGHT
        );

        logoParagraph.setSpacingBefore(0);
        logoParagraph.setSpacingAfter(0);

        XWPFRun logoRun =
                logoParagraph.createRun();

        try (
                InputStream logoStream =
                        getClass()
                                .getClassLoader()
                                .getResourceAsStream(
                                        "psr/beas-logo.png"
                                )
        ) {

            if (logoStream == null) {

                throw new IOException(
                        "BEAS logo not found: "
                                + "src/main/resources/psr/beas-logo.png"
                );
            }

            logoRun.addPicture(
                    logoStream,
                    XWPFDocument.PICTURE_TYPE_PNG,
                    "beas-logo.png",
                    Units.toEMU(158),
                    Units.toEMU(24)
            );

        } catch (Exception e) {

            throw new IOException(
                    "Failed to add BEAS logo to MOM document",
                    e
            );
        }
    }

    // ============================================================
    // TITLE
    // ============================================================

    private void addTitle(
            XWPFDocument document,
            String text
    ) {

        XWPFParagraph paragraph =
                document.createParagraph();

        paragraph.setAlignment(
                ParagraphAlignment.LEFT
        );

        paragraph.setSpacingBefore(10);
        paragraph.setSpacingAfter(8);

        XWPFRun run =
                paragraph.createRun();

        run.setText(
                text
        );

        run.setBold(
                true
        );

        run.setItalic(
                true
        );

        run.setFontSize(
                18
        );

        run.setFontFamily(
                "Times New Roman"
        );

        run.setColor(
                RED
        );
    }

    // ============================================================
    // SECTION HEADINGS
    // ============================================================

    private void addHeading(
            XWPFDocument document,
            String text
    ) {

        XWPFParagraph paragraph =
                document.createParagraph();

        paragraph.setSpacingBefore(
                8
        );

        paragraph.setSpacingAfter(
                5
        );

        XWPFRun run =
                paragraph.createRun();

        run.setText(
                text
        );

        run.setBold(
                true
        );

        run.setItalic(
                true
        );

        run.setFontSize(
                13
        );

        run.setFontFamily(
                "Times New Roman"
        );

        run.setColor(
                RED
        );
    }

    // ============================================================
    // MEETING DETAILS TABLE
    // ============================================================

    private void addDetailsTable(
            XWPFDocument document,
            Mom mom
    ) {

        XWPFTable table =
                document.createTable(
                        7,
                        2
                );

        table.setWidth(
                "100%"
        );

        table.setWidthType(
                TableWidthType.PCT
        );

        setTableBorders(
                table
        );

        /*
         * Increased padding:
         *
         * top    = 120
         * left   = 160
         * bottom = 120
         * right  = 160
         */
        setCellMargins(
                table,
                120,
                160,
                120,
                160
        );

        /*
         * Wider label column.
         */
        setCellWidth(
                table.getRow(0).getCell(0),
                2800
        );

        setCellWidth(
                table.getRow(0).getCell(1),
                7200
        );

        addDetailRow(
                table,
                0,
                "Meeting Name",
                mom.getMeetingName()
        );

        addDetailRow(
                table,
                1,
                "Project Name",
                mom.getProjectName()
        );

        addDetailRow(
                table,
                2,
                "Client Name",
                mom.getClientName()
        );

        addDetailRow(
                table,
                3,
                "Meeting Date / Time",
                buildDateTime(
                        mom.getMeetingDate(),
                        mom.getMeetingTime()
                )
        );

        addDetailRow(
                table,
                4,
                "Meeting Location",
                mom.getMeetingLocation()
        );

        addDetailRow(
                table,
                5,
                "Invitees",
                String.join(
                        ", ",
                        readInvitees(mom)
                )
        );

        addDetailRow(
                table,
                6,
                "Recorded By / Circulation",
                buildRecordedByCirculation(
                        mom
                )
        );
    }

    // ============================================================
    // BUILD DATE / TIME
    // ============================================================

    private String buildDateTime(
            LocalDate date,
            String time
    ) {

        String dateValue =
                date == null
                        ? ""
                        : date.toString();

        String timeValue =
                time == null
                        ? ""
                        : time;

        if (dateValue.isBlank()) {
            return timeValue;
        }

        if (timeValue.isBlank()) {
            return dateValue;
        }

        return dateValue
                + " / "
                + timeValue;
    }

    // ============================================================
    // RECORDED BY / CIRCULATION
    // ============================================================

    private String buildRecordedByCirculation(
            Mom mom
    ) {

        String recordedBy =
                safe(
                        mom.getRecordedBy()
                );

        String circulation =
                safe(
                        mom.getCirculation()
                );

        if (recordedBy.isBlank()
                && circulation.isBlank()) {

            return "";
        }

        if (circulation.isBlank()) {
            return recordedBy;
        }

        if (recordedBy.isBlank()) {
            return circulation;
        }

        return recordedBy
                + " / "
                + circulation;
    }

    // ============================================================
    // ADD DETAIL ROW
    // ============================================================

    private void addDetailRow(
            XWPFTable table,
            int rowIndex,
            String label,
            String value
    ) {

        XWPFTableCell labelCell =
                table.getRow(rowIndex)
                        .getCell(0);

        XWPFTableCell valueCell =
                table.getRow(rowIndex)
                        .getCell(1);

        formatLabelCell(
                labelCell,
                label
        );

        formatValueCell(
                valueCell,
                value
        );
    }

    // ============================================================
    // AGENDA TABLE
    // ============================================================

    private void addAgendaTable(
            XWPFDocument document,
            Mom mom
    ) {

        List<MomAgendaItemDto> agenda =
                readAgenda(
                        mom
                );

        int rowCount =
                Math.max(
                        agenda.size() + 1,
                        2
                );

        XWPFTable table =
                document.createTable(
                        rowCount,
                        3
                );

        table.setWidth(
                "100%"
        );

        table.setWidthType(
                TableWidthType.PCT
        );

        setTableBorders(
                table
        );

        /*
         * Increased padding so the agenda
         * does not look compressed.
         */
        setCellMargins(
                table,
                110,
                140,
                110,
                140
        );

        /*
         * Item | Action Items | Presenter
         */
        setCellWidth(
                table.getRow(0).getCell(0),
                1100
        );

        setCellWidth(
                table.getRow(0).getCell(1),
                6100
        );

        setCellWidth(
                table.getRow(0).getCell(2),
                2800
        );

        addHeaderCell(
                table.getRow(0).getCell(0),
                "Item"
        );

        addHeaderCell(
                table.getRow(0).getCell(1),
                "Action Items"
        );

        addHeaderCell(
                table.getRow(0).getCell(2),
                "Presenter"
        );

        for (int i = 0;
             i < agenda.size();
             i++) {

            MomAgendaItemDto item =
                    agenda.get(i);

            addBodyCell(
                    table.getRow(i + 1).getCell(0),
                    String.valueOf(
                            item.item()
                    )
            );

            addBodyCell(
                    table.getRow(i + 1).getCell(1),
                    item.actionItems()
            );

            addBodyCell(
                    table.getRow(i + 1).getCell(2),
                    item.presenter()
            );
        }

        if (agenda.isEmpty()) {

            addBodyCell(
                    table.getRow(1).getCell(0),
                    ""
            );

            addBodyCell(
                    table.getRow(1).getCell(1),
                    "No agenda items available."
            );

            addBodyCell(
                    table.getRow(1).getCell(2),
                    ""
            );
        }
    }

    // ============================================================
    // DISCUSSION NOTES
    // ============================================================

    private void addDiscussionNotes(
            XWPFDocument document,
            Mom mom
    ) {

        List<String> notes =
                readDiscussionNotes(
                        mom
                );

        if (notes.isEmpty()) {

            XWPFParagraph paragraph =
                    document.createParagraph();

            paragraph.setSpacingAfter(
                    5
            );

            XWPFRun run =
                    paragraph.createRun();

            run.setText(
                    "No discussion notes available."
            );

            run.setFontFamily(
                    "Times New Roman"
            );

            run.setFontSize(
                    11
            );

            run.setColor(
                    BLACK
            );

            return;
        }

        for (String note : notes) {

            XWPFParagraph paragraph =
                    document.createParagraph();

            paragraph.setIndentationLeft(
                    360
            );

            paragraph.setFirstLineIndent(
                    -180
            );

            paragraph.setSpacingBefore(
                    1
            );

            paragraph.setSpacingAfter(
                    4
            );

            /*
             * No setLineSpacing() here.
             * It is not supported by your POI version.
             */

            XWPFRun run =
                    paragraph.createRun();

            run.setText(
                    "• " + safe(note)
            );

            run.setFontFamily(
                    "Times New Roman"
            );

            run.setFontSize(
                    11
            );

            run.setColor(
                    BLACK
            );
        }
    }

    // ============================================================
    // FORMAT LABEL CELL
    // ============================================================

    private void formatLabelCell(
            XWPFTableCell cell,
            String text
    ) {

        clearCell(
                cell
        );

        cell.setVerticalAlignment(
                XWPFTableCell.XWPFVertAlign.CENTER
        );

        XWPFParagraph paragraph =
                cell.getParagraphArray(0);

        paragraph.setSpacingBefore(
                2
        );

        paragraph.setSpacingAfter(
                2
        );

        /*
         * No setLineSpacing().
         */

        XWPFRun run =
                paragraph.createRun();

        run.setText(
                safe(text)
        );

        run.setBold(
                true
        );

        run.setFontFamily(
                "Times New Roman"
        );

        run.setFontSize(
                11
        );

        run.setColor(
                RED
        );
    }

    // ============================================================
    // FORMAT VALUE CELL
    // ============================================================

    private void formatValueCell(
            XWPFTableCell cell,
            String text
    ) {

        clearCell(
                cell
        );

        cell.setVerticalAlignment(
                XWPFTableCell.XWPFVertAlign.CENTER
        );

        XWPFParagraph paragraph =
                cell.getParagraphArray(0);

        paragraph.setSpacingBefore(
                2
        );

        paragraph.setSpacingAfter(
                2
        );

        /*
         * No setLineSpacing().
         */

        XWPFRun run =
                paragraph.createRun();

        run.setText(
                safe(text)
        );

        run.setFontFamily(
                "Times New Roman"
        );

        run.setFontSize(
                11
        );

        run.setColor(
                BLACK
        );
    }

    // ============================================================
    // HEADER CELL
    // ============================================================

    private void addHeaderCell(
            XWPFTableCell cell,
            String text
    ) {

        clearCell(
                cell
        );

        cell.setVerticalAlignment(
                XWPFTableCell.XWPFVertAlign.CENTER
        );

        XWPFParagraph paragraph =
                cell.getParagraphArray(0);

        paragraph.setAlignment(
                ParagraphAlignment.LEFT
        );

        paragraph.setSpacingBefore(
                2
        );

        paragraph.setSpacingAfter(
                2
        );

        /*
         * No setLineSpacing().
         */

        XWPFRun run =
                paragraph.createRun();

        run.setText(
                safe(text)
        );

        run.setBold(
                true
        );

        run.setFontFamily(
                "Times New Roman"
        );

        run.setFontSize(
                11
        );

        run.setColor(
                RED
        );
    }

    // ============================================================
    // BODY CELL
    // ============================================================

    private void addBodyCell(
            XWPFTableCell cell,
            String text
    ) {

        clearCell(
                cell
        );

        cell.setVerticalAlignment(
                XWPFTableCell.XWPFVertAlign.CENTER
        );

        XWPFParagraph paragraph =
                cell.getParagraphArray(0);

        paragraph.setSpacingBefore(
                2
        );

        paragraph.setSpacingAfter(
                2
        );

        /*
         * No setLineSpacing().
         */

        XWPFRun run =
                paragraph.createRun();

        run.setText(
                safe(text)
        );

        run.setFontFamily(
                "Times New Roman"
        );

        run.setFontSize(
                11
        );

        run.setColor(
                BLACK
        );
    }

    // ============================================================
    // CLEAR CELL
    // ============================================================

    private void clearCell(
            XWPFTableCell cell
    ) {

        XWPFParagraph paragraph =
                cell.getParagraphArray(0);

        for (XWPFRun run :
                paragraph.getRuns()) {

            run.setText(
                    "",
                    0
            );
        }
    }

    // ============================================================
    // CELL WIDTH
    // ============================================================

    private void setCellWidth(
            XWPFTableCell cell,
            int width
    ) {

        cell.setWidth(
                String.valueOf(width)
        );

        if (cell.getCTTc().getTcPr() == null) {

            cell.getCTTc().addNewTcPr();
        }

        if (cell.getCTTc()
                .getTcPr()
                .getTcW() == null) {

            cell.getCTTc()
                    .getTcPr()
                    .addNewTcW();
        }

        cell.getCTTc()
                .getTcPr()
                .getTcW()
                .setW(
                        BigInteger.valueOf(
                                width
                        )
                );
    }

    // ============================================================
    // CELL MARGINS / PADDING
    // ============================================================

    private void setCellMargins(
            XWPFTable table,
            int top,
            int left,
            int bottom,
            int right
    ) {

        var tableProperties =
                table.getCTTbl()
                        .getTblPr();

        var cellMargins =
                tableProperties.isSetTblCellMar()
                        ? tableProperties.getTblCellMar()
                        : tableProperties.addNewTblCellMar();

        if (cellMargins.isSetTop()) {

            cellMargins.getTop()
                    .setW(
                            BigInteger.valueOf(top)
                    );

        } else {

            cellMargins.addNewTop()
                    .setW(
                            BigInteger.valueOf(top)
                    );
        }

        if (cellMargins.isSetLeft()) {

            cellMargins.getLeft()
                    .setW(
                            BigInteger.valueOf(left)
                    );

        } else {

            cellMargins.addNewLeft()
                    .setW(
                            BigInteger.valueOf(left)
                    );
        }

        if (cellMargins.isSetBottom()) {

            cellMargins.getBottom()
                    .setW(
                            BigInteger.valueOf(bottom)
                    );

        } else {

            cellMargins.addNewBottom()
                    .setW(
                            BigInteger.valueOf(bottom)
                    );
        }

        if (cellMargins.isSetRight()) {

            cellMargins.getRight()
                    .setW(
                            BigInteger.valueOf(right)
                    );

        } else {

            cellMargins.addNewRight()
                    .setW(
                            BigInteger.valueOf(right)
                    );
        }
    }

    // ============================================================
    // TABLE BORDERS
    // ============================================================

    private void setTableBorders(
            XWPFTable table
    ) {

        var tableProperties =
                table.getCTTbl()
                        .getTblPr();

        var borders =
                tableProperties.isSetTblBorders()
                        ? tableProperties.getTblBorders()
                        : tableProperties.addNewTblBorders();

        var top =
                borders.isSetTop()
                        ? borders.getTop()
                        : borders.addNewTop();

        var bottom =
                borders.isSetBottom()
                        ? borders.getBottom()
                        : borders.addNewBottom();

        var left =
                borders.isSetLeft()
                        ? borders.getLeft()
                        : borders.addNewLeft();

        var right =
                borders.isSetRight()
                        ? borders.getRight()
                        : borders.addNewRight();

        var insideH =
                borders.isSetInsideH()
                        ? borders.getInsideH()
                        : borders.addNewInsideH();

        var insideV =
                borders.isSetInsideV()
                        ? borders.getInsideV()
                        : borders.addNewInsideV();

        configureBorder(
                top
        );

        configureBorder(
                bottom
        );

        configureBorder(
                left
        );

        configureBorder(
                right
        );

        configureBorder(
                insideH
        );

        configureBorder(
                insideV
        );
    }

    private void configureBorder(
            org.openxmlformats.schemas.wordprocessingml.x2006.main.CTBorder border
    ) {

        border.setVal(
                org.openxmlformats.schemas.wordprocessingml.x2006.main.STBorder.SINGLE
        );

        border.setSz(
                BigInteger.valueOf(
                        4
                )
        );

        border.setColor(
                LIGHT_GRAY
        );

        border.setSpace(
                BigInteger.ZERO
        );
    }

    // ============================================================
    // REMOVE TABLE BORDERS
    // ============================================================

    private void removeTableBorders(
            XWPFTable table
    ) {

        var tableProperties =
                table.getCTTbl()
                        .getTblPr();

        var borders =
                tableProperties.isSetTblBorders()
                        ? tableProperties.getTblBorders()
                        : tableProperties.addNewTblBorders();

        var top =
                borders.isSetTop()
                        ? borders.getTop()
                        : borders.addNewTop();

        var bottom =
                borders.isSetBottom()
                        ? borders.getBottom()
                        : borders.addNewBottom();

        var left =
                borders.isSetLeft()
                        ? borders.getLeft()
                        : borders.addNewLeft();

        var right =
                borders.isSetRight()
                        ? borders.getRight()
                        : borders.addNewRight();

        var insideH =
                borders.isSetInsideH()
                        ? borders.getInsideH()
                        : borders.addNewInsideH();

        var insideV =
                borders.isSetInsideV()
                        ? borders.getInsideV()
                        : borders.addNewInsideV();

        top.setVal(
                org.openxmlformats.schemas.wordprocessingml.x2006.main.STBorder.NIL
        );

        bottom.setVal(
                org.openxmlformats.schemas.wordprocessingml.x2006.main.STBorder.NIL
        );

        left.setVal(
                org.openxmlformats.schemas.wordprocessingml.x2006.main.STBorder.NIL
        );

        right.setVal(
                org.openxmlformats.schemas.wordprocessingml.x2006.main.STBorder.NIL
        );

        insideH.setVal(
                org.openxmlformats.schemas.wordprocessingml.x2006.main.STBorder.NIL
        );

        insideV.setVal(
                org.openxmlformats.schemas.wordprocessingml.x2006.main.STBorder.NIL
        );
    }

    // ============================================================
    // READ INVITEES
    // ============================================================

    private List<String> readInvitees(
            Mom mom
    ) {

        try {

            return objectMapper.readValue(
                    mom.getInvitees(),
                    new TypeReference<List<String>>() {}
            );

        } catch (Exception ex) {

            return List.of();
        }
    }

    // ============================================================
    // READ AGENDA
    // ============================================================

    private List<MomAgendaItemDto> readAgenda(
            Mom mom
    ) {

        try {

            return objectMapper.readValue(
                    mom.getAgenda(),
                    new TypeReference<List<MomAgendaItemDto>>() {}
            );

        } catch (Exception ex) {

            return List.of();
        }
    }

    // ============================================================
    // READ DISCUSSION NOTES
    // ============================================================

    private List<String> readDiscussionNotes(
            Mom mom
    ) {

        try {

            return objectMapper.readValue(
                    mom.getDiscussionNotes(),
                    new TypeReference<List<String>>() {}
            );

        } catch (Exception ex) {

            return List.of();
        }
    }

    // ============================================================
    // SAFE STRING
    // ============================================================

    private String safe(
            String value
    ) {

        return value == null
                ? ""
                : value;
    }
}