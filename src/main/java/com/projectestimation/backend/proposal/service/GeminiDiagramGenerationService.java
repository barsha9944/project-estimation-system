package com.projectestimation.backend.proposal.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

import org.springframework.stereotype.Service;

import com.projectestimation.backend.common.ai.gateway.AiGateway;
import com.projectestimation.backend.common.exception.AiGenerationFailedException;
import com.projectestimation.backend.common.exception.ProposalFailedException;
import com.projectestimation.backend.opportunity.model.Opportunity;

@Service
public class GeminiDiagramGenerationService {

    private static final int MAX_OUTPUT_TOKENS = 12000;

    private final AiGateway aiGateway;
    
    private static final String ARCHITECTURE_TEMPLATE_PATH =
            "proposal/templetes/solution-architecture-template.html";

    public GeminiDiagramGenerationService(
            AiGateway aiGateway
    ) {
        this.aiGateway = aiGateway;
    }

    
    private String loadArchitectureTemplate() {

        try (InputStream inputStream = getClass()
                .getClassLoader()
                .getResourceAsStream(ARCHITECTURE_TEMPLATE_PATH)) {

            if (inputStream == null) {
                throw new ProposalFailedException(
                        "Solution architecture HTML template not found: "
                                + ARCHITECTURE_TEMPLATE_PATH
                );
            }

            return new String(
                    inputStream.readAllBytes(),
                    StandardCharsets.UTF_8
            );

        } catch (IOException ex) {
            throw new ProposalFailedException(
                    "Failed to load solution architecture HTML template",
                    ex
            );
        }
    }
    
    public String generateSolutionArchitectureHtml(
            Opportunity opportunity
    ) {

    	String architectureTemplate = loadArchitectureTemplate();

    	String prompt = buildArchitecturePrompt(
    	        opportunity,
    	        architectureTemplate
    	);
    	
        try {

        	return aiGateway.generateContent(
        	        prompt,
        	        "text/plain",
        	        MAX_OUTPUT_TOKENS
        	);

        } catch (AiGenerationFailedException ex) {

            throw new ProposalFailedException(
                    ex.getMessage(),
                    ex
            );
        }
    }

    public String generateProcessFlowHtml(
            Opportunity opportunity,
            String workflowName
    ) {

        String prompt = buildProcessFlowPrompt(
                opportunity,
                workflowName
        );

        try {

        	return aiGateway.generateContentWithImages(
        	        prompt,
        	        List.of(
        	                Path.of(
        	                        "src/main/resources/proposal/reference-images/process-flow-reference.png"
        	                )
        	        ),
        	        "text/plain",
        	        MAX_OUTPUT_TOKENS
        	);

        } catch (AiGenerationFailedException ex) {

            throw new ProposalFailedException(
                    ex.getMessage(),
                    ex
            );
        }
    }

