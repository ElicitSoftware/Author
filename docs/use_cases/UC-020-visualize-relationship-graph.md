# Use Case: Visualize Relationship Graph

## Overview

**Use Case ID:** UC-020  
**Use Case Name:** Visualize Relationship Graph  
**Primary Actor:** Survey Author  
**Goal:** See every rule of the survey as an arrow between the question it reads and the element it acts on, so that conditional logic can be understood and checked before export.  
**Status:** Draft  
**Requirements:** [FR-021, FR-022, NFR-001, NFR-007](../requirements.md)

## Preconditions

- The author is signed in with the `elicit_author` role.
- The designer board is open for a survey.

## Main Success Scenario

1. The author turns on Show rules.
2. The system draws one arrow per current rule from the upstream question row to the downstream lane header, card header or question row, with the action glyph at the midpoint and the operator and reference value as its label.
3. Arrows to or from a question inside a closed card land on that card's header until the card is opened.
4. The author selects an arrow.
5. The system opens the rule for editing, with Remove available.
6. The author opens a card, scrolls or reorders; the arrows follow the elements.

## Alternative Flows

### A1: Removed rules

**Trigger:** Show removed is on (step 2)  
**Flow:**

1. The system also draws retired rules, dashed, and rules whose target is retired, dashed.
2. Use case continues at step 4.

### A2: Rule with a dangling reference

**Trigger:** A rule reads from or targets an element that no longer exists (step 2)  
**Flow:**

1. The system draws the arrow to the board edge and lists the rule in validation (UC-007) as an error.
2. Use case continues at step 4.

### A3: No rules

**Trigger:** The survey has no rules (step 2)  
**Flow:**

1. The system draws nothing and the palette's rule glyphs explain how to draw one.
2. Use case ends.

## Postconditions

### Success Postconditions

- Nothing is changed; the author has seen the rules in place.

### Failure Postconditions

- None.

## Business Rules

### BR-001: The graph is derived

Arrows are computed from the `relationships` table on every render; nothing about their drawing is stored.

### BR-002: Arrows anchor on assignments

A rule references section and question *assignments*, so an arrow is drawn to the card or row of that assignment, not to every place the same section or question appears.

### BR-003: Accessible without the drawing

Every rule is also reachable as text: the rule dialog lists upstream, operator, value, action and target, and the details page of an element lists the rules touching it.
