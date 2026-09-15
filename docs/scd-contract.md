# Kimball Type 2 SCD Contract (Author-facing summary)

This is a short, Author-facing summary of the Kimball Type 2
slowly-changing-dimension (SCD) pattern that the shared `survey` schema uses
for structural survey elements. It exists so authoring code and docs use
consistent terminology without re-deriving the design from scratch.

**Source of truth:** `Survey/research/Kimball_type_2.md` (in the sibling
Survey repository). If this file and that one ever disagree, the Survey doc
wins — update this file to match, don't let it drift.

## Why this exists

> "Researchers need to iterate on survey instruments over time — rephrasing
> questions for clarity, renaming a section, adjusting conditional logic in
> relationships, or removing questions that perform poorly. Today the schema
> has no versioning concept. Any in-place update silently overwrites the
> definition that historical respondents saw."
> — `Survey/research/Kimball_type_2.md`

The fix: every structural element can be independently versioned, and a
respondent's experience is pinned to a point-in-time snapshot determined by
`respondents.firstAccessDt`.

## Versioned tables

**The primary key of every versioned table is still the original surrogate
`id` column** — e.g. `CONSTRAINT questions_pk PRIMARY KEY (id)`
(`Survey/src/main/resources/db/migration/V001__Create_Survey_Schema.sql:362`).
The SCD work didn't change the PK; it added new columns and secondary
constraints alongside it. Three distinct identity concepts coexist on each
of these tables — don't conflate them:

| Concept | Column(s) | Identifies |
|---|---|---|
| Primary key | `id` | One specific physical row — one specific version of one entity |
| Durable/business key | `{entity}_id` (e.g. `question_id`) | "The same logical entity," shared by every version row it's ever had — not unique by itself |
| FK-target composite | `UNIQUE(question_id, version)` | A secondary constraint (not the PK) that exists only so join tables can reference "this durable id, some version" via a real `FOREIGN KEY` — see below |

Each versioned table has a durable/business key (stable across all versions
of that row, generated from a `_durable_seq` sequence) plus the core columns
listed below:

| Table | Durable key column |
|---|---|
| `questions` | `question_id` |
| `select_groups` | `select_group_id` |
| `select_items` | `select_item_id` |
| `sections` | `section_id` |
| `steps` | `step_id` |
| `sections_questions` | `sections_question_id` |
| `steps_sections` | `steps_sections_id` |
| `relationships` | `relationship_id` |

## Non-versioned tables

- **`surveys`** — Type 1 (in-place edit). Renaming a survey doesn't change
  what respondents already saw of its structural elements.
