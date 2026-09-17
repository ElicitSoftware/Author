# Use Case: Import Survey Definition

## Overview

**Use Case ID:** UC-005  
**Use Case Name:** Import Survey Definition  
**Primary Actor:** Survey Author  
**Goal:** Load an existing `.elicit` definition file into Author as a new working copy, preserving every element's identity, so that a survey authored elsewhere can be edited and re-exported.  
**Status:** Draft  
**Requirements:** [FR-003, FR-009, C-015, C-017, NFR-011](../requirements.md)

## Preconditions

- The author is signed in with the `elicit_author` role.
- The author has a definition file in the `ELICIT_SURVEY_EXPORT_V1` format (exported by Author or by an Admin app).

## Main Success Scenario

1. The author opens Import definition and selects a `.elicit` file of at most 5 MB.
2. The system reads the file header and confirms the format marker, the survey key and the record counts.
3. The system checks that no survey with the same survey key exists in Author.
4. The system creates the survey and every structural element from the file in dependency order, allocating fresh local identifiers, keeping every survey key and element key exactly as in the file, and rewriting the leading survey component of every display key to the new local survey.
5. The system skips records marked as retired in the file and reports how many were skipped.
6. The system stores the file's revision and record counts with the result and shows a summary per table.
7. The author chooses Open to continue in the editor (UC-006), with the goal achieved: the survey is now a working copy in Author.

## Alternative Flows

### A1: Not a definition file

**Trigger:** The header lacks the format marker or a survey record (step 2)  
**Flow:**

1. The system rejects the file and explains that it is not a survey definition export.
2. Use case ends.

### A2: Survey already present in Author

**Trigger:** A survey with the file's survey key already exists (step 3)  
**Flow:**

1. The system rejects the import, names the existing survey, and offers to open it instead.
2. Use case ends.

### A3: File without a survey key

**Trigger:** The header or survey record carries no survey key (step 2)  
**Flow:**

1. The system rejects the file, explaining that it predates stable identities and cannot be matched across deployments, and suggests re-exporting it from its source.
2. Use case ends.

### A4: Unsupported record types

**Trigger:** The file carries reports, post-survey actions, dimensions, ontology or metadata records while Author's schema has no element keys for those tables (step 2)  
**Flow:**

1. The system rejects the file before writing anything and states which record types are not yet supported.
2. Use case ends.

### A5: Malformed or dangling record

**Trigger:** A record has the wrong number of fields or references an identifier that no earlier record defined (step 4)  
**Flow:**

1. The system stops, discards everything imported so far, and reports the line number and reason.
2. Use case ends.

### A6: File too large or wrong extension

**Trigger:** The selected file exceeds 5 MB or is not a `.elicit` file (step 1)  
**Flow:**

1. The system refuses the selection before uploading.
2. Use case ends.

## Postconditions

### Success Postconditions

- A new survey exists in Author whose survey key and element keys equal those in the file.
- Every current structural element from the file exists exactly once as a working copy; retired records were not created.
- Display keys refer to the new local survey.

### Failure Postconditions

- No survey or element from the file exists in Author; a partial import is never left behind.

## Business Rules

### BR-001: Identity is carried verbatim

Survey key and element keys are copied from the file unchanged. Local identifiers, list order and display-key prefixes are Author's own.

### BR-002: One working copy per survey key

Author holds at most one survey per survey key; importing a duplicate is rejected rather than merged or replaced.

### BR-003: Per-row version fields are informational

The file's version, validity window and attribution fields are read for reporting but every imported element becomes a plain working copy.

### BR-004: All or nothing

An import either completes for the whole file or leaves Author unchanged.

### BR-005: Format parity with Admin

The reader accepts exactly what Admin's importer accepts; escaping, field layouts and header lines follow Admin's definition of the format.
