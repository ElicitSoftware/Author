# Use Case: Reorder Steps, Sections, and Questions

## Overview

**Use Case ID:** UC-016  
**Use Case Name:** Reorder Steps, Sections, and Questions  
**Primary Actor:** Survey Author  
**Goal:** Change the order respondents meet steps, sections within a step, and questions within a section, by dragging on the designer board or from an element's menu.  
**Status:** Draft  
**Requirements:** [FR-016, FR-041, NFR-007, NFR-012](../requirements.md)

## Preconditions

- The author is signed in with the `elicit_author` role.
- The designer board is open for a survey with at least two elements in the container being reordered.

## Main Success Scenario

1. The author drags an element's glyph — a step lane, a section card, or a question row — and drops it on the insertion bar between two siblings in the same container.
2. The system places the element at that position and renumbers the container's elements to contiguous integers 1..n.
3. For steps and sections, the system recomputes the display key of every section assignment affected, and moves the survey's initial display key with the first section if it pointed at one that changed.
4. The system refreshes the board and the structure tree, keeping the dragged element selected.

## Alternative Flows

### A1: Keyboard path

**Trigger:** The author chooses Move up / Move down from the element's menu (step 1)  
**Flow:**

1. The system moves the element one position in that direction.
2. Use case continues at step 2.

### A2: Dropped where it already is

**Trigger:** The drop position equals the element's current position (step 1)  
**Flow:**

1. The system changes nothing.
2. Use case ends.

### A3: Dropped into a different container

**Trigger:** The insertion bar belongs to another lane or card (step 1)  
**Flow:**

1. This is a copy, not a reorder: UC-015 (sections) or UC-011 A4 (questions) applies.
2. Use case ends.

### A4: The move turns a rule backwards

**Trigger:** After the move, a rule's downstream target no longer comes after its upstream question (step 2)  
**Flow:**

1. The system completes the move; it never refuses a reorder because of a rule (UC-033 BR-002).
2. The system draws each affected rule in the error colour and tells the author how many rules now point backwards.
3. Use case continues at step 4.

## Postconditions

### Success Postconditions

- Display orders in the container are 1..n with no gaps; every section assignment's display key equals survey-stepOrder-0000-sectionOrder-0000-0000-0000; the initial display key names a current assignment.

### Failure Postconditions

- Nothing is changed.

## Business Rules

### BR-001: Positions are contiguous integers and display keys are derived

The Survey runtime addresses steps and sections by the display key, whose components are 4-digit integers, so display orders are always renumbered to 1..n and keys are recomputed by the system, never edited by hand.

### BR-002: The initial display key follows the first assignment

If the survey's initial display key no longer names a current section assignment after a reorder, it is set to the lowest current key. Export validation (UC-007) reports an initial display key that names no current assignment.

### BR-003: Reordering is not retroactive

Reordering changes what future respondents see; respondents who already started keep the definition they started with (C-011). Answers at deployed sites are keyed by display key, so a reorder after deployment is reported as a warning at export.