- **`answers`** — event record, not versioned itself. Carries
  `question_version`, which pins the exact version of the question a
  respondent saw. `answers.question_id` references the physical surrogate
  row (`id`), not the durable key — this is the actual freeze mechanism for
  historical answers.

  **Note:** `answers.question_id` is a plain single-column `FOREIGN KEY
  (question_id) REFERENCES questions (id)` — no `_version = 0` companion
  trick here, and none is needed, because `questions.id` is already unique
  on its own (it's the PK). This is different from the join-table pattern
  above, where the composite `_version = 0` sentinel is *structurally
  required* to get a DB-enforced FK at all (the durable key alone isn't
  unique there). `answers.question_version` is, by contrast, pure
  denormalization: since `answers.question_id` already pins to one
  immutable row (Type 2 rows never change `version` in place), the version
  is always derivable via a join — it's duplicated onto `answers` only to
  avoid that join for reporting/ETL joins into `dim_question`, not because
  anything would break without it.
- **`dependents`, `respondents`** — untouched. `respondents.firstAccessDt` is
  the snapshot anchor that determines which version of each element a given
  respondent's session resolves to.
- **`metadata`** — retargeted to durable keys (so ontology tagging survives
  re-versioning of the thing it tags), but not itself Type 2 versioned.
- **`ontology`, `dimensions`** — untouched (stable vocabulary tables).
- **`surveyreport.dim_step` / `dim_section`** — intentionally stay **Type 1**
  in reporting (gained a durable-key column for ETL rekeying, but remain
  current-name-only). This is a deliberate exception, not an oversight — see
  "Retroactive exceptions" below.

## Core columns on every versioned table

- `{entity}_id` — the durable key (see table above).
- `version integer NOT NULL DEFAULT 0` — incremented per new version.
- `effective_from timestamptz DEFAULT '1970-01-01 00:00:00+00'`
- `effective_to timestamptz DEFAULT '9999-12-31 23:59:59+00'` — half-open
  interval. **There is no `is_current` flag.** "Current" means
  `effective_to = '9999-12-31 23:59:59+00'`.
- `is_draft boolean NOT NULL DEFAULT false`
- `published_by text`, `published_comment text` — this doubles as the only
  audit trail; there is no separate audit-log table.

Constraints:
- `UNIQUE (durable_id, version)`
- Partial unique index on `durable_id` `WHERE effective_to = '9999-12-31
  23:59:59+00'` — at most one current row per durable id.
- Partial unique index on `durable_id` `WHERE is_draft = true` — at most one
  draft per durable id.
- Business-key uniqueness (e.g. `select_groups(survey_id, name)`) is
  re-scoped to current rows only, since a retired row and its successor
  legitimately share the old business key.

The original surrogate `id` column is kept on every table — it's what
`answers` and other event rows pin to the exact version row a respondent saw.

## How foreign keys resolve (and why there's a `_version` column — this is still Type 2, not Type 3)

A durable key like `question_id` is **not unique by itself** — every version
row of a question shares it. Postgres foreign keys must reference a column
set with an actual `UNIQUE`/PK constraint, and the only one on `questions` is
the composite `UNIQUE (question_id, version)`. So a join table referencing
"a question" has to supply a value for both columns, even though it doesn't
actually want to pin to one specific version — it wants "this question
exists"; *which* version is current or historical is resolved separately, at
query time, via the `effective_from`/`effective_to` interval.

**Worked example — don't confuse "unique in the parent" with "referenced only
once."** `UNIQUE(question_id, version)` lives on `questions` and guarantees
at most one row per *pair*, ever (version numbers per durable id strictly
increment and are never reused, even after a row is superseded):

| `questions.id` (PK) | `question_id` (durable) | `version` |
|---|---|---|
| 501 | 1 | 0 |
| 502 | 1 | 1 |
| 503 | 2 | 0 |

`(1,0)` and `(2,0)` are different pairs even though both contain `0` — the
constraint is on the pair, not on `version` alone — so `(1,0)` names exactly
one row (501), forever. That uniqueness lives on the **parent** side
(`questions`). It says nothing about how many **child** rows may reference
that pair — many rows in `sections_questions` are allowed, and expected, to
all point at the same parent row (that's just what a many-to-one foreign key
is, no different from many `orders` rows sharing one `customer_id`):

| `sections_questions.id` | `question_id` (→ durable) | `question_version` (always `0`) | `section_id` |
|---|---|---|---|
| 9001 | 1 | 0 | 5 |
| 9003 | 1 | 0 | 6 |

Both rows correctly reference `(1,0)` — i.e. both point at the same parent
row 501, because question 1 has been assigned into two different sections.
Postgres's FK check only requires a matching row to *exist* in `questions`
for `(1,0)`; it never requires that only one child row may reference it.

The trick: pin that second FK column to the constant `0` via a `CHECK`
constraint, e.g. on `sections_questions`
(`Survey/src/main/resources/db/migration-v3/V010__Kimball_Type2_SCD.sql:344-386`):

```sql
ADD COLUMN question_version integer NOT NULL DEFAULT 0,   -- FK companion, not sections_questions's own version
...
ADD CONSTRAINT sections_questions_ref_versions_ck CHECK (question_version = 0 AND section_version = 0);
...
-- version = 0: entity-existence check. Time-range join resolves current version per respondent.
FOREIGN KEY (question_id, question_version) REFERENCES survey.questions (question_id, version);
```

Version `0` is the row created when the durable key is first minted. Type 2
SCD never deletes rows — only closes date ranges and inserts new ones — so
that version-0 row exists physically forever, meaning `CHECK(question_version
= 0)` always has a real row to satisfy the FK against, permanently. It is
**not** tracking a "previous value" the way a Type 3 SCD column would (e.g. a
`previous_address` column alongside `current_address`) — it's a constant
sentinel that lets "this durable key exists" be expressed as a real foreign
key in a schema where the natural key isn't unique on its own. Note this
companion `_version` column is distinct from the join table's *own* `version`
column (e.g. `sections_questions.version`), which is that row's real Type 2
version — `sections_questions` is itself independently versioned.

**Is the column actually necessary, given it's always `0`?** Yes, but only
because of a specific, deliberate design choice: the schema wants PostgreSQL
itself to enforce, at the database level, that a join table's durable-key
reference points to an entity that actually exists — without a service-layer
orphan check or a DB trigger. Per `Kimball_type_2.md`'s Open Question 4
("Application layer enforcement," **Resolved**): "No additional DB triggers
or service-layer orphan checks are needed for entity-existence enforcement."
A `FOREIGN KEY` can only target a column set with a real `UNIQUE`/PK
constraint (a partial unique index, like `{table}_one_current_un`, doesn't
qualify), and the durable key alone can't carry one (it's deliberately
repeated across version rows) — so the composite `UNIQUE(durable_id,
version)` plus the `_version = 0` sentinel is what makes a real, DB-enforced
FK possible at all. Drop the column and you'd have to drop the FK
constraint too, falling back to app-level checks instead.

