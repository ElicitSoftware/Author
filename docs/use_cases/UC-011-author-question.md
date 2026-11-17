# Use Case: Author Question

## Overview

**Use Case ID:** UC-011  
**Use Case Name:** Author Question  
**Primary Actor:** Survey Author  
**Goal:** Create a question of a supported type inside a section, or edit an existing one, with its text, validation and choice list, from the designer board.  
**Status:** Implemented  
**Requirements:** [FR-010, FR-011, FR-013, FR-014, FR-035, FR-036, NFR-007, NFR-012, NFR-028](../requirements.md)

## Preconditions

- The author is signed in with the `elicit_author` role.
- The designer board is open and the survey has at least one section mounted in a step.

## Main Success Scenario

1. The author drags a question glyph of the wanted type from the palette and drops it between two rows of an open section card (or chooses Add question on the card).
2. The system opens the question dialog with the type preselected: question text, short text, tooltip, required, minimum and maximum, validation text, mask, placeholder, default value, presentation variant, a sample answer for previews, and — for choice types — the select group. Each control carries an information icon explaining it, only the settings the chosen type uses are shown (for example HTML has no input mask), and a live preview shows the respondent's field for the type and variants, updating as the author edits.
3. The author fills in the text and saves.
4. The system creates the question with a key minted once, assigns it to the section at the drop position, renumbers the section's questions, and refreshes the board and tree with the new row selected.

## Alternative Flows

### A1: Choice type without a list

**Trigger:** The type is RADIO, COMBOBOX, MULTI_SELECT or CHECKBOX_GROUP and no select group is chosen (step 3)  
**Flow:**

1. The system marks the select group field and refuses to save.
2. Use case continues at step 3.

### A2: Edit an existing question

**Trigger:** The author chooses Edit… on a row, or double-clicks the row (step 1)  
**Flow:**

1. The system opens the dialog with the question's current values.
2. The author changes them and saves; the system updates the question in place (same key, same durable id).
3. Use case ends.

### A3: Author cancels

**Trigger:** The author closes the dialog (step 3)  
**Flow:**

1. Nothing is created or changed.
2. Use case ends.

### A4: Ask a question again in another section

**Trigger:** The author drags a row's glyph into a different card (step 1)  
**Flow:**

1. The system assigns the same question to the target section at the drop position (a second assignment, not a second question).
2. If the target section already holds the question, the system refuses and explains.
3. Use case continues at step 4.

### A6: Reuse a question from the Questions panel

**Trigger:** The author drags a question from the drawer's Questions panel (narrowed with the panel's own search box if needed) into a card, or chooses Add to section… on it (step 1)  
**Flow:**

1. The system assigns the existing question to the target section at the drop position, exactly as A4 does for a row dragged across cards.
2. If the target section already holds the question, the system refuses and explains.
3. Use case continues at step 4.

### A7: A question that is in no section

**Trigger:** The survey holds a current question that no section assigns — an import carried it in, or its last assignment was removed while it was restored afterwards (step 1)  
**Flow:**

1. The system lists the question in the Questions panel like any other, marked as being in no section, and gives it the same Edit… and Add to section… actions plus Remove, since it has no assignment whose removal could retire it.
2. Validation reports it (UC-007), and the finding links to it here, because the board cannot show a question no card holds.
3. Use case continues at step 1.

### A5: Remove a question from a section

**Trigger:** The author chooses Remove from this section on a row (step 1)  
**Flow:**

1. The system retires the assignment and the rules bound to it; if it was the question's last assignment, the question is retired too (UC-009) with Undo.
2. Use case ends.

### A8: Dropped outside a section

**Trigger:** The author drags a question (a palette glyph, a row or a Questions panel row) over anything but a section card or one of its rows: a step lane, the bar between two steps, or a removed element (step 1)  
**Flow:**

1. Nothing that refuses the question lights up, and the pointer shows that it cannot be dropped there (UC-006 BR-007).
2. If the drop lands anyway, nothing is created, and the system warns that a question goes in a section: on a section card, or in a section added to the step first.
3. While no step holds a section, the question glyph and the Questions panel's rows cannot be dragged at all (UC-006 BR-008).
4. Use case continues at step 1.

