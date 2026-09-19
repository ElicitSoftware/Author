# Use Case: Rename Step or Section

## Overview

**Use Case ID:** UC-018  
**Use Case Name:** Rename Step or Section  
**Primary Actor:** Survey Author  
**Goal:** Change a step's or section's name and description while being told, before saving and again at export, that the new name relabels historical reports at deployed sites as well as future ones.  
**Status:** Draft  
**Requirements:** [FR-018, C-011, C-012](../requirements.md)

## Preconditions

- The author is signed in with the `elicit_author` role.
- The step or section exists and is current.

## Main Success Scenario

1. The author chooses Edit… on a lane or card (or double-clicks its title).
2. The system opens the dialog with the current name, dimension name and description.
3. The author changes the name.
4. The system shows a warning that renaming is retroactive: reports already produced at deployed sites will show the new name; Save stays disabled until the author acknowledges the warning.
5. The author acknowledges and saves.
6. The system updates the element, refreshes the board, tree and breadcrumb, and records the element for the export-time warning (UC-007).

## Alternative Flows

### A1: Duplicate step name

**Trigger:** Another current step in the survey already has the name (step 5)  
**Flow:**

1. The system refuses and marks the name field.
2. Use case continues at step 3.

### A2: Only the description changes

**Trigger:** The name is unchanged (step 3)  
**Flow:**

1. No warning is shown and no acknowledgement is required.
2. Use case continues at step 5.

### A3: Author cancels

**Trigger:** The author closes the dialog (step 3)  
**Flow:**

1. Nothing is changed.
2. Use case ends.

## Postconditions

### Success Postconditions

- The element carries the new name; the next export carries it and warns about the rename.

### Failure Postconditions

- The element is unchanged.

## Business Rules

### BR-001: Renaming is retroactive

Step and section names are the labels of reporting dimensions at deployed sites; a rename relabels history (C-012 sign-off).

### BR-002: The dimension name is not the name

`dimension_name` drives the reporting table and is edited as a separate field; changing it is a reporting change, not a rename, and is validated separately.

### BR-003: Step names are unique within a survey

The schema enforces one current step per name per survey.