**Why not snapshot the real version at write time instead, for a faster
lookup?** Because that's the right idea for `answers` and the wrong idea for
join tables — the difference is *whose* point in time matters.
`answers.question_id`/`answers.question_version` do exactly this: pin to the
exact surrogate row current when the respondent answered, because an answer
is a one-time, immutable event — the write instant *is* the meaningful
snapshot instant, and no future reader needs a different one. A join table
like `sections_questions` is not an event; a question's content and its
assignment to a section move on independent timelines, so pinning
`question_version` to "whatever was current when this join row was written"
would go stale the moment the question is revised again, and correctness
requires re-resolving as of *each reader's own* instant (`NOW()` for authoring,
`respondents.firstAccessDt` per respondent) — never the join row's creation
time. Keeping it correct by rewriting the pin on every version bump is
exactly the FK-cascade problem durable keys exist to avoid (see "Resolving
the FK Cascade Problem via Durable Keys" in `Kimball_type_2.md`). It's also
not actually trading away performance: `questions_active_range_idx` on
`(survey_id, effective_from, effective_to)` makes the time-range resolution
an indexed range scan, not a table scan.

This means the durable id never changes across versions, so join-table rows
never need to be re-pointed when the thing they reference gets a new
version.

**The `_version` FK companion column can never substitute for the
`effective_from`/`effective_to` query.** It is hardcoded to `0` forever by
the `CHECK` constraint — it does not track "the current version," "the
latest version," or any version at all. It carries no information about
which version applies at a given moment; it exists purely so a durable id
that lacks a standalone unique constraint can still be an FK target.
**Resolving which version is current (or was current at some past instant)
always requires the time-range predicate:**

```sql
-- Live/authoring path — resolve today's current row for a durable id:
SELECT * FROM survey.questions
 WHERE question_id = :durableId
   AND effective_from <= NOW() AND effective_to > NOW();

-- Respondent-facing path — resolve the row a specific respondent saw:
SELECT * FROM survey.questions
 WHERE question_id = :durableId
   AND effective_from <= :firstAccessDt AND effective_to > :firstAccessDt;
```

(`Survey/research/Kimball_type_2.md:81-82,173-175,185-203`.) The respondent
path uses `respondents.firstAccessDt` as the snapshot instant, not `NOW()` —
that's the mechanism that freezes a respondent to the version they started
with even after a researcher publishes a newer one mid-study.

**Practical implication for Author:** the authoring UI can treat durable ids
as the stable identity of "a question" / "a section" / etc. when wiring up
the structural graph (relationships, section-question assignments) — but any
code that needs to know *which version is current or was current at some
point in time* must always query `effective_from`/`effective_to`, never the
FK's `_version` column.

## What gets written to `answers` at response time

Continuing the worked example above (`questions.id` 501 = durable `1`,
version `0`, superseded; `id` 502 = durable `1`, version `1`, current):
writing an answer is a two-step resolve-then-copy, and the pair that ends up
stored is **not** `(durable_id, version)` — it's `(surrogate_id, version)`.

**Step 1 — resolve the applicable row** via the time-range predicate
(`Kimball_type_2.md:81-82,173-175`):

```sql
SELECT * FROM survey.questions
 WHERE question_id = 1
   AND effective_from <= :snapshot AND effective_to > :snapshot;
-- :snapshot = NOW() live, or respondents.firstAccessDt for a returning respondent
```

This yields exactly one row — here, `id = 502` (durable `1`, version `1`).

**Step 2 — what's actually written to `answers`**
(`V001__Create_Survey_Schema.sql:548-552`):

```sql
question_id      = 502   -- the resolved row's SURROGATE id, not the durable key "1"
question_version = 1     -- copied from that same resolved row's version column
```

The pair written is `(502, 1)` — surrogate id + version — never `(1, 1)`
(durable id + version). The durable key `1` is not stored on `answers` at
all; recovering it later means joining `questions.id = answers.question_id`
and reading `questions.question_id` off that row. This is exactly the
mechanism described earlier for `answers`: a plain single-column FK to the
surrogate PK (no `_version = 0` companion trick needed, since `questions.id`
is already unique on its own), plus a denormalized copy of `version` for
reporting/ETL convenience.

`answers.section_question_id` gets the same surrogate-pinning treatment
(`FOREIGN KEY (section_question_id) REFERENCES sections_questions (id)`),
resolved via `sections_questions`' own `effective_from`/`effective_to` at
the same snapshot instant — but the schema does not denormalize a separate
`section_question_version` column for it; only `question_id`/`question_version`
get that extra column.

## Retroactive exceptions (stakeholder-signed-off)

Two behaviors are deliberate, non-obvious exceptions to "respondents keep
seeing what was current when they started" — reviewed and accepted by
Matthew Demerath, 2026-09-12 (`Survey/docs/plans/kimball-stakeholder-review.md`):

1. **Renaming a step or section** relabels historical reports as well as
   future ones (`dim_step`/`dim_section` stay Type 1 — see above).
2. **Reassigning a question's ontology/metadata tag** reclassifies historical
   answers as well as future ones.

All other structural edits (question wording, section/step structure,
relationships, select options) are non-retroactive. Author's UI/help copy
must not give ontology-tag reassignment or step/section renaming the same
"safe, non-retroactive" affordances used for other edits.

## Terminology guardrails

- **Version** (Kimball Type 2 revision of a structural element's definition
  over time) is a different concept from the pre-existing **Display Key
  "instance" numbering**
  (`survey.step.step_instance.section.section_instance.question.question_instance`),
  which numbers *repeated occurrences* of a step/section/question within one
  published version (e.g. a repeating "list another diagnosis" section). Do
  not use "instance" and "version" interchangeably.
- There is no `is_current` boolean anywhere in this schema. Don't invent one.

## Open design surface for Author

No reusable "create new version" / "publish draft" / "supersede" service
pattern exists anywhere in the Elicit codebase yet (Survey, Admin, and FHHS
have all been checked). The pattern is documented as SQL in
`Survey/research/Kimball_type_2.md` (close-then-insert: `UPDATE ...
RETURNING version` followed by `INSERT` with `version + 1`) and exercised
only by a raw-SQL acceptance-test suite in Survey
(`Survey/src/test/java/com/elicitsoftware/scd/`) and one real, one-off manual
migration in FHHS
(`FHHS/src/main/resources/db/migration/V0.0.8__REORDER_CANCER_QUESTIONS_DURABLE.sql`).

`Kimball_type_2.md` explicitly defers "the Author Tool UI (draft editing
screens, publish-workflow controls, version-history views)" to a separate,
later effort — that's this project. Author's entity-model and use-case-spec
work is what defines the actual write pattern (draft creation, publish,
discard-draft) for the first time, not something to adopt from elsewhere.
