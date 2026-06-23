package com.projectestimation.backend.estimation.service;

import org.springframework.stereotype.Service;

import com.projectestimation.backend.common.ai.GeminiClient;
import com.projectestimation.backend.opportunity.model.Opportunity;

@Service
public class GeminiEstimationAnalysisService {

    private final GeminiClient geminiClient;

    public GeminiEstimationAnalysisService(
            GeminiClient geminiClient
    ) {
        this.geminiClient = geminiClient;
    }

    public String generateAnalysisHtml(
            Opportunity opportunity
    ) {

        String prompt = buildPrompt(opportunity);

        return geminiClient.generateContent(
                prompt,
                "text/plain",
                8192
        );
    }

    private String buildPrompt(
            Opportunity opportunity
    ) {

    	return """
    			Analyze the following project requirement.

    			Project Name:
    			%s

    			Requirement:
    			%s

    			Generate ONLY valid HTML.

    			Return exactly TWO HTML tables.

    			=========================================
    			TABLE 1 : ACTOR ANALYSIS
    			=========================================

    			Columns:
    			- Actor Name
    			- Actor Type

    			Actor Type must be one of:

    			- Simple
    			- Average
    			- Complex

    			Actor Classification Rules:

    			SIMPLE Actor (Weight = 1)

    			A simple actor is an external system that communicates with the software through a predefined API, service interface, integration layer, REST service, SOAP service, RPC, payment gateway, SMS gateway, email service, or any other system-to-system communication mechanism.

    			Examples:
    			- Payment Gateway
    			- SMS Gateway
    			- Email Service
    			- External API
    			- ERP Integration
    			- Bank API

    			AVERAGE Actor (Weight = 2)

    			An average actor is either:
    			- A human user interacting through a structured business workflow
    			OR
    			- A system interacting through a moderately flexible interface.

    			Examples:
    			- Manager
    			- Supervisor
    			- Inspection Agency User
    			- Business Operator
    			- Vendor Representative

    			COMPLEX Actor (Weight = 3)

    			A complex actor is a user interacting through a graphical user interface and performing multiple business operations, workflows, approvals, updates, searches, and management activities.

    			Examples:
    			- Admin
    			- Customer
    			- Portal User
    			- Mobile App User
    			- Procurement Officer
    			- Consignee
    			- SPA User

    			=========================================
    			TABLE 2 : USE CASE ANALYSIS
    			=========================================

    			Columns:
    			- Use Case Name
    			- Complexity

    			Complexity must be one of:

    			- Simple
    			- Average
    			- Complex

    			Use Case Classification Rules:

    			SIMPLE Use Case (Weight = 5)

    			A use case should be classified as SIMPLE if the primary screen or workflow contains LESS THAN 10 input fields and requires minimal business processing.

    			Examples:
    			- Login
    			- Logout
    			- Forgot Password
    			- View Profile

    			AVERAGE Use Case (Weight = 10)

    			A use case should be classified as AVERAGE if the primary screen or workflow contains BETWEEN 10 AND 20 input fields and requires moderate business validation.

    			Examples:
    			- User Registration
    			- Customer Creation
    			- Product Creation
    			- Profile Management

    			COMPLEX Use Case (Weight = 15)

    			A use case should be classified as COMPLEX if:
    			- The primary screen contains MORE THAN 20 fields
    			OR
    			- The workflow spans multiple screens
    			OR
    			- The workflow contains approvals
    			OR
    			- The workflow contains integrations
    			OR
    			- The workflow contains calculations
    			OR
    			- The workflow contains complex business rules

    			Examples:
    			- Payment Processing
    			- Procurement Workflow
    			- Order Management
    			- Inspection Management
    			- Reconciliation Process

    			Important Instructions:

    			- Generate meaningful actors from the requirement.
    			- Generate meaningful use cases from the requirement.
    			- Do not generate duplicate actors.
    			- Do not generate duplicate use cases.
    			- Every actor must have a valid Actor Type.
    			- Every use case must have a valid Complexity.
    			- Generate only HTML tables.
    			- Do not generate markdown.
    			- Do not generate explanations.
    			- Do not generate any text before or after the tables.

    			Return only valid HTML.
    			"""
    			.formatted(
    			        opportunity.getOpportunityName(),
    			        opportunity.getRequirementSummary()
    			);
                
    }
}