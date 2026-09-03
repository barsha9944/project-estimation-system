package com.projectestimation.backend.psr.ai;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.stereotype.Component;

import com.projectestimation.backend.psr.dto.PsrActivityDto;
import com.projectestimation.backend.psr.dto.PsrContentDto;

@Component
public class GeminiPsrPromptBuilder {

    public String build(
            String opportunityName,
            PsrContentDto content,
            int psrVersion
    ) {

        StringBuilder prompt = new StringBuilder();

        // ============================================================
        // SYSTEM INSTRUCTIONS
        // ============================================================

        prompt.append("""
                You are an experienced IT Project Manager preparing a formal
                Project Status Report (PSR).

                Generate ONLY the final PSR document.

                IMPORTANT RULES:

                1. Do NOT return JSON.
                2. Do NOT return explanations.
                3. Do NOT wrap the response in markdown code fences.
                4. Do NOT invent activities, tasks, statuses, risks or other
                   project information.
                5. Use ONLY the information supplied in this prompt.
                6. Preserve the supplied activity sequence, task name,
                   activity name and status.
                7. Activities during the Period must contain ONLY the
                   activities supplied under ACTIVITIES PERFORMED.
                8. Next 15 Days Planned Activities must contain ONLY the
        		   activities supplied under NEXT 15 DAYS PLANNED ACTIVITIES.
                9. Risk Status must use the fixed Risk Status table supplied
                   in this prompt.
                10. Training of Project Team Members must always be present
                    as the specified blank table.
                11. Issues Which Need Management Attention must always be
                    present as a blank section.
                12. Nothing must appear after Issues Which Need Management
                    Attention.
                13. Do NOT create an additional Project Name section.
                14. Do NOT change the order of the PSR sections.
                15. Do NOT add any section that is not explicitly requested.

                ============================================================
                OUTPUT FORMAT
                ============================================================

                Use Markdown for the document.

                IMPORTANT:

                - Do NOT use HTML.
                - Do NOT use <table>.
                - Do NOT use <tr>.
                - Do NOT use <td>.
                - Do NOT use <div>.
                - Do NOT use HTML tags anywhere.
                - Use Markdown tables only for the actual report tables.
                - The PSR header must NOT be a Markdown table.
                - The PSR header must be simple left-aligned text.

                ============================================================
                PSR HEADER
                ============================================================

                The PSR header must be the FIRST content in the document.

                Use exactly this structure:

                Project Status Report
                (BEAS/PM/Frm/01 Version X.0)

                Reported by: Manas Chattapadhay

                Project Code:

                Project Name:
                <project name>

                Period of Reporting:
                <start date> – <end date>

                Periodicity:
                15 days

                Date:
                <date>

                IMPORTANT:

                - Project Status Report must be on its own line.
                - The version must be on the next line.
                - Reported by must remain blank.
                - Project Code must remain blank.
                - Project Name must contain the supplied project name.
                - Period of Reporting must contain the supplied dates.
                - Periodicity must be exactly 15 days.
                - Date must contain the supplied report date.
                - Do NOT combine all header information into one paragraph.
                - Do NOT use a Markdown table for the header.
                - Do NOT use HTML for the header.
                - Do NOT add "PSR Header" as visible text.

                ============================================================
                """);

        // ============================================================
        // PSR HEADER
        // ============================================================

        prompt.append("Project Status Report\n");

        prompt.append("(BEAS/PM/Frm/01 Version ");

        prompt.append(psrVersion);

        prompt.append(".0)\n\n");

        // ------------------------------------------------------------
        // REPORTED BY
        // ------------------------------------------------------------

        prompt.append("Reported by:\n\n");

        // ------------------------------------------------------------
        // PROJECT CODE
        // ------------------------------------------------------------

        prompt.append("Project Code:\n\n");

        // ------------------------------------------------------------
        // PROJECT NAME
        // ------------------------------------------------------------

        prompt.append("Project Name:\n");

        prompt.append(
                escapeMarkdown(
                        safe(opportunityName)
                )
        );

        prompt.append("\n\n");

        // ------------------------------------------------------------
        // PERIOD OF REPORTING
        // ------------------------------------------------------------

        prompt.append("Period of Reporting:\n");

        prompt.append(
                formatDate(
                        content.getReportingStartDate()
                )
        );

        prompt.append(" – ");

        prompt.append(
                formatDate(
                        content.getReportingEndDate()
                )
        );

        prompt.append("\n\n");

        // ------------------------------------------------------------
        // PERIODICITY
        // ------------------------------------------------------------

        prompt.append("Periodicity:\n");

        prompt.append("15 days\n\n");

        // ------------------------------------------------------------
        // DATE
        // ------------------------------------------------------------

        prompt.append("Date:\n");

        prompt.append(
                formatDate(
                        content.getReportDate()
                )
        );

        prompt.append("\n\n");

        // ============================================================
        // ACTIVITIES DURING THE PERIOD
        // ============================================================

        prompt.append("""
                ============================================================
                ACTIVITIES DURING THE PERIOD
                ============================================================

                Create the Activities during the Period section using a
                Markdown table.

                Do NOT invent activities.

                Use ONLY the activities supplied below.

                """);

        appendActivities(
                prompt,
                content.getActivitiesPerformed()
        );

     // ============================================================
     // NEXT 15 DAYS PLANNED ACTIVITIES
     // ============================================================

     prompt.append("""
             ============================================================
             NEXT 15 DAYS PLANNED ACTIVITIES
             ============================================================

             Create the Next 15 Days Planned Activities section using
             the following Markdown table structure.

             | Sl. no. | Activity | Planned Duration (W/Days) | Planned Start | Planned Finish |
             |---:|---|---|---|---|

             Do NOT invent activities.

             Use ONLY the supplied next 15 days activities.

             """);

     appendNext15DaysActivities(
             prompt,
             content.getNextWeekPlannedActivities()
     );

        // ============================================================
        // RISK STATUS
        // ============================================================

        prompt.append("""
                ============================================================
                RISK STATUS
                ============================================================

                Reproduce the following Risk Status table exactly.

                Do NOT modify the wording.
                Do NOT add rows.
                Do NOT remove rows.
                Do NOT change the values.
                Do NOT summarize the table.
                Do NOT create another Risk Status table.

                | Sl.No. | Risk Identified | Probability of occurrence | Impact of Risk | Suggested Mitigation Strategy | Status |
                |---:|---|:---:|:---:|---|---|
                | 1 | Frequent changes in requirements | L | M | Arrange for better internet connection | Closed - No change request |
                | 2 | Non-availability of Project Manager (illness, absence, etc.) | M | M | PM has to discuss with the client and convince him with the fact that any major change will be treated as 'Change Request' which needs extra effort and cost | Till date no issue with the Availability of the Project Managers |
                | 3 | Absence of team member(s) | L | M | Delegation of responsibility | Till date team member are available on full time basis. |
                | 4 | Prevent Unauthorized access on files | H | M | DAR meeting was conducted and it was decided to adopt Spring Security measure to prevent unauthorized access | Closed |
                | 5 | Non-availability of high speed internet connection | H | L | Arrange for better internet connection | Sometime downtime occured but it was overcomed. For the time being there is as such no issue with the network. |
                | 6 | Unknown Payment gateway for Upgrade user | H | M | Try to convince client to use authorize.net as payment gateway | Closed |
                | 7 | Unknown Cloud server | H | L | Self-Learning | Closed- Upload to Azure cloud completed quite well. |

                ============================================================
                TRAINING OF PROJECT TEAM MEMBERS
                ============================================================

                This section MUST always be present.

                Use exactly this blank table:

                | Sl. No. | Training need Identified | Planned Training Hours | Actual Training Hours | Planned Nominations | Actual Nominations | Waiver obtained, if any |
                |---:|---|---:|---:|---:|---:|---|
                | | | | | | | |

                IMPORTANT:

                - Keep the table present.
                - Keep the data row blank.
                - Do NOT populate any values.
                - Do NOT invent training information.
                - Do NOT add additional rows.
                - Do NOT remove the blank row.

                ============================================================
                ISSUES WHICH NEED MANAGEMENT ATTENTION
                ============================================================

                This section MUST always be present.

                Keep the section blank.

                Do NOT add any issues.

                Do NOT add numbered items.

                Do NOT add bullet points.

                Do NOT add explanations.

                Do NOT add Information to Share.

                Do NOT add any section after this section.

                The document must END after the Issues Which Need
                Management Attention section.

                ============================================================
                END OF PSR
                ============================================================

                """);

        return prompt.toString();
    }


