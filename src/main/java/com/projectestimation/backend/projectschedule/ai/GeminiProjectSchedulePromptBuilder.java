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

            Integer buffer,
            
            Integer durationDays,
            
            Double estimatedHours

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

    			8. Distribute the work across tasks so that the sum of all task durations fits exactly within the required project duration.

    			9. Calculate plannedStartDate.

    			10. Calculate plannedEndDate.

    			11. actualStartDate = plannedStartDate for every task.

				12. actualEndDate = plannedEndDate for every task.
				
				13. These values represent the initial baseline schedule before execution begins.
				
				14. Initial status must always be PLANNED.

    			15. PROJECT DURATION

				The total project duration MUST be exactly %d working days.
				
				Distribute all tasks so that:
				
				- The first task starts on the given Project Start Date.
				- The final task ends exactly after %d working days.
				- Respect the working days per week.
				- Tasks may run in parallel whenever appropriate.
				- Do not exceed or shorten the total project duration.

    			16. estimatedHours should be equal to the estimated effort distributed across all tasks.
    			
    			17. Every task must contain a taskBreakdowns array.

				18. For Requirement Gathering, Documentation, Analysis and Design tasks, generate exactly two task breakdowns:
				   - Documentation
				   - Review
				
				19. For Development or Coding tasks, generate exactly three task breakdowns:
				   - Coding
				   - Code Review
				   - Unit Testing
				
				20. For System Integration Testing (SIT) and User Acceptance Testing (UAT), generate exactly two task breakdowns:

				   - Testing
				   - Debugging
				
				   Rules:
				
				   - Testing must always start on the parent task plannedStartDate.
				   - Debugging must start immediately after Testing ends.
				   - The parent task plannedEndDate must equal the Debugging plannedEndDate.
				   - The sum of the Testing and Debugging durations must exactly equal the parent task duration.
				   - Allocate approximately 70%% of the parent task duration to Testing and 30%% to Debugging.
				   - If rounding is required, allocate the remaining day to Testing.
				   - Neither Testing nor Debugging may have a duration of zero.
				   - Do not generate any other task breakdowns.
				
				21. The sum of the task breakdown durations must always equal the parent task duration.
				
				22. Distribute the TOTAL PROJECT EFFORT approximately as follows:

					• Analysis and Design activities: 30%%
					
					• Development (Coding) activities: 50%%
					
					• System Integration Testing (SIT): 10%%
					
					• User Acceptance Testing (UAT): 5%%
					
					• Reviews (Documentation Review, Code Review, General Review) and Unit Testing together: 5%%
					
				23. These percentages apply to the ENTIRE PROJECT, not to each individual task.
					
				24. While generating tasks and task breakdowns, ensure the overall schedule follows these percentages as closely as possible.
					
				25. If rounding leaves any remaining effort, allocate it to SIT and UAT while keeping the overall project duration unchanged.
					
				26. Do not generate equal duration tasks. Allocate effort realistically according to project complexity.
					
				27. Task breakdowns must execute sequentially within the parent task.

					The first task breakdown must always begin on the parent task plannedStartDate.
					
					The last task breakdown must always end on the parent task plannedEndDate.
					
					There must be no gaps or overlaps between task breakdowns.
					
					The combined duration of all task breakdowns must exactly equal the parent task duration.
					
					No task breakdown may have a duration of zero.

					For example:
					
					Requirement Analysis
					  Documentation
					  Review
					
					User Module Development
					  Coding
					  Code Review
					  Unit Testing
					  
					
					The first task breakdown starts on the parent task start date and the last task breakdown ends on the parent task end date.
					
				
				28. Every taskName must be concise and contain a maximum of 3 to 4 words.

					Examples:
					- Requirement Analysis
					- Solution Design
					- Database Design
					- User Module Development
					- API Development
					- Payment Integration
					- System Integration Testing
					- User Acceptance Testing
					- Production Deployment
					- Project Closure
					
					Do not generate long descriptive task names or sentences.

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
				      "plannedTaskStartDate": "yyyy-MM-dd",
				      "plannedTaskEndDate": "yyyy-MM-dd",
				      "actualStartDate": "yyyy-MM-dd",
				      "actualEndDate": "yyyy-MM-dd",
				      "predecessor": "",
				      "status": "PLANNED",
				      "taskBreakdowns": [
				        {
				          "activityName": "",
				          "duration": number,
				          "plannedStartDate": "yyyy-MM-dd",
				      	  "plannedEndDate": "yyyy-MM-dd",
				          "actualStartDate": null,
				      	  "actualEndDate": null,
				          
				        }
				      ]
				    }
				  ]
				}

    			Return ONLY valid JSON.
    			
    			
    			Validation Rules

				- durationDays MUST equal the required project duration.
				- The last task plannedEndDate must correspond to the required project duration.
				- Do not return a schedule longer or shorter than the required duration.
				- If parallel tasks are created, ensure the overall project still completes within the required duration.
				- Every parent task plannedStartDate must equal the first task breakdown plannedStartDate.
				- Every parent task plannedEndDate must equal the last task breakdown plannedEndDate.
				- The sum of all task breakdown durations must equal the parent task duration.
				- No task breakdown may have a duration of zero.
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

    			        buffer,
    			        
    			        durationDays,
    			        
    			        durationDays

    			);

    }

}