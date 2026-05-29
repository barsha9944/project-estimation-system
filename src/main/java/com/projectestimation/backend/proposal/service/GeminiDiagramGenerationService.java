package com.projectestimation.backend.proposal.service;

import java.nio.file.Path;
import java.util.List;

import org.springframework.stereotype.Service;

import com.projectestimation.backend.common.ai.GeminiClient;
import com.projectestimation.backend.common.exception.AiGenerationFailedException;
import com.projectestimation.backend.common.exception.ProposalFailedException;
import com.projectestimation.backend.opportunity.model.Opportunity;

@Service
public class GeminiDiagramGenerationService {

    private static final int MAX_OUTPUT_TOKENS = 4096;

    private final GeminiClient geminiClient;

    public GeminiDiagramGenerationService(
            GeminiClient geminiClient
    ) {
        this.geminiClient = geminiClient;
    }

    public String generateSolutionArchitectureHtml(
            Opportunity opportunity
    ) {

        String prompt = buildArchitecturePrompt(
                opportunity
        );

        try {

        	return geminiClient.generateContentWithImages(
        	        prompt,
        	        List.of(
        	                Path.of(
        	                        "src/main/resources/proposal/reference-images/solution-architecture-reference-1.png"
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

    public String generateProcessFlowHtml(
            Opportunity opportunity
    ) {

        String prompt = buildProcessFlowPrompt(
                opportunity
        );

        try {

        	return geminiClient.generateContentWithImages(
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
            Opportunity opportunity
    ) {

        return """
Generate ONLY valid HTML with inline CSS for a premium enterprise solution architecture infographic.

STRICT RULES:

* Output ONLY HTML.
* No markdown.
* No explanations.
* No JavaScript.
* Entire response must be wrapped inside:

<div id="diagram-container">...</div>

REFERENCE IMAGE INSTRUCTIONS:

* Use the attached reference image as the primary visual inspiration.
* Follow similar infographic-style enterprise architecture design.
* Maintain similar spacing, symmetry, alignment, layering, arrows, borders, shadows, icons, and visual richness.
* The final output should visually resemble a premium enterprise PowerPoint architecture infographic.

VISUAL STYLE REQUIREMENTS:

* Use large professional SVG-style icons throughout the architecture.
* Use infographic-style SVG icons instead of emoji icons whenever possible.
* Use modern enterprise color palette with rich gradients or premium solid colors.
* Use rounded infographic containers with subtle shadows and borders.
* Use large bold section headers with centered alignment.
* Use visually rich architecture styling instead of plain dashboard-style rectangles.
* Avoid tiny monochrome icons.
* Avoid flat/plain text-only layouts.
* Maintain strong visual hierarchy across all layers.
* Keep the layout visually balanced and presentation-quality.
* Utilize full width and height of the image.
* Avoid excessive whitespace and compressed layouts.
* Use large readable fonts.
* Use consistent spacing between all layers and components.
* Keep all sections evenly distributed.
* The final design should resemble a polished enterprise infographic or architecture presentation slide.
* All text font size = 20

PREMIUM INFOGRAPHIC RENDERING RULES:

* The final architecture must visually resemble a premium enterprise PowerPoint infographic slide.
* Do NOT generate flat dashboard-style layouts.
* Use visually rich layered infographic styling.
* Use gradient backgrounds for architecture layers.
* Add subtle shadows and depth to all containers.
* Use large colorful SVG-style icons throughout the diagram.
* Icons must be visually prominent and larger than text.
* Use infographic cards with rounded corners and visual depth.
* Add visual hierarchy using colors, spacing, and typography.
* Add proper enterprise-style connector arrows with arrowheads.
* Avoid excessive empty white areas.
* The architecture should feel visually dense, polished, and enterprise-grade.
* Use strong visual separation between architecture layers.
* Use large enterprise-style section headers.
* The overall design should look similar to a professionally designed consulting presentation slide.


INFOGRAPHIC VISUAL REQUIREMENTS:

* The diagram must visually resemble a premium enterprise infographic.
* Add arrows with visible arrowheads between connected layers.
* User icons must connect vertically to the Presentation Layer using visible arrows.
* REST API layer must visually connect Presentation Layer and Middleware Layer.
* External integrations must connect to middleware/business layers using horizontal arrows.
* Database layer must contain a large enterprise database icon.
* Security layer must contain shield/lock icons.
* API layer must contain API/cloud integration icons.
* Business components must contain individual icons inside component cards.
* Add subtle shadows, gradients, and rounded infographic cards.
* Ensure the architecture looks enterprise-grade and presentation-ready.

TOP USER LAYER:

* Include exactly 3 distinct user types with large professional icons:

  * Web User
  * Mobile User
  * Admin User
* Do not repeat user types.
* User icons must be large, colorful, visually rich, and centered.
* Each user must connect downward to the Presentation Layer using visible connector arrows with arrowheads.
* Display HTTPS Request labels between users and Presentation Layer.

MAIN ARCHITECTURE LAYOUT:

* Presentation / UI Layer at top
* REST API / JSON Communication Layer
* Authentication / Security Layer
* Middleware / Application Layer
* Service / Business Layer
* DAO / Data Access Layer
* Database Layer at bottom
* Security & Compliance section at bottom
* External Integrations section on the right side

ARCHITECTURE CONNECTIVITY RULES:

- Use inline SVG connector lines with SVG arrowheads.
- Do not use text arrows such as ↓, →, <-, ->.
- Connect Web User, Mobile User, and Admin User directly to the Presentation Layer.
- Connect Presentation Layer to REST API Layer.
- Connect REST API Layer to Authentication Layer.
- Connect Authentication Layer to Service Layer.
- Connect Service Layer to DAO/Data Access Layer.
- Connect DAO/Data Access Layer to Database Layer.
- Connect External Integrations to the Service Layer using horizontal SVG connectors.
- Every connector must start and end on a valid architecture component.
- Do not render floating arrows.
- Do not render disconnected connectors.
- All connectors must remain attached to architecture components.

LAYOUT RULES:

* Prefer a single left-to-right workflow.
* Only use multiple rows if the process contains many steps.
* If multiple rows are used, connect rows using vertical SVG arrows.
* The last box of a row must connect to the first box of the next row.
* Never place process boxes on separate rows without connectors.
* Maintain complete visual continuity from Start to End.


ARCHITECTURE REQUIREMENTS:

* Include token authentication, encryption, validation, audit logs, SSL/TLS, and PII protection.
* Include middleware/services dynamically based on opportunity components.
* Include multiple business/service component cards with icons.
* Include external integrations such as:

  * Payment Gateway
  * ERP Integration
  * Notification Services
  * Third Party APIs
  * Cloud Services
* Include prominent API/cloud/security/database icons throughout the architecture.
* Include a large enterprise database icon in the Database Layer.

BUSINESS COMPONENT REQUIREMENTS:

* Service/Business Layer should contain multiple visually rich component cards such as:

  * Reporting
  * Notifications
  * Analytics
  * Workflow Engine
  * Processing Engine
  * ERP Connector
  * Document Management
* Every component card must contain a relevant icon.
* Component cards should visually resemble infographic-style enterprise modules.

ICON REQUIREMENTS:

* Use visually rich infographic-style SVG icons similar to enterprise presentation diagrams.
* Icons must be large, centered, colorful, and visually balanced.
* Maintain consistent icon sizes throughout the diagram.
* Use icons for:
* Icons should visually resemble modern SVG enterprise icons used in consulting architecture presentations.
* Avoid tiny minimalist icons.
* Icons should be large, colorful, and visually dominant.



  * Users
  * Security
  * API
  * Database
  * Cloud
  * Notifications
  * ERP
  * Payment Gateway
  * Middleware
  * Analytics
  * Reporting
  * Workflow Engine
  * Authentication

IMPORTANT:

* Generate ONLY architecture diagram.
* Do NOT generate workflow/process flow diagrams.
* Do NOT generate descriptive paragraphs outside the visual diagram.
* Do NOT leave empty unused spaces in the layout.
* Do NOT generate plain dashboard-style rectangles.
* The final output should look like a premium enterprise infographic suitable for client proposal documents.

Opportunity Details:

* Opportunity Name: %s
* Platforms: %s
* Technologies: %s
* Enterprise Contexts: %s
* Components: %s


"""
                .formatted(
                        safe(opportunity.getOpportunityName()),
                        safe(opportunity.getPlatforms()),
                        safe(opportunity.getTechnologyCategories()),
                        safe(opportunity.getEnterpriseContexts()),
                        safe(opportunity.getComponents())
                );
    }

    private String buildProcessFlowPrompt(
            Opportunity opportunity
    ) {

        return """
Generate ONLY valid HTML with inline CSS for a professional business process flow diagram.

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
Generate a workflow based on:

* User actions
* System processing
* API interactions
* Business validations
* Database operations
* Approval workflows
* Notifications
* Exception handling (if applicable)

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
                        safe(opportunity.getOpportunityName()),
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
}
