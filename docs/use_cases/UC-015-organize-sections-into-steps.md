# Use Case: Organize Sections into Steps

## Overview

**Use Case ID:** UC-015  
**Use Case Name:** Organize Sections into Steps  
**Primary Actor:** Survey Author  
**Goal:** Place sections in the steps where respondents should meet them — by creating a section in a step, mounting one section in several steps, or copying a section into another step — using the designer board.  
**Status:** Draft  
**Requirements:** [FR-015, NFR-007, NFR-012](../requirements.md)

## Preconditions

- The author is signed in with the `elicit_author` role.
- The survey exists in Author and has at least one step (UC-003, UC-005, UC-016).
- The designer board is open for the survey (`survey/{id}/design`).

## Main Success Scenario

1. The author drags a section card's glyph out of its lane and drops it between two cards of another lane.
2. The system mounts the existing section in the target step at the drop position: one more assignment for the same section, no new section. The original card stays where it was.
3. The system renumbers the target lane's sections, recomputes their display keys (UC-016 BR-001), refreshes the board and the structure tree, and confirms that the section is now also in the target step.
4. The system selects the new card; both cards now carry a "mounted in N steps" badge, and editing the section from either card changes both.

## Alternative Flows

### A1: Copy a section into another step

**Trigger:** The author chooses Copy to step… from a card's menu and picks a step (step 1)  
**Flow:**

1. The system creates a new section with a new element key, copying the name, dimension name and description of the dragged section, mounts it at the end of the chosen lane, and assigns every question of the source section to it, in order (BR-002).
2. The system shows a notice "Copied" with Undo, which retires the new section again.
3. Use case continues at step 3.

### A2: Target step already holds the section

**Trigger:** The target step already mounts the section (step 1, A5 or A6)  
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

1. Every drag action is available from the card's menu: Also mount in step… (the drag across lanes), Copy to step… (A1), Move up, Move down, Remove from this step.
2. Use case continues at step 2.

### A6: Mount a section from the Sections panel

**Trigger:** The author drags a section from the drawer's Sections panel (narrowed with the panel's own search box if needed) into a lane, or chooses Mount in step… on it (step 1)  
**Flow:**

1. The system mounts the existing section in the target step at the drop position, exactly as the main flow does for a card dragged across lanes.
2. If the target step already holds the section, the system refuses and explains (A2).
3. Use case continues at step 3.

### A7: A section that is in no step

**Trigger:** The survey holds a current section that no step mounts — an import carried it in, or its last mount was removed while it was restored afterwards (step 1)  
**Flow:**

1. The system lists the section in the Sections panel like any other, marked as being in no step, and gives it the same Edit… and Mount in step… actions plus Remove, since it has no assignment whose removal could retire it.
2. Validation reports it (UC-007), and the finding links to it here, because the board cannot show a section no lane holds.
3. Use case continues at step 1.

## Postconditions

### Success Postconditions

- The target step's lane shows the section at the intended position with contiguous display orders and valid display keys.
- The new assignment (main flow, A6) or section (A1, A4) is current, carries a key minted once, and will be exported on the next export.

### Failure Postconditions

- Nothing is changed and the author is told why.

## Business Rules

### BR-001: A drag between steps mounts

Dragging a section from one step into another never moves it and never duplicates it: it mounts the same section in the target step and leaves the original in place (decided 2026-09-20; before that a drag copied). Moving is mount followed by Remove from this step. A separate section with its own key comes only from Copy to step… (A1).

### BR-002: Copies share questions, not rules

A copy (A1) assigns the same questions (same durable ids and keys) so ontology tags and reports keep pointing at one question. Rules are bound to specific assignments and follow neither a copy nor a new mount; the author adds rules to the new card deliberately (UC-019).

### BR-003: One section, many steps

A section may be mounted in several steps (as the Family Health History Survey does), once per step. Each mount is a separate assignment with its own display key.

### BR-004: Lane order is always contiguous

Any change to a lane renumbers its sections 1..n and recomputes display keys (UC-016 BR-001).
