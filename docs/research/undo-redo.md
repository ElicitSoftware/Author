# Undo and Redo in the Designer

**Status:** research / proposal, 2026-09-19. Nothing here is implemented.

Source: `docs/requirements.md` (FR-007, FR-008, NFR-012, C-011, C-012, C-017,
C-018, C-019), `docs/scd-contract.md`, UC-009, UC-010, UC-018, and the service
layer under `src/main/java/com/elicitsoftware/author/survey/`. Sibling
proposal: `docs/research/session-discard.md`.

## Question

An author asked: "Is there a way to use hot keys for undo and redo?" Today
there is none. The designer offers one inline Undo, after a section copy
(`SurveyDesignerView.java:866-887`, which simply removes the copy); the
details page offers Undo after a removal (`ElementView.java:81`, `:242`) and a
Restore button (`ElementView.java:78`). Note that UC-009 step 6 promises a
removal notice with Undo on the board, but `SurveyDesignerView.java:685` only
points at the details page. What would a general Cmd/Ctrl+Z and
Shift+Cmd/Ctrl+Z (or Ctrl+Y) take?

## Short Answer

Build a bounded **command history** in the designer: every write the view
makes through `DesignerService`, `RuleService`, `ListService`,
`ReportingService` and `ElementService` becomes a command that records the
before-state it needs to invert itself, and undo/redo replay those inverses
through the same services. Undo never deletes a row and never mints a key: an
undone create is a retirement, a redone create is a restore of the same row.
Snapshot-and-restore is the wrong tool for single steps (the importer creates
new surveys with new ids) and database savepoints cannot span Vaadin
requests. The history is also the natural foundation for "discard session".

## 1. What undo must mean

Every edit is committed immediately as a working copy (C-018): each service
method is its own `@Transactional` unit (`DesignerService.java:60`,
`RuleService.java:55`, `ListService.java:101`), so "undo" cannot mean "do not
save"; it means "issue the compensating edit". The compensating edit must
respect C-017 (keys never change), C-019 (removal is retirement) and the
derived ordering of UC-016 BR-001 (positions are renumbered 1..n and display
keys follow, `DisplayOrdering.java:78-131`).

| Operation (service) | Inverse | Notes |
|---|---|---|
| Create step / section / question (`DesignerService.java:61`, `:76`, `:90`) | `ElementService.remove` of the new element | Retire, do not delete. Redo = `ElementService.restore` of the same durable id, so the element key minted at `ElementService.java:79` survives undo/redo. |
| Rename step / section (`DesignerService.java:248`, `:259`) | Rename back with the saved copy | `RenameLedger.record` (`:252`) is append-only; undo must retract the entry, not add a second one. |
| Edit question fields (`DesignerService.java:285`) | `update` with the saved before-copy of the fields in `copy(Question, Question)` (`:416-430`) | |
| Move step / section / question (`DesignerService.java:207`, `:218`, `:232`) | Move back to the saved index | Renumbering is derived, so restoring the index restores the display keys. |
| Copy section into a step (`DesignerService.java:108`) | `removeSection` of the copy | Exactly today's copy Undo (`SurveyDesignerView.java:870`). Redo = restore. |
| Mount section / assign question elsewhere (`DesignerService.java:129`, `:140`) | Retire that assignment (`:151`, `:171`) | Never the last mount, since the element predates the command, so the branch at `:156`/`:175` does not fire. |
| Retire an assignment that is not the last one | Reopen that `steps_sections` / `sections_questions` row and the rules closed with it | **No service today**: `ElementService.restore` handles only STEP, SECTION and QUESTION (`ElementService.java:401-407`). A `restoreAssignment` is needed. |
| Remove step / section / question (`ElementService.java:169`) | `ElementService.restore` | The cascade set is recovered by the shared `closedAt` instant (`ElementService.java:234-237`, `:288`), so the inverse is exact. UC-010 A2 may park a restored step at the end; the command re-applies the saved position afterwards. |
| Restore (UC-010) | `remove` | |
| Rule create / edit / re-point / retire (`RuleService.java:56`, `:65`, `:74`, `:111`) | retire / `update` with saved copy / same / `restore` (`:118`) | |
| List create / edit / remove (`ListService.java:86`, `:102`, `:139`) | `removeSelectGroup` / replay the saved `OptionDraft` list, reopening options retired at `:127-128` / reopen the group and its items | Reopening retired items and groups needs new service code; `update` only creates and retires. |
| List assignment (`DesignerService.java:271`) | `assignList` with the previous group id | Only reachable when the question already had a list; a first assignment came with a create or an edit and is undone with it. |
| Tag create / edit / remove (`ReportingService.java:118`, `:137`, `:162`) | `removeTag` / `updateTag` back / re-create | `ontology` is Type 1 with a Type 1 `ontology_key` (scd-contract): remove is a physical delete, so the command must keep the row's key and value fields to re-insert it verbatim on undo. |
| Tag assign / reassign / unassign (`ReportingService.java:221`, `:256`, `:279`) | `unassign` / `reassign` back / re-insert | `unassign` calls `m.delete()` (`:284`). Undoing it must re-insert with the saved `metadata_key`, or a redo would mint a new key and Admin's update would see a delete plus an insert. Ledger entries (`:271`, `:286`) must be retracted. |

