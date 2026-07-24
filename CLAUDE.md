# Elicit Author — Claude / AIUP Context

This project follows the **AI Unified Process (AIUP)**
([unifiedprocess.ai](https://unifiedprocess.ai/)). Treat the artifacts under
`docs/` as the source of truth for *what* this system is supposed to do; the
code under `src/` is the implementation that AIUP regenerates and refactors
around them.

> **Status:** This repository currently holds the planning and design docs for
> the survey **authoring** tool (`survey-authoring-development-plan.md`,
> `survey-requirements-collection-system.md`, `creating-fhhs-style-surveys.md`).
> There is no application code yet — the AIUP forward workflow drives it from
> `docs/vision.md` toward a Quarkus/Vaadin implementation matching the rest of
> the Elicit platform.

## Stack (target implementation)

- Java 21, Quarkus 3.37.x, Maven build
- Vaadin 25.2.x (Flow, server-side UI)
- Hibernate ORM with Panache (JPA) — **not jOOQ**
- PostgreSQL (shares the Elicit survey schema)
- Docker for deployment

When using the Quarkus Agent MCP tools once code exists, follow those tools'
instructions (`quarkus_update` / `quarkus_skills` for an existing project,
`quarkus_create` when scaffolding the app).

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
