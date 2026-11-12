# Use Case: Manage Post-Survey Actions

## Overview

**Use Case ID:** UC-052  
**Use Case Name:** Manage Post-Survey Actions  
**Primary Actor:** Survey Author  
**Secondary Actors:** Admin App (Apply Survey Definition)  
**Goal:** List the post-survey actions a survey's site calls when a respondent finishes (each a name, a description, an address and an execution order), so that the exported definition carries exactly the actions the author wants, a site changes an action's address by applying a new export, and an action the author removes is removed at the site too.  
**Status:** Implemented  
**Requirements:** [FR-079, C-015, C-017](../requirements.md)

## Preconditions

- The author is signed in with the `elicit_author` role.
- The survey exists in Author.

## Main Success Scenario

1. The author opens the survey's Post-survey actions page (Post-survey actions… on the survey overview).
2. The system lists the survey's actions in execution order, each with its name, description and address, and says that a site calls them, in this order, when a respondent finishes, and that respondents never see them (BR-001).
3. The author chooses Add action…, enters a name, a description and the action's address, and saves.
4. The system validates the name (BR-002), the description (BR-003) and the address (BR-004), stores the action with a new key and the next execution order, and refreshes the list.
5. The author drags a row to another position; the system renumbers the execution order and keeps it (BR-005).
6. The author chooses Edit… on a row, changes its name, description or address, and saves; the system stores the change under the same key (BR-007).
7. The author chooses Remove on a row and confirms; the system deletes the action and renumbers the rest (BR-006).
8. The survey's actions are exactly those listed, in the order shown, ready for the next export.

## Alternative Flows

### A1: Name already used

**Trigger:** Another action of the survey has the entered name (step 4)  
**Flow:**

1. The system refuses and marks the name field.
2. The author enters another name.
3. Use case continues at step 3.

### A2: Address is not a full web address

**Trigger:** The entered address is empty, longer than 255 characters, or not an absolute `http` or `https` address (step 4)  
**Flow:**

1. The system refuses, marks the address field and says that a site calls the action at exactly this address, so it must be complete.
2. The author corrects the address.
3. Use case continues at step 3.

### A3: Removing an action a site has called

**Trigger:** The author chooses Remove on a row (step 7)  
**Flow:**

1. The confirmation says that a site applying the next export stops calling the action and deletes its record of which respondents the action was called for.
2. The author confirms, and the action is deleted like any other.
3. Use case continues at step 8.

### A4: Author cancels

**Trigger:** The author closes a dialog without saving (step 3, 6 or 7)  
**Flow:**

1. Nothing is changed.
2. Use case ends.

## Postconditions

### Success Postconditions

- The survey's post-survey actions are exactly the ones the author listed, in the order shown; the next export carries one record per action (UC-008), and a site's apply inserts, updates or deletes them by key (Admin UC-017 BR-113).

### Failure Postconditions

- No action is created, changed or removed.

## Business Rules

### BR-001: An action is the site's, and invisible to respondents

Survey calls each action at its address, with the respondent's id, at the moment the respondent finishes (Survey UC-004). Any success status counts as done and the answer's body is not read, so an action owes Survey no particular response, and certainly no HTML: that is a report service's contract (UC-048), not an action's. Nothing of it is shown to a respondent, so the name and description are notes for authors and site operators. They are never translated (UC-043 offers neither), and the page says so.

### BR-002: The name is required and unique

The name is required, at most 255 characters, and unique within the survey, as the table's own constraint requires, so that the list, the export and a site's Connections screen tell actions apart.

### BR-003: The description is required

The description is required and at most 255 characters, as `survey.post_survey_actions` defines it (Survey V001).

### BR-004: The address is a complete http(s) address

Survey calls the address exactly as written, from the site's Survey container, so it must be an absolute `http` or `https` URL of at most 255 characters, the column's width. Unlike a report service (UC-048 BR-004), an action has no path form: nothing inside Survey answers one.

### BR-005: Order is the order Survey calls them in

Survey calls a survey's actions in execution order, each on its own, and one failing does not stop the next (Survey UC-004 BR-010). The list on the page is that order; a drag renumbers the actions 1..n and nothing else about them changes.

### BR-006: A removed action is deleted, here and at the site

`post_survey_actions` is a Type 1 table with no history (C-015). Removing an action deletes it, along with any record in Author's own database of the action being called for a preview respondent. A site applying the next export deletes the action whose key the file no longer lists, with its delivery records (Admin UC-017 BR-113).

### BR-007: The key never changes

An action's key is minted when it is created in Author, or carried verbatim from an imported file, and never changes (C-017), so an edit updates the site's action in place.

### BR-008: A new survey starts with no actions

Creating a survey seeds no action. Importing a definition file carries the file's actions, keys and order unchanged (UC-005).

## Notes / Known Gaps

- The preview Survey Author runs beside it calls a survey's actions too, when a preview respondent finishes. An action whose address the authoring stack can reach is therefore called by a preview; an author previewing a survey whose action files documents elsewhere should expect that.