    // ============================================================
    // ACTIVITIES PERFORMED
    // ============================================================

    private void appendActivities(
            StringBuilder prompt,
            List<PsrActivityDto> activities
    ) {

        prompt.append("""
                ## Activities during the Period

                | Project Task | Completed | In progressing | Pending | % | Expected planned date | Actual finished date |
                |---|---|---|---|---:|---|---|
                """);

        if (
                activities == null
                || activities.isEmpty()
        ) {

            prompt.append(
                    "| | | | | | | |\n\n"
            );

            return;
        }

        for (PsrActivityDto activity : activities) {

            String status =
                    safe(
                            activity.getStatus()
                    );

            String completed = "";
            String inProgress = "";
            String pending = "";

            // ========================================================
            // COMPLETED
            // ========================================================

            if (
                    "Completed".equalsIgnoreCase(status)
                    || "Complete".equalsIgnoreCase(status)
                    || "Done".equalsIgnoreCase(status)
            ) {

                completed = "✓";
            }

            // ========================================================
            // IN PROGRESS
            // ========================================================

            else if (
                    "In Progress".equalsIgnoreCase(status)
                    || "In Progressing".equalsIgnoreCase(status)
                    || "In-Progress".equalsIgnoreCase(status)
            ) {

                inProgress = "✓";
            }

            // ========================================================
            // PENDING / NOT STARTED
            // ========================================================

            else if (
                    "Pending".equalsIgnoreCase(status)
                    || "Not Started".equalsIgnoreCase(status)
            ) {

                pending = "✓";
            }

            prompt.append("| ");

            prompt.append(
                    escapeMarkdown(
                            safe(
                                    activity.getTaskName()
                            )
                    )
            );

            prompt.append(" | ");

            prompt.append(completed);

            prompt.append(" | ");

            prompt.append(inProgress);

            prompt.append(" | ");

            prompt.append(pending);

            prompt.append(" | ");

            if (activity.getProgress() != null) {

                prompt.append(
                        activity.getProgress()
                );
            }

            prompt.append(" | ");

            prompt.append(
                    escapeMarkdown(
                            safe(
                                    activity.getPlannedEndDate()
                            )
                    )
            );

            prompt.append(" | ");

            prompt.append(
                    escapeMarkdown(
                            safe(
                                    activity.getActualEndDate()
                            )
                    )
            );

            prompt.append(" |\n");
        }

        prompt.append("\n");
    }


