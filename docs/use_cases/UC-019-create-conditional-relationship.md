# Use Case: Create Conditional Relationship

## Overview

**Use Case ID:** UC-019  
**Use Case Name:** Create Conditional Relationship  
**Primary Actor:** Survey Author  
**Goal:** Connect an upstream question's answer to a downstream step, section or question with an operator, a reference value and an action (Show, Repeat or Text), by drawing the rule on the designer board or from a question's menu.  
**Status:** Implemented  
**Requirements:** [FR-019, FR-020, FR-022, FR-036, NFR-007, NFR-012](../requirements.md)

## Preconditions

- The author is signed in with the `elicit_author` role.
- The designer board is open with Show rules on, and the survey has at least one question assigned to a section.

## Main Success Scenario

1. The author drags a rule glyph — Show, Repeat or Text — from the palette and drops it on the question row whose answer the rule reads.
2. The system attaches a dangling arrow to that row, its action glyph at the midpoint, and follows the pointer.
3. The author drops the arrowhead on the target: a step lane header, a section card header or a question row.
4. The system opens the rule dialog with the upstream question, the action and the downstream target filled in.
5. The author chooses the operator, enters the reference value (and, for Text, the token name and an example answer for the preview) and an optional description, and saves.
6. The system creates the rule with a key minted once, draws the arrow labelled by its action and target (UC-020 BR-004), and refreshes validation.

## Alternative Flows

### A1: Keyboard path

**Trigger:** The author chooses Add rule… from a question row's menu (step 1)  
**Flow:**

1. The system opens the rule dialog with the upstream question filled in; the author picks the downstream kind and target from lists.
2. Use case continues at step 5.

### A2: Text action without a token

**Trigger:** The action is Text and no token name is given, or it exceeds 10 characters (step 5)  
**Flow:**

1. The system marks the token field and refuses to save.
2. Use case continues at step 5.

### A3: Repeat onto a step

**Trigger:** The action is Repeat and the target is a step (step 3)  
**Flow:**

1. The system refuses: the runtime repeats sections and questions, not steps.
2. Use case continues at step 3.

### A4: Author cancels or drops outside a target

**Trigger:** The arrowhead is released on empty board, or the dialog is closed (steps 3, 5)  
**Flow:**

1. The system removes the dangling arrow; nothing is created.
2. Use case ends.

### A5: Re-point an existing rule

**Trigger:** The author drags an arrow's tail or head to another row or header (step 1)  
**Flow:**

1. The system moves that end of the rule to the new element, subject to A3, and redraws.
2. Use case ends.

## Postconditions

### Success Postconditions

- The rule is current, reads from one question assignment, targets exactly one step, section assignment or question assignment, and is drawn on the board.

### Failure Postconditions

- Nothing is created or changed.

## Business Rules

### BR-001: Exactly one downstream target

A rule targets one step, one section assignment or one question assignment, never several and never none (FR-022).

### BR-002: Upstream is always a question assignment

A rule reads a question *in a section*; the same question assigned elsewhere is a different upstream.

### BR-003: Rules read as a sentence

Upstream question, operator, reference value, action, target — the order the runtime evaluates and the arrow draws (UC-029 BR-002).

### BR-004: Rules on one target are all required

The runtime applies a target's rules with AND; the board draws them as separate arrows converging on the target.

### BR-005: A Text rule may carry a sample answer

A Text rule may record an example answer for its token. It is authoring-only — it drives the question preview's token substitution (UC-011 BR-004) and is never written to the export — so removing or changing it never affects a deployed site.
