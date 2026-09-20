# Use Case: Validate Survey Definition

## Overview

**Use Case ID:** UC-007  
**Use Case Name:** Validate Survey Definition  
**Primary Actor:** Survey Author  
**Goal:** Learn, before exporting, which problems would make the definition fail or behave unexpectedly at a deployed site, and which edits will apply retroactively there.  
**Status:** Draft  
**Requirements:** [FR-005, FR-022, FR-018, FR-024, C-011, C-012](../requirements.md)

## Preconditions

- The author is signed in with the `elicit_author` role.
- The survey exists in Author.

## Main Success Scenario

1. The author opens the survey in the editor (UC-006) or chooses Export (UC-008).
2. The system checks the current elements of the survey: every reference from one element to another resolves to a current element; every relationship has a downstream path that holds together (a step, a section mounted in it, a question assigned to that section; a question outside the upstream question's own section names its step and section) and a valid operator and action; every choice-type question refers to a select group that has at least one current item; every `{TOKEN}` a step name, section name or question text uses is filled by a rule that reaches it, and every Text rule's token is used by a text it reaches; every step has at least one section and every section at least one question; ordering values are unique within their parent; the survey's initial display key names a current section assignment.
3. The system classifies each finding as an error (blocks export) or a warning (informational).
4. The system adds a retroactivity warning for each step or section whose name changed since the last export and for each question whose ontology assignment changed since the last export.
5. The system displays the findings in the validation panel, each linked to the element concerned.
6. The author selects a finding and the system navigates to the element so that it can be corrected; with no errors present, the goal is achieved and export is allowed.

## Alternative Flows

### A1: No findings

**Trigger:** The checks produce neither errors nor warnings (step 3)  
**Flow:**

1. The system shows that the definition is ready to export.
2. Use case ends.

### A2: Errors present when exporting

**Trigger:** At least one error exists and the author is in the export flow (step 5)  
**Flow:**

1. The system disables Export and lists the errors in the export dialog.
2. The author corrects the elements and re-runs validation.
3. Use case continues at step 2.

### A3: No previous export to compare with

**Trigger:** The survey has never been exported (step 4)  
**Flow:**

1. The system omits the change-based retroactivity warnings and instead shows a general note that step and section names and ontology assignments apply retroactively at deployed sites.
2. Use case continues at step 5.

## Postconditions

### Success Postconditions

- The author has an up-to-date list of errors and warnings, each pointing to its element.
- Export is enabled exactly when no errors exist.

### Failure Postconditions

- Validation could not run; the system reports why and export remains disabled.

## Business Rules

### BR-001: Errors block export, warnings do not

Anything a deployed site would reject or that would leave a respondent without a required option is an error. Structural emptiness and retroactivity are warnings.

### BR-002: References must point to current elements

A reference to a retired element is an error, because the deployed site would otherwise install a relationship or assignment to something respondents can no longer see.

### BR-003: Retroactive edits are called out every time

Renaming a step or section relabels historical reports at deployed sites; reassigning a question's ontology reclassifies historical answers. These two, and only these two, edits are flagged as retroactive, per the stakeholder sign-off of 2026-09-12.

### BR-004: Validation is repeatable and read-only

Running validation changes nothing in the survey.

### BR-005: Tokens are checked where they are shown

A rule reaches its target and everything inside it (UC-019 BR-005), so a token in a question's text is filled when a rule carrying that token targets the question assignment, its section mount, or its step — checked for every mount of the section. A placeholder with no default that nothing fills is an error, because the runtime would show the respondent the raw placeholder; one with a default is a warning. A Text rule whose token appears in no text it reaches is a warning.
