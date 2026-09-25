# Use Case: Manage Content Languages

## Overview

**Use Case ID:** UC-044  
**Use Case Name:** Manage Content Languages  
**Primary Actor:** Survey Author  
**Goal:** Declare the language a survey's content is written in and the languages it is published in, so that a finished language reaches every site and a half-finished one is held back.  
**Status:** Draft  
**Requirements:** [FR-057, C-024](../requirements.md)

## Preconditions

- The author is authenticated (UC-001) and the survey is open in the tool.
- The survey has a base language; a survey created before this feature has `en`.

## Main Success Scenario

1. The author opens the survey's metadata dialog.
2. The system shows the base language, the language the survey's content is written in, beside the survey's name, title and description.
3. The system shows the content languages, the set the survey is published in, each with its count of translated, missing and out-of-date strings.
4. The author adds a language to the published set.
5. The system records it with the survey and offers it on the Translations page (UC-043).
6. The author translates the survey (UC-043, UC-045, UC-046) and exports it (UC-008); every site that mounts that language for its own chrome now serves the survey in it.

## Alternative Flows

### A1: The author publishes an incomplete language

**Trigger:** The author adds a language with missing or out-of-date strings to the published set (step 4)  
**Flow:**

1. The system accepts it and warns how many strings will fall back to the base language at every site.
2. Use case continues at step 5.

### A2: The author withdraws a language

**Trigger:** The author removes a language from the published set (step 3)  
**Flow:**

1. The system explains that the translations are kept, not deleted, and that no site will serve them until the language is published again.
2. The author confirms; the language leaves the set at the next export.
3. Use case ends.

### A3: The author changes the base language

**Trigger:** The author changes the language the content is written in (step 2)  
**Flow:**

1. The system warns that every existing translation was made from text in the old base language and that changing it does not translate the content.
2. The author confirms; the base language changes and every translation of the new base language's own tag, if any, is redundant and reported.
3. Use case ends.

## Postconditions

### Success Postconditions

- The survey records its base language and its published set; both travel in the definition file and are applied at every site.

### Failure Postconditions

- A rejected tag leaves both settings unchanged.

## Business Rules

### BR-001: The published set is the author's decision, not a count

A language is published because the author says so, not because translations exist for it. A language may be fully translated and unpublished while work is reviewed, or published and incomplete with the missing strings falling back.

### BR-002: A site serves a language only when its own chrome has it

The published set says what the survey offers; what a respondent is offered at a given site is the intersection of that set with the languages mounted for the application's own texts there (Survey UC-009 BR-009). A site that has not adopted a language holds its rows without serving them, so one file suits every site.

### BR-003: Every site gets the same file

The definition file carries every language the survey publishes. There is no per-site export: one revision is one file, distributed to every site, and a site's own mount decides what its respondents see.

### BR-004: Language tags are written as the chrome writes them

A content language is a BCP-47 tag in the same form the mounted chrome bundles use (`es-419`, `ar`), so the two sets can be intersected without guessing.

### BR-005: The base language is the fallback for every string

Whatever a survey is published in, every untranslated and every out-of-date string is shown in the base language (Survey UC-009 BR-005). A survey is therefore never partly unreadable.
