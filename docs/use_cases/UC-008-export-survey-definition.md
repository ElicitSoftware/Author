# Use Case: Export Survey Definition

## Overview

**Use Case ID:** UC-008  
**Use Case Name:** Export Survey Definition  
**Primary Actor:** Survey Author  
**Secondary Actors:** Admin App (Apply Survey Definition)  
**Goal:** Produce a `.elicit` definition file for the survey, attributed to the author with a release note and a fresh revision, so that any Elicit site can install or update the survey through its Admin app.  
**Status:** Draft  
**Requirements:** [FR-004, FR-006, FR-007, C-015, C-017, C-019, NFR-002, NFR-009, NFR-011](../requirements.md)

## Preconditions

- The author is signed in with the `elicit_author` role.
- The survey exists in Author.

## Main Success Scenario

1. The author opens the survey and chooses Export.
2. The system runs validation (UC-007) and shows the export dialog with the findings, a release-note field and a reminder that renames and ontology changes apply retroactively at deployed sites.
3. The author enters a release note and confirms.
4. The system records the author and the release note on the survey as its last export attribution.
5. The system writes the definition file: the header with the format marker, the survey key, the survey name, per-table record counts, the generation time and a new revision equal to the export instant; then the survey record and every structural element in dependency order, including retired elements with their closing instant, with all keys carried verbatim.
6. The system offers the file for download under a name derived from the survey name and the export time, and shows the record counts and any warnings.
7. The author saves the file and hands it to the sites that will apply it, with the goal achieved.

## Alternative Flows

### A1: Validation errors

**Trigger:** Validation reports at least one error (step 2)  
**Flow:**

1. The system disables the confirm action and lists the errors with links to the elements.
2. The author corrects the elements and chooses Export again.
3. Use case continues at step 2.

### A2: Author cancels

**Trigger:** The author closes the dialog without confirming (step 3)  
**Flow:**

1. The system records nothing.
2. Use case ends.

### A3: Tables Author does not model

**Trigger:** The format carries a table Author does not yet model (post-survey actions) (step 5)  
**Flow:**

1. The system writes that table with a record count of zero; dimensions, ontology and metadata records are written whenever the survey carries analysis tags (UC-021, UC-023), and `reports` and `report_items` records whenever it lists report services (UC-048) or report marks (UC-049).
2. Use case continues at step 6.

### A4: File cannot be produced

**Trigger:** Writing the file fails (step 5)  
**Flow:**

1. The system reports the failure and leaves the last export attribution as it was before step 4.
2. Use case ends.

## Postconditions

### Success Postconditions

- A definition file exists whose content reproduces the survey's current and retired elements field for field and whose keys equal Author's.
- The survey shows the author, time and release note of this export.
- The file's revision is later than that of any earlier export of the same survey.

### Failure Postconditions

- No file is offered; the survey's last export attribution is unchanged.

## Business Rules

### BR-001: One file for every site

Each export mints a new revision, so one exported file should be distributed to every site rather than re-exported per site; a site refuses a file whose revision is older than the one it already applied.

### BR-002: Retired elements travel with the file

An element removed in Author (UC-009) is written as retired so that a site applying the file closes it; it is never omitted, because omission would leave it live at the site.

### BR-003: Attribution is mandatory

An export always records the exporting author; the release note is optional but recommended.

### BR-004: Byte compatibility

The file is written exactly as Admin's exporter would write it, so Admin's install and update read it without special handling.

### BR-005: Local identifiers are not identity

Local numeric identifiers in the file are used only to link records within the file; sites allocate their own. Only the survey key, element keys and revision have meaning across deployments.

### BR-006: Report services and report marks travel whole

Every `reports` row of the survey is written, in display order, and every `report_items` row, ordered by report, then display order, then id, with their keys verbatim, so that a site's apply installs the author's list and deletes what the author removed (UC-048 BR-006, UC-049 BR-005). A mark on a removed element is still written, with an export warning naming it (UC-049 A3).
