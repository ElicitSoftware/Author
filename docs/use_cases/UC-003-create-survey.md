# Use Case: Create Survey

## Overview

**Use Case ID:** UC-003  
**Use Case Name:** Create Survey  
**Primary Actor:** Survey Author  
**Goal:** Create a new, empty survey definition with a permanent identity so that the author can begin adding structure and later export it.  
**Status:** Draft  
**Requirements:** [FR-001, FR-009](../requirements.md)

## Preconditions

- The author is signed in with the `elicit_author` role (UC-001, UC-002).
- The author is on the Surveys page.

## Main Success Scenario

1. The author chooses New survey.
2. The system opens the survey details form with fields for name, title, description, initial display key and post-survey address.
3. The author enters a name and a title, optionally the other fields, and chooses Save.
4. The system validates the entries: name and title are present and within length limits; the name is not used by another survey in Author.
5. The system assigns the survey a permanent survey key, places it last in the survey list order, records the author as the last editor, and stores the survey.
6. The system closes the form, shows the new survey in the list, and offers to open it in the editor (UC-006).

## Alternative Flows

### A1: Required field missing or too long

**Trigger:** Validation fails on a field value (step 4)  
**Flow:**

1. The system marks the offending field with a message and keeps the form open with the author's entries.
2. The author corrects the value and chooses Save again.
3. Use case continues at step 4.

### A2: Name already in use

**Trigger:** Another survey in Author already has the same name (step 4)  
**Flow:**

1. The system marks the name field as already in use and names the existing survey.
2. The author changes the name or cancels.
3. Use case continues at step 4, or ends if canceled.

### A3: Author cancels

**Trigger:** The author chooses Cancel (step 3)  
**Flow:**

1. The system closes the form without storing anything.
2. Use case ends.

## Postconditions

### Success Postconditions

- A survey exists with a permanent survey key, the entered metadata, a list position after all existing surveys, and no structural elements.
- The survey appears in the Surveys list and can be opened in the editor.

### Failure Postconditions

- No survey is created; the list is unchanged.

## Business Rules

### BR-001: Survey key is minted once

The survey key is generated when the survey is created and is never changed, regenerated or reused, because deployed sites identify the survey by it (see UC-008).

### BR-002: Name is unique within Author

Two surveys in one Author database cannot share a name. Names need not be unique across deployments; the key carries identity.

### BR-003: List order is local

The position of a survey in Author's list is an Author concern; a deployed site assigns its own order when it installs the survey.

### BR-004: Last editor is recorded

Creating or editing a survey records the author's principal name on the survey; exporting overwrites it with the exporting author (UC-008).

### BR-005: Name and title are single-line

A survey's name and title are one line of text each. A value holding a control character — a line break, a tab or any other — is refused with the field marked (A1), because both are shown in lists and headings and the name is written into the header of the exported file (UC-008 BR-004).
