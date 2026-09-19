# Use Case: Assign Ontology Tag

## Overview

**Use Case ID:** UC-021  
**Use Case Name:** Assign Ontology Tag  
**Primary Actor:** Survey Author  
**Goal:** Attach a reporting tag to a question or to a section as mounted in a step, choosing whether the respondent's answer or a fixed value is reported, so that the site's dimensional reporting captures the element.  
**Status:** Implemented  
**Requirements:** [FR-023, C-017](../requirements.md)

## Preconditions

- The author is signed in with the `elicit_author` role.
- The survey has at least one current section or question.

## Main Success Scenario

1. The author chooses Reporting tags… on a question row or section card in the designer (or Assign tag… on the survey's Reporting page).
2. The system opens the assignment dialog showing the element, the scopes it offers (BR-002), the survey's tags, and the reported-value choice.
3. The author picks the scope, picks a tag (or creates one in place, UC-023) and chooses "Use the respondent's answer" or enters a fixed value.
4. The system shows the table and column the tag fills (UC-024) and the author saves.
5. The system validates that exactly one element is named (BR-001) and that the element does not already carry the tag (A1), stores the assignment with a new element key, and refreshes the element's tag list.

## Alternative Flows

### A1: Element already carries the tag

**Trigger:** An assignment with the same scope, element and tag exists (step 5)  
**Flow:**

1. The system refuses and marks the tag field.
2. The author picks another tag or edits the existing assignment (UC-022).
3. Use case continues at step 3.

### A2: No tag exists yet

**Trigger:** The survey has no reporting tags (step 3)  
**Flow:**

1. The author chooses New tag… and completes UC-023.
2. The new tag is selected in the dialog.
3. Use case continues at step 3.

### A3: Fixed value chosen without a value

**Trigger:** "Fixed value" is selected and the field is empty (step 4)  
**Flow:**

1. The system marks the value field and keeps the dialog open.
2. Use case continues at step 3.

### A4: Author cancels

**Trigger:** The author closes the dialog (step 3)  
**Flow:**

1. Nothing is changed.
2. Use case ends.

## Postconditions

### Success Postconditions

- A metadata row names the element by durable id and the tag; the next export carries it and the site's ETL fills the tag's column from it.

### Failure Postconditions

- No assignment is created.

## Business Rules

### BR-001: One element per assignment

Exactly one of the step/section assignment, the question, or the section/question assignment is named. The schema's check constraint does not enforce this, so Author does.

### BR-002: Scope follows the element

A question offers two scopes: wherever it appears (`question_id`) and only in this section (`sections_question_id`). A section offers one: as mounted in this step (`steps_sections_id`). Copies of a question keep its durable id, so a question-wide tag follows every copy.

### BR-003: Value

A null value reports the respondent's answer (lower-cased and trimmed by the site's ETL). A fixed value is reported whenever the element is answered, which is how a section about the respondent's mother reports `Gender = Female`.

### BR-004: Durable references

Element columns hold durable ids, not surrogate ids, so one assignment covers every version of the element at a deployed site.
