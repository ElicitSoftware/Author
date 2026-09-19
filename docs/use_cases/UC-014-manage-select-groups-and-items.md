# Use Case: Manage Select Groups and Items

## Overview

**Use Case ID:** UC-014  
**Use Case Name:** Manage Select Groups and Items  
**Primary Actor:** Survey Author  
**Goal:** Define the lists of options (select groups and their items) a survey's choice questions offer, and attach a list to a question, so that respondents see the right choices.  
**Status:** Implemented  
**Requirements:** [FR-014, NFR-012](../requirements.md)

## Preconditions

- The author is signed in with the `elicit_author` role.
- The survey is open in the designer.

## Main Success Scenario

1. The author opens the Lists panel in the designer drawer, which lists the survey's current lists with their option counts.
2. The author chooses New list…
3. The system opens the list dialog: name, description, data type, and the options table (display text, coded value) with Add, Move up, Move down and Remove.
4. The author names the list and adds its options in the order respondents should see them.
5. The author saves; the system creates the list and its options with new element keys and shows the list in the panel.
6. The author drags the list's glyph onto a choice question on the board (or picks the list in the question dialog, the keyboard route, NFR-012).
7. The system attaches the list to the question and refreshes the board.

## Alternative Flows

### A1: Edit a list

**Trigger:** The author chooses Edit… on a list (step 1)  
**Flow:**

1. The system opens the dialog with the current name, description, data type and options.
2. The author changes fields, reorders, adds or removes options and saves.
3. The system updates the list and existing options in place, retires removed options and creates new ones; questions using the list keep it.
4. Use case ends.

### A2: Remove a list

**Trigger:** The author chooses Remove on a list (step 1)  
**Flow:**

1. If a current question uses the list, the system refuses and names the count.
2. Otherwise the system retires the list and its options (UC-009 semantics).
3. Use case ends.

### A3: Dropped on a non-choice question

**Trigger:** The list is dropped on a question whose type does not take options (step 6)  
**Flow:**

1. The system refuses with a message naming the question's type.
2. Use case ends.

### A4: Duplicate name or empty option

**Trigger:** Another list in the survey has the name, or an option has no display text (step 5)  
**Flow:**

1. The system marks the field and keeps the dialog open.
2. Use case continues at step 4.

## Postconditions

### Success Postconditions

- The list and its options exist as working copies with element keys and are exported with the survey; the question's `select_group_id` names the list by durable id.

### Failure Postconditions

- No list, option or question is changed.

## Business Rules

### BR-001: Choice types only

Only RADIO, COMBOBOX, MULTI_SELECT and CHECKBOX_GROUP questions take a list (UC-011 BR-002); a list is required for them and refused for other types.

### BR-002: Coded value defaults to the display text

An option's coded value is what the answer stores; when left blank it is the display text.

### BR-003: Lists are survey-scoped and named uniquely

A list belongs to one survey and its name is unique within it (`select_groups_name_un`). Copying a question into another survey is out of scope.

### BR-004: Options are ordered

Display order is renumbered from 1 on every save in the order shown in the dialog.
