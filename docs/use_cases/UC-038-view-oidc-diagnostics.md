# Use Case: View OIDC Diagnostics

## Overview

**Use Case ID:** UC-038  
**Use Case Name:** View OIDC Diagnostics  
**Primary Actor:** System Administrator  
**Goal:** Inspect the current sign-in's authentication state to troubleshoot why authoring access is or is not granted.  
**Status:** Tested  
**Requirements:** [FR-046, NFR-014, C-008](../requirements.md)

## Preconditions

- The administrator is authenticated and holds the `elicit_admin` role (UC-001). That role grants no authoring access (UC-002).

## Main Success Scenario

1. The administrator opens OIDC from the System section.
2. The system displays the current identity's details: principal name, whether the identity is anonymous, the full set of roles, where the roles came from, whether the authoring and administrator roles are held, the token issuer and email claim, and the identity and access tokens masked to their last four characters.
3. The administrator reads the information to diagnose an access issue, typically an author whose token carries no `elicit_author` role.

## Alternative Flows

### A1: Token not available

**Trigger:** The identity or access token cannot be resolved (step 2)  
**Flow:**

1. The system shows a "not available" indicator for that item instead of failing.
2. Use case continues at step 3.

## Postconditions

### Success Postconditions

- The administrator has seen the effective authentication state; no data is changed.

### Failure Postconditions

- None; the view is read-only.

## Business Rules

### BR-001: Tokens are masked

Tokens show only their last four characters unless `elicit.debug.reveal-tokens=true` is set for a debugging session (NFR-014).

### BR-002: Roles are shown as received

The screen reports the roles the identity provider supplied, unchanged, so a missing `elicit_author` is visible as such (UC-002).

---

## Reference

Traces to FR-046. Mirrors the Admin console's diagnostics page. Implemented by `OidcDiagnosticsView` and
`RoleSecurityIdentityAugmentor` (the role source). Verified by `SystemViewsRenderTest`.
