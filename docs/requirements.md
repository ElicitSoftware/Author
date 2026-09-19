# Elicit Author — Requirements Catalog

Source: `docs/vision.md`, `docs/scd-contract.md`, Admin's definition-file
implementation (`SurveyDefinitionExportService`, `SurveyDefinitionImportService`,
`SurveyDefinitionUpdateService`, `SurveyDefinitionApplyService`; Admin UC-013,
UC-014, UC-017, UC-018), and the repo-root planning docs
(`survey-authoring-development-plan.md`, `survey-requirements-collection-system.md`,
`creating-fhhs-style-surveys.md`) for authoring capabilities.

> **Revision note (2026-09-17):** the previous catalog framed every structural
> edit as a draft → publish transition in a shared production database
> (former FR-003..FR-009, FR-031, FR-034/035). Author now edits working copies
> in its own database and **exports a `.elicit` file**; Admin installs or
> updates it at each site. The lifecycle requirements below replace those
> entries; question/section/logic/metadata requirements are unchanged in
> intent.

Roles referenced below:
- **Survey Author** — the primary actor; builds, validates, and exports survey definitions.
- **System Administrator** — configures platform-level access control.
- **Admin app** — secondary system actor that consumes exported files (out of scope here).

## Functional Requirements

### Survey Definition Lifecycle

| ID     | Title                              | User Story                                                                                                                                                                                                                    | Priority | Status |
|--------|------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------|--------|
| FR-001 | Create Survey                      | As a survey author, I want to create a new survey record so that I can begin authoring its structure. The survey receives a `survey_key` that never changes.                                                                  | High     | Open   |
| FR-002 | Edit Survey Metadata               | As a survey author, I want to edit a survey's name, title, description, initial display key, and post-survey URL in place so that the survey's identification stays current (`surveys` is Type 1).                              | High     | Open   |
| FR-003 | Import Survey Definition           | As a survey author, I want to import an existing `.elicit` file into Author so that I can continue editing a survey authored elsewhere. Every `survey_key`/`element_key` is preserved verbatim; a file whose `survey_key` already exists in Author is rejected with a pointer to the existing survey. | High     | Open   |
| FR-004 | Export Survey Definition           | As a survey author, I want to export a survey as an `ELICIT_SURVEY_EXPORT_V1` `.elicit` file, attaching my identity and a release note (`published_by`/`published_comment`) and a fresh `survey_revision`, so that Admin can install or update it at any site. | High     | Open   |
| FR-005 | Validate Definition Before Export  | As a survey author, I want export to be blocked by definition errors (dangling durable references, relationships without exactly one downstream target, choice questions without a select group, unresolved token names) and to see warnings (empty steps/sections, retroactive edits) so that I never ship a broken file. | High     | Open   |
| FR-006 | Show Last Export Attribution       | As a survey author, I want to see who last exported a survey, when, and with what release note so that I know what has been handed off.                                                                                         | Medium   | Open   |
| FR-007 | Remove Structural Element          | As a survey author, I want to remove a step, section, question, select group, select item, relationship, or assignment so that it disappears from the survey — implemented as **retire** (close `effective_to`), cascading to dependent assignments and relationships, never as a physical delete, so that sites applying the export close the element and keep recorded answers. | High     | Open   |
| FR-008 | Restore Removed Element            | As a survey author, I want to see removed elements and restore one (reopen its validity window) as long as its parents are current, so that a mistaken removal is recoverable before or after export.                            | Medium   | Open   |
| FR-009 | Preserve Element Identity          | As a survey author, I want every element's key to be minted once when it is created and never regenerated, so that re-exporting matches existing rows at every deployed site instead of installing duplicates.                 | High     | Open   |

### Question Authoring

