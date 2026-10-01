# Validating Rule Direction: Origin Before Target

**Status:** implemented on `feature/rule-direction`, 2026-09-19, as FR-041 / UC-033
(`docs/use_cases/UC-033-validate-rule-direction.md`). The catalog entries below
were moved into `requirements.md`, `use_cases.puml` and UC-016 A4; the code lives in
`RuleOrdering`, `RuleService.validate`, `RelationshipQuery.edges`, `ExportValidation`,
`SurveyDesignerView.afterMove` and `elicit-rule-arrows.js`. Decisions taken while
building it are marked *Decided* in section 5.

Source: `docs/requirements.md` (FR-005, FR-016, FR-019, FR-021, FR-022),
`docs/use_cases/UC-007`, `UC-016`, `UC-019`, `UC-020`, `UC-029`,
`src/main/java/com/elicitsoftware/author/survey/RuleService.java`,
`RelationshipQuery.java`, `DisplayOrdering.java`, `DesignerService.java`,
`SurveyStructureQuery.java`,
`src/main/java/com/elicitsoftware/author/definition/ExportValidation.java`,
`src/main/java/com/elicitsoftware/author/flow/designer/SurveyDesignerView.java`,
`src/main/frontend/elicit-rule-arrows.js`, and the Survey runtime's
`src/main/java/com/elicitsoftware/QuestionManager.java` and
`QuestionService.java`. Sibling proposals: `docs/research/undo-redo.md`,
`docs/research/session-discard.md`.

## Question

A rule's origin must come before its target: it is impossible to show a
question in step 1 from a question in step 2, because the respondent has
already left step 1 when the answer that fires the rule is given. Author does
not check this today. A rule can be drawn backwards, and a rule that was
valid when drawn can be turned backwards by a reorder (UC-016) without anyone
noticing. An invalid rule should show in red on the board.

## Short Answer

Define "before" as a comparison of board positions — the step order, section
order and question order the designer already derives (UC-016 BR-001) — and
check it in three places that all call one new pure function:

1. **`RuleService.validate`** refuses to create or re-point a rule whose
   target is not after its origin, with the same kind of `AuthoringException`
   it already throws for a self-target (`RuleService.java:161-163`).
2. **`ExportValidation`** reports every backward rule as an error, so export
   is blocked (UC-007 BR-001) until the author fixes it.
3. **`RelationshipQuery.edges`** marks each backward edge, and the arrow
   overlay draws a marked edge in the error color with the reason in its
   tooltip.

A reorder is never refused. It is the one legitimate way a stored rule becomes
invalid, so after a move the board redraws with the affected arrows red and
the notification says how many rules now point backwards. The check is
read-only and derived on every render, like the arrows themselves
(UC-020 BR-001); nothing about validity is stored.

## 1. Why the order matters: what the runtime does

The Survey runtime never evaluates a rule ahead of time. A downstream element
exists for a respondent only after the upstream answer is saved:
`QuestionService.saveAnswer` (`QuestionService.java:257-288`) persists the
answer and only then calls `QuestionManager.buildDownstreamQuestions`
(`QuestionManager.java:591`), which creates the answers for the target. The
initial answers of a section deliberately exclude everything that is the
target of a SHOW or REPEAT rule (`QuestionManager.java:376-388`, `:451-454`).

The respondent's path is the list of section answers ordered by display key
(`buildNavItems`, `QuestionManager.java:1815-1820`), and a target's display
key is built from the target's display order (`buildDisplayKey`,
`QuestionManager.java:823-853`). So a target whose position sorts before the
origin is inserted *behind* the respondent: the forward path never showed it,
the progress bar gains an entry the respondent has already walked past, and
the only way to reach it is to go back. That is what "impossible to show a
question in step 1 from step 2" means in practice: the rule fires, the row
exists, and the respondent never sees it.

Two consequences shape the check:

- **A page is a section.** Navigation is by section display key
  (`QuestionManager.navigate`, `:280-293`), and a step is only the grouping in
  the progress bar. Ordering therefore has to compare sections, not just
  steps: a section earlier in the *same* step is as unreachable as one in an
  earlier step.
- **The same section is a special case.** After a save the runtime re-reads
  the section's answers sorted by display key
  (`getAnswersBySection`, `:325-341`), so a question target in the origin's
  own section is rendered on the page the respondent is looking at, even when
  its order is lower — it appears above the origin. That is reachable but
  surprising, and it is the one place where the strict rule and the runtime
  disagree. Section 5 recommends keeping the strict rule.

## 2. Defining "before"

Everything needed is already derived and kept contiguous by
`DisplayOrdering` (`DisplayOrdering.java:78-119`): steps carry
`display_order`, section assignments carry `(step_display_order,
section_display_order)` and their display key, and question assignments
carry `display_order` within their section. `SurveyStructureQuery` sorts the
board by exactly these values (`SurveyStructureQuery.java:73`, `:80`, `:94`).

