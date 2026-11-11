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

- Java 25, Quarkus 3.40.1 LTS, Maven build
- Vaadin 25.3.0 (Flow, server-side UI)
- Hibernate ORM with Panache (JPA) — **not jOOQ**. Author owns **no
  production Flyway migrations**; it maps entities onto the `survey` schema that
  Survey's `db/migration` creates. Tests replay a verbatim mirror of those
  scripts (`src/test/resources/db/schema-mirror`, refreshed by
  `scripts/sync-survey-schema.sh`) into a Testcontainers Postgres.
- PostgreSQL — Author's **own** database carrying the `survey` schema (the
  `author` database in the umbrella's `docker-compose.yml`), never a deployed
  site's database. Connects as `survey_user`.
- OIDC / Keycloak authorization (matches Admin's setup); client
  `elicit-author`, role `elicit_author` (not implied by `elicit_admin`).
  Dev port **8084**.
- Docker for deployment

## Author is a module of the umbrella

Author is the fifth module of the open-source Elicit platform, cloned by the
umbrella's `cloneAllProjects.sh` to `Elicit/Author/`. Everything it shares with
the platform lives in the umbrella, once, and nothing of it is duplicated here:

| In the umbrella (`..`) | What Author uses it for |
| --- | --- |
| `docker-compose.yml` | The stack: `author` (8084), `author-survey` (the preview Survey on the `author` database, 8085), `db`, `keycloak` and the site applications |
| `keycloak/elicit-realm.json` | The `elicit-author` client, its roles and the `author` / `author` test user |
| `db-init/V0.0.4__CREATE_AUTHOR_DATABASE.sql` | The `author` database, created when the cluster is first initialized |
| `elicit-brand/`, `elicit-i18n/author/` | The brand and the curated translations, mounted and read by the tests (`../elicit-i18n`) |
| `samples/`, `../FHHS/family-history-survey.elicit` | Definitions the tests import |
| `e2e-tests/`, `e2e_multisite/`, `e2e_multisite_multilingual/` | The platform's Playwright suites, which drive Author with Admin and Survey (see the umbrella's `CLAUDE.md`) |
| `buildDockerImages.sh`, `checkLanguages.sh`, `resetDatabase.sh`, `status.sh` | Building, checking and resetting the whole platform |

For dev mode, start what Author calls from the umbrella —
`docker compose up -d db keycloak author-survey` in `..` — and run Author on
8084. A change to the stack, the realm, the database script or a suite is made
in the umbrella, on a branch with the same name as the one here (Author's CI
clones the umbrella's same-named branch, else `V3`).

Author's integration branch is `main`; unlike Survey, Admin and FHHS it has no
`V3`.

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
  only a question-text placeholder (`{<KEY>|default}`, `relationships.token`).
- Versioning vocabulary: durable key, element key, version, current, retired,
  revision. Never `is_current`, `active`, or `is_draft`; never "publish" for a
  database state — the verb is **export**.

## Git Commits

- `main` changes only through a pull request whose CI check passed; a ruleset
  enforces it on GitHub and `.claude/hooks/branch-guard.py` (a `PreToolUse` hook in
  `.claude/settings.json`) refuses `git commit` and `git push` on `main`, `master`, `V2` or
  `V3` locally. Branch from `main` as `<type>/<issue>-<slug>`; never set the hook's
  `ELICIT_ALLOW_PROTECTED` override yourself.
- Never append a `Co-Authored-By: Claude` (or similar AI co-author) trailer to
  commit messages.

## Working Agreements

- **AIUP first, for every change.** Before writing or changing code, update the
  artifacts it implements: the FR row in `docs/requirements.md`, the use case in
  `docs/use_cases.puml`, and its spec under `docs/use_cases/` (`/use-case-spec`,
  validated with the skill's `validate_use_case.py --strict`). Code and tests
  then cite the spec's `UC-XXX` and `BR-XXX` IDs. A bug fix that changes no
  specified behavior is the only exception; a fix that does gets its spec
  corrected first.
- Re-run upstream skills when requirements change so downstream artifacts
  (entity model, use-case specs) stay consistent.
- Tests must be traceable to a use case — reference the `UC-XXX` ID in test
  names or comments.
- The `docs/` folder is institutional memory. Commit it to version control.
- Do not mix in jOOQ-specific patterns; data access is Hibernate/Panache.
- Do not regenerate code that bypasses Vaadin component APIs (accessibility).

## Reference

- Vision: `docs/vision.md`
- AI authoring spec: `docs/ai/AUTHORING_FROM_AI.md` — the survey model and the `.elicit`
  record format written for an AI agent, so an author can get a draft definition out of an
  AI session without Author running (UC-042, FR-048). Drafts carry `ELICIT_SURVEY_DRAFT_V1`,
  which only Author reads; `AuthoringFromAiDocTest` gates the document against the code
  (NFR-017), so change the document whenever the format, the lookup ids or validation change.
  The same file is packaged onto the classpath at `ai/` by a `<resource>` block in `pom.xml`
  and served role-gated at `/api/authoring-spec`, so `AiDraftView` (route `ai-draft`, FR-063)
  can hand it to an author who has no checkout — the third way to start a survey, beside the
  blank form and importing a definition. Edit the document under `docs/ai/`, never a copy.
- Reference deployment: [FHHS](https://github.com/ElicitSoftware/FHHS)
- Marketplace: https://github.com/AI-Unified-Process/marketplace
