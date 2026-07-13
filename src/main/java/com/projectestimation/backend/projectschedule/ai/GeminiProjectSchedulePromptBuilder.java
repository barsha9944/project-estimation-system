package com.projectestimation.backend.projectschedule.ai;

import org.springframework.stereotype.Component;

@Component
public class GeminiProjectSchedulePromptBuilder {

    public String build(

            String proposal,

            String actors,

            String useCases,

            String projectStartDate,

            Integer teamSize,

            Integer workingDays,

            Integer workingHours,

            Integer buffer

    ) {

    	return """
    			You are an expert IT Project Manager and Project Planner.

    			Generate a COMPLETE project schedule STRICTLY as VALID JSON.

    			DO NOT return markdown.

    			DO NOT return HTML.

    			DO NOT return explanations.

    			Return ONLY valid JSON.

    			PROJECT INFORMATION

    			Proposal:
    			%s

    			Actors:
    			%s

    			Use Cases:
    			%s

    			PROJECT INPUTS

    			Project Start Date : %s

    			Team Size : %d

    			Working Days Per Week : %d

    			Working Hours Per Day : %d

    			Buffer Percentage : %d

    			INSTRUCTIONS

    			1. Analyse the proposal, actors and use cases.

    			2. Identify ALL implementation activities required.

    			3. Create realistic enterprise software implementation tasks.

    			4. Arrange tasks in execution order.

    			5. Generate predecessor values.

    			6. Calculate duration for every task.

    			7. Calculate planned start date.

    			8. Calculate planned end date.

    			9. Initial status must be PLANNED.

    			10. actualStartDate must be null.

    			11. actualEndDate must be null.

    			12. Return summary information.

    			OUTPUT FORMAT

    			{
    			  "durationDays": number,

    			  "totalTasks": number,

    			  "completedTasks": 0,

    			  "criticalTasks": number,

    			  "estimatedHours": number,

    			  "tasks":[
    			     {
    			        "sequence":1,

    			        "taskName":"",

    			        "plannedStartDate":"yyyy-MM-dd",

    			        "plannedEndDate":"yyyy-MM-dd",

    			        "actualStartDate":null,

    			        "actualEndDate":null,

    			        "predecessor":"",

    			        "status":"PLANNED"
    			     }
    			  ]
    			}

    			IMPORTANT

    			Return ONLY JSON.

    			Do not wrap inside ```.

    			Do not explain anything.

    			""".formatted(

    			proposal,

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