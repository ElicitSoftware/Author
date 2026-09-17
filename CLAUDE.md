# Elicit Author — Claude / AIUP Context

This project follows the **AI Unified Process (AIUP)**
([unifiedprocess.ai](https://unifiedprocess.ai/)). Treat the artifacts under
`docs/` as the source of truth for *what* this system is supposed to do; the
code under `src/` is the implementation that AIUP regenerates and refactors
around them.

> **Status:** The Quarkus/Vaadin scaffold exists and boots; milestone 1
> (brand, OIDC shell, entities, survey list, `.elicit` import/export, editor
> navigation) is in progress on `feature/foundation`. The planning docs in the
> repo root (`survey-authoring-development-plan.md`,
> `survey-requirements-collection-system.md`, `creating-fhhs-style-surveys.md`)
> remain useful input for *what* authors need; `docs/` is authoritative for
> *how* Author works.

## How Author fits the platform (read this first)

Author's "publish" is **exporting a `.elicit` file** (`ELICIT_SURVEY_EXPORT_V1`,
defined by Admin's `SurveyDefinitionExportService`). Admin's *Apply Survey
Definition* installs it at a site or updates in place, matched by the UUID
element keys (`survey_key`, `question_key`, …) and versioned Kimball Type 2
style **at the site**. Author edits **plain working copies** in its own local
database (one row per element, `version 0`), never writes drafts or versions,
and expresses removals as retirements (`effective_to` closed). See
`docs/vision.md` and `docs/scd-contract.md`.

## Stack

- Java 25, Quarkus 3.39.2, Maven build
- Vaadin 25.2.7 (Flow, server-side UI)
- Hibernate ORM with Panache (JPA) — **not jOOQ**. Author owns **no
  production Flyway migrations**; it maps entities onto the `survey` schema that
  Survey's `db/migration` creates. Tests replay a verbatim mirror of those
  scripts (`src/test/resources/db/schema-mirror`, refreshed by
  `scripts/sync-survey-schema.sh`) into a Testcontainers Postgres.
- PostgreSQL — Author's **own** database instance carrying the `survey` schema
  (local stack in `docker-compose.yml`), never a deployed site's database.
  Connects as `survey_user`.
- OIDC / Keycloak authorization (matches Admin's setup); client
  `elicit-author`, role `elicit_author` (not implied by `elicit_admin`).
  Dev port **8084**.
- Docker for deployment

This is now an **existing project** for the Quarkus Agent MCP tools — start
with `quarkus_update` / `quarkus_skills` per those tools' instructions, not
`quarkus_create`.

## AIUP Workflow

The plugin `aiup-core` provides these methodology skills. Run them roughly in
this order; review and hand-edit each artifact before continuing.

1. `/requirements` — generates `docs/requirements.md` from `docs/vision.md`.
2. `/entity-model` — produces a Mermaid ER diagram and attribute tables.
3. `/use-case-diagram` — creates a PlantUML diagram with stable UC IDs.
4. `/use-case-spec UC-XXX` — writes detailed per-use-case specifications.
5. *(implementation + tests — stack-specific; we are not using `aiup-vaadin-jooq`
   because this project targets Hibernate/Panache, not jOOQ.)*

Steps 1–3 are done; per-UC specs live in `docs/use_cases/` (UC ids from
`docs/use_cases.puml`, revised 2026-09-17). `/reverse-engineer` becomes
relevant only once implementation code exists.

## Terminology

- **Access code** = the respondent credential (Admin issues it). **Token** =
  only a question-text placeholder (`{KEY|default}`, `relationships.token`).
- Versioning vocabulary: durable key, element key, version, current, retired,
  revision. Never `is_current`, `active`, or `is_draft`; never "publish" for a
  database state — the verb is **export**.

## Git Commits

- Never append a `Co-Authored-By: Claude` (or similar AI co-author) trailer to
  commit messages.

## Working Agreements

- Re-run upstream skills when requirements change so downstream artifacts
  (entity model, use-case specs) stay consistent.
- Tests must be traceable to a use case — reference the `UC-XXX` ID in test
  names or comments.
- The `docs/` folder is institutional memory. Commit it to version control.
- Do not mix in jOOQ-specific patterns; data access is Hibernate/Panache.
- Do not regenerate code that bypasses Vaadin component APIs (accessibility).

## Reference

- Vision: `docs/vision.md`
- Reference deployment: [FHHS](https://github.com/ElicitSoftware/FHHS)
- Marketplace: https://github.com/AI-Unified-Process/marketplace