| ID     | Title                                      | User Story                                                                                                                                                                                                                       | Priority | Status |
|--------|--------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------|--------|
| FR-010 | Define Question Type                       | As a survey author, I want to choose from the supported question types (text, textarea, integer, double, checkbox, radio, checkbox group, multi-select, combobox, date picker, datetime, email, password, HTML) so that I capture the right kind of respondent input. | High     | Implemented |
| FR-011 | Configure Question Validation              | As a survey author, I want to configure a question's required flag, min/max values, validation text, mask, placeholder, default value, tooltip, and presentation variant so that respondent input is properly constrained, guided, and styled — showing only the settings the chosen type uses (e.g. HTML has no input mask). | High     | Implemented |
| FR-012 | Author Token Placeholders in Question Text | As a survey author, I want to insert token placeholders (e.g. `{TOKEN|default}`) into question text so that the rendered question can be personalized per respondent.                                                            | Medium   | Open   |
| FR-013 | Preview Token Replacement                  | As a survey author, I want the question editor's preview to substitute a Text rule's sample answer for its token (e.g. sample "Bob" renders "Hello {PName}" as "Hello Bob") so that I can validate wording and grammar before exporting. | Medium   | Implemented |
| FR-014 | Manage Select Groups and Items             | As a survey author, I want to create and order select groups and their select items (coded value, display text, display order) so that choice-based questions have the options respondents will see.                             | High     | Implemented |
| FR-035 | Preview Question Rendering                 | As a survey author, I want the question editor to show a live preview of the respondent's field for the selected type and presentation variants (with its label, placeholder, required indicator, and tooltip; HTML rendered as HTML) so that I can see how the question will appear before exporting. | Medium   | Implemented |
| FR-036 | Record a Text Rule Sample Answer           | As a survey author, I want to give a Text rule's token an example answer so that the question preview can show the substituted text; the sample is authoring-only and is not written to the export. | Low | Implemented |

### Section & Step Organization

| ID     | Title                              | User Story                                                                                                                                                                                                        | Priority | Status |
|--------|------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------|--------|
| FR-015 | Organize Sections into Steps       | As a survey author, I want to assign sections to steps and control their order so that the survey follows the intended navigation flow.                                                                           | High     | Implemented |
| FR-016 | Reorder Steps, Sections, Questions | As a survey author, I want to reorder steps, sections within a step, and questions within a section so that I can control the sequence respondents experience.                                                     | High     | Implemented |
| FR-017 | Configure Repeating Elements       | As a survey author, I want to mark a step, section, or question as repeatable and define what determines its repetition count so that the survey can dynamically generate the right number of instances.           | Medium   | Open   |
| FR-018 | Rename Step or Section             | As a survey author, I want to rename a step or section and see an explicit warning (at edit and at export) that the rename relabels historical reports at deployed sites as well as future ones.                   | High     | Implemented |
### Logic & Relationships

| ID     | Title                             | User Story                                                                                                                                                                                                                  | Priority | Status |
|--------|-----------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------|--------|
| FR-019 | Create Conditional Relationship   | As a survey author, I want to define a relationship between an upstream question/answer and a downstream step, section, or question using an operator (BOOLEAN, GREATER_THAN, EQUAL, NOT_EQUAL, FIELD_EXIST, CONTAINS) so that I can control conditional survey behavior. | High     | Implemented |
| FR-020 | Configure Relationship Action     | As a survey author, I want to choose the action a relationship triggers (SHOW, REPEAT, TEXT replacement) so that the relationship produces its intended effect on flow or content.                                            | High     | Implemented |
| FR-021 | Visualize Relationship Graph      | As a survey author, I want to see a visual map of how questions, sections, and relationships connect so that I can understand and validate complex conditional logic before exporting.                                       | Medium   | Implemented |
| FR-022 | Validate Relationship Consistency | As a survey author, I want the system to flag relationships that reference removed or missing elements, or that have zero or several downstream targets, so that I can fix broken logic (surfaced through FR-005).             | High     | Open   |

### Ontology & Metadata

| ID     | Title                               | User Story                                                                                                                                                                                                        | Priority | Status |
|--------|-------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------|--------|
| FR-023 | Assign Ontology Tag to Question     | As a survey author, I want to tag a question with an ontology entry so that its answers are captured in the corresponding dimensional reporting table.                                                            | High     | Implemented |
| FR-024 | Reassign Ontology Tag               | As a survey author, I want to change a question's ontology tag and see an explicit warning (at edit and at export) that the change reclassifies historical answers at deployed sites as well as future ones.        | High     | Implemented |
| FR-025 | Create Ontology & Dimension Entries | As a survey author, I want to define new ontology tags and their target dimension so that questions I'm authoring can be linked to the reporting structure they need.                                              | Medium   | Implemented |
| FR-026 | Preview Dimensional Impact          | As a survey author, I want to preview which dimension table an ontology tag maps to (existing or newly created) so that I understand the reporting consequence of a metadata assignment before exporting.           | Low      | Implemented |

