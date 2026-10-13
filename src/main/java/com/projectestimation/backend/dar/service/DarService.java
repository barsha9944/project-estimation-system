package com.projectestimation.backend.dar.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFHeader;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.apache.poi.util.Units;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.projectestimation.backend.common.exception.ResourceNotFoundException;
import com.projectestimation.backend.dar.ai.GeminiDarOrchestrator;
import com.projectestimation.backend.dar.dto.DarAgendaItemDto;
import com.projectestimation.backend.dar.dto.DarDiscussionNoteDto;
import com.projectestimation.backend.dar.dto.DarResponseDto;
import com.projectestimation.backend.dar.model.Dar;
import com.projectestimation.backend.dar.repository.DarRepository;
import com.projectestimation.backend.opportunity.model.Opportunity;
import com.projectestimation.backend.opportunity.model.ProjectTeam;
import com.projectestimation.backend.opportunity.repository.OpportunityRepository;
import com.projectestimation.backend.opportunity.repository.ProjectTeamRepository;
import com.projectestimation.backend.projectschedule.model.ProjectSchedule;
import com.projectestimation.backend.projectschedule.model.ProjectScheduleTask;
import com.projectestimation.backend.projectschedule.model.ProjectScheduleTaskBreakdown;
import com.projectestimation.backend.projectschedule.repository.ProjectScheduleRepository;
import com.projectestimation.backend.sdd.dto.SddDto;
import com.projectestimation.backend.sdd.service.SddService;
import com.projectestimation.backend.srs.dto.SrsDto;
import com.projectestimation.backend.srs.service.SrsService;

@Service
public class DarService {

    private static final String REQUIREMENT_ANALYSIS =
            "Requirement Analysis";

    private static final String SOLUTION_DESIGN =
            "Solution Design";

    private final DarRepository darRepository;
    private final OpportunityRepository opportunityRepository;
    private final ProjectScheduleRepository projectScheduleRepository;
    private final ProjectTeamRepository projectTeamRepository;
    private final SrsService srsService;
    private final SddService sddService;
    private final GeminiDarOrchestrator geminiDarOrchestrator;
    private final ObjectMapper objectMapper;

    public DarService(
            DarRepository darRepository,
            OpportunityRepository opportunityRepository,
            ProjectScheduleRepository projectScheduleRepository,
            ProjectTeamRepository projectTeamRepository,
            SrsService srsService,
            SddService sddService,
            GeminiDarOrchestrator geminiDarOrchestrator,
            ObjectMapper objectMapper
    ) {
        this.darRepository = darRepository;
        this.opportunityRepository = opportunityRepository;
        this.projectScheduleRepository = projectScheduleRepository;
        this.projectTeamRepository = projectTeamRepository;
        this.srsService = srsService;
        this.sddService = sddService;
        this.geminiDarOrchestrator = geminiDarOrchestrator;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public DarResponseDto generateDar(Long opportunityId) {

        Opportunity opportunity = opportunityRepository.findById(opportunityId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Opportunity not found: " + opportunityId
                ));

        // Read the existing SRS only. Never generate it from DAR.
        SrsDto srs = srsService.getGeneratedSrs(opportunityId);

        if (srs == null) {
            throw new ResourceNotFoundException(
                    "SRS not found for opportunity " + opportunityId
                            + ". Generate SRS before generating DAR."
            );
        }

        // Read the existing SDD only. Never generate it from DAR.
        SddDto sdd = sddService.getGeneratedSdd(opportunityId);

        if (sdd == null) {
            throw new ResourceNotFoundException(
                    "SDD not found for opportunity " + opportunityId
                            + ". Generate SDD before generating DAR."
            );
        }

        ProjectSchedule schedule = projectScheduleRepository
                .findByOpportunityIdWithTasks(opportunityId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Project Schedule not found for opportunity "
                                + opportunityId
                ));

        PhaseSelection phase = determineCurrentPhase(schedule);

