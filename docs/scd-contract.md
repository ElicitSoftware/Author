# Kimball Type 2 SCD Contract (Author-facing summary)

This is a short, Author-facing summary of the Kimball Type 2
slowly-changing-dimension (SCD) pattern that the shared `survey` schema uses
for structural survey elements, and of **how Author relates to it**. It exists
so authoring code and docs use consistent terminology without re-deriving the
design from scratch.

**Sources of truth.**
- Schema mechanics (columns, constraints, trigger, as-of resolution):
  `Survey/src/main/resources/db/migration/V001__Create_Survey_Schema.sql`
  (+ `V013` for the trigger's final form) and `Survey/research/Kimball_type_2.md`.
- What Author writes and reads: the `.elicit` definition-file contract in Admin —
  `SurveyDefinitionExportService` (class javadoc = format spec),
  `SurveyDefinitionImportService`, `SurveyDefinitionUpdateService`,
  `SurveyDefinitionFileFields`, and Admin UC-013/014/017/018.
- `Kimball_type_2.md`'s "Workflow 1: Survey Alteration — Draft and Publish
  Phases" and its statements that "the Author Tool" publishes in-database or owns
  the SCD columns are **superseded** by the file pipeline below. Where that doc
  and Admin's implementation disagree about Author, Admin's implementation wins.

## Why the schema is versioned

> "Researchers need to iterate on survey instruments over time — rephrasing
> questions for clarity, renaming a section, adjusting conditional logic in
> relationships, or removing questions that perform poorly. Today the schema
> has no versioning concept. Any in-place update silently overwrites the
> definition that historical respondents saw."
> — `Survey/research/Kimball_type_2.md`

Every structural element can be independently versioned **at a deployed
site**, and a respondent's experience is pinned to a point-in-time snapshot
determined by `respondents.first_access_dt`.

## Where Author sits

```
Author working DB ──export──▶ survey.elicit ──apply (Admin)──▶ site DB (versioned, pinned)
```

- Author edits **working copies** in its own database: exactly one row per
  element, `version = 0`, `effective_from` = epoch, `effective_to` = the
  `9999-12-31 23:59:59+00` sentinel while current. Author never writes
  `version` and never creates draft rows.
- **Export** emits current rows (sentinel `effective_to`) and retired rows
  (closed `effective_to`); the file's per-row `version`/`effective_*`/
  `published_*` fields are informational.
- **Admin install** (first apply at a site) inserts every current row as
  version 0 / current and skips retired rows.
- **Admin update** (later apply) matches rows by element key: identical →
  untouched; changed → closes the site's current row at `now()` and inserts
  `version + 1` under the **same durable id**; new key → new row; file row
  retired → closes the site's current row; key absent from the file → left
  alone. The site derives its own version numbers; the only cross-site
  identifiers are the UUID keys and the file's `survey_revision`.
- Author therefore needs no draft/publish service. Its obligations are: keep
  keys stable (below), write `0` into every `_version` FK companion column,
  and express removals as retirements.

## Four identity concepts — don't conflate them

| Concept | Column(s) | Identifies | Scope |
|---|---|---|---|
| Primary key | `id` | One physical row (one version of one entity at one site) | per database |
| Durable key | `{entity}_id` (e.g. `question_id`) | "The same logical entity" across its versions; what join tables reference | per database (reallocated on import) |
| FK-target composite | `UNIQUE({entity}_id, version)` | Lets a durable id be a real FK target | per database |
| **Element key** | `{entity}_key` uuid, `survey_key` on `surveys` | The same authored element across **separate deployments** and across Author ↔ site | global, minted once, never regenerated |

| Table | Durable key | Element key |
|---|---|---|
| `surveys` (Type 1) | `id` | `survey_key` |
| `questions` | `question_id` | `question_key` |
| `select_groups` | `select_group_id` | `select_group_key` |
| `select_items` | `select_item_id` | `select_item_key` |
| `sections` | `section_id` | `section_key` |
| `steps` | `step_id` | `step_key` |
| `sections_questions` | `sections_question_id` | `sections_question_key` |
| `steps_sections` | `steps_sections_id` | `steps_sections_key` |
| `relationships` | `relationship_id` | `relationship_key` |
| `translations` | `translation_id` | `translation_key` |
| `reports`, `post_survey_actions`, `dimensions`, `ontology`, `metadata` (Type 1) | `id` | `report_key`, `post_survey_action_key`, `dimension_key`, `ontology_key`, `metadata_key` — **expected by Admin's export/update, added by Survey `V015`** |

Element keys are `uuid NOT NULL` with `UNIQUE({entity}_key, version)` and an
index on `({entity}_key, effective_from, effective_to)`. Admin's import/update
carry them verbatim; a generated definition (e.g. the meta-survey sample) uses
UUIDv5 from stable names so regeneration yields identical keys.

`translations` (Survey `V019`) is the ninth versioned table and the only one
that is not structural. It carries the core columns and the trigger unchanged,
for the same reason the others do: a respondent who started under one wording
keeps seeing it, and a corrected translation does not rewrite what earlier
respondents read. What differs is how it attaches to the rest of the schema.
The eight reference their neighbours by a durable id in a companion column with
a `_version` sentinel; a translation names what it translates by that element's
**element key** alone, in a generic `(element_type, element_key, field,
language)` target, and holds no durable id of any other table. So no FK
companion column, no `_version` sentinel, and no cascade when a question is
versioned: the new version keeps `question_key`, and the translation still
attaches. Its own durable id, `translation_id`, versions the translation itself
exactly as `question_id` versions a question.

Two consequences for Author. Retiring an element must close its translations at
the same instant, because nothing else links them (UC-043 A3, `ElementService`);
and editing an element's base text must **not** touch them, because a wording
change is a translator's problem, not a structural one — the translation goes
out of date by hash instead (UC-043 BR-003).

## Core columns on every versioned table

- `{entity}_id` — durable key, default `nextval('{table}_durable_seq')`.
- `{entity}_key` — element key (above).
- `version integer NOT NULL DEFAULT 0`.
- `effective_from timestamptz DEFAULT '1970-01-01 00:00:00+00'`,
  `effective_to timestamptz DEFAULT '9999-12-31 23:59:59+00'` — half-open
  interval. **There is no `is_current` flag.** Current means
  `effective_to = '9999-12-31 23:59:59+00'`; retired means any earlier value.
- `published_by text`, `published_comment text` — on structural rows, whatever
  the applied file carried; on `surveys`, what Author stamped at export
  (Admin's install leaves them NULL; update copies them from the file).
- `is_draft boolean` — **being dropped** (Survey `V015`, both migration tracks)
  together with the `{table}_one_draft_un` indexes and the trailing `is_draft`
  field of every `.elicit` record. Nothing produces or consumes draft rows.
  Author does not map it.

Constraints: `UNIQUE({entity}_id, version)`, `UNIQUE({entity}_key, version)`,
partial unique `{table}_one_current_un` on the durable id `WHERE effective_to =
sentinel` (at most one current row per durable id), and business-key
uniqueness re-scoped to current rows (e.g. `select_groups(survey_id, name)`).

The `BEFORE INSERT` trigger `scd_close_predecessor(durable_col)` (V013 form):
when an inserted row claims to be current and a predecessor current row exists,
it stamps `effective_from = now()` (if it was epoch) and closes the predecessor
at that instant; with no predecessor it leaves epoch alone. It does not fire
on `UPDATE`. Author's plain inserts of new elements therefore keep epoch.

## How foreign keys resolve (and why there's a `_version` column)

A durable key like `question_id` is **not unique by itself** — every version
row shares it. Postgres foreign keys must reference a column set with a real
`UNIQUE`/PK constraint, and the only one on `questions` is
`UNIQUE (question_id, version)`. Join and reference tables therefore carry a
companion column pinned to the constant `0` by a `CHECK`, e.g. on
`sections_questions`:

```sql
question_version integer NOT NULL DEFAULT 0,   -- FK companion, not this row's own version
CONSTRAINT sections_questions_ref_versions_ck CHECK (question_version = 0 AND section_version = 0),
FOREIGN KEY (question_id, question_version) REFERENCES survey.questions (question_id, version)
```

Version `0` is the row created when the durable key is first minted; Type 2
never deletes rows, so it exists forever and the FK is an entity-existence
check. It carries no information about which version applies at a given
moment. **Resolving which version is current (or was current at some past
instant) always requires the time-range predicate:**

```sql
-- Authoring / live path
SELECT * FROM survey.questions
 WHERE question_id = :durableId AND effective_from <= NOW() AND effective_to > NOW();

-- Respondent-facing path (Survey app, QuestionManager.resolveAsOf)
SELECT * FROM survey.questions
 WHERE question_id = :durableId
   AND effective_from <= :firstAccessDt AND effective_to > :firstAccessDt;
```

**Practical implications for Author:**
- Model cross-references as plain durable-id integers, not JPA associations on
  the durable column (a second version row at a site makes such an association
  ambiguous).
- Always write `0` into `select_group_version`, `step_version`, `section_version`,
  `question_version`, `upstream_*_version`, `downstream_*_version`.
- In Author's own database the time-range predicate degenerates to "the one
  row whose `effective_to` is the sentinel", which is how the editor decides
  what is current versus removed.

## What gets written to `answers` at response time (site side)

`answers.question_id` references the physical `questions.id` (surrogate) of
the row the respondent saw and denormalizes its `version` into
`answers.question_version`; `answers.section_question_id` likewise pins the
exact `sections_questions.id`. `answers` also stores `text_value` and
`display_text`. Consequences for authoring:

- Retiring or re-versioning a question never touches recorded answers.
- A physical `DELETE` of a question row that has answers fails on the FK —
  another reason removal is a retirement, never a delete.
- Reports resolve select-item labels by `coded_value`, without an effective
  range filter, so retired options still label historical answers.

## Removal = retirement, end to end

| Where | What happens |
|---|---|
| Author | `effective_to = now()` on the working row (cascade to dependent assignments/relationships); the row stays and can be restored. |
| Export | Retired rows are included with their close instant. |
| Admin install | Retired rows are skipped. |
| Admin update | A retired file row that matches a current site row closes that row at `now()`. Respondents whose `first_access_dt` predates it keep seeing the element; new respondents do not. |

## Retroactive exceptions (stakeholder-signed-off 2026-09-12)

Two behaviors are deliberate exceptions to "respondents keep seeing what was
current when they started", reviewed and accepted by Matthew Demerath
(`Survey/docs/plans/kimball-stakeholder-review.md`):

1. **Renaming a step or section** relabels historical reports as well as
   future ones (`surveyreport.dim_step`/`dim_section` stay Type 1, rekeyed by
   durable id).
2. **Reassigning a question's ontology/metadata tag** (`metadata` is Type 1
   and references durable keys) reclassifies historical answers as well as
   future ones.

Both take effect at a site when it applies the exported file. Author warns
about them at the edit and again at export (FR-005, FR-018, FR-024) and must
not give them the same "safe, non-retroactive" affordances used for other
edits.

## Terminology guardrails

- **Version** (a site's Type 2 revision of an element) ≠ **Display Key
  "instance"** (`survey.step.step_instance.section.section_instance.question.question_instance`),
  which numbers repeated occurrences within one definition. Do not use
  "instance" and "version" interchangeably.
- **Revision** is the file-level `# survey_revision:` header (the export
  instant), the only identifier comparable across sites; Admin refuses a file
  whose revision predates the one it already applied.
- **Display keys** carry the survey id as their leading component; Admin's
  import rebases it onto the id the site allocates, so Author may use any
  survey prefix.
- There is no `is_current` boolean and, after Survey `V015`, no `is_draft`.
  Don't invent either. "Publish" is not a database state in Author; the verb
  is **export**.