### Requirements Collection & First-Draft Generation

The requirements-collection "meta-survey" (`samples/ElicitDesigner.elicit`) is
itself an ordinary Elicit Survey — authored in Author, exported, applied through
Admin, and answered in the Survey app. Author's role is (a) authoring and
maintaining that meta-survey's definition and (b) consuming its completed
answers to generate a first-draft structure for a *different*, new survey.

| ID     | Title                                                    | User Story                                                                                                                                                                                                                       | Priority | Status |
|--------|----------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------|--------|
| FR-027 | Author the Requirements-Collection Meta-Survey           | As a survey author, I want to author and maintain the requirements-collection meta-survey using Author's own tools so that other authors are guided, through a normal Elicit Survey, to provide the information needed to generate a first draft. | Medium   | Open   |
| FR-028 | Generate First-Draft Structure from Meta-Survey Answers  | As a survey author, I want the system to read a completed requirements-collection response and generate a first-draft structure (steps, sections, questions, relationships) as a new survey in Author, with stable UUIDv5 element keys, so that I start from a working draft instead of a blank survey. | Medium   | Open   |
| FR-029 | Refine Generated Structure                               | As a survey author, I want to edit the generated structure with the same tools as any other survey so that I can customize the auto-generated starting point before exporting.                                                    | Medium   | Open   |

### Preview & Navigation

