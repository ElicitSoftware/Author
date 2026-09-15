# Elicit Author — Vision

> **Draft.** This is the AIUP seed document. Edit freely — `/requirements`,
> `/entity-model`, and the rest of the AIUP workflow read from here. Existing
> planning docs in the repo root are useful input:
> `survey-authoring-development-plan.md`,
> `survey-requirements-collection-system.md`, `creating-fhhs-style-surveys.md`.
> **Caveat:** those three docs predate the Survey module's Kimball Type 2
> versioning schema (see Constraints below, and `docs/scd-contract.md`) and
> describe authoring as simple mutable-row CRUD with no draft/version/publish
> lifecycle. Treat them as useful for *what* authors need to do (question
> banks, branching, metadata, requirements-collection flow), not for *how*
> writes happen — the draft/publish mechanics below supersede anything they
> imply about direct in-place edits.

## Mission

Elicit Author is the survey **authoring** module of the
[Elicit Software](https://github.com/ElicitSoftware/) platform. It enables
researchers, educators, analysts, and other survey creators to build
sophisticated surveys through visual interfaces — defining steps, sections,
questions, branching logic, dynamic tokens, and dimensional metadata — and to
publish those definitions into the shared Elicit survey database that the
Survey (respondent) and Admin modules consume.

The shared `survey` schema versions every structural element — steps,
sections, questions, select groups/items, relationships, and the join tables
that connect them — using a Kimball Type 2 slowly-changing-dimension pattern:
each element has a durable key that never changes, an incrementing version
number, an effective-dated validity window, and an explicit draft/current
state. Author is the **only** application that writes to these versioned
tables (Survey and Admin only read them), so a core part of Author's mission
is giving authors a safe draft → publish workflow instead of letting edits
silently overwrite what past respondents already saw.

## Target Users

- **Survey authors** — researchers, educators, business analysts, and other
  designers who define question banks, branching logic, and dimensional
  metadata, then publish surveys.
- **Survey administrators** — staff (in the Admin app) who operate the surveys
  authors publish.
- **Respondents** — the eventual audience (in the Survey app) whose experience
  is shaped by what authors build here.

## Goals

- Provide a visual authoring experience for survey structure (steps, sections,
  questions) without hand-writing SQL.
- Capture branching logic, relationships, and conditional display rules.
- Support dynamic text / token requirements in question content.
- Model dimensional / ontology metadata for downstream analytics.
- Generate first-draft survey structures (e.g. SQL scripts) from a guided
  requirements-collection flow ("survey-to-build-surveys" meta-survey).
- Provide an explicit **draft → publish lifecycle** for every versioned
  structural element (steps, sections, questions, select groups, select
  items, relationships, and their join tables): authors edit a draft, and
  publishing closes the currently-open version and opens a new version under
  the same durable key — never an in-place mutation of a published row.
- Surface **version history** for structural elements: which version is
  current, the effective date range of past versions, and who published each
  change and why (`published_by` / `published_comment` are the only audit
  trail the schema keeps — there is no separate audit-log table).
- Publish survey definitions into the shared Elicit PostgreSQL schema,
  respecting that `surveys` itself is not versioned (a Type 1, in-place-edit
  table) while the eight structural tables above are.

## Scope

**In scope**
- Visual definition and editing of survey structure and content.
- Draft/version/publish lifecycle management for steps, sections, questions,
  select groups, select items, relationships, and their join tables —
  creating and validating drafts, and publishing them as new durable-key
  versions in the shared schema (never in-place edits to a published row).
- Version-history browsing for structural elements (current vs. past
  versions, effective date ranges, publish attribution/comments).
- Branching / conditional logic and dynamic token configuration.
- Ontology / dimensional metadata authoring, including reassigning a
  question's ontology tag — understanding that, unlike other structural
  edits, this is a **retroactive** change (see Constraints).
- Requirements collection and first-draft structure generation.
- Publishing survey definitions to the shared database.

**Out of scope** (handled by sibling modules)
- Rendering surveys and recording respondent answers → Survey module.
- Subject registration, token generation, invitation email dispatch → Admin app.
- Cross-survey analytics dashboards → downstream reporting tools.
- Changing how the reporting schema keys `dim_step`/`dim_section` — those
  dimensions are a stakeholder-approved Type 1 (non-versioned) exception in
  Survey's reporting ETL; Author does not alter that behavior, only needs to
  represent it accurately in any UI copy about version history.

## Constraints

- **License:** PolyForm Noncommercial 1.0.0.
- **Stack (do not deviate without explicit approval):**
  - Java 25
  - Quarkus 3.39.2
  - Vaadin 25.2.6 (Flow / server-side UI)
  - Hibernate ORM with Panache (JPA) — *not* jOOQ. Author owns no Flyway
    migrations of its own; it maps entities onto the tables Survey's
    `db/migration` already created in the shared `survey` database/schema —
    it is a pure editor over that schema, not a co-owner of it.
  - PostgreSQL
  - Maven build, Docker deploy
  - OIDC / Keycloak authorization, matching Admin's setup
- **Branching:** part of the multi-module Elicit Software platform; database
  schema and message contracts are shared with the Survey and Admin modules.
- **Versioning model (Kimball Type 2):** `questions`, `select_groups`,
  `select_items`, `sections`, `steps`, `sections_questions`,
  `steps_sections`, and `relationships` each carry a durable/business key
  stable across edits, an integer `version` (0-based), an
  `effective_from`/`effective_to` half-open validity interval (sentinels
  `1970-01-01` / `9999-12-31 23:59:59+00`), and `is_draft` /
  `published_by` / `published_comment` columns. **There is no `is_current`
  flag** — "current" means `effective_to = '9999-12-31 23:59:59+00'`. Author
  must use this vocabulary consistently (durable key, version, draft,
  publish, current) and must not introduce a competing `active`/`is_current`
  flag convention.
  - Cross-table references among these tables (join tables, `select_group_id`,
    etc.) point at the **durable key** plus a `_version` column that is a
    `CHECK`-pinned existence check equal to `0`, not at a specific versioned
    row. The current/historical version resolves at query time via the
    effective-dated interval, not via the foreign key. Author's UI can treat
    durable ids as the stable identity for wiring the structural graph
    (relationships, section/question assignments) and only needs to reason
    about version chains when editing or publishing content.
  - This `version` concept is distinct from, and must not be conflated with,
    the pre-existing **Display Key "instance" numbering**
    (`survey.step.step_instance.section.section_instance.question.question_instance`),
    which addresses repeated sections/questions within a single structural
    version, not revisions over time.
  - `surveys` remains **Type 1** (in-place edits, not versioned).
    `answers`, `dependents`, and `respondents` are untouched by this scheme;
    `answers.question_version` pins a respondent's answer to the exact
    question version shown to them (`answers.question_id` references the
    physical row, not the durable key).
  - Two retroactive-editing exceptions are stakeholder-signed-off and must be
    respected in Author's UI/help copy: (1) renaming a step or section
    relabels historical reports as well as future ones; (2) reassigning a
    question's ontology/metadata tag reclassifies historical answers as well
    as future ones. All other structural edits (question wording,
    section/step structure, relationships, select options) are
    **non-retroactive** — respondents keep seeing what was current when they
    started. Author must not give ontology/metadata tag reassignment or
    step/section renaming the same "safe, non-retroactive" affordances used
    for other edits.
  - Full technical design: `Survey/research/Kimball_type_2.md` in the
    sibling Survey repository, summarized for authoring purposes in
    `docs/scd-contract.md` (see below).
- **Compliance:** authored surveys may collect PHI in clinical deployments
  (e.g., the Family Health History Survey) — design with sensitive data in mind.
- **Accessibility:** Vaadin components should be used in a way that preserves
  the framework's built-in WCAG support; avoid raw HTML that bypasses it.

## Reference Deployment

The [Family Health History Survey (FHHS)](https://github.com/ElicitSoftware/FHHS)
is the canonical reference deployment of the Elicit platform and a model for the
kind of survey Author is meant to produce.
