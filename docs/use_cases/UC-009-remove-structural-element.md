# Use Case: Remove Structural Element

## Overview

**Use Case ID:** UC-009  
**Use Case Name:** Remove Structural Element  
**Primary Actor:** Survey Author  
**Goal:** Take a step, section, question, select group, select item, relationship or assignment out of the survey so that respondents at deployed sites stop seeing it, while everything already recorded about it is preserved.  
**Status:** Draft  
**Requirements:** [FR-007, FR-022, C-011, C-019](../requirements.md)

## Preconditions

- The author is signed in with the `elicit_author` role.
- The element exists in the survey and is current.

## Main Success Scenario

1. The author selects the element in the structure tree (UC-006) and chooses Remove.
2. The system determines the dependent elements that would be left dangling: a question's section assignments and the relationships that use them; a section's step assignments and question assignments; a step's section assignments; a select group's items.
3. The system shows a confirmation naming the element and listing the dependents that will be removed with it.
4. The author confirms.
5. The system retires the element and its dependents by closing their validity window at the current instant; no row is deleted.
6. The system refreshes the tree so the retired elements disappear from the default view, and shows a notice with Undo (UC-010).

## Alternative Flows

### A1: Element still referenced by a current element that cannot be retired with it

**Trigger:** A select group is referenced by a current question, or a step or section is still the target of a relationship the author did not include (step 2)  
**Flow:**

1. The system refuses the removal and lists the referencing elements with links.
2. The author edits or removes those elements first.
3. Use case ends.

### A2: Author cancels

**Trigger:** The author declines the confirmation (step 4)  
**Flow:**

1. The system changes nothing.
2. Use case ends.

### A3: Element never exported

**Trigger:** The survey has never been exported and the author chooses Purge instead of Remove (step 3)  
**Flow:**

1. The system confirms that the element and its dependents will be deleted permanently because no site can have received them.
2. The author confirms and the system deletes the rows.
3. Use case ends.

## Postconditions

### Success Postconditions

- The element and its dependents are retired: they carry a closing instant, are hidden from the default tree, are excluded from validation of references, and will be written as retired on the next export.
- The element can be restored (UC-010).

### Failure Postconditions

- Nothing is retired or deleted; the referencing elements are listed.

## Business Rules

### BR-001: Removal is retirement

A removed element keeps its row and its element key with a closing instant. Deployed sites applying the export close their copy in the same way, so respondents who already started keep seeing it and recorded answers remain valid.

### BR-002: Dependents go together

Assignments and relationships that would dangle are retired in the same action, at the same instant.

### BR-003: Choice groups outlive their questions, not the reverse

A select group cannot be removed while a current question uses it; a question can be removed without touching its group.

### BR-004: Retired elements are invisible to validation

Validation (UC-007) treats a reference to a retired element as an error, which is why dependents are retired together.

### BR-005: Permanent deletion only before first export

Physical deletion is offered only for surveys that have never been exported; afterwards every removal is a retirement.