    private String buildArchitecturePrompt(
        Opportunity opportunity,
        String architectureTemplate
) {

    return """
You are adapting an APPROVED MASTER HTML TEMPLATE to create a Solution
Architecture diagram for the opportunity provided below.

THIS IS NOT A DESIGN GENERATION TASK.

The supplied HTML template is the authoritative source for the complete
visual design and HTML structure.

Your task is to take the supplied template and adapt its architecture
CONTENT to the opportunity.

============================================================
CRITICAL RULE
============================================================

START FROM THE SUPPLIED HTML TEMPLATE.

PRESERVE THE TEMPLATE.

DO NOT RECREATE THE HTML FROM MEMORY.

DO NOT DESIGN A NEW DIAGRAM.

DO NOT SIMPLIFY THE TEMPLATE.

DO NOT REPLACE THE TEMPLATE WITH YOUR OWN HTML.

DO NOT create a new CSS design.

DO NOT create a new layout.

DO NOT remove visual elements from the template.

The supplied HTML itself is the source of truth for the visual design.

All The font size should be 20px and all font colours should be black.

============================================================
WHAT MUST REMAIN UNCHANGED
============================================================

Preserve the template's existing:

- HTML structure
- CSS
- CSS variables
- colors
- backgrounds
- borders
- border radii
- shadows
- typography
- spacing
- card styling
- section headers
- panel styling
- grid layout
- column layout
- icon styling
- SVG elements
- SVG connection layer
- JavaScript
- connection-drawing logic
- legend
- overall dimensions
- visual hierarchy

Do NOT replace any of these with newly generated alternatives.

The final result must look like the supplied manager template.

============================================================
ICONS
============================================================

Preserve the existing icon implementation from the template.

DO NOT remove icons.

DO NOT replace icons with text.

DO NOT replace icons with Unicode characters.

DO NOT use emoji.

DO NOT use:

📱
👤
👨‍⚕️
🏥
🔒
💬
📊
☁️
🔔
💳
🛡️
or any other emoji.

If a template icon is not suitable for the opportunity, replace only the
icon itself with a professional inline SVG while keeping the same:

- size
- position
- visual weight
- container
- styling
- color treatment

============================================================
COLORS
============================================================

Preserve the exact colors defined in the supplied HTML template.

Do NOT create a new color palette.

Do NOT make the diagram monochrome.

Do NOT replace colored section headers with white headers.

Do NOT replace colored backgrounds with white backgrounds.

Do NOT remove borders.

Do NOT remove shadows.

Do NOT replace the template's visual styling with simple boxes.

============================================================
STRUCTURE
============================================================

The following major regions from the template must remain:

1. Header / Title
2. Users / Clients Layer
3. Protocol Layer
4. Middle Tier / Backend Services
5. Application Services / API Gateway
6. Business Modules / Engines
7. Utilities / Infrastructure Services
8. External Integrations
9. Data Access / Security Layer
10. Database / Persistence Layer
11. Legend
12. SVG connection layer

Do not remove these regions.

Do not merge these regions.

Do not reorder these regions.

Do not replace them with a different layout.

============================================================
OPPORTUNITY ADAPTATION
============================================================

The opportunity determines the CONTENT.

Adapt:

- title
- subtitle
- users
- client applications
- portals
- APIs
- backend services
- microservices
- business modules
- infrastructure services
- security components
- databases
- external integrations
- technology labels
- component descriptions
- connection definitions

The healthcare content in the template is example content only.

If the opportunity is not healthcare, replace healthcare-specific content with
content relevant to the opportunity.

For example:

Patient Gateway
→ appropriate client/application for this opportunity

Doctor App
→ appropriate user-facing application

Healthcare Business Modules
→ opportunity-specific business modules

Healthcare integrations
→ opportunity-specific integrations

MongoDB / healthcare data
→ appropriate persistence components supported by the opportunity

However, these replacements must happen INSIDE the existing template
structure.

============================================================
TECHNOLOGY ACCURACY
============================================================

Use the opportunity information as the source of architecture content.

Do not invent specific technologies, vendors, cloud services, databases,
integrations or frameworks unless supported by the opportunity.

If a technology is not specified, use a generic architectural term.

============================================================
CONNECTIONS
============================================================

Preserve the template's SVG connection layer and JavaScript connection
mechanism.

Update connection definitions only where necessary so they point to the
opportunity-specific components.

Every connection must point to an element that actually exists.

Do not leave connections pointing to removed components.

Use the same connector appearance as the template.

============================================================
IMPORTANT HTML RULE
============================================================

Return the COMPLETE HTML.

Do not return a simplified version.

Do not return only modified sections.

Do not return a new HTML design.

Do not return Markdown.

Do not return code fences.

Do not return explanations.

Return ONLY the final HTML.

The root architecture container MUST remain:

<div class="diagram-container" id="diagram-container">

============================================================
VISUAL QUALITY
============================================================

The final output must visually match the supplied manager template.

It must retain:

- professional colors
- professional icons
- colored section headers
- colored layer backgrounds
- rounded cards
- subtle shadows
- borders
- consistent typography
- professional SVG connectors
- the same overall architecture infographic appearance

The opportunity changes the architecture CONTENT.

The template controls the visual REPRESENTATION.

============================================================
APPROVED MASTER TEMPLATE
============================================================

%s

============================================================
OPPORTUNITY
============================================================

%s

============================================================
FINAL INSTRUCTION
============================================================

Adapt the APPROVED MASTER TEMPLATE to the opportunity.

Do not redesign it.

Do not simplify it.

Do not replace its CSS.

Do not replace its visual design.

Do not remove its icons.

Do not remove its colors.

Do not remove its SVG connection system.

Return the complete adapted HTML only.
"""
            .formatted(
                    architectureTemplate,
                    opportunity
            );
}

