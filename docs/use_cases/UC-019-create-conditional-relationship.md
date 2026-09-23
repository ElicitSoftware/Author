# Use Case: Create Conditional Relationship

## Overview

**Use Case ID:** UC-019  
**Use Case Name:** Create Conditional Relationship  
**Primary Actor:** Survey Author  
**Goal:** Connect an upstream question's answer to a downstream step, section or question with an operator, a reference value and an action (Show, Repeat or Text), by drawing the rule on the designer board or from a question's menu.  
**Status:** Implemented  
**Requirements:** [FR-012, FR-019, FR-020, FR-022, NFR-007, NFR-012](../requirements.md)

## Preconditions

- The author is signed in with the `elicit_author` role.
- The designer board is open with Show rules on, and the survey has at least one question assigned to a section.

## Main Success Scenario

1. The author drags a rule glyph — Show, Repeat or Text — from the palette and drops it on the question row whose answer the rule reads.
2. The system attaches a dangling arrow to that row, its action glyph at the midpoint, and follows the pointer.
3. The author drops the arrowhead on the target: a step lane header, a section card header or a question row.
4. The system opens the rule dialog with the upstream question, the action and the downstream target filled in; the card the row or header sits in tells the system which mount of a shared section is meant.
5. The author chooses the operator, enters the reference value, optionally names the token the rule fills — picked from the names the survey already uses or typed new; optional for Show and Repeat, required for Text (BR-005) — and an optional description, and saves.
6. The system creates the rule with a key minted once — stored question-only when the target sits in the upstream question's own section, otherwise with the step and section the target is shown in (BR-001) — draws the arrow labelled by its action and target (UC-020 BR-004), and refreshes validation.

## Alternative Flows

### A1: Keyboard path

**Trigger:** The author chooses Add rule… from a question row's menu (step 1)  
**Flow:**

1. The system opens the rule dialog with the upstream question filled in; the author picks the downstream kind and target from lists, in which a section mounted in several steps appears once per mount, labelled by its path.
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

1. The system moves that end of the rule to the new element in the card it was dropped in, completes the path (BR-001), subject to A3, and redraws.
2. Use case ends.

## Postconditions

### Success Postconditions

- The rule is current, reads from one question assignment, carries a downstream path whose innermost element is the step, section assignment or question assignment it acts on, and is drawn on the board at that element.

### Failure Postconditions

- Nothing is created or changed.

## Business Rules

### BR-001: The downstream target is a path

A rule's downstream is the path the Survey runtime walks from the outside in: the step, the section mounted in that step, the question assigned to that section. The innermost element named is what the rule acts on; the outer ones say where the runtime places it. The author names only the element the rule acts on; the system completes the path from where that element sits on the board. A question in the upstream question's own section is stored question-only: no downstream step, no downstream section and no upstream step, whichever card the author drew it in — the runtime shows it on the page being answered, whichever mount that is, so the rule fires from every mount of that section. (The runtime's login-time query hides every question of a section or step a rule names downstream; a same-section rule stored with a path would blank its own section for a fresh respondent.) A section, a step, or a question in another section names its step (and section), and the rule names the upstream step it fires from. A path that does not hold together — a section outside its step, a question outside its section, a question in another section with no mount named — is refused (FR-022).

### BR-002: Upstream is always a question assignment

A rule reads a question *in a section*; the same question assigned elsewhere is a different upstream.

### BR-003: Rules read as a sentence

Upstream question, operator, reference value, action, target — the order the runtime evaluates and the arrow draws (UC-029 BR-002).

### BR-004: Rules on one target are all required

The runtime applies a target's rules with AND; the board draws them as separate arrows converging on the target.

### BR-005: A rule reaches everything inside its target, and a token is a slot, not a question

The runtime builds a text's token values from the rules that target its step, then its section, then the question itself, the inner overriding the outer. A Text rule on a step therefore fills its token in every section and question of that step; one rule per step, not one per element. A token is the name of a slot in a text, declared by being used: the same token may be filled by several rules from different questions, each where the text is shown (the Family History Survey fills `{S1}` from fifteen places), and a rule of any action that carries a token fills it. The token is therefore offered for every action, not only Text: a Show or Repeat rule may name the slot the upstream answer fills in the target's texts (the Family History Survey's step-showing Show rules carry `S1` and `CS2`), and a Text rule must name one (A2). The example answer a preview substitutes belongs to the question the rule reads (UC-011 BR-005), not to the rule.
