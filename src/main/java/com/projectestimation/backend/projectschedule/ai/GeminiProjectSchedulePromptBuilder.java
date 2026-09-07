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

1. PROJECT ANALYSIS

   Analyse the project information, actors and use cases.

2. TASK GENERATION

   Create a realistic enterprise software implementation
   schedule containing all relevant functional and technical
   implementation tasks.

   Include analysis, design, development, testing, deployment
   and support activities wherever applicable.

3. TASK ORDER

   Arrange tasks in a logical execution order.

   Tasks may execute in parallel when there is no dependency
   between them.

4. TASK PREDECESSORS

   Do NOT create predecessors at the parent task level.

   The parent task "predecessor" field must always be "".

   All actual dependency relationships must be defined at
   the task breakdown/activity level.

5. WORKING DAYS

   The schedule uses a 5-day working week.

   Monday, Tuesday, Wednesday, Thursday and Friday are working
   days.

   Saturday and Sunday are non-working days.

   NEVER schedule any task or task breakdown on Saturday
   or Sunday.

   NEVER count Saturday or Sunday as a working day.

6. PROJECT START DATE

   Use the Project Start Date supplied in PROJECT INPUTS as
   the first working day of the project.

   The supplied Project Start Date has already been normalized
   by the application to a working day.

7. PROJECT DURATION

   The total project duration MUST be exactly %d working days.

   The first task must start on the supplied Project Start Date.

   The final task must finish on the final working day of the
   required project duration.

   Do not make the project longer or shorter than the required
   duration.

8. TASK DATES

   Calculate plannedTaskStartDate and plannedTaskEndDate for
   every task.

   Every task start and end date must be a working day.

   Task duration must represent the number of working days
   between its start and end dates, inclusive.

9. TASK BREAKDOWNS

   Every task MUST contain a taskBreakdowns array.

   Task breakdowns represent the actual activities performed
   within the parent task.

10. REQUIREMENT, ANALYSIS AND DESIGN BREAKDOWNS

   For Requirement Gathering, Documentation, Analysis and
   Design tasks, generate exactly two task breakdowns:

   - Documentation
   - Review

11. DEVELOPMENT BREAKDOWNS

   For Development or Coding tasks, generate exactly three
   task breakdowns:

   - Coding
   - Code Review
   - Unit Testing

12. SIT AND UAT BREAKDOWNS

   For System Integration Testing (SIT) and User Acceptance
   Testing (UAT), generate exactly two task breakdowns:

   - Testing
   - Debugging

   Rules:

   - Testing must start on the parent task plannedTaskStartDate.
   - Debugging must start on the next working day after Testing.
   - Debugging must end on the parent task plannedTaskEndDate.
   - The Testing and Debugging durations must equal the
     parent task duration when added together.
   - Allocate approximately 70%% of the parent task duration
     to Testing and 30%% to Debugging.
   - If rounding is required, allocate the remaining day
     to Testing.
   - Neither Testing nor Debugging may have a duration of zero.
   - Do not generate any other breakdowns for SIT or UAT.

13. BREAKDOWN SEQUENCE

   Every task breakdown must have a sequence number starting
   from 1 within its parent task.

   Example:

   Task 1
      1.1 Documentation
      1.2 Review

   Task 2
      2.1 Coding
      2.2 Code Review
      2.3 Unit Testing

14. BREAKDOWN ORDER

   Task breakdowns within the same parent task MUST execute
   sequentially.

   The first breakdown must start on the parent task
   plannedTaskStartDate.

   Every following breakdown must start on the next working
   day after the previous breakdown ends.

   There must be no gaps.

   There must be no overlaps.

   The last breakdown must end on the parent task
   plannedTaskEndDate.

15. BREAKDOWN DURATION

   The sum of all task breakdown durations MUST exactly equal
   the parent task duration.

   No task breakdown may have a duration of zero.

16. BREAKDOWN PREDECESSORS

   Predecessors MUST be defined at the task breakdown level.

   Use the format:

   <task sequence>.<breakdown sequence>

   Examples:

   1.1
   1.2
   2.1
   2.2

   A breakdown with no predecessor must use "".

   For sequential breakdowns within the same task:

   - The first breakdown has predecessor "" unless it depends
     on another task.
   - Each subsequent breakdown must reference the immediately
     preceding breakdown.

   Example:

   Task 1
      1.1 Documentation
          predecessor: ""
      1.2 Review
          predecessor: "1.1"

17. CROSS-TASK BREAKDOWN PREDECESSORS

   When an activity depends on an activity from another task,
   its predecessor must reference the specific preceding
   breakdown.

   Example:

   Task 1
      1.1 Documentation
      1.2 Review

   Task 2
      2.1 Coding
          predecessor: "1.2"

   Do NOT reference only the parent task number.

18. PREDECESSOR VALIDITY

   Every predecessor must reference an existing task breakdown.

   A breakdown must never reference itself.

   Do not create circular dependencies.

