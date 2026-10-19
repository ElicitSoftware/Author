# Use Case: Reassign Analysis Tag

## Overview

**Use Case ID:** UC-022  
**Use Case Name:** Reassign Analysis Tag  
**Primary Actor:** Survey Author  
**Goal:** Change or remove an existing tag assignment while being told, before saving and again at export, that the change reclassifies answers already recorded at deployed sites.  
**Status:** Implemented  
**Requirements:** [FR-024, C-011, C-012](../requirements.md)

## Preconditions

- The author is signed in with the `elicit_author` role.
- The assignment exists.

## Main Success Scenario

1. The author chooses Edit… on an assignment (element tag list in the designer, or the Analysis page).
2. The system opens the assignment dialog with the element fixed, the current tag and the current value.
3. The author changes the tag or the value.
4. The system shows a warning that the change is retroactive; Save stays disabled until the author acknowledges it.
5. The author acknowledges and saves.
6. The system updates the assignment in place, records it for the export-time warning (UC-007 step 4) and refreshes the lists.

## Alternative Flows

### A1: Nothing changed

**Trigger:** Tag and value are unchanged (step 3)  
**Flow:**

1. No warning is shown and Save closes the dialog without recording a change.
2. Use case ends.

### A2: Assignment removed

**Trigger:** The author chooses Remove on the assignment (step 1)  
**Flow:**

1. The system asks for confirmation, stating that answers already recorded at deployed sites stop being classified by the tag.
2. On confirmation the system deletes the assignment and records the removal for the export-time warning.
3. Use case ends.

### A3: Scope must change

**Trigger:** The author wants the tag on a different scope of the same element (step 3)  
**Flow:**

1. The scope of an existing assignment is fixed; the author removes it (A2) and assigns anew (UC-021).
2. Use case ends.

### A4: Author cancels

**Trigger:** The author closes the dialog (step 3)  
**Flow:**

1. Nothing is changed.
2. Use case ends.

## Postconditions

### Success Postconditions

- The assignment carries the new tag or value under the same element key; the validation panel warns about it until the next export.

### Failure Postconditions

- The assignment is unchanged.

## Business Rules

### BR-001: Reassignment is retroactive

`metadata` is a Type 1 table and Admin's update applies it in place, so a changed tag reclassifies historical answers as well as future ones (C-012 sign-off). Removal is warned about in the same way because it stops the classification at the site.

### BR-002: Warned twice

The warning appears in the dialog (step 4) and as a validation warning on the survey overview and in the export dialog until an export is stamped (UC-008 step 4), which clears the record.