Give every board anchor a **position** `(step, section, question)` of
integers, 0 meaning "the whole container":

| Anchor | Position |
|---|---|
| `step-N` | `(stepOrder, 0, 0)` |
| `ss-N` (section assignment) | `(stepOrder, sectionOrder, 0)` |
| `sq-N` (question assignment) | `(stepOrder, sectionOrder, questionOrder)` for each mount of its section |

The **origin** is the upstream question assignment. Every current mount of
its section is checked, one by one; `upstream_step_id` is *not* used to narrow
the check, because the runtime fires the rule from whichever mount the
respondent answers in (*Decided*, see BR-004).

A rule is **forward** when the target's position is strictly greater than the
origin's, comparing the tuple lexicographically, with one adjustment per
target kind:

| Target kind | Valid when | Why |
|---|---|---|
| step | `target.step > origin.step` | Showing or repeating the step the origin sits in is meaningless: the respondent is already on it. |
| section assignment | `(target.step, target.section) > (origin.step, origin.section)` | The origin's own section is already shown; an earlier one has been passed. |
| question assignment | `(target.step, target.section, target.question) > (origin.step, origin.section, origin.question)` | A later question in the same section is the ordinary follow-up case; an earlier one, or one on an earlier page, has been passed. |

**Multi-mount.** A section can be mounted in several steps (UC-015), and a
question assignment belongs to the section, not the mount, so one `sq` can
have several positions. The rule should be valid only if it is forward for
*every* pairing of an origin mount and a target mount; a rule that works in
one mount and not another will fail for some respondents, which is exactly
what the check exists to catch. This is a conservative choice; section 5
lists the alternative.

**What the check is not.** It does not look at operators, values, actions or
retired rows. Retired rules are drawn dashed and never validated
(`ExportValidation.java:69`, `RelationshipQuery.java:78-80`); a backward
retired rule is not an error until it is restored, and
`RuleService.restore` already runs `validate` (`RuleService.java:124`), so
restoring a rule that is backward now is refused with the reason.

## 3. Where to enforce it

Three call sites, one function. Put the comparison in a new
`RuleOrdering` bean in `com.elicitsoftware.author.survey` with a single
entry point that takes the current steps, section assignments and question
assignments of a survey (the same lists `ExportValidation.validate` and
`RelationshipQuery.edges` already load) and answers, for one `Relationship`,
an `Optional<String>` reason: empty when forward, otherwise text such as
"targets 'Consent' in step 1, before the question it reads in step 2". Loading
once and checking many keeps the board render at one query per table, as
today.

### (a) On write — `RuleService.validate`

Add the ordering check after the self-target check
(`RuleService.java:161-163`), throwing `AuthoringException` with the field
`"downstream"` so the rule dialog marks the target
(`RelationshipDialog.java:144`) and a drop that would create a backward
arrow is refused with the reason in the notification, the way UC-019 A3
refuses Repeat onto a step. `create`, `update`, `repoint` and `restore` all
pass through `validate` (`:60`, `:67`, `:105`, `:124`), so no new path is
needed. The import path does *not* call `RuleService` and must keep
accepting files as they are (C-015): an imported backward rule is shown red
and reported at export, never rejected on the way in.

### (b) On reorder — nothing is refused

`DesignerService.moveStep`, `moveSectionWithinStep` and
`moveQuestionWithinSection` (`DesignerService.java:206-245`) stay as they
are. Refusing a move because it inverts a rule would make large
restructurings impossible — the author often has to move the target first
and the origin second, and the intermediate state is backward. Instead,
after the move the view already rebuilds the board (`afterChange`,
`SurveyDesignerView.java:438`, `:444`, `:454`, `:500`), and the rebuilt
edges carry the new marks. The notification gains a count: "Moved step
'Family' — 2 rules now point backwards" — computed by diffing the marked
edges before and after the move, both of which the view has in hand.

### (c) On export — `ExportValidation`

In the relationships loop (`ExportValidation.java:118-140`) add one more
`ValidationIssue.error("relationships", r.relationshipId, …)` with the
reason text. This is what makes a red arrow block export (UC-007 BR-001,
FR-022), and it is what the survey overview's validation panel
(`SurveyEditorView.java:101-113`) and the export dialog
(`ExportDialog.java:53-58`) list, without any change to either.

## 4. Drawing it red

`RelationshipQuery.RuleEdge` (`RelationshipQuery.java:40-42`) gains two
fields, `boolean invalid` and `String problem`, filled from `RuleOrdering`
inside `edges` — the method already has every list the check needs
(`:47-74`). The JSON reaches the overlay unchanged through
`SurveyDesignerView.edgesJson` (`:737-743`).