    // ============================================================
// NEXT 15 DAYS PLANNED ACTIVITIES
// ============================================================

private void appendNext15DaysActivities(
        StringBuilder prompt,
        List<PsrActivityDto> activities
) {

    prompt.append("""
            ## Next 15 Days Planned Activities

            | Sl. no. | Activity | Planned Duration (W/Days) | Planned Start | Planned Finish |
            |---:|---|---|---|---|
            """);

    if (
            activities == null
            || activities.isEmpty()
    ) {

        prompt.append(
                "| | | | | |\n\n"
        );

        return;
    }

    int serialNumber = 1;

    for (PsrActivityDto activity : activities) {

        prompt.append("| ");

        prompt.append(serialNumber++);

        prompt.append(" | ");

        prompt.append(
                escapeMarkdown(
                        safe(
                                activity.getActivityName()
                        )
                )
        );

        prompt.append(" | ");

        prompt.append(
                activity.getDuration() != null
                        ? activity.getDuration()
                        : ""
        );

        prompt.append(" | ");

        prompt.append(
                escapeMarkdown(
                        safe(
                                activity.getPlannedStartDate()
                        )
                )
        );

        prompt.append(" | ");

        prompt.append(
                escapeMarkdown(
                        safe(
                                activity.getPlannedEndDate()
                        )
                )
        );

        prompt.append(" |\n");
    }

    prompt.append("\n");
}


    // ============================================================
    // DATE FORMATTER
    // ============================================================

    private String formatDate(
            String date
    ) {

        if (
                date == null
                || date.isBlank()
        ) {

            return "";
        }

        try {

            return LocalDate
                    .parse(date)
                    .format(
                            DateTimeFormatter.ofPattern(
                                    "dd-MMMM-yyyy"
                            )
                    );

        } catch (Exception ex) {

            return date;
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


    // ============================================================
    // MARKDOWN ESCAPE
    // ============================================================

    private String escapeMarkdown(
            String value
    ) {

        if (value == null) {

            return "";
        }

        return value
                .replace("|", "\\|")
                .replace("\n", " ")
                .replace("\r", " ");
    }
}