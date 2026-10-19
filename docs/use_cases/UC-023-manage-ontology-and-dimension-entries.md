# Use Case: Manage Analysis Tags and Dimensions

## Overview

**Use Case ID:** UC-023  
**Use Case Name:** Manage Analysis Tags and Dimensions  
**Primary Actor:** Survey Author  
**Goal:** Create, edit and remove a survey's analysis tags, choosing for each an existing dimension, a new dimension or none, so that questions can be linked to the analysis structure they need.  
**Status:** Implemented  
**Requirements:** [FR-025, C-017](../requirements.md)

## Preconditions

- The author is signed in with the `elicit_author` role.
- The survey exists in Author.

## Main Success Scenario

1. The author opens the survey's Analysis page (Analysis… on the survey overview) and chooses New tag… (or New tag… inside the assignment dialog, UC-021 A2).
2. The system opens the tag dialog with the platform's dimensions to choose from.
3. The author enters the tag and either picks a dimension, types a new dimension name, or leaves the dimension empty.
4. The system previews the dimension table and column the tag produces (UC-024) and validates the names (BR-002, BR-003).
5. The author saves; the system creates the dimension if it is new (BR-001), stores the tag under the survey's namespace (BR-004) with a new element key, and lists it.

## Alternative Flows

### A1: Duplicate tag

**Trigger:** A tag with the same namespace and name exists (step 5)  
**Flow:**

1. The system refuses and marks the tag field.
2. Use case continues at step 3.

### A2: Tag in use cannot be removed

**Trigger:** The author chooses Remove on a tag that has assignments (step 1)  
**Flow:**

1. Remove is disabled and explains that the assignments must be removed first (UC-022 A2).
2. Use case ends.

### A3: Name the site's analysis build would reject

**Trigger:** The tag or dimension would produce a table or column name outside `a-z`, `0-9`, `_` or longer than 63 characters — for example a hyphenated tag with no dimension (step 4)  
**Flow:**

1. The system marks the field with the reason.
2. Use case continues at step 3.

### A4: Editing an assigned tag

**Trigger:** The author changes the name or dimension of a tag that has assignments (step 3)  
**Flow:**

1. The system shows the retroactivity warning; Save waits for an acknowledgement (as UC-022 step 4).
2. On save the change is recorded once per assignment for the export-time warning.
3. Use case continues at step 5.

## Postconditions

### Success Postconditions

- The tag (and any new dimension) exists and is exported with the survey; Admin reuses dimensions by name and tags by namespace and tag at a site.

### Failure Postconditions

- No tag or dimension is created or changed.

## Business Rules

### BR-001: Dimensions are platform-wide

`dimensions` is a shared vocabulary; a name is matched case-insensitively and reused rather than duplicated. Tags on the same dimension share one table of values (`dim_<dimension>`).

### BR-002: Tag names

Letters, digits, spaces and hyphens, starting with a letter or digit. The column is `<lower(replace(replace(tag,' ','_'),'-',''))>_key`.

### BR-003: Dimension names

Letters, digits and underscores only, at most 50 characters; the table is `dim_<lower(name)>`. A tag without a dimension names its own table `dim_<lower(replace(tag,' ','_'))>`, so it may not contain a hyphen.

### BR-004: Namespace

`ontology.name` is a namespace the ETL never reads; Author writes the survey's name at creation and never rewrites it. `(name, tag)` is unique across the database.
