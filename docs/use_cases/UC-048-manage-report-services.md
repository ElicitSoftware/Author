# Use Case: Manage Report Services

## Overview

**Use Case ID:** UC-048  
**Use Case Name:** Manage Report Services  
**Primary Actor:** Survey Author  
**Secondary Actors:** Admin App (Apply Survey Definition)  
**Goal:** List the reports a survey offers its respondents — the built-in respondent report every new survey starts with, and any custom report service a site runs — with a name, a description, an address and an order, so that the exported definition carries exactly the reports the author wants and a site installs them without editing a row by hand.  
**Status:** Implemented  
**Requirements:** [FR-068, C-015, C-017](../requirements.md)

## Preconditions

- The author is signed in with the `elicit_author` role.
- The survey exists in Author.

## Main Success Scenario

1. The author opens the survey's Reports page (Reports… on the survey overview).
2. The system lists the survey's report services in display order: name, description, address, and a built-in badge on the respondent report (BR-002).
3. The author chooses Add report service…, enters a name, a description and the service's address, and saves.
4. The system validates the name (BR-003) and the address (BR-004), stores the row with a new report key and the next display order, and refreshes the list.
5. The author drags a row to another position; the system renumbers the display order and keeps it (BR-005).
6. The author chooses Edit… on a row, changes its name, description or address, and saves; the system stores the change under the same report key.
7. The author chooses Remove on a row and confirms; the system deletes the row and renumbers the rest (BR-006).

## Alternative Flows

### A1: A new survey

**Trigger:** The survey was just created from the blank form (UC-003) or imported from an AI draft (UC-042) (step 2)  
**Flow:**

1. The list already holds the built-in respondent report, seeded when the survey was created (BR-001).
2. Use case continues at step 3.

### A2: Name already used

**Trigger:** Another report service of the survey has the entered name (step 4)  
**Flow:**

1. The system refuses and marks the name field.
2. The author enters another name.
3. Use case continues at step 3.

### A3: Removing the built-in report

**Trigger:** The author removes the built-in respondent report (step 7)  
**Flow:**

1. The system says that respondents of this survey will see no report, and that the survey's Respondent marks (UC-049) will stay but reach no report until a report service lists them.
2. The author confirms; the row is deleted like any other.
3. Use case ends.

### A4: Author cancels

**Trigger:** The author closes a dialog without saving (step 3 or 6)  
**Flow:**

1. Nothing is changed.
2. Use case ends.

## Postconditions

### Success Postconditions

- The survey's report rows name exactly the services the author listed, in the order shown; the next export carries one record per row (UC-008) and a site's apply installs, updates or deletes them by report key.

### Failure Postconditions

- No row is created, changed or removed.

## Business Rules

### BR-001: Every new survey starts with the built-in respondent report

Creating a survey from the blank form (UC-003) or from an AI draft (UC-042) seeds one row: the respondent report, named "Summary of your answers", whose address is the built-in service's path `/api/reports/respondent-summary`. Importing a definition file (UC-005) seeds nothing, because the file is the whole list. The row is a starting point: the author keeps it, moves it, renames it or removes it like any other row. A survey whose list is empty has no report page at a site (Survey UC-005 BR-014): Finish takes the respondent straight to the post-survey URL.

### BR-002: Built-in is a badge, not a kind

A row is the built-in report when its address is the built-in service's path. Nothing else distinguishes it: it is stored, exported, translated and applied exactly as a custom row, and a site's Survey and Admin modules call it through the same contract. The badge only tells an author which row needs no service of their own.

### BR-003: Name and description are respondent-facing and translatable

The name heads the report's card and the description is shown with it, so both are required, at most 255 characters, translatable (UC-043), and the name is unique within the survey. The address is never translated.

### BR-004: The address is a path or an absolute URL

A path starting with `/` names a service inside the site's Survey module and is resolved against the site's own Survey address when the report is requested (Survey UC-005, Admin UC-005 BR-023), so one file serves every site. An absolute `http` or `https` URL names a service outside Survey, such as a site-specific report, and is carried verbatim. Anything else is refused.

### BR-005: Order is the order on the page

Reports render on the respondent's report page in display order (Survey UC-005 BR-012). The list on the Reports page is that order; a drag renumbers the rows 1..n and nothing else about them changes.

### BR-006: A removed row is deleted, and the site deletes it too

`reports` is a Type 1 table with no history (C-015): removing a row deletes it, and a site applying the next export deletes the row whose key the file no longer lists (Admin UC-017 BR-113). The report key is minted once in Author and never changes (C-017), so an edit updates the site's row in place.

## Notes / Known Gaps

- The built-in service (`/api/reports/respondent-summary`) is phase 4 of the umbrella's default reports plan (`Elicit/docs/research/default_reports.md`, section 3), in Survey. Until it ships, a site renders the seeded row's card as an error; nothing is released, so no site sees it.
- The preview of the respondent report through the preview Survey (plan section 2.2) is part of phase 4 and is not offered by this page yet.
