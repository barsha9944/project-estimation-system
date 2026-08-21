package com.projectestimation.backend.psr.ai;

import java.util.List;

import org.springframework.stereotype.Component;

import com.projectestimation.backend.psr.dto.PsrActivityDto;
import com.projectestimation.backend.psr.dto.PsrContentDto;

@Component
public class GeminiPsrPromptBuilder {

    public String build(
            String opportunityName,
            PsrContentDto content
    ) {

        StringBuilder prompt = new StringBuilder();

        prompt.append("""
                You are an experienced IT Project Manager preparing a formal
                Project Status Report (PSR).

                Generate ONLY the final PSR document in VALID MARKDOWN.

                IMPORTANT RULES:

                1. Do NOT return JSON.
                2. Do NOT return explanations.
                3. Do NOT wrap the response in markdown code fences.
                4. Do NOT invent activities, tasks, statuses, risks or other
                   project information.
                5. Use ONLY the information supplied in this prompt.
                6. Preserve the supplied activity sequence, task name,
                   activity name and status.
                7. Activities Performed must contain ONLY the activities
                   supplied under ACTIVITIES PERFORMED.
                8. Next Week Planned Activities must contain ONLY the
                   activities supplied under NEXT WEEK PLANNED ACTIVITIES.
                9. Risk Status must use the supplied Risk Status exactly.
                10. Training of Project Team Members must remain blank when
                    no information is supplied.
                11. Issues Which Need Management Attention must remain blank
                    when no information is supplied.

                ====================================================
                PROJECT INFORMATION
                ====================================================

                Project Name:
                """);

        prompt.append(safe(opportunityName));

        prompt.append("""

                ====================================================
                ACTIVITIES PERFORMED
                ====================================================

                """);

        appendActivities(
                prompt,
                content.getActivitiesPerformed()
        );

        prompt.append("""

                ====================================================
                NEXT WEEK PLANNED ACTIVITIES
                ====================================================

                """);

        appendActivities(
                prompt,
                content.getNextWeekPlannedActivities()
        );

        prompt.append("""
                ====================================================
                FIXED RISK STATUS
                ====================================================

                The following Risk Status table is FIXED.

                You MUST reproduce this table exactly.
                Do NOT modify the wording.
                Do NOT add rows.
                Do NOT remove rows.
                Do NOT change the values.
                Do NOT summarize the table.

                | Sl.No. | Risk Identified | Probability of occurrence | Impact of Risk | Suggested Mitigation Strategy | Status |
                |---:|---|:---:|:---:|---|---|
                | 1 | Frequent changes in requirements | L | M | Arrange for better internet connection | Closed - No change request |
                | 2 | Non-availability of Project Manager (illness, absence, etc.) | M | M | PM has to discuss with the client and convince him with the fact that any major change will be treated as 'Change Request' which needs extra effort and cost | Till date no issue with the Availability of the Project Managers |
                | 3 | Absence of team member(s) | L | M | Delegation of responsibility | Till date team member are available on full time basis. |
                | 4 | Prevent Unauthorized access on files | H | M | DAR meeting was conducted and it was decided to adopt Spring Security measure to prevent unauthorized access | Closed |
                | 5 | Non-availability of high speed internet connection | H | L | Arrange for better internet connection | Sometime downtime occured but it was overcomed. For the time being there is as such no issue with the network. |
                | 6 | Unknown Payment gateway for Upgrade user | H | M | Try to convince client to use authorize.net as payment gateway | Closed |
                | 7 | Unknown Cloud server | H | L | Self-Learning | Closed- Upload to Azure cloud completed quite well. |

                """);

        prompt.append(
                content.getRiskStatus() == null
                        ? ""
                        : content.getRiskStatus()
        );

        prompt.append("""

                ====================================================
                TRAINING OF PROJECT TEAM MEMBERS
                ====================================================

                """);

        prompt.append(
                content.getTrainingOfProjectTeamMembers() == null
                        ? ""
                        : content.getTrainingOfProjectTeamMembers()
        );

        prompt.append("""

                ====================================================
                ISSUES WHICH NEED MANAGEMENT ATTENTION
                ====================================================

                """);

        prompt.append(
                content.getIssuesManagementAttention() == null
                        ? ""
                        : content.getIssuesManagementAttention()
        );

        prompt.append("""
                ====================================================
                DOCUMENT STRUCTURE
                ====================================================

                Create the report with the following sections:

                # Project Status Report

                ## Project Name

                ## Activities Performed

                ## Next Week Planned Activities

                ## Risk Status

                ## Training of Project Team Members

                ## Issues Which Need Management Attention

                ====================================================
                ACTIVITIES PERFORMED FORMAT
                ====================================================

                Activities Performed MUST be presented using exactly this
                table structure:

                | Project Task | Completed | In progressing | Pending | % | Expected planned date | Actual finished date |
                |---|---|---|---|---:|---|---|

                For every activity:

                - Put the task name under Project Task.
                - If status is Completed, put ✓ only under Completed.
                - If status is In Progress or In Progressing, put ✓ only under In progressing.
                - If status is Pending or Not Started, put ✓ only under Pending.
                - Put the supplied progress percentage in the % column.
                - Put the supplied planned end date in Expected planned date.
                - Put the supplied actual end date in Actual finished date.
                - Do not calculate or invent progress percentages.
                - Do not calculate or invent dates.
                - Do not add Sequence as a column.
                - Do not add Activity as a separate column.
                - Do not invent project activities.
                - Use only the supplied activity data.

                ====================================================
                NEXT WEEK PLANNED ACTIVITIES FORMAT
                ====================================================

                Next Week Planned Activities must also be presented
                as a clear Markdown table.

                Use only the supplied activities.

                Do not invent activities.

                ====================================================
                RISK STATUS
                ====================================================

                The Risk Status table is fixed.

                Reproduce the supplied Risk Status exactly.

                Do not modify, summarize, add or remove any risk.

                ====================================================
                EMPTY SECTIONS
                ====================================================

                Training of Project Team Members must remain blank if
                no information is supplied.

                Issues Which Need Management Attention must remain blank
                if no information is supplied.

                Return ONLY the final Markdown document.

                """);

        return prompt.toString();
    }


