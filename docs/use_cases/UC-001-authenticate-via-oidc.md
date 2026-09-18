# Use Case: Authenticate via OIDC

## Overview

**Use Case ID:** UC-001  
**Use Case Name:** Authenticate via OIDC  
**Primary Actor:** Survey Author  
**Secondary Actors:** Identity Provider (Keycloak/OIDC), System Administrator  
**Goal:** Sign in through the organization's identity provider and reach the authoring workspace with the roles that govern what the author may do.  
**Status:** Draft  
**Requirements:** [FR-032, NFR-006](../requirements.md)

## Preconditions

- The author has an account in the configured OIDC provider (Keycloak in the local stack; any OIDC-compliant server in production).
- Author is registered with the identity provider as its own client (`elicit-author`).
- Author and the identity provider are reachable.

## Main Success Scenario

1. The author opens any protected page of Author.
2. The system detects that the author is not authenticated and redirects to the identity provider.
3. The author authenticates with the identity provider.
4. The identity provider returns the author to Author with an identity.
5. The system reads the roles the identity carries for the Author client and grants exactly those roles; no role is inferred from roles held for other Elicit applications.
6. The system restores the page the author originally requested and displays it inside the branded application shell.

## Alternative Flows

### A1: Authentication is cancelled or fails at the identity provider

**Trigger:** The identity provider does not return a valid identity (step 4)  
**Flow:**

1. The identity provider shows its own error or the author abandons the sign-in.
2. Author remains unavailable to that browser session; no page is shown.
3. Use case ends.

### A2: Authenticated but holds no authoring role

**Trigger:** The identity carries none of the Author roles (step 5)  
**Flow:**

1. The system signs the author in but grants no authoring role.
2. When the author requests an authoring page, the system shows the access-restricted page (see UC-002) with a sign-out option.
3. Use case ends.

### A3: Sign out

**Trigger:** The author chooses Sign out from the navigation (step 6)  
**Flow:**

1. The system ends the Author session.
2. The system ends the identity-provider session and returns the author to the landing page, which starts the sign-in flow again on the next protected request.
3. Use case ends.

## Postconditions

### Success Postconditions

- The author has an authenticated session with the roles granted for the Author client.
- The originally requested page is displayed.

### Failure Postconditions

- No authenticated session exists in Author, or a session exists without authoring roles and no authoring page is displayed.
- No survey data has been read or changed.

## Business Rules

### BR-001: Roles come only from the Author client

Roles are read from what the identity provider assigns to the `elicit-author` client. Roles held for the Admin or Survey applications, including `elicit_admin`, do not grant authoring access.

### BR-002: Every authoring route requires authentication

Only the sign-out, health, metrics and brand-asset endpoints are reachable without an authenticated session.

### BR-003: Same identity across the platform

The identity provider is shared with Admin and Survey; the author's principal name is the identity recorded on exports (see UC-008).
