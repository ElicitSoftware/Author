# Use Case: Author Question

## Overview

**Use Case ID:** UC-011  
**Use Case Name:** Author Question  
**Primary Actor:** Survey Author  
**Goal:** Create a question of a supported type inside a section, or edit an existing one, with its text, validation and choice list, from the designer board.  
**Status:** Implemented  
**Requirements:** [FR-010, FR-011, FR-013, FR-014, FR-035, FR-036, NFR-007, NFR-012](../requirements.md)

## Preconditions

- The author is signed in with the `elicit_author` role.
- The designer board is open and the survey has at least one section mounted in a step.

## Main Success Scenario

1. The author drags a question glyph of the wanted type from the palette and drops it between two rows of an open section card (or chooses Add question on the card).
2. The system opens the question dialog with the type preselected: question text, short text, tooltip, required, minimum and maximum, validation text, mask, placeholder, default value, presentation variant, and — for choice types — the select group. Each control carries an information icon explaining it, only the settings the chosen type uses are shown (for example HTML has no input mask), and a live preview shows the respondent's field for the type and variants, updating as the author edits.
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

### A4: Copy a question into another section

**Trigger:** The author drags a row's glyph into a different card (step 1)  
**Flow:**

1. The system assigns the same question to the target section at the drop position (a second assignment, not a second question).
2. If the target section already holds the question, the system refuses and explains.
3. Use case continues at step 4.

### A6: Reuse a question from the Questions panel

**Trigger:** The author drags a question from the drawer's Questions panel into a card, or chooses Add to section… on it (step 1)  
**Flow:**

1. The system assigns the existing question to the target section at the drop position, exactly as A4 does for a row dragged across cards.
2. If the target section already holds the question, the system refuses and explains.
3. Use case continues at step 4.

### A5: Remove a question from a section

**Trigger:** The author chooses Remove from this section on a row (step 1)  
**Flow:**

1. The system retires the assignment and the rules bound to it; if it was the question's last assignment, the question is retired too (UC-009) with Undo.
2. Use case ends.

## Postconditions

### Success Postconditions

- The question is current, assigned at the intended position with contiguous display orders in its section, and passes export validation for its type.

### Failure Postconditions

- Nothing is created or changed.

## Business Rules

### BR-001: A question's key is minted once

Editing never re-mints the key; copying into another section adds an assignment, never a new question.

### BR-002: Choice types need a list

RADIO, COMBOBOX, MULTI_SELECT and CHECKBOX_GROUP questions must reference a current select group of the same survey (UC-014).

### BR-003: Rules bind to assignments

A rule reads or targets a question *in a section*; copying the question elsewhere does not carry its rules.

### BR-004: The editor shows only what the type uses, and previews it

The dialog reveals only the settings the selected type supports (numeric bounds for numbers, a select group for choice types, no input mask for HTML, and so on), and the live preview renders the actual respondent field for that type and its presentation variants. In the preview, a `{TOKEN}` in the text is replaced by the sample answer of the Text rule that fills it (UC-019 BR-005), falling back to the token's default.