**Delete instead of retire for never-exported elements?** Tempting, because
an undone create leaves a retired row behind. Rejected for single commands:
the only "never exported" fact the schema holds is the survey-level stamp
(`ElementService.java:339`), and the existing Purge (UC-009 A3,
`purgeRemoved` at `:334`) already cleans such rows in bulk before the first
export. Undo therefore always retires; Purge remains the one physical delete.

**Ledgers.** `RenameLedger` and `ReportingChangeLedger` are append-only maps
keyed by survey (`RenameLedger.java:31-38`, `ReportingChangeLedger.java:32-37`),
read by `ExportValidation.java:160-163` and cleared only by
`SurveyService.stampExport` (`SurveyService.java:109-110`). Both need a
`retract(surveyId, kind, durableId)` so that rename → undo produces no export
warning, and redo records it again.

**Not undoable.** The export stamp (`SurveyService.java:98-110`): the file is
already downloaded and the ledgers cleared. Undo history is cut at an export.
Purge is likewise final.

## 2. Design options

**(a) Command stack in the UI.** A `DesignerHistory` object holding a bounded
deque of `Command { label(); undo(); redo(); target() }`. The view wraps each
write: `history.run(new RenameStep(designer, surveyId, before, after))`. The
before-state is captured just before the service call from the managed row
the view already loads (`designer.currentStep(...)` at
`SurveyDesignerView.java:461`). Commands live beside the services in
`com.elicitsoftware.author.survey.history` so `DesignerServiceTest`-style
Testcontainers tests can assert `run → undo` round-trips row for row.

**(b) Snapshot per step.** Serialize the survey through
`SurveyDefinitionExporter` before each edit and restore on undo. The importer
cannot do the restore: it allocates new ids from every sequence
(`SurveyDefinitionImporter.java:191-201`) and refuses a `survey_key` that
already exists (`:182-184`). An in-place restore would need a diff-apply
matched by element key, the job Admin's `SurveyDefinitionUpdateService` does
(`Admin/.../SurveyDefinitionUpdateService.java:51-70`). That is real code, it
would touch every row of fifteen tables per keystroke, and it still would not
know how to rewind the ledgers. Right tool for session discard, wrong tool for
one step.

**(c) Database savepoints or a long transaction.** Rejected. Each service call
commits on return; a Vaadin UI is many HTTP requests over minutes, and holding
a JTA transaction (and its row locks) across them is neither possible with
Quarkus's request-scoped transactions nor safe with two authors.

**Recommendation: (a).** Bound the history at **50 commands** per survey;
commands hold ids, a `closedAt` instant and small field copies (a `Question`
is a dozen strings), so the ceiling is a few tens of kilobytes. Redo is
cleared on a new command. A command whose undo is refused by a service (an
`AuthoringException`, say a step name that has since become a duplicate) is
reported and dropped rather than retried.

## 3. Interaction

Register on the view with `Shortcuts.addShortcutListener(this, this::undo,
Key.KEY_Z, KeyModifier.CONTROL)` and the `META` variant for macOS; redo as
`Shift` plus the same, and `Ctrl+Y`. `ShortcutRegistration.listenOn(board)`
scopes the keys to the board so a toolbox search field keeps the browser's
own text undo. Dialogs are modal and, in 25.2, a shortcut ignores keyboard
events from nested modals unless `allowEventsFromNestedModals()` is called,
so Cmd+Z inside `ElementDialog` or `QuestionDialog` stays a text-field undo.

Per NFR-012 the keys get button equivalents: **Undo** and **Redo** in the top
bar, disabled when their stack is empty, tooltip naming the next command
("Undo rename step 'Family'"). After an undo the view shows the existing
bottom-start toast (`SurveyDesignerView.java:928`) with an inline **Redo**
button, mirroring the copy notice. Each command carries a target
`StructureNode`; after `load()` the view calls `select(find(kind, durableId,
assignmentId))` (`:196`, `:366`), which survives the full board rebuild at
`:211` because nodes are matched by value, and `openAncestors` reopens the
lane. The details page's removal Undo and Restore, and the copy notice, should
push through the same history so a later Cmd+Z sees them.

## 4. Relationship to session discard

`docs/research/session-discard.md` proposes discarding everything since the
designer was opened or since the last export. Undo-all over the history covers
"since opened" only up to the 50-step bound and only for this UI; a
key-matched snapshot restore covers "since last export" regardless of who
edited. The two share one foundation: the history supplies the human-readable
list of changes the discard confirmation shows, is the fast path when it is
complete, and both must rewind the ledgers the same way. Build the history
first; the discard doc's restore is the fallback when the history is truncated.

## 5. Proposed catalog entries

Not added to `requirements.md` or `use_cases.puml`; paste from here.

### Requirements

Functional (Section & Step Organization table):