19. ACTUAL DATES

   For the initial baseline schedule:

   actualStartDate = plannedStartDate

   actualEndDate = plannedEndDate

   actualDuration must equal the working-day duration of the
   breakdown.

20. INITIAL STATUS

   The initial status of every parent task and every task
   breakdown must always be PLANNED.

21. PROJECT EFFORT

   estimatedHours must represent the total estimated effort
   distributed across all tasks and breakdowns.

22. EFFORT DISTRIBUTION

   Distribute the TOTAL PROJECT EFFORT approximately as follows:

   - Analysis and Design activities: 30%%
   - Development / Coding activities: 50%%
   - System Integration Testing: 10%%
   - User Acceptance Testing: 5%%
   - Reviews and Unit Testing: 5%%

23. EFFORT SCOPE

   The percentages in instruction 22 apply to the ENTIRE
   PROJECT, not to each individual task.

24. EFFORT REALISM

   Distribute effort realistically according to project
   complexity.

   Do not generate equal duration for every task.

   If rounding leaves a small amount of remaining effort,
   allocate it reasonably while keeping the total project
   duration unchanged.

25. TASK NAMES

   Every taskName must be concise and contain a maximum of
   3 to 4 words.

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

26. TASK AND BREAKDOWN DATE CONSISTENCY

   Every parent task plannedTaskStartDate MUST equal the
   plannedStartDate of its first breakdown.

   Every parent task plannedTaskEndDate MUST equal the
   plannedEndDate of its last breakdown.

   Every breakdown date must be a working day.

27. PROJECT SCHEDULE CONSISTENCY
- NO task may start after the final working day of the required project duration.
- NO task may end after the final working day of the required project duration.
- NO task breakdown may start after the final working day of the required project duration.
- NO task breakdown may end after the final working day of the required project duration.
- The calculated final working day of the required project duration is a HARD PROJECT BOUNDARY.
- All tasks and task breakdowns MUST fit completely within this boundary.
- If the remaining project duration is insufficient for another task, adjust the task distribution and durations so that all required tasks fit within the project duration while preserving a realistic sequence and dependencies.
- NEVER extend the schedule beyond the required project end date to accommodate additional tasks.

   The complete schedule must satisfy all of the following:

   - First task starts on the project start date.
   - Final task ends on the required project end date.
   - Total project duration equals the required duration.
   - No Saturday or Sunday is scheduled.
   - No breakdown has zero duration.
   - Breakdown durations equal their parent task duration.
   - Breakdown dates are sequential within each parent task.
   - Predecessors reference valid breakdowns.
   - No circular dependencies exist.

28. OUTPUT

   Return the schedule using the exact JSON structure specified
   below.

   Return ONLY valid JSON.

   Do NOT return markdown.

   Do NOT return explanations.

   Do NOT wrap the response inside ```.


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
          "sequence": 1,
          "activityName": "",
          "duration": number,
          "plannedStartDate": "yyyy-MM-dd",
          "plannedEndDate": "yyyy-MM-dd",
          "actualStartDate": "yyyy-MM-dd",
          "actualEndDate": "yyyy-MM-dd",
          "actualDuration": number,
          "predecessor": "",
          "status": "PLANNED"
        }
      ]
    }
  ]
}


====================================================
FINAL VALIDATION
====================================================

Before returning the JSON, verify ALL of the following:

1. durationDays equals the required project duration.

2. The first task starts on the supplied Project Start Date.

3. The final task ends on the final working day of the
   required project duration.

4. No task plannedStartDate is Saturday or Sunday.

5. No task plannedEndDate is Saturday or Sunday.

6. No breakdown plannedStartDate is Saturday or Sunday.

7. No breakdown plannedEndDate is Saturday or Sunday.

8. No Saturday or Sunday is counted in any duration.

9. Every parent task has a taskBreakdowns array.

10. Every breakdown has a sequence number.

11. Breakdown sequences start at 1 within each task.

12. Breakdown durations sum exactly to the parent task duration.

13. The first breakdown starts on the parent task start date.

14. The last breakdown ends on the parent task end date.

15. There are no gaps between sequential breakdowns.

16. There are no overlaps between sequential breakdowns.

17. Every predecessor references an existing breakdown.

18. No parent task predecessor is populated.

19. No breakdown references itself.

20. No circular predecessor dependency exists.

21. Every initial status is PLANNED.

22. actualStartDate equals plannedStartDate.

23. actualEndDate equals plannedEndDate.

24. actualDuration represents the working-day duration.

25. Return ONLY valid JSON.

26. NO task plannedStartDate is after the required project end date.

27. NO task plannedEndDate is after the required project end date.

28. NO breakdown plannedStartDate is after the required project end date.

29. NO breakdown plannedEndDate is after the required project end date.

30. The maximum plannedEndDate across ALL tasks and ALL breakdowns MUST equal the required project end date.

31. The required project end date is a HARD boundary and MUST NEVER be exceeded.
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