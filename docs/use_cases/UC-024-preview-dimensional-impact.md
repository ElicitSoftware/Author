# Use Case: Preview Dimensional Impact

## Overview

**Use Case ID:** UC-024  
**Use Case Name:** Preview Dimensional Impact  
**Primary Actor:** Survey Author  
**Goal:** See, before exporting, which dimension tables and columns the survey's analysis tags will create at a site, and anything that would make that build fail.  
**Status:** Implemented  
**Requirements:** [FR-026](../requirements.md)

## Preconditions

- The author is signed in with the `elicit_author` role.
- The survey exists in Author.

## Main Success Scenario

1. The author opens the survey's Analysis page.
2. The system derives, from the survey's tags and assignments, the dimension tables (shared per dimension or one per dimension-less tag) with the tags and assignment counts behind each, and the `fact_sections` columns with the table each one joins (BR-001). When the survey has a Repeat rule reading a multi-select question into a section, it also lists the two fixed columns the site fills for such a section, `question_key` and `item_key`, with the dimensions `dim_question` and `dim_item` behind them (BR-003).
3. The system lists findings: errors for names the site's analysis build would reject, for a tag claiming a fixed column's name, or for two tags producing the same column; warnings for tags with no assignments, assignments on removed elements, and an element carrying the same tag twice.
4. The author reads the preview; the same derivation runs inside every tag and assignment dialog as a one-line preview.

## Alternative Flows

### A1: No tags

**Trigger:** The survey has no analysis tags (step 2)  
**Flow:**

1. The system states that the survey adds nothing to the site's analysis schema.
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

Names are computed exactly as Survey's reporting ETL computes them: `dim_<lower(dimension)>` for a tag on a dimension, `lower('dim_' || replace(tag,' ','_'))` for a tag without one, and `lower(replace(replace(tag,' ','_'),'-','')) || '_key'` for the column. The objects live in a reporting schema the site creates for this survey alone (Survey UC-008 BR-006), whose name the site chooses, so the preview shows unqualified table and column names and never a schema name. Author never contacts a site's reporting schema; the preview describes what the ETL will build when the exported survey is applied and answers arrive. Because every survey has a schema of its own, what another survey in this database names is of no concern to this one.

### BR-002: Errors block export

A name outside `^[a-z0-9_]+$`, a name longer than 63 characters, a column named `step_key`, `section_key`, `question_key` or `item_key` (the fixed columns of every survey's fact table, which the site's build refuses a tag for), two tags on one column, or an assignment naming no single element would make the site's reporting build fail, so they are export errors; everything else is a warning.

### BR-003: The per-item columns are the site's, not the author's

A section repeated once per selected item (UC-019 BR-007) gets, on each of its reporting rows, the question the Repeat rule reads and the item the instance is about, keyed by the question's and the item's own keys (Survey UC-008 BR-012). No tag produces them; the preview lists them so an author knows those rows can be grouped by item, and that a reorder of the list moves nothing in reporting.
