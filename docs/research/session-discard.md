# Discarding a Session's Changes

**Status:** research / proposal, 2026-09-19. Nothing here is implemented.

Source: `docs/vision.md`, `docs/requirements.md`, `docs/scd-contract.md`,
`docs/use_cases/UC-005`, `UC-008`, `UC-009`, `UC-010`,
`src/main/java/com/elicitsoftware/author/survey/SurveyService.java`,
`src/main/java/com/elicitsoftware/author/definition/SurveyDefinitionExporter.java`,
`SurveyDefinitionImporter.java`, Admin's
`src/main/java/com/elicitsoftware/service/SurveyDefinitionUpdateService.java`,
and the sibling proposal `docs/research/undo-redo.md`.

## Question

An author asked to "cancel an entire session if it hasn't been saved". What
can that mean in Author, and how should it be built?

## Short Answer

Author has no unsaved state. Every designer edit is committed to the working
database by an `@Transactional` service call; there is no draft row (C-018)
and `docs/scd-contract.md` records that Author never keeps versions. The only
thing that resembles "save" is export (UC-008), which stamps
`published_by` / `published_comment` and clears the two warning ledgers
(`SurveyService.java:98-113`). "Cancel the session" therefore has to become
**"put the survey back the way it was at a checkpoint"**, and the recommended
way to do that is to snapshot the survey with the existing exporter at the
checkpoint and restore it in place by element key, the way Admin's update
service applies a file onto an existing survey — minus the versioning.

## 1. What "session" and "saved" can mean

| Reading | Baseline | Fit |
|---|---|---|
| (a) since the designer was opened | the moment the view attached | What the author literally said, but "opened" is fuzzy in a browser: a refresh, a second tab, or navigating Overview → Design → Reporting each "opens" the designer again and would silently move the baseline. |
| (b) since the last export | the export stamp | Matches Author's vocabulary (export is the save) and needs no new concept, but is dangerous for a survey exported weeks ago and edited across many sittings, and undefined for a survey never exported (the baseline is creation, so discard means "empty survey"). |
| (c) explicit checkpoint | an author-set "start editing" point | Precise and cheap to explain; the cost is that the author must remember to set it. |

**Recommendation: (c), seeded by (b).** Export always takes a checkpoint, so
"Discard changes since last export" is available without the author doing
anything, and an explicit *Set checkpoint* action lets them narrow the window
mid-session. The checkpoint can never predate the last export, because export
moves it; that single rule is what keeps removals and new elements simple in
section 2. Whether opening the designer should also take a checkpoint
automatically is left open (section 5): it is what reading (a) wants, but the
refresh problem is real.

## 2. Design options

### (a) Snapshot on checkpoint, restore in place — recommended

At checkpoint time, run `SurveyDefinitionExporter.export(survey)` — it already
serializes the survey record, the eight Type 2 tables including retired rows,
and the reporting tables in one `@Transactional` call
(`SurveyDefinitionExporter.java:50-83`) — and store the bytes against the
survey with the instant taken. Validation errors must not block a checkpoint
the way they block export (`SurveyDefinitionExporter.java:51-55`), so the
serializer needs a `validate=false` entry point; otherwise nothing new is
needed on the write side.

Restore is a new `SurveyCheckpointService.restore(surveyId)` modelled on
Admin's `SurveyDefinitionUpdateService.updateFromFile`
(`SurveyDefinitionUpdateService.java:193`), which already parses the format,
matches every row by element key, never by local id
(`SurveyDefinitionUpdateService.java:50-58`), and treats a retired file row as
"close the current row" (`:1277-1281`). The Author port differs in one
respect: it never versions. Where Admin closes the current row and inserts
`version + 1` (`:60-66`, `closeCurrentVersion` at `:1335`), Author's restore
is Type 1 for every table, because C-018 allows exactly one row per element:

| Element at restore time | Snapshot says | Action |
|---|---|---|
| current, content differs | current | `UPDATE` the working row in place (keys, durable ids untouched — C-017) |
| current | retired at *t* | set `effective_to = t` (the element was restored after the checkpoint, UC-010) |
| retired | current | `effective_to = sentinel` (removed after the checkpoint, UC-009); same cascade rules as `ElementService.restore` (`ElementService.java:279-306`) |
| retired | retired | untouched |
| present | absent | created after the checkpoint and, by the rule in section 1, never exported: physically **delete**, in reverse dependency order. C-019 forbids deleting only elements that *may have been exported*, so this respects it; retiring instead would leave phantom rows in the Removed list and in every future file. |

