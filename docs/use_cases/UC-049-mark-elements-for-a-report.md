# Use Case: Mark Elements for a Report

## Overview

**Use Case ID:** UC-049  
**Use Case Name:** Mark Elements for a Report  
**Primary Actor:** Survey Author  
**Secondary Actors:** Admin App (Apply Survey Definition)  
**Goal:** Say which questions, question placements or sections the respondent report echoes back and which ones the administrator report monitors, in what order and under what label, so that a site's default reports show what the author chose and nothing else.  
**Status:** Implemented  
**Requirements:** [FR-069, C-015, C-017](../requirements.md)

## Preconditions

- The author is signed in with the `elicit_author` role.
- The survey has at least one current section or question.

## Main Success Scenario

1. The author chooses Report marks… on a question row or section card in the designer (or Mark element… on the survey's Reports page).
2. The system opens the marks dialog showing the element, the marks it already carries, the scopes it offers (BR-002), the two reports, and an optional label.
3. The author picks the scope and the report, enters a label or leaves it empty, and saves.
4. The system validates that the element can be marked (BR-003) and does not already carry a mark of that report at that scope (A1), stores the mark with a new report item key at the end of that report's order, and refreshes the element's badge.
5. On the Reports page, the author opens the Respondent or Administrator tab, sees that report's marks in order, drags one to another position, and the system renumbers the order (BR-004).
6. The author chooses Edit label… on a mark and saves a new label; the system stores it under the same key.
7. The author chooses Unmark on a mark and confirms; the system deletes it (BR-005).

## Alternative Flows

### A1: Element already marked for that report

**Trigger:** A mark with the same report, scope and element exists (step 4)  
**Flow:**

1. The system refuses and points at the existing mark.
2. The author edits that mark's label instead, or picks the other report.
3. Use case continues at step 3.

### A2: Element cannot be marked

**Trigger:** The element is an HTML or PASSWORD question (step 2)  
**Flow:**

1. The system offers no mark for it and says why (BR-003).
2. Use case ends.

### A3: Marked element was removed

**Trigger:** An element carrying a mark is removed (UC-009) and the author opens the Reports page or exports (step 5)  
**Flow:**

1. The list shows the mark against "removed element" and the export reports a warning naming it (UC-007).
2. The author unmarks it, or restores the element.
3. Use case continues at step 5.

### A4: Marking a question for the respondent report

**Trigger:** The author picks the Respondent report (step 3)  
**Flow:**

1. The dialog reminds the author that the respondent report is reachable again with the access code, so a marked answer is exactly as private as that code (BR-006).
2. Use case continues at step 3.

### A5: Author cancels

**Trigger:** The author closes the dialog without saving (step 3)  
**Flow:**

1. Nothing is changed.
2. Use case ends.

## Postconditions

### Success Postconditions

- A report item row names the element by durable id, the report it is marked for, its order within that report and its label; the next export carries it (UC-008) and a site's apply installs it beside the survey (Survey UC-013).
- The board marks the element with a report badge naming the reports it is on; a question-wide mark appears on every placement of the question.

### Failure Postconditions

- No mark is created, changed or removed.

## Business Rules

### BR-001: One element per mark, by durable id

Exactly one of the step/section assignment, the question, or the section/question assignment is named, by durable id, so one mark covers every version of the element at a site (Survey UC-013 BR-001). Author refuses a mark naming none or two before the database does.

### BR-002: Scope follows the element

A question offers two scopes: wherever it appears (`question_id`) and only in this section (`sections_question_id`). A section offers one: as mounted in this step (`steps_sections_id`), which puts every question of that section on the report. Copies of a question keep its durable id, so a question-wide mark follows every copy. The scopes are the ones analysis tags offer (UC-021 BR-002).

### BR-003: Only elements with a value can be marked

An HTML question is informational and a PASSWORD question's value must never be shown back, so neither can be marked for either report. Every other question type, and every section placement, can be.

### BR-004: Two reports, two orders

A mark belongs to the Respondent report or the Administrator report; an element shown on both carries two marks (Survey UC-013 BR-002). Each report's marks are ordered on their own, 1..n, and a drag on one tab moves nothing on the other.

### BR-005: A mark has no history

`report_items` is a Type 1 table (C-015): unmarking deletes the row, changing a label updates it in place, and a site applying the next export deletes the marks whose keys the file no longer lists (Survey UC-013 BR-003). The report item key is minted once and never changes (C-017).

### BR-006: The label is respondent-facing and translatable

A label replaces the element's own text on the report ("Your height" for "What is your height in centimeters?"). It is optional, at most 255 characters, and translatable through the Translations page (UC-043) with element type `report_items` and field `label`; nothing else on a mark is translated.

## Notes / Known Gaps

- The reports that read the marks are later phases of the umbrella's default reports plan: the respondent report in Survey (phase 4) and the administrator report in Admin (phase 5). Until then a site installs the rows and reads them with nothing.
- A mark on a removed element travels in the file as an export warning, like an analysis tag on a removed element (UC-024). Admin's install of a fresh site skips retired rows and cannot resolve such a mark; its update of a site that already has the element can. This is Admin's gap for `metadata` too.
