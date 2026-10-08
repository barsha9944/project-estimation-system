package com.projectestimation.backend.momAI;

import java.time.LocalDate;

import org.springframework.stereotype.Service;

import com.projectestimation.backend.common.ai.GeminiClient;

@Service
public class GeminiMomOrchestrator {

    private static final int MAX_OUTPUT_TOKENS = 8192;

    private final GeminiClient geminiClient;

    public GeminiMomOrchestrator(
            GeminiClient geminiClient
    ) {
        this.geminiClient = geminiClient;
    }

    public String generate(
            String projectName,
            String clientName,
            String meetingType,
            int meetingSequence,
            LocalDate meetingDate,
            String projectInformation,
            String previousMeetingInformation
    ) {

        String prompt = """
                You are an experienced software project manager.

                Your task is to generate a formal Minutes of Meeting (MOM)
                for the project described below.

                ============================================================
                PROJECT INFORMATION
                ============================================================

                Project Name:
                %s

                Client Name:
                %s

                Project Information:
                %s

                ============================================================
                MEETING INFORMATION
                ============================================================

                Meeting Type:
                %s

                Meeting Sequence:
                %d

                Meeting Date:
                %s

                ============================================================
                PREVIOUS MEETING INFORMATION
                ============================================================

                %s

                ============================================================
                MEETING SEQUENCE RULES
                ============================================================

                Meeting 1:
                KICKOFF

                The kickoff meeting occurs only once at the beginning
                of the project.

                After the kickoff meeting, meetings occur every 7 days
                in the following repeating sequence:

                TEAM
                TEAM
                CLIENT
                SENIOR_MANAGEMENT

                Then repeat:

                TEAM
                TEAM
                CLIENT
                SENIOR_MANAGEMENT

                Continue this sequence until the project end date.

                ============================================================
                MEETING PURPOSE
                ============================================================

                KICKOFF:
                Cover project introduction, objectives, scope, deliverables,
                responsibilities, expectations, communication, risks and
                project execution approach.

                TEAM:
                Cover current project progress, completed work, ongoing work,
                upcoming work, technical matters, blockers, testing,
                resources, risks and action items.

                CLIENT:
                Cover project progress with the client, requirements,
                clarifications, feedback, deliverables, issues, acceptance
                and client decisions.

                SENIOR_MANAGEMENT:
                Cover project health, progress, schedule, effort, quality,
                risks, issues, resources, escalations and management
                decisions.

                ============================================================
                IMPORTANT RULES
                ============================================================

                1. Generate both agenda and discussion notes.

                2. The content must be specific to the supplied project.

                3. Do not invent specific project facts that are not present
                   in the project information.

                4. Maintain continuity with the previous meeting.

                5. If previous meeting information contains action items,
                   decisions or unresolved issues, use them appropriately
                   in the current meeting.

                6. Do not repeat exactly the same discussion notes in
                   every meeting.

                7. Meeting content should evolve as the project progresses.

                8. Keep the language professional and suitable for an
                   official project document.

                9. Agenda items must be relevant to the meeting type.

                10. Discussion notes must explain what was discussed.

                11. Do not create information unrelated to the project.

                12. Return ONLY valid JSON.

                ============================================================
                REQUIRED JSON FORMAT
                ============================================================

                {
                  "meetingName": "",
                  "meetingTime": "",
                  "meetingLocation": "",
                  "invitees": [
                    ""
                  ],
                  "recordedBy": "",
                  "circulation": "",
                  "agenda": [
                    {
                      "item": 1,
                      "actionItems": "",
                      "presenter": ""
                    }
                  ],
                  "discussionNotes": [
                    ""
                  ]
                }

                Do not add any additional JSON fields.
                """.formatted(
                safe(projectName),
                safe(clientName),
                safe(projectInformation),
                safe(meetingType),
                meetingSequence,
                meetingDate,
                safe(previousMeetingInformation)
        );

        return geminiClient.generateJsonContent(
                prompt,
                MAX_OUTPUT_TOKENS
        );
    }

    private String safe(String value) {

        if (value == null || value.isBlank()) {
            return "Not Available";
        }

        return value;
    }
}