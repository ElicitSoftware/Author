# Use Case: View System Overview

## Overview

**Use Case ID:** UC-034  
**Use Case Name:** View System Overview  
**Primary Actor:** System Administrator  
**Goal:** See on one screen whether this Author deployment is wired correctly: what is running, whether it is healthy, and whether every required setting is present.  
**Status:** Tested  
**Requirements:** [FR-042, NFR-014, C-020, C-021](../requirements.md)

## Preconditions

- The administrator is authenticated and holds the `elicit_admin` role (UC-001). That role grants no authoring access (UC-002).

## Main Success Scenario

1. The administrator opens Overview from the System section of the navigation.
2. The system shows what is running: application name, version, build time, active profile and the time since the service started.
3. The system shows health: the database connection, whether the survey schema is present, a one-line branding summary, and how many outbound targets are configured, each linking to its own screen (UC-035 to UC-037).
4. The system lists every setting that has no default and must be supplied by the deployment (the database password and the OIDC client secret) and reports each as present or absent, never its value.
5. The administrator reads the result and follows a link to the screen that explains a failure.

## Alternative Flows

### A1: A required setting is absent

**Trigger:** A setting listed in step 4 has no value (step 4)  
**Flow:**

1. The system marks the setting absent and names the environment variable that supplies it.
2. Use case continues at step 5; the screen still renders.

### A2: The database connection fails

**Trigger:** The connection probe fails (step 3)  
**Flow:**

1. The system marks the connection failed with the driver's message and reports the schema as not checked.
2. Use case continues at step 4.

### A3: The build information was not recorded

**Trigger:** The version or build time is not available in this build (step 2)  
**Flow:**

1. The system shows "unknown" for that value instead of failing.
2. Use case continues at step 3.

## Postconditions

### Success Postconditions

- The administrator has seen the running version, health and required settings; no data is changed.

### Failure Postconditions

- None; the screen is read-only.

## Business Rules

### BR-001: Secrets are reported as present or absent

No password, client secret or token value is rendered on any System screen (NFR-014). A required secret is shown only as present or absent, together with the name of the setting that supplies it.

### BR-002: The overview reports what is true now

Health and settings are re-established each time the screen is opened, not captured at service start.

### BR-003: Configuration is shown, not edited

Datasource, OIDC and telemetry settings are startup configuration (C-020). The screen names the setting and reports its state; it never offers to change it.

---

## Reference

Traces to FR-042. Implemented by `SystemOverviewView` with `BuildInfo`, `RequiredConfigCheck` and the per-area
summaries from `DatabaseDiagnostics`, `BrandDiagnostics` and `ConnectionChecks` (all under
`com.elicitsoftware.author.diagnostics`). The version and build time come from the Maven-filtered
`quarkus.application.version` and `build.timestamp` properties. Verified by `SystemViewsRenderTest`.
