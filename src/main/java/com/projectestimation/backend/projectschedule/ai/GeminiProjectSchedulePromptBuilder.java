package com.projectestimation.backend.projectschedule.ai;

import org.springframework.stereotype.Component;

import com.projectestimation.backend.estimation.model.EstimationAnalysis;
import com.projectestimation.backend.opportunity.model.Opportunity;

@Component
public class GeminiProjectSchedulePromptBuilder {

    public String build(

            Opportunity opportunity,
            
            EstimationAnalysis analysis,

            String actors,

            String useCases,

            String projectStartDate,

            Integer teamSize,

            Integer workingDays,

            Integer workingHours,

            Integer buffer

    ) {

    	return """
    			You are a Senior IT Project Manager with expertise in enterprise software delivery.

    			Generate a COMPLETE and REALISTIC project schedule STRICTLY as VALID JSON.

    			Return ONLY valid JSON.

    			Do NOT return markdown.

    			Do NOT return explanations.

    			Do NOT wrap the response inside ```.

    			====================================================
    			PROJECT INFORMATION
    			====================================================

    			Project Name:
    			%s

    			Client Name:
    			%s

    			Implementation Type:
    			%s

    			Actor Weight:
    			%d

    			UUCP:
    			%d

    			Technical Complexity Factor:
    			%.2f

    			Environmental Factor:
    			%.2f

    			Use Case Points (UCP):
    			%.2f

    			Estimated Hours Of Effort:
    			%.2f

    			====================================================
    			ACTORS
    			====================================================

    			%s

    			====================================================
    			USE CASES
    			====================================================

    			%s

    			====================================================
    			PROJECT INPUTS
    			====================================================

    			Project Start Date:
    			%s

    			Team Size:
    			%d

    			Working Days Per Week:
    			%d

    			Working Hours Per Day:
    			%d

    			Buffer Percentage:
    			%d

    			====================================================
    			INSTRUCTIONS
    			====================================================

    			1. Analyse the project information, actors and use cases.

    			2. Create a realistic enterprise software implementation schedule.

    			3. Generate all functional and technical implementation tasks.

    			4. Include analysis, design, development, testing, deployment and support activities wherever applicable.

    			5. Arrange tasks in logical execution order.

    			6. Create realistic predecessors.

    			7. Multiple tasks may execute in parallel whenever possible.

    			8. Calculate realistic duration for every task.

    			9. Calculate plannedStartDate.

    			10. Calculate plannedEndDate.

    			11. actualStartDate must be null.

    			12. actualEndDate must be null.

    			13. Initial status must always be PLANNED.

    			14. durationDays should represent the complete project duration.

    			15. estimatedHours should be equal to the estimated effort distributed across all tasks.

    			====================================================
    			OUTPUT FORMAT
    			====================================================

    			{
    			  "durationDays": number,
    			  "totalTasks": number,
    			  "completedTasks": 0,
    			  "criticalTasks": number,
    			  "estimatedHours": number,
    			  "tasks": [
    			    {
    			      "sequence": 1,
    			      "taskName": "",
    			      "duration": number,
    			      "plannedStartDate": "yyyy-MM-dd",
    			      "plannedEndDate": "yyyy-MM-dd",
    			      "actualStartDate": null,
    			      "actualEndDate": null,
    			      "predecessor": "",
    			      "status": "PLANNED"
    			    }
    			  ]
    			}

    			Return ONLY valid JSON.
    			"""
    			.formatted(

    			        opportunity.getOpportunityName(),

    			        opportunity.getClientName(),

    			        opportunity.getImplementationType().name(),

    			        analysis.getActorWeight(),

    			        analysis.getUucp(),

    			        analysis.getTcf(),

    			        analysis.getEf(),

    			        analysis.getUcp(),

    			        analysis.getHoursOfEffort(),

    			        actors,

    			        useCases,

    			        projectStartDate,

    			        teamSize,

    			        workingDays,

    			        workingHours,

    			        buffer

    			);

    }

}