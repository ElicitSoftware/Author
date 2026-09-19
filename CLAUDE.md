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
- PostgreSQL — Author's **own** database carrying the `survey` schema (the
  `author` database in the umbrella repo's compose stack), never a deployed
  site's database. Connects as `survey_user`.
- OIDC / Keycloak authorization (matches Admin's setup); client
  `elicit-author`, role `elicit_author` (not implied by `elicit_admin`).
  Dev port **8084**.
- Docker for deployment

## MCP Servers and Skills

Use the configured MCP servers and their skills instead of raw Maven, web
search, or recalled API knowledge.

**Quarkus (`quarkus-agent` plugin)** — this is an **existing project**: start
with `quarkus_update`, then `quarkus_skills` for every extension you are about
to touch; never `quarkus_create`. Look up Quarkus configuration and APIs with
`quarkus_searchDocs`, not Context7 or web search. Manage dev mode with
`quarkus_start` / `quarkus_stop` / `quarkus_status` / `quarkus_logs`; reload
after code changes via `quarkus_callTool` → `devui-logstream_forceRestart`,
and do a full stop/start after any `pom.xml` change. Run tests through
`quarkus_callTool` → `devui-testing_runTests` (or `devui-testing_runTest`
with a class name), not `mvn test`, and never `mvn clean` while dev mode is
running. `quarkus_searchTools` lists the Dev MCP tools on the running app;
re-run it after adding or removing an extension. The full workflow
(extension-first rule, error recovery) is in [AGENTS.md](AGENTS.md).

**Vaadin (`vaadin-skills` plugin)** — before writing Flow code, call
`get_vaadin_primer` and `get_new_apis` for version `25.2`; API added after
the model's training data is API it cannot know it is missing. Call
`get_java_symbol` before concluding a method or enum constant does not exist
(it lists inherited members too). Use `search_vaadin_docs` (ui_language
`java`, vaadin_version `25.2`), `get_component_java_api`,
`get_component_styling`, and `get_theme_css_properties` for component and
theme questions. Skills: `/vaadin-form-layout` for forms and entity editors,
`/vaadin-frontend-design` for view polish, `/aura-theme` for theme CSS. Do
not target 25.3; it is unreleased.

**IntelliJ IDEA (`idea` MCP server)** — `.run/Author.run.xml` is the
Quarkus dev-mode run configuration; launch it with
`execute_run_configuration` (see `get_run_configurations`) rather than
hand-rolling `mvn quarkus:dev`. When the project is open in the IDE, prefer
`search_symbol`, `get_symbol_info`, `analyze_calls`, `get_file_problems`,
`lint_files`, `rename_refactoring`, `reformat_file`, and `build_project` over
grep-and-edit. The database tools (`list_database_connections`,
`introspect_schema`, `execute_sql_query`, `preview_table_data`) can inspect
the local `survey` schema; the `xdebug_*` tools set breakpoints and step
through a running debug session.

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