    private void appendActivities(
        StringBuilder prompt,
        java.util.List<PsrActivityDto> activities
) {

    if (activities == null || activities.isEmpty()) {
        prompt.append("No activities.\n");
        return;
    }

    prompt.append("""
            | Project Task | Completed | In progressing | Pending | % | Expected planned date | Actual finished date |
            |---|---|---|---|---:|---|---|
            """);

    for (PsrActivityDto activity : activities) {

        String status = safe(activity.getStatus());

        String completed = "";
        String inProgress = "";
        String pending = "";

        if ("Completed".equalsIgnoreCase(status)) {
            completed = "✓";
        } else if ("In Progress".equalsIgnoreCase(status)
                || "In Progressing".equalsIgnoreCase(status)) {
            inProgress = "✓";
        } else if ("Pending".equalsIgnoreCase(status)
                || "Not Started".equalsIgnoreCase(status)) {
            pending = "✓";
        }

        prompt.append("| ")
                .append(safe(activity.getTaskName()));

        prompt.append(" | ")
                .append(completed);

        prompt.append(" | ")
                .append(inProgress);

        prompt.append(" | ")
                .append(pending);

        prompt.append(" | ")
                .append(
                        activity.getProgress() == null
                                ? ""
                                : activity.getProgress()
                );

        prompt.append(" | ")
                .append(safe(activity.getPlannedEndDate()));

        prompt.append(" | ")
                .append(safe(activity.getActualEndDate()));

        prompt.append(" |\n");
    }
}


    private String safe(String value) {

        return value == null ? "" : value;
    }
}