The ledgers are in-memory maps (`RenameLedger.java:31`) that export clears
(`SurveyService.java:109-110`). Because the checkpoint never predates an
export, a discard to the export checkpoint clears both ledgers outright; for a
mid-session checkpoint each ledger entry needs a timestamp so entries after
the checkpoint can be dropped (`truncateTo(instant)`) and the export warnings
(`ExportValidation.java:161-163`) stay honest.

Why this option: it reuses the format Author is already obliged to keep
byte-compatible (C-015), it is testable through the round-trip machinery that
NFR-011 already demands, and the "what will be lost" summary in section 3
falls out of the same key-by-key diff the restore performs. A shared library
with Admin is tempting but premature: Admin's service is 1,348 lines of
Admin-owned semantics (site versioning, `survey_log`, revision guard) and
Author needs perhaps a third of it.

### (b) Undo-all on a command history

`docs/research/undo-redo.md` proposes a command stack; a discard would be
"undo every command back to the checkpoint marker". This is the right shape
*if* the stack exists, but it makes discard only as reliable as the inverse of
every command, it lives in the UI session (lost on refresh, invisible to a
second tab), and it cannot reach a change made by a path the stack does not
wrap (import, reporting view, a future API). A snapshot is a ground truth
that does not depend on replaying history. If both ship, discard should clear
the stack rather than drive it. Today's only undo is the eight-second
notification after copying a section (`SurveyDesignerView.java:870`).

### (c) A real draft copy

Duplicate the survey, edit the copy, promote on keep. This collides with the
one-working-copy-per-key rule: the importer rejects a `survey_key` already
present (`SurveyDefinitionImporter.java:132`, `:182-189`, UC-005 BR-002), the
schema's business-key uniqueness is scoped per survey, and two rows per
element key in one database is exactly what C-018 forbids. Promotion would
also have to rewrite durable ids on the original, which is option (a) with
extra steps.

### (d) Database-level snapshot

`pg_dump` / schema copy or a savepoint held open for the session. Rejected:
it is not portable to the desktop edition, cannot scope to one survey (shared
`dimensions` / `ontology`), and an open transaction across HTTP requests is a
non-starter in Quarkus.

## 3. UX

- **Where.** On the survey overview toolbar next to Export
  (`SurveyEditorView.java:63,72`) and in the designer toolbar beside
  *Overview* / *Add step* (`SurveyDesignerView.java:120-121`), a tertiary
  button *Discard changes since …* whose label names the baseline ("since
  export on 19 Sep 14:02" or "since checkpoint 14:37"), disabled when the
  survey matches the snapshot. A small *Set checkpoint* action sits in the
  same overflow; the overview's attribution line ("Last exported by …",
  `SurveyEditorView.java:97-100`) gains "Checkpoint: …".
- **Confirmation.** A dialog listing what will be lost, computed from the
  key-by-key diff: "3 questions edited, 1 section added (will be deleted),
  2 options removed (will be restored), 1 step renamed", plus the ledger
  warnings that will be cleared. Confirm is destructive-styled and requires
  typing nothing; the diff is the safeguard.
- **Keyboard.** No single-key shortcut — a discard is too destructive for a
  chord to fire by accident. `Esc` closes the dialog; `Enter` does not
  confirm it.
- **Export stays the save.** The dialog and the FR text both say so; the
  feature is named *discard*, never *revert to saved*, to avoid implying a
  saved state that Author does not have.

## 4. Proposed catalog entries

Not added to `requirements.md` or `use_cases.puml`; for the maintainer to paste.

### Requirement row (Survey Definition Lifecycle table)

| ID     | Title                              | User Story                                                                                                                                                                                                                    | Priority | Status |
|--------|------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------|--------|
| FR-040 | Discard Unsaved Session Changes    | As a survey author, I want to set a checkpoint on a survey (taken automatically at every export) and later discard every change made since it — edits restored in place by element key, removals reopened, never-exported additions deleted — after seeing a count of what will be lost, so that I can abandon an editing session without exporting and without Author keeping versions. | Medium   | Proposed |

### UC-032 Discard Unsaved Session Changes