| ID     | Title                        | User Story                                                                                                                                                                                                        | Priority | Status |
|--------|------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------|--------|
| FR-030 | Preview Survey as Respondent | As a survey author, I want to preview the survey as a respondent would experience it, using the Survey app in the local authoring stack against Author's working database, so that I can validate flow and content before exporting. | High     | Open   |
| FR-038 | Preview the Selection on the Designer | As a survey author, I want the designer to show, beside the board, the selected section as a respondent sees it (the section name as the page title, each question's field with its text, options, placeholder, required indicator and tooltip, HTML rendered; a selected question highlighted; a step shown as its pages), so that I can check wording and layout without leaving the board. | Medium   | Implemented |
| FR-031 | Browse Survey Structure      | As a survey author, I want a navigation tree of steps → sections → questions beside the editor that highlights the element I am working on and lets me jump to any element, so that I always know where I am in a large survey. | High     | Open   |

### Guidance

| ID     | Title                    | User Story                                                                                                                                                                                                                                     | Priority | Status |
|--------|--------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------|--------|
| FR-034 | Explain the Survey Model | As a new survey author, I want an in-app page that explains how a survey is put together (steps, sections, questions, the default path, and the rules that connect them) so that I understand the designer's vocabulary before I use it. | Low      | Implemented |
### Access & Integration

| ID     | Title                     | User Story                                                                                                                                                                | Priority | Status |
|--------|---------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------|--------|
| FR-032 | Authenticate via OIDC     | As a survey author, I want to log in using the platform's OIDC/Keycloak provider so that I use the same identity as the Admin and Survey apps.                             | High     | Open   |
| FR-033 | Restrict Authoring Access | As a system administrator, I want authoring functions restricted to users holding the `elicit_author` role (granted explicitly; not implied by `elicit_admin`) so that unauthorized users cannot alter survey definitions. | High     | Open   |

### Branding

| ID     | Title                     | User Story                                                                                                                                                                | Priority | Status      |
|--------|---------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------|-------------|
| FR-037 | Brand the Authoring Interface | As a system administrator, I want Author to display the deployment's brand — logo, display name, and theme — from a mounted brand directory, resolved through a fixed fallback order (external mount, then a local directory, then the brand embedded in the application) and served only from within the brand root, so that Author matches the Survey and Admin apps at a site and never serves a file from outside the brand directory. | Medium | Implemented |

## Non-Functional Requirements

| ID      | Title                              | Requirement                                                                                                                                                                                                | Category        | Priority | Status |
|---------|------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-----------------|----------|--------|
| NFR-001 | Authoring View Load Time           | Any authoring view must render within 2 seconds for a survey containing up to 200 questions.                                                                                                               | Performance     | High     | Open   |
| NFR-002 | Export Latency                     | Validating and exporting a survey of up to 200 questions must complete within 3 seconds under normal load.                                                                                                | Performance     | High     | Open   |
| NFR-003 | Concurrent Author Capacity         | The system must support at least 20 concurrent authoring sessions without export latency exceeding NFR-002's threshold.                                                                                   | Scalability     | Medium   | Open   |
| NFR-004 | Business-Hours Uptime              | Author must maintain 99.5% uptime during business hours.                                                                                                                                                   | Availability    | Medium   | Open   |
| NFR-005 | Transport Encryption               | All traffic between the browser and Author must use TLS 1.2 or higher.                                                                                                                                     | Security        | High     | Open   |
| NFR-006 | Mandatory Authentication           | 100% of authoring routes must require a valid OIDC session; zero anonymous access to authoring views.                                                                                                     | Security        | High     | Open   |
| NFR-007 | Accessibility Compliance           | All authoring views must conform to WCAG 2.1 AA, verified by automated accessibility testing on every release.                                                                                            | Usability       | High     | Open   |
| NFR-008 | Versioning Vocabulary Consistency  | UI copy and code use only durable key / element key / version / current / retired / revision; zero occurrences of `is_current`, `active`, or `is_draft`; "publish" is never used for a database state.      | Maintainability | Medium   | Open   |
| NFR-009 | Export Attribution Completeness    | 100% of exports record `published_by` (and `published_comment` when supplied) on the survey row and in the file, and mint a `survey_revision`.                                                             | Maintainability | High     | Open   |
| NFR-010 | Deployment Portability             | Author must run identically via Docker across dev, staging, and production with zero environment-specific code paths.                                                                                     | Portability     | Medium   | Open   |
| NFR-011 | Format Round Trip                  | Importing a `.elicit` file and exporting it again must reproduce every record field-for-field (ignoring remapped local ids, rebased display-key prefixes, and header timestamps), verified by automated test. | Maintainability | High     | Open   |
| NFR-012 | Keyboard Equivalence               | Every drag-and-drop action in the designer has a keyboard-reachable equivalent (element menu or button), so WCAG 2.1 success criterion 2.1.1 holds without a pointer. Adding a step uses a top-bar "Add step" button and the step menu's "Add step after"; sections and questions use "Add …" in a container's menu; reordering uses the "Move up / Move down" menu items; the new element is positioned by the current selection. | Usability | High | Implemented |
| NFR-013 | Minimum Viewport                   | The designer targets tablet-sized screens and larger; below 768px it shows a full-screen "use a wider screen" notice instead of reflowing into an unusable phone layout. | Usability | Low | Implemented |

## Constraints

| ID    | Title                              | Constraint                                                                                                                                                                                                                | Category   | Priority | Status      |
|-------|------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------|----------|-------------|
| C-001 | Runtime Platform                   | Backend must run on Java 25.                                                                                                                                                                                              | Technical  | High     | Open        |
| C-002 | Application Framework              | Backend must use Quarkus 3.39.2.                                                                                                                                                                                          | Technical  | High     | Open        |
| C-003 | UI Framework                       | UI must use Vaadin 25.2.7 (Flow, server-side); accessibility must rely on Vaadin's built-in WCAG support rather than raw HTML that bypasses it.                                                                            | Technical  | High     | Open        |
| C-004 | Data Access Layer                  | Data access must use Hibernate ORM with Panache (JPA); jOOQ-specific patterns are prohibited.                                                                                                                              | Technical  | High     | Open        |
| C-005 | No Owned Database Migrations       | Author must not create or own production Flyway migrations; the schema is created by Survey's `db/migration`. A verbatim, test-only mirror of those scripts may be replayed into a throwaway test database.               | Technical  | High     | Open        |
| C-006 | Database Platform                  | Author runs against its own PostgreSQL database instance carrying the shared `survey` schema (the local authoring stack), never against a deployed site's production database.                                            | Technical  | High     | Open        |
| C-007 | Build & Deployment                 | System must build with Maven and deploy via Docker.                                                                                                                                                                       | Technical  | Medium   | Open        |
| C-008 | Identity Provider                  | Authentication/authorization must use OIDC/Keycloak matching the Admin app's setup, with Author's own client `elicit-author` and role `elicit_author`; Author must not implement a custom auth mechanism.                    | Technical  | High     | Open        |
| C-009 | License                            | The codebase is governed by the PolyForm Noncommercial 1.0.0 license.                                                                                                                                                     | Regulatory | High     | Open        |
| C-010 | Versioning Vocabulary              | Author must exclusively use durable key / element key / version / current / retired / revision terminology and must not introduce a competing `active`/`is_current` flag.                                                  | Technical  | High     | Open        |
| C-011 | Non-Retroactive Default            | All structural edits except step/section renaming and ontology/metadata tag reassignment are non-retroactive at deployed sites — respondents keep seeing the version current when they started.                            | Technical  | High     | Open        |
| C-012 | Retroactive Exceptions Sign-off    | Only step/section renaming and ontology/metadata tag reassignment may behave retroactively, per the stakeholder sign-off recorded 2026-09-12; no further retroactive behavior may be introduced without equivalent sign-off. | Business   | Medium   | Open        |
| C-013 | PHI Compliance                     | Authored surveys may collect PHI in clinical deployments (e.g. FHHS); Author must not expose or export PHI without appropriate safeguards. Definition files contain no respondent data.                                    | Regulatory | High     | Open        |
| C-014 | Browser Support                    | UI must support the latest two versions of Chrome, Firefox, and Safari.                                                                                                                                                   | Technical  | Medium   | Open        |
| C-015 | Definition-File Contract           | Export and import must be byte-compatible with `ELICIT_SURVEY_EXPORT_V1` as implemented by Admin's `SurveyDefinitionExportService` / `ImportService` / `UpdateService` / `SurveyDefinitionFileFields`; Author ports that logic rather than inventing a format. | Technical  | High     | Open        |
| C-016 | Token Terminology                  | In Author, "token" means only a question-text placeholder (`{TOKEN\|default}`, `survey.relationships.token`). The credential a respondent enters to reach a survey is the **access code** and is issued by the Admin app; Author must not call it a token. | Business   | Medium   | Implemented |
| C-017 | Immutable Element Keys             | `survey_key` and every `{entity}_key` are minted once and never regenerated or rewritten; import carries them verbatim.                                                                                                     | Technical  | High     | Open        |
| C-018 | Working-Copy Model                 | In Author's database each element has exactly one row (`version = 0`, epoch `effective_from`, sentinel `effective_to` while current); Author never writes `version` and never creates draft rows.                            | Technical  | High     | Open        |
| C-019 | Removal Means Retire               | Author never physically deletes a structural element that may have been exported; removal closes `effective_to`, and exported files carry retired rows so Admin's update can close them at each site.                       | Technical  | High     | Open        |

## Open Items for Review

- **Type 1 element keys.** Survey `V015` added `report_key`, `post_survey_action_key`,
  `dimension_key`, `ontology_key` and `metadata_key`. Author maps and exports the last three (the
  reporting-tag tables); `reports` and `post_survey_actions` are still exported with zero records
  until they are modelled.
- **Ontology namespace.** `ontology.name` is a namespace the reporting ETL never reads;
  `(name, tag)` is unique across a database. Author writes the survey's name at tag creation and
  never rewrites it, so a later survey rename leaves earlier tags under the old namespace.
- **`is_draft` column.** Being dropped from the schema (Survey V015) and from the file format (Admin);
  Author never maps it.
- **Retire semantics in Admin.** Admin's update currently "never deletes" and does not close rows that
  the file marks retired; that change is tracked as an Admin-repo task and is required for FR-007 to
  take effect at deployed sites.
- **FR-028** assumes the meta-survey's questions map predictably onto target-survey structure; the
  mapping is not designed yet and is the riskiest FR in the catalog.
- No constraint sets a survey-size ceiling beyond NFR-001/NFR-002's 200 questions; FHHS scale is
  15 steps, 48+ questions, 82+ relationships, 67+ metadata mappings, 16 select groups.