```
| FR-038 | Undo and Redo Designer Edits | As a survey author, I want to undo my last designer edits (create, rename, edit, move, copy, mount, remove, restore, rule, list and reporting-tag changes) and redo them, so that a slip is corrected in one keystroke without hunting for the element; undo retires and restores rows rather than deleting or re-creating them, so element keys are unchanged (C-017, C-019), and the export stamp is not undoable. | Medium | Proposed |
```

Non-functional (Usability):

```
| NFR-014 | Undo Shortcuts | Undo is bound to Cmd+Z (macOS) and Ctrl+Z, redo to Shift+Cmd+Z / Shift+Ctrl+Z and Ctrl+Y, active only while the designer board has focus and never inside a dialog or text field, with Undo and Redo buttons in the top bar as the keyboard-equivalent counterpart (NFR-012); the history holds at least the last 50 edits per survey. | Usability | Medium | Proposed |
```

### UC-031 Undo or Redo a Designer Edit

```
# Use Case: Undo or Redo a Designer Edit

## Overview

**Use Case ID:** UC-031
**Use Case Name:** Undo or Redo a Designer Edit
**Primary Actor:** Survey Author
**Goal:** Reverse the most recent designer edit, or reapply one just reversed, so that a mistake is corrected in place without changing any element's identity.
**Status:** Proposed
**Requirements:** [FR-038, NFR-012, NFR-014, C-017, C-019](../requirements.md)

## Preconditions

- The author is signed in with the `elicit_author` role and has the designer open.
- At least one edit has been made in this designer session (for undo) or undone (for redo).

## Main Success Scenario

1. The author presses Cmd/Ctrl+Z or chooses Undo in the top bar.
2. The system takes the most recent command from the history and applies its inverse through the same service that made the edit: a created element is retired, a retired one restored, a rename or field edit reverted, a move returned to its former index, a list or tag change replayed backwards.
3. The system retracts any rename or reporting-change ledger entry the edit recorded.
4. The system reloads the board, tree and breadcrumb, selects the element the edit concerned, and shows a notice "Undid <command>" with a Redo action.
5. The author presses Shift+Cmd/Ctrl+Z (or Ctrl+Y) or chooses Redo.
6. The system reapplies the command, restoring the same rows and keys, records the ledger entry again, and selects the element.

## Alternative Flows

### A1: Nothing to undo or redo

**Trigger:** The relevant stack is empty (step 1 or 5)
**Flow:**

1. The buttons are disabled and the shortcut does nothing.
2. Use case ends.

### A2: Inverse refused

**Trigger:** The service refuses the inverse, for example a step name that has since become a duplicate or a parent that is now removed (step 2)
**Flow:**

1. The system shows the service's message, drops the command from the history and changes nothing.
2. Use case ends.

### A3: New edit after undo

**Trigger:** The author makes a new edit while redo is available (after step 4)
**Flow:**

1. The redo stack is cleared.
2. Use case ends.

### A4: Export in between

**Trigger:** The author exports the survey (UC-008)
**Flow:**

1. The history is cleared; edits before the export are no longer undoable.
2. Use case ends.

## Postconditions

### Success Postconditions

- The survey's working copy is as it was before (undo) or after (redo) the command; every element keeps its durable id and element key.
- The ledgers describe only edits still in effect.

### Failure Postconditions

- The working copy is unchanged and the failed command is no longer in the history.

## Business Rules

### BR-001: Undo never deletes and never mints

An undone create is a retirement; a redone create is a restore of the same row. Keys are never regenerated (C-017, C-019).

### BR-002: The stamp is final

Exporting clears the history; the stamp and the ledger reset it performs are not undoable.

### BR-003: Bounded history

The history keeps the last 50 commands per survey for the life of the session.
```

## 6. Open questions and risks

- **Two authors on one survey.** Services are `@ApplicationScoped` over one
  database; a before-state captured in one session can overwrite another
  author's later edit. Mitigation: each command compares the row's current
  state with its recorded after-state and refuses ("changed since") on a
  mismatch, as A2 describes.
- **Undo across navigation.** Per-view history is lost on leaving the board;
  holding it in `VaadinSession` keyed by survey id keeps it across the
  details page and back. Which one, and whether the details page and
  `ElementView` removal Undo push onto it, is the main scoping decision.
- **Existing Undo and Restore.** The copy notice and the removal notice
  become thin wrappers over `history.undo()` when the last command is theirs;
  Restore from the details page becomes a command so that it is itself
  undoable. UC-009 step 6 and the board's "restored from its details page"
  copy at `SurveyDesignerView.java:685` should be reconciled either way.
- **Ledgers.** Retraction assumes an entry can be found by (survey, table,
  durable id); a survey renamed twice needs the original name kept so the
  export warning is dropped only when the name is back where it started.
- **Missing inverses.** Reopening a single retired assignment, a retired
  list and its options, and re-inserting Type 1 tag and metadata rows with
  their original keys are new service methods and need tests of their own.
- **Tests.** Round-trip tests per command in the `survey` test package
  (`run → undo` leaves rows equal except a retired-then-restored
  `effective_to`; `run → undo → redo` equals `run`, same keys), plus a flow
  test that the shortcut is inert with a dialog open.
