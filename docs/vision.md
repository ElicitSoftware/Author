# Elicit Author — Vision

> This is the AIUP seed document. `/requirements`, `/entity-model`, and the rest of
> the AIUP workflow read from here. The planning docs in the repo root
> (`survey-authoring-development-plan.md`, `survey-requirements-collection-system.md`,
> `creating-fhhs-style-surveys.md`) remain useful for *what* authors need to do
> (question banks, branching, tokens, metadata, requirements collection). They predate
> two platform changes that this document reflects and they do not: the Kimball Type 2
> versioning of the shared schema, and the **`.elicit` definition-file pipeline** that
> Admin now implements for installing and updating surveys. Where they describe writing
> straight into a shared production database, this document supersedes them.
>
> **Revision note (2026-09-17):** an earlier draft of this vision described Author as
> the only writer to the shared schema's versioned tables, with an in-database
> draft → publish lifecycle. That was overtaken by Admin's export / import / update
> work (Admin UC-013, UC-014, UC-017, UC-018, 2026-09-12..15) and Survey's portable
> `*_key` identities (2026-09-14). Author's "publish" is exporting a file.

## Mission

Elicit Author is the survey **authoring** module of the
[Elicit Software](https://github.com/ElicitSoftware/) platform. It enables
researchers, educators, analysts, and other survey creators to build
sophisticated surveys through visual interfaces — defining steps, sections,
questions, branching logic, dynamic tokens, and dimensional metadata — and to
**export those definitions as `.elicit` files** that any Elicit deployment
installs or updates through its Admin app.

Author works in its **own** database (a local instance of the shared `survey`
schema, created by the Survey module's migrations). It never connects to a
production survey database. The hand-off between authoring and deployment is
the definition file:

```
Author  ── edit working copy ── validate ── export survey.elicit ──▶  target site
                                                                     Admin "Apply Survey Definition"
                                                                       first time: install (all rows version 0)
                                                                       later:      update in place, matched by element key
                                                                                   (new version rows at the target;
                                                                                    respondents pinned to what they started with)
```

Because the target side versions and pins on its own, Author does not need a
draft/publish lifecycle of its own. What it must get right is **identity**:
every survey and every structural element carries a UUID key
(`survey_key`, `question_key`, `step_key`, …) that is minted once when the
element is created and never regenerated, so that a later export of the same
survey updates the same rows at every site instead of installing a second copy.

## Target Users

- **Survey authors** — researchers, educators, business analysts, and other
  designers who define question banks, branching logic, and dimensional
  metadata, then export surveys for deployment.
- **Survey administrators** — staff (in the Admin app) who apply the exported
  definitions and operate the surveys.
- **Respondents** — the eventual audience (in the Survey app) whose experience
  is shaped by what authors build here.

## Goals

- Provide a visual authoring experience for survey structure (steps, sections,
  questions) without hand-writing SQL or definition files.
- Capture branching logic, relationships, and conditional display rules.
- Support dynamic text / token requirements in question content.
- Model dimensional / ontology metadata for downstream analytics.
- Import an existing `.elicit` definition for further editing, preserving its keys.
- Validate a definition (dangling references, malformed rules, missing options)
  before it can be exported.
- Export definitions in the `ELICIT_SURVEY_EXPORT_V1` format that Admin's
  apply workflow consumes, stamping who exported it and a release note, and
  minting the file's `survey_revision`.
- Express **removals** so they reach deployed sites: a removed element is
  retired (its validity window closed), exported as retired, and closed at the
  target by Admin's update — never deleted, so recorded answers survive.
- Generate first-draft survey structures from a guided
  requirements-collection flow ("survey-to-build-surveys" meta-survey).
- Present the tool itself in each author's language (browser or on-page choice),
  including right-to-left layout, with translations a deployment can mount
  without rebuilding.
- Teach the definition format to an AI agent: a self-contained specification an
  author can hand to any AI session, away from Author, and get back a draft
  `.elicit` file that Author imports and they refine here.
- Let authors preview a survey as a respondent using the Survey app in the
  local authoring stack.
- Teach the tool itself: an in-app guide to the survey model and a printable
  author's manual, built from the documentation sources and shipped inside the
  application image so its instructions always match the running version.

## Scope

**In scope**
- Visual definition and editing of survey structure and content in Author's
  working database.
- Branching / conditional logic and dynamic token configuration.
- Ontology / dimensional metadata authoring, including reassigning a
  question's ontology tag — with an explicit warning that this is a
  **retroactive** change at deployed sites (see Constraints).
- Validation, export, and import of `.elicit` definition files.
- Removing and restoring structural elements (retire semantics).
- Requirements collection and first-draft structure generation.
- The AI-facing specification of the survey model and the `.elicit` record
  format, and importing the drafts written from it. A draft carries its own format
  marker so it reaches a deployed site only after review and export here.
- Author-facing documentation: the in-app survey-model guide and the printable
  author's manual packaged with the application.

**Out of scope** (handled by sibling modules)
- Installing or updating a definition at a deployed site → Admin app
  (`survey-apply`, `POST /api/secured/survey/apply`), which also owns the
  per-site audit trail (`survey.survey_log`) and the revision-regression guard.
- Version history of a deployed survey → lives in each site's database.
- Rendering surveys and recording respondent answers → Survey module.
- Subject registration, access code generation, invitation email dispatch → Admin app.
- Cross-survey analytics dashboards → downstream reporting tools.
- Changing how the reporting schema keys `dim_step`/`dim_section` — those
  dimensions are a stakeholder-approved Type 1 (non-versioned) exception in
  Survey's reporting ETL; Author only needs to represent it accurately in UI copy.

## Constraints

- **License:** PolyForm Noncommercial 1.0.0.
- **Stack (do not deviate without explicit approval):**
  - Java 25
  - Quarkus 3.39.5
  - Vaadin 25.2.7 (Flow / server-side UI)
  - Hibernate ORM with Panache (JPA) — *not* jOOQ.
  - PostgreSQL. Author runs against **its own** database instance carrying the
    shared `survey` schema. Author owns no production Flyway migrations; the
    schema is created by Survey's `db/migration`. (A verbatim, test-only mirror
    of those scripts may be replayed into a throwaway test database.)
  - Maven build, Docker deploy
  - OIDC / Keycloak authorization, matching Admin's setup; Author uses its own
    Keycloak client (`elicit-author`) and role (`elicit_author`).
- **Definition-file contract:** Author must stay byte-compatible with the
  `ELICIT_SURVEY_EXPORT_V1` format as implemented by Admin's
  `SurveyDefinitionExportService` / `SurveyDefinitionImportService` /
  `SurveyDefinitionUpdateService`. Those classes and Admin's UC-013/014/017/018
  are the source of truth for the format, not this repository.
- **Identity:** `survey_key` and every `{entity}_key` are minted once
  (`UUID.randomUUID()` or a stable UUIDv5 for generated content) and never
  regenerated; import carries them verbatim. Durable integer ids
  (`question_id`, …) and surrogate ids are per-database and disposable.
- **Working-copy model:** in Author's database each element has exactly one
  row — `version = 0`, `effective_from` = epoch, `effective_to` = the
  9999-12-31 sentinel while current, or a real close instant once removed.
  Author never writes `version` and never creates draft rows. (The schema's
  `is_draft` column is being dropped in Survey V015; Author does not map it.)
- **Versioning at the target (Kimball Type 2):** the eight structural tables
  (`questions`, `select_groups`, `select_items`, `sections`, `steps`,
  `sections_questions`, `steps_sections`, `relationships`) are versioned by
  Admin's update service at each site; `surveys` is Type 1. The vocabulary is
  durable key, element key, version, current, retired, revision. There is no
  `is_current` flag and Author must not introduce one.
  - This `version` concept is distinct from the **Display Key "instance"
    numbering** (`survey.step.step_instance.section.section_instance.question.question_instance`),
    which addresses repeated sections/questions within one definition.
  - Two retroactive-editing exceptions are stakeholder-signed-off
    (2026-09-12) and take effect when a site applies the file: (1) renaming a
    step or section relabels historical reports as well as future ones;
    (2) reassigning a question's ontology/metadata tag reclassifies historical
    answers as well as future ones. Author must warn about both at export and
    must not give them the same "safe" affordances as other edits.
  - Technical design: `Survey/research/Kimball_type_2.md` (schema mechanics;
    its in-database Author Tool workflow section is superseded) summarized for
    authoring purposes in `docs/scd-contract.md`.
- **Compliance:** authored surveys may collect PHI in clinical deployments
  (e.g., the Family Health History Survey) — design with sensitive data in mind.
- **Accessibility:** Vaadin components should be used in a way that preserves
  the framework's built-in WCAG support; avoid raw HTML that bypasses it.

## Reference Deployment

The [Family Health History Survey (FHHS)](https://github.com/ElicitSoftware/FHHS)
is the canonical reference deployment of the Elicit platform and a model for the
kind of survey Author is meant to produce. `samples/ElicitDesigner.elicit` is a
worked example of the file Author exists to produce.
