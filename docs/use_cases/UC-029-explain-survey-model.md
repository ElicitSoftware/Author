# Use Case: Explain the Survey Model

## Overview

**Use Case ID:** UC-029  
**Use Case Name:** Explain the Survey Model  
**Primary Actor:** Survey Author  
**Goal:** Understand how an Elicit survey is put together — the nested containers, the default path, and the rules that connect them — using the same vocabulary and glyphs the designer uses, so that the designer is understandable on first use.  
**Status:** Implemented  
**Requirements:** [FR-034, NFR-007](../requirements.md)

## Preconditions

- The author is signed in with the `elicit_author` role.

## Main Success Scenario

1. The author chooses How surveys work in the application navigation.
2. The system shows the guide page: a heading, a picture of the designer board showing a small survey, captured from the designer itself, and one section per idea, in this order:
   1. Elicit is a decision tree — the respondent walks a path, and answers decide what comes next.
   2. Containers nest — a survey holds steps, a step holds sections, a section holds questions; what each is for the respondent and for the author.
   3. Order is the default path — steps, sections and questions run in display order unless a rule says otherwise.
   4. Rules are the arrows — a rule reads one upstream question's answer, compares it with an operator and a reference value, and performs an action on one downstream target: SHOW, REPEAT or TEXT.
   5. The designer draws all of this — each element is a box that opens in place to reveal its children (steps, then their sections, then their questions), arrows are rules, every element has a glyph that is also its drag handle, one click selects, a double click edits, and the ⋮ menu lists every action.
   6. Designing a survey — a how-to: the Controls, Lists (UC-014) and Questions (UC-011 A6) panels with their per-panel search boxes, Reporting tags and the Survey structure tree, and the order of work from structure to rules, tags, validation and export.
   7. Reporting tags — a tag names a column of the site's report; tagging a question reports its answers, tagging a section in a step with a fixed value classifies everyone who reaches it, and changing an assigned tag is retroactive (UC-021..UC-024).
   8. Identity and publishing — every element's key is minted once, removal retires, and publishing exports a `.elicit` file that Admin applies.
3. The system shows a legend of the glyphs and an example rule written as "Question (operator) value → (Action) target".
4. The author reads the page and continues to the Surveys list or, from within a survey, to its designer.

## Alternative Flows

### A1: Opened from an empty survey

**Trigger:** The author follows the guide link shown when a survey has no steps (step 1)  
**Flow:**

1. The system shows the guide page.
2. The author returns to the survey with the browser's back or the Surveys link.
3. Use case ends.

## Postconditions

### Success Postconditions

- Nothing is changed; the author has seen the vocabulary the designer and the structure tree use.

### Failure Postconditions

- None; the page has no state.

## Business Rules

### BR-001: One vocabulary

The guide, the structure tree, the palette and the board use the same names and the same glyphs for step, section, question and each rule action. The guide never introduces a term the designer does not show.

### BR-002: Rules read as a sentence

A rule is always presented as upstream question, operator, reference value, action, downstream target — in that order — because that is the order the runtime evaluates it in and the order the designer's arrow draws it.

### BR-003: Accessible without the diagram

Every idea the diagram shows is also stated in text, and glyphs carry an accessible name, so the page conforms to NFR-007 without the image.
