# Use Case: Organize Sections into Steps

## Overview

**Use Case ID:** UC-015  
**Use Case Name:** Organize Sections into Steps  
**Primary Actor:** Survey Author  
**Goal:** Place sections in the steps where respondents should meet them — by creating a section in a step, copying a section into another step, or mounting one section in several steps — using the designer board.  
**Status:** Draft  
**Requirements:** [FR-015, NFR-007, NFR-012](../requirements.md)

## Preconditions

- The author is signed in with the `elicit_author` role.
- The survey exists in Author and has at least one step (UC-003, UC-005, UC-016).
- The designer board is open for the survey (`survey/{id}/design`).

## Main Success Scenario

1. The author drags a section card's glyph out of its lane and drops it between two cards of another lane.
2. The system creates a new section with a new element key, copying the name, dimension name and description of the dragged section, mounts it in the target step at the drop position, and assigns every question of the dragged section to it, in order.
3. The system renumbers the target lane's sections, recomputes their display keys (UC-016 BR-001), refreshes the board and the structure tree, and shows a notice "Copied" with Undo.
4. The author selects the new card; the breadcrumb and the tree follow.

## Alternative Flows

### A1: Mount the same section in another step

**Trigger:** The author chooses Also mount in step… from a card's menu and picks a step (step 1)  
**Flow:**

1. The system mounts the existing section in the chosen step at the end of that lane: one more assignment for the same section, no new section.
2. Both cards now carry a "mounted in N steps" badge; editing the section from either card changes both.
3. Use case continues at step 3.

### A2: Target step already holds the section

**Trigger:** The chosen step already mounts the section (A1 step 1)  
**Flow:**

1. The system refuses and explains that a section can be mounted once per step.
2. Use case ends.

### A3: Remove a section from a step

**Trigger:** The author chooses Remove from this step from a card's menu (step 1)  
**Flow:**

1. The system retires the assignment and every rule that targets it.
2. If that was the section's last mount, the system retires the section and its question assignments as well (UC-009 BR-002) and offers Undo.
3. The system renumbers the lane and continues at step 3.

### A4: Create a section in a step

**Trigger:** The author drags the Section glyph from the palette into a lane, or chooses Add section on a lane (step 1)  
**Flow:**

1. The system opens the section dialog (name, dimension name, description).
2. The author saves; the system creates the section and mounts it at the drop position.
3. Use case continues at step 3. If the author cancels, nothing is created.

### A5: Keyboard path

**Trigger:** The author cannot or does not want to drag (step 1)  
**Flow:**

1. Every drag action is available from the card's menu: Copy to step…, Also mount in step…, Move up, Move down, Remove from this step.
2. Use case continues at step 2.

## Postconditions

### Success Postconditions

- The target step's lane shows the section at the intended position with contiguous display orders and valid display keys.
- The new section (main flow) or assignment (A1) is current, carries a key minted once, and will be exported on the next export.

### Failure Postconditions

- Nothing is changed and the author is told why.

## Business Rules

### BR-001: A drag between steps copies

Dragging a section from one step into another never moves it: it creates a brand-new section in the target step and leaves the original in place. Moving is copy followed by Remove from this step.

### BR-002: Copies share questions, not rules

The copy assigns the same questions (same durable ids and keys) so ontology tags and reports keep pointing at one question. Rules are bound to specific assignments and do not follow the copy; the author adds rules to the copy deliberately (UC-019).

### BR-003: One section, many steps

A section may be mounted in several steps (as the Family Health History Survey does), once per step. Each mount is a separate assignment with its own display key.

### BR-004: Lane order is always contiguous

Any change to a lane renumbers its sections 1..n and recomputes display keys (UC-016 BR-001).
