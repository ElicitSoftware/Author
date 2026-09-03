# Elicit Author — Claude / AIUP Context

This project follows the **AI Unified Process (AIUP)**
([unifiedprocess.ai](https://unifiedprocess.ai/)). Treat the artifacts under
`docs/` as the source of truth for *what* this system is supposed to do; the
code under `src/` is the implementation that AIUP regenerates and refactors
around them.

> **Status:** The Quarkus/Vaadin scaffold exists and boots, wired into the
> shared platform's database and OIDC conventions (matching `../Survey` and
> `../Admin`), but has no authoring domain code yet (no entities, no views).
> The planning docs in the repo root (`survey-authoring-development-plan.md`,
> `survey-requirements-collection-system.md`, `creating-fhhs-style-surveys.md`)
> remain useful input for the AIUP forward workflow that will add that code.

## Stack

- Java 25, Quarkus 3.39.2, Maven build
- Vaadin 25.2.6 (Flow, server-side UI)
- Hibernate ORM with Panache (JPA) — **not jOOQ**. Author owns **no Flyway
  migrations**; it maps entities onto the tables Survey's `db/migration`
  already created in the shared `survey` database/schema.
- PostgreSQL (shared Elicit `survey` schema)
- OIDC / Keycloak authorization (matches Admin's setup)
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

**Greenfield:** run the forward workflow starting from `docs/vision.md`. The
existing planning docs in the repo root are useful input for `/requirements`.
`/reverse-engineer` becomes relevant only once implementation code exists.

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
