# Use Case: Edit Survey Metadata

## Overview

**Use Case ID:** UC-004  
**Use Case Name:** Edit Survey Metadata  
**Primary Actor:** Survey Author  
**Goal:** Change a survey's name, title, description, initial display key or post-survey address in place so that its identification stays current without affecting its structure or identity.  
**Status:** Draft  
**Requirements:** [FR-002](../requirements.md)

## Preconditions

- The author is signed in with the `elicit_author` role.
- The survey exists in Author.

## Main Success Scenario

1. The author selects a survey in the Surveys list, or opens it in the editor, and chooses Edit details.
2. The system opens the survey details form pre-filled with the current values; the survey key is shown read-only.
3. The author changes one or more fields and chooses Save.
4. The system validates the entries as in UC-003 step 4.
5. The system updates the survey in place, keeping its key and list position, and records the author as the last editor.
6. The system closes the form and shows the updated values.

## Alternative Flows

### A1: Validation fails

**Trigger:** A required field is missing, too long, the name or title holds a control character (BR-004), or the name is used by another survey (step 4)  
**Flow:**

1. The system marks the field with a message and keeps the form open.
2. The author corrects the value and chooses Save again.
3. Use case continues at step 4.

### A2: No changes made

**Trigger:** The author chooses Save without changing anything (step 3)  
**Flow:**

1. The system closes the form without recording a change.
2. Use case ends.

### A3: Author cancels

**Trigger:** The author chooses Cancel (step 3)  
**Flow:**

1. The system closes the form and discards the edits.
2. Use case ends.

## Postconditions

### Success Postconditions

- The survey carries the new metadata; its key, structure and list position are unchanged.
- The last-editor attribution reflects the author.

### Failure Postconditions

- The survey is unchanged.

## Business Rules

### BR-001: Metadata edits are in place

The survey record is not versioned; edits overwrite the previous values. Deployed sites receive the new values on the next export and apply, where they likewise replace the old ones.

### BR-002: Key is immutable

The survey key cannot be edited through this or any other use case.

### BR-003: Initial display key prefix is not significant

The leading survey component of the initial display key is rewritten by each deployed site when it applies the definition; authors may leave Author's own prefix in place.

### BR-004: Name and title are single-line

As when the survey is created (UC-003 BR-005), the name and the title may hold no control character. A survey that arrived by import with one keeps it until the author saves the details, and is reported by validation until then (UC-007 BR-015).
