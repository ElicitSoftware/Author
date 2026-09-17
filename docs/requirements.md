# Elicit Author — Requirements Catalog

Source: `docs/vision.md`, `docs/scd-contract.md`, and the repo-root planning
docs (`survey-authoring-development-plan.md`,
`survey-requirements-collection-system.md`, `creating-fhhs-style-surveys.md`).
Per `docs/vision.md`'s caveat, those planning docs describe authoring
capabilities (question banks, branching, tokens, metadata,
requirements-collection) accurately, but predate the Kimball Type 2
draft/version/publish lifecycle — every requirement below that touches a
versioned structural element (`questions`, `select_groups`, `select_items`,
`sections`, `steps`, `sections_questions`, `steps_sections`,
`relationships`) reflects the draft/publish model in `docs/scd-contract.md`,
not the simple in-place-edit CRUD the planning docs assume.

Roles referenced below:
- **Survey Author** — the primary actor; builds and publishes survey structure.
- **System Administrator** — configures platform-level access control (not a
  survey author's day-to-day role, but distinct from them).

## Functional Requirements

### Survey & Structural Element Lifecycle

| ID     | Title                                | User Story                                                                                                                                                                                    | Priority | Status |
|--------|---------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------|--------|
| FR-001 | Create Survey                        | As a survey author, I want to create a new survey record so that I can begin authoring its structure.                                                                                          | High     | Open   |
| FR-002 | Edit Survey Metadata                 | As a survey author, I want to edit a survey's name, title, and description in place so that the survey's own identification stays current (`surveys` is Type 1, not versioned).               | High     | Open   |
| FR-003 | Create Structural Element Draft      | As a survey author, I want to create a draft of a structural element (step, section, question, select group, select item, relationship, or section/step assignment) so that I can define or revise it without affecting what's currently published. | High     | Open   |
| FR-004 | Edit Structural Element Draft        | As a survey author, I want to edit a structural element's open draft so that I can refine its content before publishing.                                                                       | High     | Open   |
| FR-005 | Publish Structural Element Draft     | As a survey author, I want to publish a structural element's draft so that it becomes the new current version under its durable key while the prior version's date range is closed, never mutated in place. | High     | Open   |
| FR-006 | Discard Structural Element Draft     | As a survey author, I want to discard an open draft so that I can abandon in-progress changes without affecting the currently published version.                                              | High     | Open   |
| FR-007 | View Structural Element Version History | As a survey author, I want to view a structural element's version history (current and past versions, effective-date ranges, publish attribution) so that I can understand how it evolved over time. | High     | Open   |
| FR-008 | Record Publish Attribution           | As a survey author, I want to attach my identity and an optional comment when I publish a draft so that other authors can see who published a change and why, since `published_by`/`published_comment` are the only audit trail the schema keeps. | High     | Open   |
| FR-009 | Prevent Concurrent Drafts on One Element | As a survey author, I want to be blocked from opening a second draft on a structural element that already has one open so that concurrent edits don't produce an ambiguous published outcome. | Medium   | Open   |

### Question Authoring

| ID     | Title                                  | User Story                                                                                                                                                                        | Priority | Status |
|--------|------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------|--------|
| FR-010 | Define Question Type                   | As a survey author, I want to choose from the supported question types (text, textarea, integer, double, checkbox, radio, checkbox group, multi-select, combobox, date picker, datetime, email, password, HTML) so that I capture the right kind of respondent input. | High     | Open   |
| FR-011 | Configure Question Validation           | As a survey author, I want to configure a question's required flag, min/max values, validation text, mask, placeholder, default value, and tooltip so that respondent input is properly constrained and guided. | High     | Open   |
| FR-012 | Author Token Placeholders in Question Text | As a survey author, I want to insert token placeholders (e.g. `{TOKEN|default}`) into question text so that the rendered question can be personalized per respondent.        | Medium   | Open   |
| FR-013 | Preview Token Replacement               | As a survey author, I want to preview how a question's tokens render with sample data so that I can validate wording and grammar before publishing.                             | Medium   | Open   |
| FR-014 | Manage Select Groups and Items          | As a survey author, I want to create and order select groups and their select items (coded value, display text, display order) so that choice-based questions have the options respondents will see. | High     | Open   |

### Section & Step Organization

| ID     | Title                              | User Story                                                                                                                                                                                       | Priority | Status |
|--------|--------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------|--------|
| FR-015 | Organize Sections into Steps        | As a survey author, I want to assign sections to steps and control their order so that the survey follows the intended navigation flow.                                                          | High     | Open   |
| FR-016 | Reorder Steps, Sections, Questions  | As a survey author, I want to reorder steps, sections within a step, and questions within a section so that I can control the sequence respondents experience.                                    | High     | Open   |
| FR-017 | Configure Repeating Elements        | As a survey author, I want to mark a step, section, or question as repeatable and define what determines its repetition count so that the survey can dynamically generate the right number of instances (e.g. one section per family member). | Medium   | Open   |
| FR-018 | Rename Step or Section              | As a survey author, I want to rename a step or section and see an explicit warning that the rename relabels historical reports as well as future ones so that I make the decision with full knowledge of its retroactive effect. | High     | Open   |

### Logic & Relationships

| ID     | Title                              | User Story                                                                                                                                                                       | Priority | Status |
|--------|---------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------|--------|
| FR-019 | Create Conditional Relationship     | As a survey author, I want to define a relationship between an upstream question/answer and a downstream step, section, or question using an operator (BOOLEAN, GREATER_THAN, EQUAL, NOT_EQUAL, FIELD_EXIST, CONTAINS) so that I can control conditional survey behavior. | High     | Open   |
| FR-020 | Configure Relationship Action       | As a survey author, I want to choose the action a relationship triggers (SHOW, REPEAT, TEXT replacement) so that the relationship produces its intended effect on flow or content. | High     | Open   |
| FR-021 | Visualize Relationship Graph        | As a survey author, I want to see a visual map of how questions, sections, and relationships connect so that I can understand and validate complex conditional logic before publishing. | Medium   | Open   |
| FR-022 | Validate Relationship Consistency   | As a survey author, I want the system to flag relationships that reference missing or unpublished elements so that I can fix broken logic before publishing.                  | High     | Open   |

### Ontology & Metadata

| ID     | Title                              | User Story                                                                                                                                                                                | Priority | Status |
|--------|---------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------|--------|
| FR-023 | Assign Ontology Tag to Question     | As a survey author, I want to tag a question with an ontology entry so that its answers are captured in the corresponding dimensional reporting table.                                   | High     | Open   |
| FR-024 | Reassign Ontology Tag               | As a survey author, I want to change a question's ontology tag and see an explicit warning that the change reclassifies historical answers as well as future ones so that I make the retroactive decision knowingly. | High     | Open   |
| FR-025 | Create Ontology & Dimension Entries | As a survey author, I want to define new ontology tags and their target dimension so that questions I'm authoring can be linked to the reporting structure they need.                    | Medium   | Open   |
| FR-026 | Preview Dimensional Impact          | As a survey author, I want to preview which dimension table an ontology tag maps to (existing or newly created) so that I understand the reporting consequence of a metadata assignment before publishing. | Low      | Open   |

### Requirements Collection & First-Draft Generation

The requirements-collection "meta-survey" is itself an ordinary Elicit Survey
— authored in Author like any other survey, then rendered and answered in
the Survey app (rendering surveys and recording answers is out of Author's
scope regardless of which survey is being taken; see `docs/vision.md`
Scope). Author's role is limited to (a) authoring/maintaining that
meta-survey's definition and (b) consuming its completed answers to generate
a first-draft structure for a *different*, new target survey.

| ID     | Title                                              | User Story                                                                                                                                                                                                          | Priority | Status |
|--------|------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------|--------|
| FR-027 | Author the Requirements-Collection Meta-Survey     | As a survey author, I want to author and maintain the requirements-collection meta-survey (its steps, sections, and questions) using Author's own survey-authoring tools so that other authors are guided, through a normal Elicit Survey taken in the Survey app, to provide the structural, logic, token, and metadata information needed to generate a first draft. | Medium   | Open   |
| FR-028 | Generate First-Draft Structure from Meta-Survey Answers | As a survey author, I want the system to read a completed requirements-collection survey response and generate a first-draft structure (steps, sections, questions, relationships) for my new survey, as open drafts, so that I start from a working draft instead of a blank survey. | Medium   | Open   |
| FR-029 | Refine Generated Draft                             | As a survey author, I want to edit the generated first-draft structure using the same draft/publish tools as any other survey so that I can customize the auto-generated starting point.                          | Medium   | Open   |

### Preview & Publish

| ID     | Title                        | User Story                                                                                                                                                          | Priority | Status |
|--------|--------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------|----------|--------|
| FR-030 | Preview Survey as Respondent | As a survey author, I want to preview the survey's current draft state as a respondent would experience it so that I can validate flow and content before publishing. | High     | Open   |
| FR-031 | Batch-Publish Pending Drafts | As a survey author, I want to publish multiple pending drafts across a survey in one action so that I don't have to publish each changed element individually after a coordinated round of edits. | Medium   | Open   |

### Access & Integration

| ID     | Title                     | User Story                                                                                                                                          | Priority | Status |
|--------|-----------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------|----------|--------|
| FR-032 | Authenticate via OIDC     | As a survey author, I want to log in using the platform's OIDC/Keycloak provider so that I use the same identity as the Admin and Survey apps.     | High     | Open   |
| FR-033 | Restrict Authoring Access | As a system administrator, I want authoring functions restricted to users holding the author role so that unauthorized users cannot alter survey structure. | High     | Open   |

### Export & Import

| ID     | Title                       | User Story                                                                                                                            | Priority | Status |
|--------|--------------------------------|---------------------------------------------------------------------------------------------------------------------------------|----------|--------|
| FR-034 | Export Survey Definition    | As a survey author, I want to export a survey's structure so that I can share, back up, or migrate it outside Author.                | Low      | Open   |
| FR-035 | Import Survey Definition    | As a survey author, I want to import a previously exported survey definition so that I can restore or replicate a survey's structure into a draft state. | Low      | Open   |

## Non-Functional Requirements

| ID      | Title                            | Requirement                                                                                                                    | Category        | Priority | Status |
|---------|-----------------------------------|----------------------------------------------------------------------------------------------------------------------------------|------------------|----------|--------|
| NFR-001 | Authoring View Load Time         | Any authoring view must render within 2 seconds for a survey containing up to 200 questions.                                     | Performance      | High     | Open   |
| NFR-002 | Publish Latency                  | Publishing a structural element's draft must complete within 3 seconds under normal load.                                        | Performance      | High     | Open   |
| NFR-003 | Concurrent Author Capacity       | The system must support at least 20 concurrent authoring sessions without publish latency exceeding NFR-002's 3-second threshold. | Scalability      | Medium   | Open   |
| NFR-004 | Business-Hours Uptime            | Author must maintain 99.5% uptime during business hours.                                                                          | Availability     | Medium   | Open   |
| NFR-005 | Transport Encryption             | All traffic between the browser and Author must use TLS 1.2 or higher.                                                            | Security         | High     | Open   |
| NFR-006 | Mandatory Authentication         | 100% of authoring routes must require a valid OIDC session; zero anonymous access to authoring views.                            | Security         | High     | Open   |
| NFR-007 | Accessibility Compliance         | All authoring views must conform to WCAG 2.1 AA, verified by automated accessibility testing on every release.                   | Usability        | High     | Open   |
| NFR-008 | Versioning Vocabulary Consistency | UI copy and code must use only the durable-key/version/draft/publish/current vocabulary for the eight Type 2 tables, with zero occurrences of an `is_current` or competing `active` flag, verified by periodic code/doc review. | Maintainability  | Medium   | Open   |
| NFR-009 | Audit Trail Completeness         | 100% of publish actions must record `published_by`, and `published_comment` when supplied, since no separate audit-log table exists. | Maintainability  | High     | Open   |
| NFR-010 | Deployment Portability           | Author must run identically via Docker across dev, staging, and production with zero environment-specific code paths.            | Portability      | Medium   | Open   |

## Constraints

| ID    | Title                          | Constraint                                                                                                                                              | Category    | Priority | Status |
|-------|-----------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------|-------------|----------|--------|
| C-001 | Runtime Platform                | Backend must run on Java 25.                                                                                                                             | Technical   | High     | Open   |
| C-002 | Application Framework           | Backend must use Quarkus 3.39.2.                                                                                                                         | Technical   | High     | Open   |
| C-003 | UI Framework                    | UI must use Vaadin 25.2.6 (Flow, server-side); accessibility must rely on Vaadin's built-in WCAG support rather than raw HTML that bypasses it.          | Technical   | High     | Open   |
| C-004 | Data Access Layer               | Data access must use Hibernate ORM with Panache (JPA); jOOQ-specific patterns are prohibited.                                                            | Technical   | High     | Open   |
| C-005 | No Owned Database Migrations    | Author must not create or own Flyway migrations; it maps entities onto tables already created by Survey's `db/migration` in the shared schema.          | Technical   | High     | Open   |
| C-006 | Database Platform               | System must use the shared Elicit `survey` PostgreSQL schema, not a dedicated Author database.                                                          | Technical   | High     | Open   |
| C-007 | Build & Deployment              | System must build with Maven and deploy via Docker.                                                                                                      | Technical   | Medium   | Open   |
| C-008 | Identity Provider                | Authentication/authorization must use OIDC/Keycloak matching the Admin app's setup; Author must not implement a custom auth mechanism.                  | Technical   | High     | Open   |
| C-009 | License                         | The codebase is governed by the PolyForm Noncommercial 1.0.0 license.                                                                                    | Regulatory  | High     | Open   |
| C-010 | Versioning Vocabulary           | Author must exclusively use durable-key/version/draft/publish/current terminology for the eight Type 2 tables and must not introduce a competing `active`/`is_current` flag. | Technical   | High     | Open   |
| C-011 | Non-Retroactive Default         | All structural edits except step/section renaming and ontology/metadata tag reassignment must be non-retroactive — respondents keep seeing the version current when they started. | Technical   | High     | Open   |
| C-012 | Retroactive Exceptions Sign-off | Only step/section renaming and ontology/metadata tag reassignment may behave retroactively, per the stakeholder sign-off recorded 2026-09-12; no further retroactive behavior may be introduced without equivalent sign-off. | Business    | Medium   | Open   |
| C-013 | PHI Compliance                  | Authored surveys may collect PHI in clinical deployments (e.g. FHHS); Author must not expose or export PHI without appropriate safeguards.               | Regulatory  | High     | Open   |
| C-014 | Browser Support                 | UI must support the latest two versions of Chrome, Firefox, and Safari.                                                                                  | Technical   | Medium   | Open   |
| C-015 | No Existing Publish Pattern to Reuse | No reusable draft/publish/discard service pattern exists elsewhere in the Elicit codebase; Author must design it from the SQL sketch in `docs/scd-contract.md`, not adopt a shared implementation. | Technical   | Medium   | Open   |
| C-016 | Token Terminology               | In Author, "token" means only a question-text placeholder (`{TOKEN\|default}`, `survey.relationships.token`). The credential a respondent enters to reach a survey is the **access code** and is issued by the Admin app; Author must not call it a token. | Business    | Medium   | Implemented |

## Open Items for Review

- **FR-031 (Batch-Publish Pending Drafts)** extrapolates beyond what `docs/vision.md` states explicitly (which describes publishing per structural element/durable key). Confirm whether a coordinated multi-element publish action is actually wanted, or whether each element must always be published individually.
- **FR-028** assumes the meta-survey's *questions* map predictably onto target-survey structure (steps, sections, questions, relationships) closely enough to auto-generate drafts. The actual mapping logic (which meta-survey answer produces which structural element) isn't designed yet — that's entity-model/use-case-spec work, not a requirements-catalog concern, but flagging it here since it's the riskiest FR in the catalog.
- No NFR/constraint currently sets a target survey size ceiling (FHHS-scale: 15 steps, 48+ questions, 82+ relationships, 67+ metadata mappings, 16 select groups) beyond NFR-001's 200-question performance threshold — confirm that threshold is generous enough, or set an explicit "supports FHHS-scale surveys" acceptance target.
