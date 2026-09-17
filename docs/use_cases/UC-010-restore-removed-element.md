# Use Case: Restore Removed Element

## Overview

**Use Case ID:** UC-010  
**Use Case Name:** Restore Removed Element  
**Primary Actor:** Survey Author  
**Goal:** Bring a removed element back into the survey, with its identity intact, so that a mistaken removal can be corrected before or after export.  
**Status:** Draft  
**Requirements:** [FR-008, C-017, C-019](../requirements.md)

## Preconditions

- The author is signed in with the `elicit_author` role.
- The element was removed (UC-009) and still exists as a retired row.

## Main Success Scenario

1. The author turns on Show removed in the structure tree (UC-006) and selects the retired element, or chooses Undo on the removal notice.
2. The system checks that every parent the element depends on is current: the section for a question assignment, the step for a section assignment, the select group for an item, the survey itself.
3. The system shows a confirmation naming the element and, when they were retired in the same action, the dependents that will be restored with it.
4. The author confirms.
5. The system reopens the validity window of the element and the listed dependents, keeping their element keys.
6. The system refreshes the tree so the elements appear in their former positions and highlights the restored element.

## Alternative Flows

### A1: Parent is still removed

**Trigger:** A parent of the element is retired (step 2)  
**Flow:**

1. The system refuses and names the parent to restore first.
2. Use case ends.

### A2: Position is now taken

**Trigger:** Another current element now occupies the same ordering value within the parent (step 5)  
**Flow:**

1. The system restores the element at the end of its parent's order and tells the author.
2. Use case continues at step 6.

### A3: Author cancels

**Trigger:** The author declines the confirmation (step 4)  
**Flow:**

1. The system changes nothing.
2. Use case ends.

## Postconditions

### Success Postconditions

- The element and any restored dependents are current again with their original element keys.
- The next export writes them as current, so sites that applied the removal will see them versioned back in.

### Failure Postconditions

- The element remains retired.

## Business Rules

### BR-001: Restoring keeps identity

A restored element has the same element key as before; it is not a new element, so sites match it to the row they already hold.

### BR-002: Parents first

An element cannot be current inside a retired parent.

### BR-003: Sites treat a restore as an update

At a deployed site, a restored element arrives as a current row for a known key whose local copy is closed; the site opens a new version of it. Respondents who started while it was closed do not see it.
