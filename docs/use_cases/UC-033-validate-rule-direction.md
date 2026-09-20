# Use Case: Validate Rule Direction

## Overview

**Use Case ID:** UC-033  
**Use Case Name:** Validate Rule Direction  
**Primary Actor:** Survey Author  
**Goal:** Ensure every rule's downstream target comes after its upstream question, so that the rule can take effect for a respondent moving forward through the survey.  
**Status:** Implemented  
**Requirements:** [FR-041, FR-005, FR-016, FR-019, FR-021, FR-022, NFR-012](../requirements.md)

## Preconditions

- The author is signed in with the `elicit_author` role.
- The designer board is open for a survey with at least one rule, or the author is creating one (UC-019).

## Main Success Scenario

1. The author reorders a step, section or question (UC-016).
2. The system renumbers positions and rebuilds the board.
3. For every current rule the system compares the downstream target's position (step, section, question) with the upstream question's, for every mount of both.
4. The system draws each backward rule in the error colour, adds "points backwards" to its label, puts the reason in its tooltip, and tells the author in the move notification how many rules now point backwards.
5. The author drags the arrow's head to a later downstream target, drags its tail to an earlier upstream question, or moves the elements again.
6. The system re-checks, draws the rule normally, and export validation (UC-007) no longer lists it; the goal is achieved.

## Alternative Flows

### A1: Drawing a backward rule

**Trigger:** The author drops the arrowhead, or picks a downstream target in the rule dialog, that is not after the upstream question (step 5)  
**Flow:**

1. The system refuses with the reason and marks the downstream field.
2. Use case continues at step 5.

### A2: Restoring a rule that is now backward

**Trigger:** The author restores a removed rule (UC-010) whose downstream target has since been moved before its upstream question (step 5)  
**Flow:**

1. The system refuses with the reason; the rule stays removed.
2. Use case ends.

### A3: Export with a backward rule

**Trigger:** The author chooses Export while a backward rule exists (step 4)  
**Flow:**

1. The system lists the rule as an error and keeps Export disabled (UC-007 A2).
2. Use case ends.

### A4: Show rules is off

**Trigger:** Arrows are hidden when the board is rebuilt (step 4)  
**Flow:**

1. The notification count and the validation panel still report the rules.
2. Use case continues at step 5.

### A5: Imported survey with a backward rule

**Trigger:** The author opens a survey imported from a file that already contained a backward rule (step 2)  
**Flow:**

1. The system accepts the file as it is (C-015), draws the rule in the error colour and reports it at export.
2. Use case continues at step 5.

## Postconditions

### Success Postconditions

- Every current rule's downstream target is after its upstream question in every mount; nothing about validity is stored.

### Failure Postconditions

- A backward rule remains, drawn in the error colour and listed as an export error; the survey is otherwise unchanged.

## Business Rules

### BR-001: Forward means later on the respondent's path

A downstream target is after its upstream question when its (step, section, question) position is greater, comparing step first. A downstream step must be a later step; a downstream section a later section (a later section in the same step counts); a downstream question a later question (a later question in the same section counts). The upstream question's own step, own section and own position are never after it.

### BR-002: Reordering is never refused

A move that makes a rule backward is allowed and shown; only creating, re-pointing and restoring a backward rule are refused. This lets the author keep moving elements until the rule points forward again instead of removing and redrawing it.

### BR-003: Validity is derived

Direction is computed from current positions on every render and at export; a reorder or re-point changes it with no separate write.

### BR-004: Every mount counts

A section can be mounted in several steps, and a rule fires from whichever mount the respondent answers in. A rule is therefore valid only if it is forward for every mount of the upstream question's section paired with every mount of the downstream target's section. The one exception is a downstream question in the upstream question's own section: the respondent sees it on the page being answered, so it is compared within the same mount and only the question order matters.

### BR-005: Colour is never the only cue

A backward arrow also changes its label and tooltip, and is listed in text in the validation panel and in the move notification.

### BR-006: Every action is order-dependent

Show, Repeat and Text rules are all checked. The runtime substitutes Text rules into an answer the respondent already holds, so a Text rule pointing backwards changes a page the respondent has already passed.
