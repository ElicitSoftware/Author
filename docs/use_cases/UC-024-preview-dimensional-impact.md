# Use Case: Preview Dimensional Impact

## Overview

**Use Case ID:** UC-024  
**Use Case Name:** Preview Dimensional Impact  
**Primary Actor:** Survey Author  
**Goal:** See, before exporting, which dimension tables and report columns the survey's tags will create at a site, and anything that would make that build fail.  
**Status:** Implemented  
**Requirements:** [FR-026](../requirements.md)

## Preconditions

- The author is signed in with the `elicit_author` role.
- The survey exists in Author.

## Main Success Scenario

1. The author opens the survey's Reporting page.
2. The system derives, from the survey's tags and assignments, the dimension tables (shared per dimension or one per dimension-less tag) with the tags and assignment counts behind each, and the `fact_sections` columns with the table each one joins (BR-001).
3. The system lists findings: errors for names the site's reporting build would reject or for two tags producing the same column, warnings for tags with no assignments, assignments on removed elements, an element carrying the same tag twice, and a column another survey in this database maps to a different table.
4. The author reads the preview; the same derivation runs inside every tag and assignment dialog as a one-line preview.

## Alternative Flows

### A1: No tags

**Trigger:** The survey has no reporting tags (step 2)  
**Flow:**

1. The system states that the survey adds nothing to the reporting schema.
2. Use case ends.

### A2: Errors present

**Trigger:** A finding is an error (step 3)  
**Flow:**

1. The same finding appears in the survey's validation panel and blocks the export (UC-007, UC-008 A1) until fixed.
2. Use case ends.

## Postconditions

### Success Postconditions

- None; the preview changes nothing.

## Business Rules

### BR-001: The derivation mirrors the site's ETL

Names are computed exactly as Survey's reporting ETL computes them: `dim_<lower(dimension)>` for a tag on a dimension, `lower('dim_' || replace(tag,' ','_'))` for a tag without one, and `lower(replace(replace(tag,' ','_'),'-','')) || '_key'` for the column. Author never contacts a site's reporting schema; the preview describes what the ETL will build when the exported survey is applied and answers arrive.

### BR-002: Errors block export

A name outside `^[a-z0-9_]+$`, a name longer than 63 characters, two tags on one column, or an assignment naming no single element would make the site's reporting build fail, so they are export errors; everything else is a warning.