## Postconditions

### Success Postconditions

- The question is current, assigned at the intended position with contiguous display orders in its section, and passes export validation for its type.

### Failure Postconditions

- Nothing is created or changed.

## Business Rules

### BR-001: A question's key is minted once

Editing never re-mints the key; dragging a row into another card, or Also add to section…, adds an assignment, never a new question.

### BR-002: Choice types need a list

RADIO, COMBOBOX, MULTI_SELECT and CHECKBOX_GROUP questions must reference a current select group of the same survey (UC-014).

### BR-003: Rules bind to assignments

A rule reads or targets a question *in a section*; placing the question in another section does not carry its rules.

### BR-004: The editor shows only what the type uses, and previews it

The dialog reveals only the settings the selected type supports (a select group for choice types, no input mask for HTML, and so on). The minimum and maximum are offered wherever the runtime bounds the answer: for INTEGER and DOUBLE as values, and for TEXT, TEXTAREA and PASSWORD as character counts, where they are labeled as lengths. They are not offered for EMAIL, whose field reads the pair only as a switch for its address-pattern check, nor for the date and time types, which encode theirs as epoch days, HHMM and nanoseconds, and the live preview renders the actual respondent field for that type and its presentation variants. In the preview, a placeholder naming a `<TOKEN>` is replaced by what the rules reaching this question would write there — a rule on the question's step, on its section, or on the question itself, the inner overriding the outer, exactly as the runtime scopes it (UC-019 BR-005) — using whichever of the rule's two possible values the runtime would use. The sample answer of the question the rule reads (BR-005) stands in for the respondent's own answer, so it wins wherever a real answer would: for a free-text source, and for a date or time source. The rule's own default upstream value wins where a real answer would lose — for a choice question, which stores an option's code rather than its text, and for a number or a boolean, none of which read as prose in a sentence — and is the only thing a rule reading a display-only question can write. Either way the other is the fallback, so a question with no sample recorded previews its rule's constant. The phrase is filled all-or-nothing, so a placeholder naming a token with no value falls back whole: to the placeholder's default where it has one — `{<S1>|}` is an empty default, which is a default, and reads as nothing — and otherwise to the phrase as written, brackets and all, which is what a respondent reads. `{Q#}` and `{S#}` are the runtime's own counters and are left exactly as written. A rule reading a display-only question has no sample to fall back on, because such a question collects no answer for a token to take (UC-019 BR-006), so the preview shows the token's default there — exactly what the respondent would read. One case reverses the choice-question rule: where a text sits in a section or question that a Repeat rule builds once per selected item of a multi-select, a rule reading that same multi-select fills its token with the item the instance belongs to (UC-019 BR-008), so the preview shows one instance with the first item of the list in the slot, ahead of the rule's default upstream value, and treats it as it treats a sample answer.

A value the author wrote and a value standing in for a respondent are not spliced in the same way, because the runtime does not splice them the same way. A sample answer is HTML-escaped, so a sample of `<b>Bob</b>` previews as the literal text `<b>Bob</b>` rather than as bold, and it is never re-read for placeholders of its own, so a sample that happens to contain braces is shown as typed. A rule's own value is neither escaped nor withheld from the re-reading: it is prose the author wrote for the respondent to read, and it is frequently a placeholder in its own right — the Family History Survey fills `<S1>` with `{<G1>'s|your} mother`, which only reads as a sentence once `<G1>` has been filled in turn. Previewing a sample answer raw showed the author markup the site renders as text, which is the forgiving direction and the one that reaches respondents unnoticed.

### BR-005: A question may carry a sample answer

A question may record an example answer. It is authoring-only — the designer substitutes it wherever a rule writes this question's answer into another text, so one sample serves every rule that reads the question — and it is never written to the export, so changing it never affects a deployed site. It stands in for what a respondent would type, which decides both when it is used (BR-004) and how: it is escaped and never re-read, exactly as the runtime treats the answer it represents. A display-only question's sample is never substituted anywhere: it collects no answer, so no rule reading it can write one into another text (UC-019 BR-006).
