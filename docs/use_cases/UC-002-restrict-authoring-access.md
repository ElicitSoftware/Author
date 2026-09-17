# Use Case: Restrict Authoring Access

## Overview

**Use Case ID:** UC-002  
**Use Case Name:** Restrict Authoring Access  
**Primary Actor:** System Administrator  
**Secondary Actors:** Survey Author, Identity Provider (Keycloak/OIDC)  
**Goal:** Ensure that only users holding the `elicit_author` role can view or change survey definitions, so that unauthorized users cannot alter what will be deployed.  
**Status:** Draft  
**Requirements:** [FR-033, NFR-006](../requirements.md)

## Preconditions

- The identity provider defines the Author client with the role `elicit_author`.
- The system administrator can manage users and role assignments in the identity provider.

## Main Success Scenario

1. The system administrator assigns the `elicit_author` role, on the Author client, to a user in the identity provider.
2. The user signs in to Author (UC-001).
3. The system grants the `elicit_author` role to the session.
4. The user opens an authoring page.
5. The system verifies that the session holds `elicit_author` and displays the page.
6. The user works with survey definitions according to the other use cases.

## Alternative Flows

### A1: User without the role opens an authoring page

**Trigger:** The session does not hold `elicit_author` (step 5)  
**Flow:**

1. The system denies the request and shows the access-restricted page explaining that the `elicit_author` role is required and whom to contact.
2. The page offers Sign out.
3. Use case ends.

### A2: Role is revoked while a session is active

**Trigger:** The system administrator removes the role while the session is active (step 3)  
**Flow:**

1. The current session keeps its granted roles until it ends.
2. On the next sign-in the system grants no authoring role and A1 applies.
3. Use case ends.

### A3: Administrator-only pages

**Trigger:** A future page is restricted to a narrower role (step 5)  
**Flow:**

1. The system checks the narrower role instead and, when it is missing, shows the access-restricted page.
2. Use case ends.

## Postconditions

### Success Postconditions

- Only sessions holding `elicit_author` can reach authoring pages.
- Navigation entries and page routes are consistent: no entry is shown for a page the session cannot open.

### Failure Postconditions

- The request is denied and the access-restricted page is shown; no survey data is displayed or changed.

## Business Rules

### BR-001: Single authoring role

`elicit_author` is the only role that grants authoring access. It is granted explicitly per user; `elicit_admin` on any client does not imply it.

### BR-002: Deny by default

A page without an explicit access declaration is treated as restricted to `elicit_author`.

### BR-003: Role decisions are enforced on navigation, not just hidden in menus

Typing a route directly is checked the same way as following a navigation entry.
