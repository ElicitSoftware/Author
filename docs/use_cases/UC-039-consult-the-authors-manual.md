# Use Case: Consult the Author's Manual

## Overview

**Use Case ID:** UC-039  
**Use Case Name:** Consult the Author's Manual  
**Primary Actor:** Survey Author  
**Goal:** Obtain a printable, illustrated manual that walks through building a survey from an empty Surveys list to an exported `.elicit` file, stamped with the version of Author it was shipped with, so that the procedure can be followed away from the screen and attributed to one build.  
**Status:** Implemented  
**Requirements:** [FR-047, NFR-016](../requirements.md)

## Preconditions

- The author is signed in with the `elicit_author` role.
- The running application was built from an image that carries the manual.

## Main Success Scenario

1. The author chooses Manual in the application header.
2. The system opens the author's manual in a new browser tab as a printable document.
3. The system shows a title page naming the application, the version of the application the manual was built with, and the date that build was made.
4. The author reads the manual: an orientation chapter that names the pieces of a survey, then one chapter per stage of the work — signing in, creating a survey, laying out steps and sections, adding questions, giving choice questions their lists, connecting questions with rules, tagging for reporting, checking the validation panel, and exporting the definition.
5. The system illustrates each stage with a screenshot of the screen the step describes, captioned and numbered, so that the author can match the instruction to what is on screen.
6. The system repeats the version and the build date in the footer of every page.
7. The author follows the manual, on screen or on paper, and builds a survey.

## Alternative Flows

### A1: The image carries no manual

**Trigger:** The application was built without the manual, so there is nothing to open (step 1)  
**Flow:**

1. The system leaves the Manual link out of the header.
2. The author uses the in-app guide (UC-029) instead.
3. Use case ends.

### A2: The author prints the manual

**Trigger:** The author prints or saves the opened document (step 4)  
**Flow:**

1. The author prints the manual from the browser or saves the file.
2. The printed copy carries the same version and build date on the title page and in every footer, so the paper copy stays attributable to the build it came from.
3. Use case continues at step 7.

### A3: The build recorded no version

**Trigger:** The manual is built outside the application build, so no version or build date was supplied (step 3)  
**Flow:**

1. The system builds the manual with the version and the date both shown as "unknown".
2. The manual is otherwise complete and readable.
3. Use case continues at step 4.

### A4: The author is signed in without the authoring role

**Trigger:** The signed-in user does not hold `elicit_author` (step 1)  
**Flow:**

1. The system refuses access to the manual as it refuses the authoring views.
2. Use case ends.

## Postconditions

### Success Postconditions

- Nothing in the survey data is changed; the author holds a manual whose title page and page footers name the version and build date of the running application.

### Failure Postconditions

- No manual is shown; the application is otherwise unaffected and the in-app guide remains available.

## Business Rules

### BR-001: One vocabulary with the guide and the designer

The manual uses the same names and the same glyphs for step, section, question and each rule action as the in-app guide (UC-029), the structure tree and the designer board. It never introduces a term the application does not show.

### BR-002: Every stage is shown as well as told

Each stage of the procedure carries at least one captioned screenshot of the screen it describes. A stage described only in prose is incomplete.

### BR-003: The manual is stamped with its build

The version and the date recorded by the application build appear on the title page and in the footer of every page. When the build supplies neither, both read "unknown" rather than being omitted, so a copy is never silently undated.

### BR-004: The manual ships with the application

The manual is produced by the same build that produces the application image and travels inside it. The application never fetches the manual from an external location, and a manual that is absent is simply not offered (A1).

### BR-005: The manual is the default brand

The manual is typeset in the Elicit default brand — its logo, colors and typefaces — regardless of which brand a deployment mounts, so that one document serves every site.