    private String buildProcessFlowPrompt(
            Opportunity opportunity,
            String workflowName
    ) {

        return """
Generate ONLY valid HTML with inline CSS for a professional business process flow diagram.

Generate a process flow ONLY for:

Workflow:
%s

FONT SIZE REQUIREMENTS

- Use large presentation-grade fonts.
- Main title: 36px to 42px
- Section titles: 28px to 32px
- Box titles: 22px to 26px
- Content text: minimum 18px
- Labels: minimum 16px
- Never use font sizes below 16px.
- Optimize for readability inside Word/PDF documents.
- Assume the image will be inserted into a proposal document.
- Text must remain readable without zooming.

IMPORTANT WORKFLOW SCOPING RULES:

- Generate ONLY the workflow specified above.
- Ignore all other workflows.
- Do not generate an end-to-end project workflow.
- Do not generate a generic application workflow.
- All process steps must be directly related to the specified workflow.
- The workflow title must appear as the main heading of the diagram.
- Every generated process step must be unique to this workflow.

Requirement Summary:
%s

STRICT RULES:

* Output ONLY HTML.
* No markdown.
* No explanations.
* No JavaScript.
* Entire response must be wrapped inside:

<div id="diagram-container">...</div>

REFERENCE IMAGE INSTRUCTIONS:

* Use the attached reference image as the primary visual inspiration.
* Follow similar flowchart styling, spacing, colors, arrows, and layout.
* The diagram should resemble a professionally designed business process workflow.
* Use a clean PowerPoint-style flowchart appearance.
* Keep the diagram visually balanced and presentation-ready.

LAYOUT RESTRICTION:

- Generate only a single workflow lane.
- Do not generate multiple rows.
- Do not wrap the workflow.
- Keep all steps in one horizontal sequence.

FLOW STRUCTURE RULES:

- Generate exactly ONE continuous workflow path.
- Every step must be connected to the next step.
- Do not create floating process boxes.
- Do not create disconnected workflow lanes.
- If a decision box exists:
  - YES path must connect to the next process step.
  - NO path must connect to an alternate process step.
- All paths must eventually connect to the End node.
- Draw connectors before placing process boxes.
- Never place a process box unless it is connected.


VISUAL REQUIREMENTS:

* Use a large rounded outer container that encloses the entire flow.
* Use soft business-friendly colors similar to the reference image.
* Use large readable fonts.
* Use consistent spacing between all process steps.
* Avoid excessive whitespace.
* Utilize the full width of the image.
* Use subtle borders and shadows.
* Use presentation-quality styling.

FLOWCHART COMPONENTS:

* Start node must be an oval or circle.
* End node must be an oval or circle.
* Process steps must be rectangular boxes.
* Decision points must use diamond-shaped decision boxes when applicable.
* Use visible arrows with arrowheads between all connected steps.
* Keep arrows straight and easy to follow.
* Maintain a clear flow direction.

CONNECTIVITY REQUIREMENTS:

* Every process step must be connected to at least one previous or next step.
* Do NOT generate isolated process boxes.
* Do NOT generate disconnected workflow lanes.
* Every workflow path must eventually lead to the End node.
* Decision boxes must have outgoing arrows.
* If the workflow moves to another row, connect the rows using vertical arrows.
* Maintain a single continuous workflow from Start to End.
* The diagram must visually show complete process continuity.
* No process box should appear without incoming or outgoing connectors.


LAYOUT REQUIREMENTS:

* Use left-to-right process flow wherever possible.
* Use vertical transitions only when required.
* Group related steps together.
* Avoid overlapping arrows.
* Ensure the workflow is easy to understand visually.
* Keep all boxes aligned consistently.

PROCESS CONTENT REQUIREMENTS:

- Generate steps ONLY for the specified workflow.
- Use Requirement Summary context only when relevant to the workflow.
- Do not include unrelated modules.
- Do not include unrelated integrations.
- Do not include unrelated approvals.
- Generate 5-12 steps specifically for this workflow.

IMPORTANT:

* Generate ONLY a process flow diagram.
* Do NOT generate architecture diagrams.
* Do NOT generate descriptive paragraphs outside the flowchart.
* Every process box should contain concise business-oriented text.
* Use 5–12 process steps depending on the opportunity complexity.
* Add decision boxes only when logically required.

Opportunity Details:

* Opportunity Name: %s
* Requirement Summary: %s
* Components: %s
"""
                .formatted(
                		safe(workflowName),
                        safe(opportunity.getOpportunityName()),
                        safe(opportunity.getRequirementSummary()),
                        safe(opportunity.getComponents()),
                        safe(opportunity.getRequirementSummary()),
                        safe(opportunity.getComponents())
                );
    }

    private String safe(
            Object value
    ) {

        return value == null
                ? ""
                : value.toString();
    }
    
    
    public String identifyProcessFlows(
            Opportunity opportunity
    ) {

        String prompt = """
    Analyze the Requirement Summary.

    Requirement Summary:
    %s

    Identify the 3 to 5 most important business workflows.

    Rules:
    - Return workflow names only.
    - One workflow per line.
    - No numbering.
    - No explanation.

    Example:

    User Authentication Flow
    Order Processing Flow
    Notification Workflow
    Approval Workflow
    """
                .formatted(
                        safe(opportunity.getRequirementSummary())
                );

        try {

            return aiGateway.generateContent(
                    prompt,
                    "text/plain",
                    1000
            );

        } catch (AiGenerationFailedException ex) {

            throw new ProposalFailedException(
                    ex.getMessage(),
                    ex
            );
        }
    }
}
