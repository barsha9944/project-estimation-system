package com.projectestimation.backend.prompt;

public class AiPrompt {
	public static final String CREATE_SUMMARY = """
								## MANDATORY SOURCE COVERAGE RULE

			Before writing the final scope, build an internal checklist of every requirement-bearing item found in the source document, including:

			* Every numbered work package

			* Every phase

			* Every module

			* Every feature

			* Every business rule

			* Every validation

			* Every workflow

			* Every integration

			* Every technical requirement

			* Every security requirement

			* Every non-functional requirement

			* Every testing requirement

			* Every infrastructure/DevOps requirement

			* Every deliverable

			* Every acceptance criterion

			* Every assumption

			* Every dependency

			* Every constraint

			* Every explicit exclusion

			The final scope must represent every item from this checklist.

			A requirement is considered represented only if its specific meaning remains visible in the output. A broad summary bullet does not count as coverage for multiple detailed source requirements.

			Example:

			The following source requirements are NOT fully represented by a single bullet saying "Tender management":

			* Criteria lock

			* Partial-bid restriction

			* Intent to bid

			* VAT evaluation basis

			* Addendum re-acknowledgment

			* Q&A fairness gate

			Each materially distinct requirement must remain visible in the output.

			Do not finish until all checklist items have been accounted for.

			The output structure is a flexible template, not a fixed checklist of required sections. Adapt it to the source document. Include a section only when the source contains relevant information for that section. Do not create, infer, or assume functionality merely because the template contains a corresponding heading. If the source uses terminology, categories, phases, modules, or work packages that differ from this template, preserve the source terminology and place the information under the closest appropriate section. If an important source requirement does not fit any predefined section, include it under "Additional Scope" rather than omitting it.

			## OUTPUT FORMAT

								Return the complete project scope as **structured plain text** suitable for display in a text editor or ChatGPT-style response.

								DO NOT return:

								* JSON

								* XML

								* YAML

								* CSV

								* a programming object

								* a database structure

								* a code block

								* a table

								Use normal text with clear headings, subheadings, numbered sections, and bullet points.

								The output should read like a professional **Project Scope / Scope of Work summary**.

								Use the following structure when the information is available:

								# Project Scope

								## 1. Project Overview

								* Project name

								* Project type

								* Primary objective

								* Business context

								* Target users/customers

								* Overall scope

								## 2. Applications, Portals and User Interfaces

								For each application, portal, or interface:

								* Name

								* Purpose

								* User types

								* Major capabilities

								* Device/browser requirements

								* Localization/accessibility requirements

								## 3. Functional Scope

								## 3A. Work Package Breakdown

								If the source document contains phases, work packages, WBS IDs, numbered scope 						items, or pricing packages, include this section.

								For every source work package:

								### [Original ID] [Original Name]

								* Detailed scope summary

								* Functional capabilities

								* Technical implications where stated

								* Dependencies where stated

								* Phase

								Rules:

								* Preserve the original ID exactly.

								* Preserve the original name exactly.

								* Do not merge multiple work packages.

								* Do not skip a work package because its details are described elsewhere.

								* If a work package is referenced only briefly, still include it.

								* If the source contains 36 work packages, this section must contain all 36 work 					packages.



								### 3.B [Functional Area]

								* Capability

								* Sub-capability

								* Workflow

								* Business rules

								* Validations

								* Approvals

								* Notifications

								* Reports/documents

								* Other relevant functionality

								Create separate subsections for materially different functional areas.

								Do not combine materially distinct source requirements into a generic statement.

			If the source lists separate features, workflows, controls, or work packages, they must remain separately identifiable in the output even if they belong to the same functional area.

			A concise summary may be added above them, but it must not replace the individual scope items.

			For estimation purposes, every distinct requirement that could create separate frontend, backend, database, integration, QA, DevOps, security, or documentation effort must remain visible.



								For example, do not write:
			> User Management

								when the source separately specifies registration, authentication, password reset, MFA, RBAC, permissions, account locking, and user administration.

								Instead, preserve each meaningful capability as a separate bullet.

								## 4. Technical Scope

								### 4.1 Technology Stack

								* Technology

								* Purpose

								* Mandatory/optional status when explicitly stated

								* Relevant constraints

								### 4.2 Architecture

								* Architecture requirements

								* Application structure

								* Service/module structure

								* Multi-tenancy

								* Data isolation

								* Integration architecture

								* Real-time/asynchronous processing

								* Other architectural requirements

								### 4.3 Frontend

								* Frameworks

								* Applications

								* UI requirements

								* State/data handling

								* Localization

								* Accessibility

								* Responsive/mobile requirements

								### 4.4 Backend

								* Frameworks

								* APIs

								* Business logic

								* Services/modules

								* Background processing

								* Authentication/authorization

								* Other backend requirements

								### 4.5 Database and Data

								* Database technology

								* Data models/entities where relevant

								* Data relationships

								* Data isolation

								* Data lifecycle

								* Migration/transformation

								* Storage

								* Backup/retention/archival

								### 4.6 Infrastructure and DevOps

								* Development environment

								* Staging

								* Production

								* Docker/containers

								* Infrastructure as code

								* CI/CD

								* Deployment

								* Monitoring

								* Backup/restore

								* Disaster recovery

								* Operational requirements

								## 5. Integration Scope

								List every external system, service, provider, API, or dependency separately.

								For each integration include:

								* System/service name

								* Purpose

								* Integration type

								* Direction

								* Protocol/technology

								* Data exchanged when specified

								* Authentication/security

								* Frequency/trigger

								* Other relevant requirements

								Do not combine multiple integrations into "third-party integrations."

								## 6. Security, Privacy and Compliance

								* Authentication

								* Authorization

								* Roles/permissions

								* MFA

								* Encryption

								* Key management

								* Data protection

								* Privacy

								* Data residency

								* Audit logging

								* Security standards

								* Compliance requirements

								* Penetration testing

								* Security testing

								* Other security requirements

								Preserve exact standards, regulations, and numerical requirements.

								## 7. Non-Functional Requirements

								### Performance

								* Explicit response-time targets

								* Throughput

								* Latency

								* Processing-time requirements

								### Scalability

								* Users

								* Concurrent users

								* Tenants

								* Transactions

								* Data volume

								* Scaling requirements

								### Availability and Reliability

								* Availability targets

								* Failover

								* Reliability

								* Recovery requirements

								### Backup and Disaster Recovery

								* Backup requirements

								* RPO

								* RTO

								* Restore requirements

								* DR testing

								### Accessibility and Compatibility

								* Accessibility standards

								* Browser support

								* Device support

								* Mobile requirements

								### Localization

								* Languages

								* Regional requirements

								* Currency/timezone/locale requirements

								### Observability

								* Logging

								* Monitoring

								* Metrics

								* Alerting

								* Tracing

								Do not replace measurable requirements with generic descriptions.

								## 8. Testing and Acceptance Scope

								* Unit testing

								* Integration testing

								* API testing

								* UI testing

								* End-to-end testing

								* Regression testing

								* Security testing

								* Performance/load testing

								* Concurrency testing

								* Migration testing

								* Accessibility testing

								* Acceptance testing

								* Test coverage

								* CI quality gates

								* Contractual/mandatory tests

								* Acceptance criteria

								Clearly identify requirements that are mandatory, contractual, or cannot be descoped.

								## 9. AI / ML / Automation Scope

								If applicable:

								* AI/ML feature

								* Purpose

								* Inputs

								* Outputs

								* Human approval

								* Automation level

								* Model/provider

								* Integration

								* Security/privacy requirements

								* Performance/accuracy requirements

								If not applicable, omit this section.

								## 10. Data Migration and Transformation

								If applicable:

								* Source systems

								* Target systems

								* Data scope

								* Mapping

								* Transformation

								* Cleansing

								* Validation

								* Reconciliation

								* Migration testing

								* Cutover

								* Rollback

								If migration is explicitly excluded, mention it under Out of Scope instead.

								## 11. Documents, Reports and Exports

								* Reports

								* Dashboards

								* PDF generation

								* Excel/CSV

								* Imports/exports

								* Document generation

								* Templates

								* Document storage

								* Versioning

								* Signatures

								* Localization

								## 12. Project Phases / Releases / Milestones

								If phases, releases, or milestones exist, preserve them exactly as described.

								For each:

								### [Phase/Release/Milestone Name]

								* Scope

								* Major capabilities

								* Technical work

								* Dependencies

								* Mandatory/optional status if explicitly stated

								Do not create phases if the source does not define them.

								Do not assume future phases are optional.

								## 13. Deliverables

								## 13A. Delivery and Commercial Obligations

			If explicitly stated in the source, include:

			* Warranty period

			* Defect resolution obligations

			* Support obligations

			* Handover requirements

			* Training requirements

			* Knowledge transfer

			* Intellectual property ownership

			* Repository/code ownership

			* Payment milestone dependencies

			* Third-party cost responsibilities

			* Maintenance obligations

			Do not omit these items merely because they are contractual rather than functional; they may affect project estimation and delivery scope.



								List all explicitly required deliverables separately:

								* Source code

								* Applications

								* APIs

								* Database/schema

								* Infrastructure

								* Deployment

								* Documentation

								* Test suites

								* Test reports

								* Training

								* Knowledge transfer

								* Production go-live

								* Support/warranty

								* Other deliverables

								## 14. Constraints

								List significant:

								* Technical constraints

								* Technology constraints

								* Business constraints

								* Contractual constraints

								* Deployment constraints

								* Security/compliance constraints

								* Responsibility boundaries

								## 15. Assumptions and Dependencies

								List only assumptions and dependencies explicitly stated or reasonably supported by the source.

								## 16. Out of Scope

								List all explicitly excluded functionality, services, systems, responsibilities, or activities.

								If something is partially included and partially excluded, clearly explain the boundary.

								## 17. Additional Scope

								Include any important scope item that does not fit naturally into the sections above.

								---

								## IMPORTANT SCOPE RULES

								The result must be **detailed rather than highly compressed**.

								Preserve every meaningful scope item that could affect:

								* development effort

								* UI effort

								* backend effort

								* database effort

								* integration effort

								* infrastructure effort

								* security effort

								* testing effort

								* deployment effort

								* operational effort

								* compliance effort

								* documentation/training effort

								Do not collapse several meaningful requirements into a single generic statement.

								For example:

								BAD:
			> Notification system

								GOOD:
			> Notifications:
			>
			> * Email notifications
			> * SMS notifications
			> * In-app notifications
			> * Delivery status tracking
			> * Bounce/complaint handling
			> * Opt-out handling
			> * Failed critical-notification alerts
			> * Background/outbox processing

								However, do not artificially break a single simple requirement into unnecessary fragments.

								The appropriate level of detail is the level at which a software estimator can understand the work involved.

								### Document independence

								Do not assume that every document contains all of the above sections.

								Use only the sections that are relevant to the actual source.

								Do not invent missing functionality.

								Do not add recommendations.

								Do not add implementation approaches.

								Do not add estimates, effort, cost, timelines, or opinions.

								Do not mention that you are analyzing a document.

								Do not say "according to the document", "the document states", or similar phrases.

								Before returning the final answer, silently perform this audit:

			1. Identify whether the source contains phases, releases, milestones, work packages, WBS IDs, or numbered scope items.

			2. If any of these exist, verify that every one appears in the output.

			3. If they do not exist, do not invent them.

			4. Verify that every numbered work package appears in the output.

			5. Verify that every explicit integration appears separately.

			6. Verify that every measurable non-functional requirement is preserved with its exact value.

			7. Verify that every mandatory test category is represented.

			8. Verify that every deliverable is represented.

			9. Verify that every acceptance criterion is represented.

			10. Verify that every assumption, dependency, constraint, and exclusion is represented.

			11. Verify that no source requirement has been replaced solely by a broader generic statement.

			If any source item fails this audit, revise the scope before returning it.

			Return only the project scope text.

Return only the project scope text.


						""";
}
