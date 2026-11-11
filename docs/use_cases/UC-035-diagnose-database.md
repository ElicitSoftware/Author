# Use Case: Diagnose Database

## Overview

**Use Case ID:** UC-035  
**Use Case Name:** Diagnose Database  
**Primary Actor:** System Administrator  
**Goal:** Confirm that Author's database connection works, that the survey schema it depends on is present, and which Survey migration created it, so that a credential problem can be told apart from a schema that was never created.  
**Status:** Tested  
**Requirements:** [FR-043, C-005, C-006](../requirements.md)

## Preconditions

- The administrator is authenticated and holds the `elicit_author_admin` role (UC-001). That role grants no authoring access (UC-002).

## Main Success Scenario

1. The administrator opens Database from the System section.
2. The system connects, runs a trivial query, and shows the connected user, the database, the server version, and the round-trip time.
3. The system reports whether the survey schema is present, since Author never creates it (C-005): it is created by the Survey preview instance sharing the database.
4. The system shows the latest Survey migration recorded in that database, when it was applied and whether it succeeded, and whether the Kimball durable-key sequences exist.
5. The system shows how many surveys are authored in this database.
6. The administrator reads the result to locate the problem.

## Alternative Flows

### A1: The connection fails

**Trigger:** Connecting or the trivial query fails (step 2)  
**Flow:**

1. The system marks the connection failed with the driver's message and names the setting that supplies its password as present or absent (UC-034 BR-001).
2. Use case ends; the schema cannot be examined.

### A2: The Survey migration history is absent

**Trigger:** The history table does not exist (step 4)  
**Flow:**

1. The system reports the migration as not installed rather than failing the screen; the schema-presence check in step 3 says whether the schema exists at all.
2. Use case continues at step 5.

## Postconditions

### Success Postconditions

- The administrator has seen the state of the connection and the schema; no data is changed.

### Failure Postconditions

- None; the screen is read-only.

## Business Rules

### BR-001: Schema state is read, never changed

The screen reads the schema and the history table. Creating or migrating the schema stays with the Survey module (C-005).

### BR-002: The schema is the preview instance's

The database Author connects to is shared with a Survey preview instance (C-006); a missing schema means that instance has not started against this database yet, and the screen says so.

---

## Reference

Traces to FR-043. Implemented by `DatabaseDiagnostics` and `SystemDatabaseView`. Verified by
`DatabaseDiagnosticsTest` and `SystemViewsRenderTest`.