```markdown
# Use Case: Discard Unsaved Session Changes

## Overview

**Use Case ID:** UC-032
**Use Case Name:** Discard Unsaved Session Changes
**Primary Actor:** Survey Author
**Goal:** Return a survey to the state captured at its checkpoint — the last export or an explicit checkpoint — so that an editing session can be abandoned without exporting.
**Status:** Proposed
**Requirements:** [FR-040, FR-007, FR-008, C-015, C-017, C-018, C-019, NFR-011](../requirements.md)

## Preconditions

- The author is signed in with the `elicit_author` role.
- The survey has a checkpoint (every export takes one; the author may set one explicitly).

## Main Success Scenario

1. The author chooses Discard changes since … on the survey overview or in the designer.
2. The system compares the survey with the checkpoint snapshot by element key and shows counts of elements edited, added, removed and restored since the checkpoint, and the export warnings that will be cleared.
3. The author confirms.
4. The system, in one transaction, updates changed elements in place, reopens elements removed since the checkpoint, re-closes elements restored since it, deletes elements created since it, and drops ledger entries recorded since it.
5. The system reloads the board and reports what was restored, with the goal achieved.

## Alternative Flows

### A1: Nothing to discard
**Trigger:** The survey matches the snapshot (step 1)
**Flow:** 1. The control is disabled and shows the checkpoint time. 2. Use case ends.

### A2: Author cancels
**Trigger:** The author closes the dialog (step 3)
**Flow:** 1. Nothing is changed. 2. Use case ends.

### A3: Restore fails
**Trigger:** A row cannot be written (step 4)
**Flow:** 1. The transaction rolls back; the survey and ledgers are as before step 4. 2. The system reports the failure. 3. Use case ends.

### A4: Setting a checkpoint
**Trigger:** The author chooses Set checkpoint (before step 1)
**Flow:** 1. The system serializes the survey as an export would, without blocking on validation errors, and stores it with the instant. 2. Use case ends.

## Postconditions

### Success Postconditions
- Every element's content, validity window and reporting tags equal the snapshot; keys and durable ids are unchanged.
- Elements created since the checkpoint no longer exist.
- The ledgers hold only entries from before the checkpoint.

### Failure Postconditions
- The survey and ledgers are unchanged.

## Business Rules

### BR-001: Export is the save
Discard never restores to a point before the last export; exporting moves the checkpoint.

### BR-002: Identity survives a discard
Matching is by element key; local ids are never reallocated (C-017).

### BR-003: Deletion only for the never-exported
An element absent from the snapshot was created after the last export and is deleted; anything that may have been exported is retired, never deleted (C-019).

### BR-004: Working copies, not versions
Restore writes the one working row in place; it never creates a version row (C-018).
```

## 5. Open questions and risks

- **Where the snapshot lives.** Author owns no Flyway migrations
  (`docs/vision.md`, Constraints); a `survey.author_checkpoints(survey_id,
  taken_at, definition bytea)` table means a Survey migration or the
  Author-only migration set the desktop proposal already needs. In-memory
  storage would vanish on restart like the ledgers do today.
- **Two authors, one survey.** Author has no per-survey lock. A discard by
  one author silently wipes the other's work; the confirmation dialog should
  at least show who took the checkpoint, and the ledgers' `published_by`
  overwrite (`SurveyService.java:87`) is the only hint that someone else
  edited.
- **An export between checkpoint and discard.** Handled by making export
  take a checkpoint, but that means a mid-session checkpoint is discarded by
  an export; the export dialog should say so.
- **Size.** The samples are 10–18 KB (`samples/*.elicit`); the importer's
  5 MB limit (UC-005) is a sensible cap and a snapshot is one row per
  checkpoint, replaced rather than accumulated.
- **Tests.** A restore test is the NFR-011 round trip with edits in between:
  checkpoint, mutate every kind of element, discard, assert the export bytes
  match the snapshot ignoring header timestamps. Cascade cases (a section
  removed and its questions reassigned) need explicit coverage.
- **Automatic checkpoint on opening the designer.** Attractive, but a page
  refresh would overwrite the baseline unless "open" is debounced to "no
  checkpoint newer than the last export exists".
- **Interaction with undo/redo.** If the command stack ships first, discard
  must clear it; if discard ships first, the stack must record the checkpoint
  as a barrier.
