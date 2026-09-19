# Use Case: Visualize Relationship Graph

## Overview

**Use Case ID:** UC-020  
**Use Case Name:** Visualize Relationship Graph  
**Primary Actor:** Survey Author  
**Goal:** See every rule of the survey as an arrow between the question it reads and the element it acts on, so that conditional logic can be understood and checked before export.  
**Status:** Implemented  
**Requirements:** [FR-021, FR-022, NFR-001, NFR-007, NFR-012](../requirements.md)

## Preconditions

- The author is signed in with the `elicit_author` role.
- The designer board is open for a survey.

## Main Success Scenario

1. The author turns on Show rules.
2. The system draws one arrow per current rule from the box of the element it reads to the box of the element it acts on, with the action glyph at the midpoint and a label that reads what the rule does — the action and the target's name ("Show Demographics", "Repeat Child Name", "Replace {token}"). The condition (operator and value) is on the arrow's tooltip.
3. Arrows leave and arrive on the boxes' left edge and run in a channel to the left of the fixed step spine, so a rule between two steps runs in the board's left gutter and a rule between two elements inside one step runs in that step's own gutter, staying inside its box.
4. A rule whose two ends fall inside the same closed box is out of context and is not drawn; it reappears once that box is opened and the ends separate. With every step and section closed, only rules between steps are drawn.
5. The author selects an element (a box, or a tree row); the system emphasizes the rules that touch it and fades the rest, so a dense graph stays readable.
6. The author clicks a rule glyph; the system shifts focus to the rule's target element (selecting and scrolling to it) and opens the rule for editing, with Remove available.
7. The author opens a box or reorders elements; the arrows follow the elements.

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

### BR-004: The label says what the rule does

The visible arrow label is the action verb plus the target's name (Show / Repeat / Replace); the condition (operator and reference value, and the token for a Text rule) is carried on the tooltip, not the label.

### BR-005: Rules are shown in their context

An arrow is drawn only when its two ends resolve to different visible boxes. A rule contained inside a single closed box is hidden until that box is opened, so with everything collapsed only inter-step rules are visible, and each arrow routes in the gutter local to its own boxes rather than crossing the canvas.

### BR-006: Selection focuses the rules

When an element is selected, the rules that touch it (including via a collapsed ancestor) are drawn normally and every other rule is faded and drops its label, so the selected element's logic stands out in a dense graph. With nothing selected, all rules are drawn.