In `elicit-rule-arrows.js`, `_renderEdge` builds the class list from the
edge's flags (`:340`); add `invalid` beside `retired` and `selected`, and
style it with the error tokens the rest of the app uses
(`styles.css:75-78` maps `--lumo-error-color` onto `--brand-error`, which
each brand defines, e.g. `brand-colors.css:90`):

```css
.edge.invalid path.line { stroke: var(--brand-error, var(--lumo-error-color, #c62828)); }
.edge.invalid .glyph circle, .edge.invalid .end { fill: var(--brand-error, var(--lumo-error-color, #c62828)); }
.edge.invalid .label { fill: var(--brand-error, var(--lumo-error-color, #c62828)); }
```

The arrowhead is a shared SVG marker (`:310`, `:343`), so an `invalid-head`
marker in the error color is needed alongside `head` and `pending-head`.
The glyph tooltip (`:351`) already concatenates label, condition and
description; prefix it with the problem text so hovering a red arrow says
why. Red on top of the accent color is a color-only cue, so the label
should also change: "Show Consent — points backwards" keeps the
information readable without color (NFR-001, UC-020 BR-003).

With **Show rules off** the arrows are hidden and so is the red. The
validation panel still lists the error, and the reorder notification still
counts it; a per-row badge on the board is possible but adds a second
rendering of rule state and is left out of the first cut.

Selection behavior is unchanged: a red edge selects, opens and re-points
exactly like any other (`:344-346`), which is how the author fixes it —
drag the head to a later target, or drag the tail to an earlier question.

## 5. Open questions and risks

- **Same-section, earlier question.** *Decided: strict.* The runtime *does*
  render such a target (section 1), above the origin, but UC-029's "order is
  the default path" and the arrow's left-to-right reading both promise that a
  rule reveals something further on. One refinement was needed for multi-mount:
  a downstream question in the upstream question's *own* section is compared
  within the same mount only (the runtime shows it on the page being answered),
  otherwise every same-section rule in a twice-mounted section would be red.
- **Question targets in another section.** For a question target the runtime
  copies the origin answer's step and section into the new key and only
  sets the question order (`QuestionManager.java:612`, `:827-851`), so a
  question shown across sections materialises on the *origin's* page, not
  where the board draws it. That is a runtime question outside this
  proposal, but it means the board's picture of a cross-section question
  rule is already approximate; the ordering check should not pretend to
  more precision than the runtime has, and the reason text should name
  pages ("in an earlier section") rather than rows.
- **Multi-mount strictness.** *Decided: strict* — forward for every mount
  pairing (UC-033 BR-004), with the same-section refinement above. The
  alternative, checking only the mount named by `upstream_step_id`, is simpler
  and wrong for the other mounts. Watch for complaints.
- **TEXT rules.** *Decided: checked like SHOW and REPEAT* (UC-033 BR-006).
  Traced: after an upstream save the runtime looks the downstream answer up by
  display key and rewrites its text (`QuestionManager.java:666-675`), and an
  answer's text is built with the upstream values known when it is created
  (`buildDipslayText`, `:1160-1191`). A backward TEXT rule therefore edits a
  page the respondent has already passed.
- **Imported surveys.** Existing `.elicit` files may contain backward rules
  that have "worked" only because nobody noticed. They will turn red on
  first open and block the next export. That is the intended effect, but the
  release note should say so.
- **Cost.** `RelationshipQuery.edges` already loads every current row of five
  tables per render; the check adds a map lookup per rule and mount, nothing
  more. `RuleService.validate` currently runs count queries per field
  (`:146-160`); the ordering check needs the positions of two assignments,
  two or three more small queries per save.
- **Database enforcement (considered, rejected 2026-09-19).** *Decided: not built.* A deferred
  constraint trigger on `relationships`, `steps`, `steps_sections` and
  `sections_questions` could refuse any commit that leaves a rule backward;
  a plain CHECK cannot, because the positions live in other tables. It was
  rejected for now: it would make a reorder fail, and the whole point of
  drawing the rule red instead is that the author can keep moving things
  until the rule is forward again, rather than deleting and redrawing it.
  A trigger would also reject imported files with backward rules (C-015),
  apply to the Survey runtime database through the shared migrations, and
  still need the Java check for a readable message. Revisit only if red
  arrows prove to be missed in practice.
- **Undo/redo and discard.** A move that turns rules red is undone by the
  inverse move (`docs/research/undo-redo.md`, section 1) and the red
  disappears with it because nothing was stored. A discard restore
  (`docs/research/session-discard.md`) likewise needs no special handling.

## 6. Proposed catalog entries

Superseded: added to `requirements.md` and `use_cases.puml` as **FR-041 /
UC-033** (FR-039 / UC-031 belong to `undo-redo.md`, FR-040 / UC-032 to
`session-discard.md`). The authoritative spec is
`docs/use_cases/UC-033-validate-rule-direction.md`; the draft below is kept for
the record and uses the old provisional numbers.