        ProjectTeam team = projectTeamRepository
                .findByOpportunityId(opportunityId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Project Team not found for opportunity "
                                + opportunityId
                ));

        List<String> invitees = buildInvitees(team);

        String srsContext = writeJson(srs);
        String sddContext = writeJson(sdd);
        String scheduleContext = buildScheduleContext(schedule);

        DarResponseDto generated = geminiDarOrchestrator.generate(
                opportunityId,
                opportunity.getOpportunityName(),
                srsContext,
                sddContext,
                scheduleContext,
                invitees
        );

        // Prevent a generated phase from contradicting the schedule.
     // The Project Schedule is the source of truth for the phase.

        // The Project Schedule is the source of truth for the phase.
        generated = new DarResponseDto(
                generated.id(),
                generated.opportunityId(),
                phase.phase(),
                generated.phaseJustification(),
                phase.meetingDate(),
                generated.meetingName(),
                invitees,
                generated.agenda(),
                generated.discussionNotes(),
                generated.evaluationCriteria(),
                generated.alternatives(),
                generated.recommendedAlternativeId(),
                generated.recommendationReason(),
                makeSafeFileName(opportunity.getOpportunityName()) + "_DAR.docx",
                generated.createdAt(),
                generated.updatedAt()
        );


        String documentName =
                makeSafeFileName(opportunity.getOpportunityName()) + "_DAR.docx";

        // Keep one current DAR record per opportunity.
        Dar dar = darRepository.findByOpportunityId(opportunityId)
                .orElseGet(Dar::new);

        dar.setOpportunityId(opportunityId);
        dar.setProjectPhase(phase.phase());
        dar.setMeetingDate(phase.meetingDate());
        dar.setMeetingName(generated.meetingName());
        dar.setInvitees(writeJson(invitees));
        dar.setDarContent(writeJson(generated));
        dar.setDocumentName(documentName);

        Dar saved = darRepository.save(dar);

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public DarResponseDto getDar(Long opportunityId) {

        Dar dar = darRepository.findByOpportunityId(opportunityId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "DAR has not been generated for opportunity "
                                + opportunityId
                ));

        return toResponse(dar);
    }

    @Transactional(readOnly = true)
    public byte[] downloadDar(Long opportunityId) throws IOException {

        Dar dar = darRepository.findByOpportunityId(opportunityId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "DAR has not been generated for opportunity "
                                + opportunityId
                ));

        DarResponseDto dto = toResponse(dar);
        String projectName = getProjectName(opportunityId);

        ProjectTeam team = projectTeamRepository
                .findByOpportunityId(opportunityId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Project Team not found for opportunity " + opportunityId
                ));

        String projectManager = team.getProjectManager();
        if (projectManager == null || projectManager.isBlank()) {
            projectManager = "Project Manager";
        }

        try (
                XWPFDocument document = new XWPFDocument();
                ByteArrayOutputStream output = new ByteArrayOutputStream()
        ) {
            // Page setup matching the supplied BEAS Meeting Minutes template.
            var section = document.getDocument().getBody().addNewSectPr();
            var pageSize = section.addNewPgSz();
            pageSize.setW(java.math.BigInteger.valueOf(12240));
            pageSize.setH(java.math.BigInteger.valueOf(15840));
            var margins = section.addNewPgMar();
            margins.setTop(java.math.BigInteger.valueOf(720));
            margins.setBottom(java.math.BigInteger.valueOf(720));
            margins.setLeft(java.math.BigInteger.valueOf(720));
            margins.setRight(java.math.BigInteger.valueOf(720));

            // Put the real BEAS logo in the document body so it appears
            // only once, on the first page (not in a repeating Word header).
            XWPFParagraph logoParagraph = document.createParagraph();
            logoParagraph.setAlignment(ParagraphAlignment.RIGHT);
            logoParagraph.setSpacingBefore(0);
            logoParagraph.setSpacingAfter(100);

            try (InputStream logoStream = getClass().getClassLoader()
                    .getResourceAsStream("psr/beas-logo.png")) {
                if (logoStream == null) {
                    throw new IOException(
                            "BEAS logo not found: src/main/resources/psr/beas-logo.png");
                }

                XWPFRun logoRun = logoParagraph.createRun();
                try {
                    logoRun.addPicture(
                            logoStream,
                            XWPFDocument.PICTURE_TYPE_PNG,
                            "beas-logo.png",
                            Units.toEMU(158),
                            Units.toEMU(24)
                    );
                } catch (org.apache.poi.openxml4j.exceptions.InvalidFormatException e) {
                    throw new IOException("Failed to add BEAS logo to DAR document", e);
                }
            }

            addTitle(document, "Meeting Minutes");

            addHeading(document, "1.1. Meeting Details");

            XWPFTable details = document.createTable(5, 2);
            details.setWidth("100%");
            setTableBorders(details, "D0D0D0", 4);
            setCell(details.getRow(0).getCell(0), "Meeting Name:");
            setCell(details.getRow(0).getCell(1), safeText(dto.meetingName()));
            setCell(details.getRow(1).getCell(0), "Meeting Date:");
            setCell(details.getRow(1).getCell(1), safeText(dto.meetingDate()));
            setCell(details.getRow(2).getCell(0), "Project:");
            setCell(details.getRow(2).getCell(1), projectName);
            setCell(details.getRow(3).getCell(0), "Project Phase:");
            setCell(details.getRow(3).getCell(1), safeText(dto.projectPhase()));
            setCell(details.getRow(4).getCell(0), "Invitees:");
            setCell(details.getRow(4).getCell(1),
                    String.join("\n", safeList(dto.invitees())));
            for (XWPFTableRow row : details.getRows()) {
                styleTableRow(row, false);
                XWPFRun labelRun = row.getCell(0).getParagraphs().get(0).getRuns().isEmpty()
                        ? row.getCell(0).getParagraphs().get(0).createRun()
                        : row.getCell(0).getParagraphs().get(0).getRuns().get(0);
                labelRun.setBold(true);
                labelRun.setColor("C00000");
            }

            addHeading(document, "1.2. Agenda:");
            XWPFTable agendaTable = document.createTable(1, 3);
            agendaTable.setWidth("100%");
            setTableBorders(agendaTable, "000000", 6);
            setCell(agendaTable.getRow(0).getCell(0), "Item:");
            setCell(agendaTable.getRow(0).getCell(1), "Action Items:");
            setCell(agendaTable.getRow(0).getCell(2), "Presenter:");
            styleTableRow(agendaTable.getRow(0), true);

            int agendaNumber = 1;
            for (DarAgendaItemDto item : safeList(dto.agenda())) {
                XWPFTableRow row = agendaTable.createRow();
                setCell(row.getCell(0), String.valueOf(agendaNumber++));
                setCell(
                        row.getCell(1),
                        safeText(item.topic()) + "\n" + safeText(item.description())
                );
                setCell(
                        row.getCell(2),
                        projectManager + "\n(DAR Coordinator)"
                );
                styleTableRow(row, false);
            }

            addHeading(document, "1.3. Discussion Notes:");
            XWPFTable discussionTable = document.createTable(1, 2);
            discussionTable.setWidth("100%");
            setTableBorders(discussionTable, "000000", 6);
            setCell(discussionTable.getRow(0).getCell(0), "Topic");
            setCell(discussionTable.getRow(0).getCell(1), "Discussion and Outcome");
            styleTableRow(discussionTable.getRow(0), true);

            for (DarDiscussionNoteDto note : safeList(dto.discussionNotes())) {
                XWPFTableRow row = discussionTable.createRow();
                setCell(row.getCell(0), safeText(note.topic()));
                setCell(row.getCell(1),
                        "Discussion: " + safeText(note.discussion())
                                + "\nOutcome: " + safeText(note.outcome()));
                styleTableRow(row, false);
            }

            addHeading(document, "1.4 Decision Matrix:");

            var alternatives = safeList(dto.alternatives());
            List<String> criteria = safeList(dto.evaluationCriteria());

            int scoreCount = alternatives.stream()
                    .filter(java.util.Objects::nonNull)
                    .mapToInt(a -> safeList(a.criterionScores()).size())
                    .max()
                    .orElse(0);
            int criterionCount = Math.max(criteria.size(), scoreCount);

            // Solution + Evaluation Method + one column per criterion + Total Score.
            XWPFTable matrix = document.createTable(
                    1,
                    criterionCount + 3
            );
            matrix.setWidth("100%");
            setTableBorders(matrix, "000000", 6);

            XWPFTableRow headerRow = matrix.getRow(0);
            setCell(headerRow.getCell(0), "Solution");
            setCell(headerRow.getCell(1), "Evaluation Method");

            for (int i = 0; i < criterionCount; i++) {
                setCell(
                        headerRow.getCell(i + 2),
                        "Criteria " + (i + 1)
                );
            }

            setCell(
                    headerRow.getCell(criterionCount + 2),
                    "Total Score"
            );
            styleTableRow(headerRow, true);

            for (var alternative : alternatives) {
                XWPFTableRow row = matrix.createRow();

                // Ensure the row has the same number of cells as the header.
                while (row.getTableCells().size() < criterionCount + 3) {
                    row.addNewTableCell();
                }

                setCell(row.getCell(0), safeText(alternative.alternativeName()));
                setCell(row.getCell(1), "Comparison of result");

                var scores = safeList(alternative.criterionScores());
                double weightedTotal = 0;

                for (int i = 0; i < criterionCount; i++) {
                    String scoreValue = "";

                    if (i < scores.size()) {
                        var score = scores.get(i);

                        if (score.score() != null) {
                            scoreValue = String.valueOf(score.score());
                        }

                        if (score.weight() != null && score.score() != null) {
                            double weight = Double.parseDouble(
                                    String.valueOf(score.weight())
                            );
                            double mark = Double.parseDouble(
                                    String.valueOf(score.score())
                            );

                            // The reference matrix uses weighted sum (weight × score),
                            // not a weighted average. Example: 5 × total weight 18 = 90.
                            weightedTotal += weight * mark;
                        }
                    }

                    setCell(row.getCell(i + 2), scoreValue);
                }

                setCell(
                        row.getCell(criterionCount + 2),
                        String.format(
                                java.util.Locale.ROOT,
                                "%.0f",
                                weightedTotal
                        )
                );

                styleTableRow(row, false);
            }

            addHeading(document, "1.5 Evaluation Criteria:");
            XWPFTable criteriaTable = document.createTable(1, 3);
            criteriaTable.setWidth("100%");
            setTableBorders(criteriaTable, "000000", 6);
            setCell(criteriaTable.getRow(0).getCell(0), "No.");
            setCell(criteriaTable.getRow(0).getCell(1), "Criteria");
            setCell(criteriaTable.getRow(0).getCell(2), "Weight");
            styleTableRow(criteriaTable.getRow(0), true);

            List<com.projectestimation.backend.dar.dto.DarCriterionScoreDto> weightScores =
                    alternatives.isEmpty()
                            ? java.util.Collections.emptyList()
                            : alternatives.get(0).criterionScores();
            for (int i = 0; i < criteria.size(); i++) {
                XWPFTableRow row = criteriaTable.createRow();
                setCell(row.getCell(0), String.valueOf(i + 1));
                setCell(row.getCell(1), criteria.get(i));

                String weightText = "";
                for (var score : weightScores) {
                    if (score.criterionName() != null
                            && score.criterionName().trim()
                                    .equalsIgnoreCase(criteria.get(i).trim())
                            && score.weight() != null) {
                        weightText = String.valueOf(score.weight());
                        break;
                    }
                }

                // Criterion labels sometimes differ slightly; use the corresponding
                // score's weight as a fallback when the names do not match.
                if (weightText.isBlank() && i < weightScores.size()) {
                    var score = weightScores.get(i);

                    if (score.weight() != null) {
                        weightText = String.valueOf(score.weight());
                    }
                }
                setCell(row.getCell(2), weightText);
                styleTableRow(row, false);
            }

            addHeading(document, "Recommended Decision");
            addParagraph(document, "Recommended Alternative: "
                    + safeText(dto.recommendedAlternativeId()));
            addParagraph(document, "Recommendation Reason: "
                    + safeText(dto.recommendationReason()));
            addParagraph(document,
                    "Note: This is an AI-generated recommendation and requires review and approval by the project team.");

            document.write(output);
            return output.toByteArray();
        }
    }

    private DarResponseDto toResponse(Dar dar) {
        try {
            DarResponseDto content = objectMapper.readValue(
                    dar.getDarContent(),
                    DarResponseDto.class
            );

            List<String> invitees = objectMapper.readValue(
                    dar.getInvitees(),
                    objectMapper.getTypeFactory()
                            .constructCollectionType(List.class, String.class)
            );

            return new DarResponseDto(
                    dar.getId(),
                    dar.getOpportunityId(),
                    dar.getProjectPhase(),
                    content.phaseJustification(),
                    dar.getMeetingDate(),
                    dar.getMeetingName(),
                    invitees,
                    content.agenda(),
                    content.discussionNotes(),
                    content.evaluationCriteria(),
                    content.alternatives(),
                    content.recommendedAlternativeId(),
                    content.recommendationReason(),
                    dar.getDocumentName(),
                    dar.getCreatedAt(),
                    dar.getUpdatedAt()
            );

        } catch (Exception e) {
            throw new IllegalStateException(
                    "Could not read saved DAR for opportunity "
                            + dar.getOpportunityId(),
                    e
            );
        }
    }

    private PhaseSelection determineCurrentPhase(
            ProjectSchedule schedule
    ) {
        List<ProjectScheduleTask> tasks = safeList(schedule.getTasks());

        if (tasks.isEmpty()) {
            throw new IllegalStateException(
                    "The Project Schedule has no tasks for phase detection."
            );
        }

        ProjectScheduleTask requirementTask = tasks.stream()
                .filter(t -> contains(
                        t.getTaskName(),
                        "requirement analysis"
                ))
                .findFirst()
                .orElse(null);

        ProjectScheduleTask designTask = tasks.stream()
                .filter(t -> contains(
                        t.getTaskName(),
                        "solution design"
                ))
                .findFirst()
                .orElse(null);

        if (requirementTask == null && designTask == null) {
            throw new IllegalStateException(
                    "Requirement Analysis and Solution Design tasks "
                            + "were not found in the Project Schedule."
            );
        }

        // Select the earliest incomplete phase.
        if (requirementTask != null && !isTaskComplete(requirementTask)) {
            return selectPhase(requirementTask, REQUIREMENT_ANALYSIS);
        }

        if (designTask != null && !isTaskComplete(designTask)) {
            return selectPhase(designTask, SOLUTION_DESIGN);
        }

        throw new IllegalStateException(
                "Requirement Analysis and Solution Design are both complete. "
                        + "No active phase is available for DAR generation."
        );
    }

    private PhaseSelection selectPhase(
            ProjectScheduleTask task,
            String phase
    ) {
        LocalDate date = task.getPlannedStartDate();

        if (date == null) {
            date = task.getActualStartDate();
        }

        List<ProjectScheduleTaskBreakdown> breakdowns =
                safeList(task.getTaskBreakdowns());

        if (date == null) {
            date = breakdowns.stream()
                    .map(ProjectScheduleTaskBreakdown::getPlannedStartDate)
                    .filter(d -> d != null)
                    .min(LocalDate::compareTo)
                    .orElse(null);
        }

        if (date == null) {
            date = breakdowns.stream()
                    .map(ProjectScheduleTaskBreakdown::getActualStartDate)
                    .filter(d -> d != null)
                    .min(LocalDate::compareTo)
                    .orElse(null);
        }

        if (date == null) {
            throw new IllegalStateException(
                    "No planned or actual start date is available for "
                            + phase + "."
            );
        }

        return new PhaseSelection(phase, date);
    }

    private boolean isTaskComplete(ProjectScheduleTask task) {
        if (isCompletedStatus(task.getStatus())) {
            return true;
        }

        List<ProjectScheduleTaskBreakdown> breakdowns =
                safeList(task.getTaskBreakdowns());

        if (breakdowns.isEmpty()) {
            return false;
        }

        return breakdowns.stream().allMatch(b ->
                isCompletedStatus(b.getStatus())
                        || (b.getProgress() != null && b.getProgress() >= 100)
        );
    }

    private boolean isCompletedStatus(String status) {
        if (status == null) {
            return false;
        }

        String normalized = status.trim().toLowerCase();

        return normalized.equals("completed")
                || normalized.equals("complete")
                || normalized.equals("done");
    }

    private List<String> buildInvitees(ProjectTeam team) {
        Set<String> invitees = new LinkedHashSet<>();

        addIfPresent(invitees, team.getProjectManager());
        addIfPresent(invitees, team.getTeamLead());
        addIfPresent(invitees, team.getDevelopers());
        addIfPresent(invitees, team.getTester());
        addIfPresent(invitees, team.getDatabaseDevelopers());

        // Admin and HR are deliberately excluded.

        if (invitees.isEmpty()) {
            throw new IllegalStateException(
                    "No eligible project-team invitees were found."
            );
        }

        return new ArrayList<>(invitees);
    }

    private void addIfPresent(Set<String> values, String value) {
        if (value != null && !value.isBlank()) {
            values.add(value.trim());
        }
    }

    private void addIfPresent(
            Set<String> values,
            List<String> valuesToAdd
    ) {
        if (valuesToAdd != null) {
            valuesToAdd.stream()
                    .filter(v -> v != null && !v.isBlank())
                    .map(String::trim)
                    .forEach(values::add);
        }
    }

    private String buildScheduleContext(ProjectSchedule schedule) {
        StringBuilder context = new StringBuilder();

        context.append("Project start date: ")
                .append(schedule.getProjectStartDate())
                .append("\nProject end date: ")
                .append(schedule.getProjectEndDate())
                .append("\nCompleted tasks: ")
                .append(schedule.getCompletedTasks())
                .append("\nTotal tasks: ")
                .append(schedule.getTotalTasks())
                .append("\nTasks:\n");

        for (ProjectScheduleTask task : safeList(schedule.getTasks())) {
            context.append("- Task: ")
                    .append(task.getTaskName())
                    .append(", status=")
                    .append(task.getStatus())
                    .append(", plannedStart=")
                    .append(task.getPlannedStartDate())
                    .append(", plannedEnd=")
                    .append(task.getPlannedEndDate())
                    .append(", actualStart=")
                    .append(task.getActualStartDate())
                    .append(", actualEnd=")
                    .append(task.getActualEndDate())
                    .append("\n");

            for (ProjectScheduleTaskBreakdown breakdown :
                    safeList(task.getTaskBreakdowns())) {
                context.append("  - Breakdown: ")
                        .append(breakdown.getActivityName())
                        .append(", status=")
                        .append(breakdown.getStatus())
                        .append(", progress=")
                        .append(breakdown.getProgress())
                        .append(", plannedStart=")
                        .append(breakdown.getPlannedStartDate())
                        .append(", plannedEnd=")
                        .append(breakdown.getPlannedEndDate())
                        .append(", actualStart=")
                        .append(breakdown.getActualStartDate())
                        .append(", actualEnd=")
                        .append(breakdown.getActualEndDate())
                        .append("\n");
            }
        }

        return context.toString();
    }

    private String getProjectName(Long opportunityId) {
        return opportunityRepository.findById(opportunityId)
                .map(Opportunity::getOpportunityName)
                .orElse("Project");
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Could not serialize DAR data.",
                    e
            );
        }
    }

    private String makeSafeFileName(String name) {
        if (name == null || name.isBlank()) {
            return "Project";
        }

        return name.replaceAll("[\\\\/:*?\"<>|\\s]+", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "");
    }

    private boolean contains(String value, String search) {
        return value != null
                && value.toLowerCase().contains(search.toLowerCase());
    }

    private <T> List<T> safeList(List<T> values) {
        return values == null ? List.of() : values;
    }

    private void addTitle(XWPFDocument document, String title) {
        XWPFParagraph paragraph = document.createParagraph();
        paragraph.setAlignment(ParagraphAlignment.LEFT);
        paragraph.setSpacingAfter(220);
        XWPFRun run = paragraph.createRun();
        run.setBold(true);
        run.setItalic(true);
        run.setFontFamily("Arial");
        run.setFontSize(19);
        run.setColor("000000");
        run.setText(title);
    }

    private void addHeading(XWPFDocument document, String text) {
        XWPFParagraph paragraph = document.createParagraph();
        paragraph.setSpacingBefore(180);
        paragraph.setSpacingAfter(100);
        XWPFRun run = paragraph.createRun();
        run.setBold(true);
        run.setItalic(true);
        run.setFontFamily("Times New Roman");
        run.setFontSize(14);
        run.setColor("C00000");
        run.setText(text);
    }

    private void addSubheading(XWPFDocument document, String text) {
        XWPFParagraph paragraph = document.createParagraph();
        paragraph.setSpacingBefore(80);
        paragraph.setSpacingAfter(50);
        XWPFRun run = paragraph.createRun();
        run.setBold(true);
        run.setFontFamily("Times New Roman");
        run.setFontSize(10);
        run.setColor("000000");
        run.setText(text == null ? "" : text);
    }

    private void addParagraph(XWPFDocument document, String text) {
        XWPFParagraph paragraph = document.createParagraph();
        paragraph.setSpacingAfter(60);
        XWPFRun run = paragraph.createRun();
        run.setFontFamily("Times New Roman");
        run.setFontSize(9);
        run.setColor("000000");
        run.setText(text == null ? "" : text);
    }

    private void addBullet(XWPFDocument document, String text) {
        XWPFParagraph paragraph = document.createParagraph();
        paragraph.setStyle("List Bullet");
        XWPFRun run = paragraph.createRun();
        run.setFontFamily("Times New Roman");
        run.setFontSize(9);
        run.setText(text == null ? "" : text);
    }

    private void setCell(XWPFTableCell cell, String text) {
        while (!cell.getParagraphs().isEmpty()) {
            cell.removeParagraph(0);
        }

        String[] lines = (text == null ? "" : text).split("\\n", -1);
        for (String line : lines) {
            XWPFParagraph paragraph = cell.addParagraph();
            paragraph.setSpacingBefore(40);
            paragraph.setSpacingAfter(60);

            XWPFRun run = paragraph.createRun();
            run.setFontFamily("Times New Roman");
            run.setFontSize(10);
            run.setColor("000000");
            run.setText(line);
        }
    }

    private void styleTableRow(XWPFTableRow row, boolean header) {
        for (XWPFTableCell cell : row.getTableCells()) {
            cell.setVerticalAlignment(XWPFTableCell.XWPFVertAlign.CENTER);

            // Add comfortable padding inside each cell.
            var tcPr = cell.getCTTc().getTcPr();
            if (tcPr == null) {
                tcPr = cell.getCTTc().addNewTcPr();
            }

            var margins = tcPr.isSetTcMar()
                    ? tcPr.getTcMar()
                    : tcPr.addNewTcMar();

            var top = margins.isSetTop() ? margins.getTop() : margins.addNewTop();
            top.setW(java.math.BigInteger.valueOf(120));

            var bottom = margins.isSetBottom()
                    ? margins.getBottom()
                    : margins.addNewBottom();
            bottom.setW(java.math.BigInteger.valueOf(120));

            var left = margins.isSetStart()
                    ? margins.getStart()
                    : margins.addNewStart();
            left.setW(java.math.BigInteger.valueOf(140));

            var right = margins.isSetEnd()
                    ? margins.getEnd()
                    : margins.addNewEnd();
            right.setW(java.math.BigInteger.valueOf(140));

            if (header) {
                shadeCell(cell, "E7E6E6");
            }

            for (XWPFParagraph paragraph : cell.getParagraphs()) {
                paragraph.setSpacingBefore(40);
                paragraph.setSpacingAfter(60);
    
                for (XWPFRun run : paragraph.getRuns()) {
                    run.setFontFamily("Times New Roman");
                    run.setFontSize(10);
                    if (header) {
                        run.setBold(true);
                    }
                }
            }
        }
    }

    private void shadeCell(XWPFTableCell cell, String fill) {
        var tcPr = cell.getCTTc().getTcPr();
        if (tcPr == null) {
            tcPr = cell.getCTTc().addNewTcPr();
        }
        var shd = tcPr.isSetShd() ? tcPr.getShd() : tcPr.addNewShd();
        shd.setFill(fill);
    }

    private void setTableBorders(XWPFTable table, String color, int size) {
        var borders = table.getCTTbl().getTblPr().isSetTblBorders()
                ? table.getCTTbl().getTblPr().getTblBorders()
                : table.getCTTbl().getTblPr().addNewTblBorders();
        for (var edge : new org.openxmlformats.schemas.wordprocessingml.x2006.main.CTBorder[] {
                borders.isSetTop() ? borders.getTop() : borders.addNewTop(),
                borders.isSetLeft() ? borders.getLeft() : borders.addNewLeft(),
                borders.isSetBottom() ? borders.getBottom() : borders.addNewBottom(),
                borders.isSetRight() ? borders.getRight() : borders.addNewRight(),
                borders.isSetInsideH() ? borders.getInsideH() : borders.addNewInsideH(),
                borders.isSetInsideV() ? borders.getInsideV() : borders.addNewInsideV()
        }) {
            edge.setVal(org.openxmlformats.schemas.wordprocessingml.x2006.main.STBorder.SINGLE);
            edge.setSz(java.math.BigInteger.valueOf(size));
            edge.setColor(color);
        }
    }

    private String safeText(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private record PhaseSelection(
            String phase,
            LocalDate meetingDate
    ) {
    }
}
