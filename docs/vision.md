# Elicit Author — Vision

> **Draft.** This is the AIUP seed document. Edit freely — `/requirements`,
> `/entity-model`, and the rest of the AIUP workflow read from here. Existing
> planning docs in the repo root are useful input:
> `survey-authoring-development-plan.md`,
> `survey-requirements-collection-system.md`, `creating-fhhs-style-surveys.md`.

## Mission

Elicit Author is the survey **authoring** module of the
[Elicit Software](https://github.com/ElicitSoftware/) platform. It enables
researchers, educators, analysts, and other survey creators to build
sophisticated surveys through visual interfaces — defining steps, sections,
questions, branching logic, dynamic tokens, and dimensional metadata — and to
publish those definitions into the shared Elicit survey database that the
Survey (respondent) and Admin modules consume.

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
- Publish survey definitions into the shared Elicit PostgreSQL schema.

## Scope

**In scope**
- Visual definition and editing of survey structure and content.
- Branching / conditional logic and dynamic token configuration.
- Ontology / dimensional metadata authoring.
- Requirements collection and first-draft structure generation.
- Publishing survey definitions to the shared database.

**Out of scope** (handled by sibling modules)
- Rendering surveys and recording respondent answers → Survey module.
- Subject registration, token generation, invitation email dispatch → Admin app.
- Cross-survey analytics dashboards → downstream reporting tools.

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
- **Compliance:** authored surveys may collect PHI in clinical deployments
  (e.g., the Family Health History Survey) — design with sensitive data in mind.
- **Accessibility:** Vaadin components should be used in a way that preserves
  the framework's built-in WCAG support; avoid raw HTML that bypasses it.

## Reference Deployment

The [Family Health History Survey (FHHS)](https://github.com/ElicitSoftware/FHHS)
is the canonical reference deployment of the Elicit platform and a model for the
kind of survey Author is meant to produce.