### Requirement row (Conditional Logic table)

| ID     | Title                              | User Story | Priority | Status |
|--------|------------------------------------|------------|----------|--------|
| FR-040 | Validate Rule Direction | As a survey author, I want a rule whose target comes before the question it reads — by step, section and question order — to be refused when I draw it, drawn in red with the reason when a reorder makes it backwards, and reported as an error that blocks export, so that I never ship a rule the respondent can never see fire. | High | Proposed |

### UC-033 Validate Rule Direction

```markdown
# Use Case: Validate Rule Direction

## Overview

**Use Case ID:** UC-033
**Use Case Name:** Validate Rule Direction
**Primary Actor:** Survey Author
**Goal:** Ensure every rule's target comes after the question it reads, so that the rule can take effect for a respondent moving forward through the survey.
**Status:** Proposed
**Requirements:** [FR-040, FR-005, FR-016, FR-019, FR-021, FR-022, NFR-001](../requirements.md)

## Preconditions

- The author is signed in with the `elicit_author` role.
- The designer board is open for a survey with at least one rule, or the author is creating one (UC-019).

## Main Success Scenario

1. The author reorders a step, section or question (UC-016).
2. The system renumbers positions and rebuilds the board.
3. For every current rule the system compares the target's position (step, section, question) with the origin's, for every mount of both.
4. The system draws each backward rule in the error color, with "points backwards" in its label and the reason in its tooltip, and reports in the move notification how many rules now point backwards.
5. The author drags the arrow's head to a later target, or its tail to an earlier question, or moves the elements again.
6. The system re-checks and draws the rule normally; export validation (UC-007) no longer lists it, with the goal achieved.

## Alternative Flows

### A1: Drawing a backward rule
**Trigger:** The author drops the arrowhead, or picks a target in the rule dialog, that is before the origin (UC-019 steps 3-5)
**Flow:** 1. The system refuses with the reason and marks the target field. 2. Use case continues at UC-019 step 3.

### A2: Restoring a rule that is now backward
**Trigger:** The author restores a removed rule (UC-010) whose target has since been moved before its origin
**Flow:** 1. The system refuses with the reason; the rule stays removed. 2. Use case ends.

### A3: Export with a backward rule
**Trigger:** The author chooses Export while a backward rule exists (UC-008)
**Flow:** 1. The system lists the rule as an error and keeps Export disabled (UC-007 A2). 2. Use case ends.

### A4: Show rules is off
**Trigger:** Arrows are hidden (step 4)
**Flow:** 1. The notification count and the validation panel still report the rules. 2. Use case continues at step 5.

## Postconditions

### Success Postconditions
- Every current rule's target is after its origin in every mount; nothing about validity is stored.

### Failure Postconditions
- A backward rule remains, drawn red and listed as an export error; the survey is otherwise unchanged.

## Business Rules

### BR-001: Forward means later on the respondent's path
A target is after its origin when its (step, section, question) position is greater, comparing step first; a step target must be a later step, a section target a later section, a question target a later question — later in the same section counts.

### BR-002: Reordering is never refused
A move that makes a rule backward is allowed and shown; only creating, re-pointing and restoring a backward rule are refused.

### BR-003: Validity is derived
Direction is computed from current positions on every render and at export; a reorder or re-point changes it with no separate write.

### BR-004: Every mount counts
A rule is valid only if it is forward for every mount of the origin's section and every mount of the target's section.

### BR-005: Color is never the only cue
A backward arrow also changes its label and tooltip, and is listed in text in the validation panel.
```

## 7. Tests

- `RuleServiceTest`: `create_refusesTargetBeforeOrigin` alongside
  `create_refusesTwoTargetsNoTargetAndSelfTarget` (`RuleServiceTest.java:147`):
  step target in the origin's own step, section target earlier in the same
  step, question target earlier in the same section; and
  `repoint_refusesBackwardHead`.
- `ExportValidationTest`: `ruleWhoseTargetPrecedesItsOriginIsAnError`
  using the `AuthorFixture` survey (`ExportValidationTest.java:38`) — move
  a step with `DesignerService.moveStep` inside `@TestTransaction` and assert
  a `relationships` error appears and the fixture is otherwise clean.
- `DesignerServiceTest`: a move is *not* refused when it inverts a rule,
  and `RelationshipQuery.edges` marks that edge `invalid` with a reason
  afterwards, mirroring `edges_anchorOnAssignmentsAndReadAsASentence`
  (`RuleServiceTest.java:68`).
- Multi-mount: a section mounted in steps 1 and 3, origin in step 2 —
  valid for one mount, backward for the other — is refused on create and
  red on the board.
