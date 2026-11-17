# Use Case: Install Author from the Installation Manual

## Overview

**Use Case ID:** UC-050  
**Use Case Name:** Install Author from the Installation Manual  
**Primary Actor:** System Administrator  
**Goal:** Take a machine with no Author to a running, verified Author — its own database, its identity-provider client, its preview Survey and the application itself — by following a printed manual, without reading Author's source or the platform's compose file.  
**Status:** Draft  
**Requirements:** [FR-070, FR-072, FR-073, FR-074, FR-075, FR-076, FR-077, FR-078, NFR-020, NFR-021, NFR-022, NFR-025, C-027](../requirements.md)

## Preconditions

- The administrator holds the installation manual for the Author release being installed, from that release or built from its sources (UC-051).
- A PostgreSQL cluster carrying the roles the Elicit database image creates is available, either a site's cluster or one of Author's own.
- An OIDC provider is available, on which the administrator can create a client and roles.
- A container runtime is available. Author and its preview are released as container images only (BR-007).

## Main Success Scenario

1. The administrator opens the installation manual and confirms from its title page that its version is the release being installed.
2. The system's manual explains what Author is and what it needs: a database of its own, an OIDC client, a preview Survey, and the brand and translations directories. It states that Author calls neither Survey, Admin, FHHS nor Pedigree at a site, and that the exported `.elicit` file is the only thing that travels from Author to a site.
3. The administrator works through the prerequisites checklist.
4. The administrator provisions the `author` database following the manual: creating it in the cluster, granting it to the Elicit roles, and creating the `survey` schema owned by `elicit_owner`. The tables are left to the preview Survey's migrations (BR-002).
5. The administrator creates the `elicit-author` client at the identity provider, with its redirect and post-logout URIs and its secret. They create the client's two roles, `elicit_author` and `elicit_author_admin`, and grant each to the people who need it (BR-003).
6. The administrator starts the preview Survey: a Survey instance of the same release, pointed at the `author` database, with the preview settings the manual lists and the deployment's service token (BR-008). They wait for it to report ready. On its first start it builds the `survey` schema there.
7. The administrator starts Author with its database, its client secret, the same service token as the preview, the identity provider's address, the preview Survey's addresses (the one Author calls and, when it differs, the one a browser opens), and the brand and translations directories. The values are taken from the configuration reference, which marks each setting as one to set, one that may be set, or one to leave alone, and identifies every secret.
8. The administrator mounts the site's brand directory and the curated translations, so that Author reads its own `author/` sub-directory. They leave `author.content.languages` at the release's value (BR-005).
9. The administrator verifies the installation in the order the manual gives, starting with Author's health endpoint, which reports ready.
10. A user holding `elicit_author` signs in and reaches the Surveys list.
11. A user holding `elicit_author_admin` opens the System overview, which reports every required setting as present (UC-034), and the database diagnostics, which report the `survey` schema (UC-035).
12. A survey created and exported in Author is applied at a site through Admin.
13. The administrator hands Author to its authors, pointing them at the author's manual (UC-039).

## Alternative Flows

### A1: The site does not author surveys

**Trigger:** The administrator reads the topology chapter and finds the site only applies definitions written elsewhere (step 2)  
**Flow:**

1. The manual states that a site applying definition files needs no Author, and that the platform's installation manual covers that site completely.
2. Use case ends.

### A2: The cluster already exists and was initialized without the `author` database

**Trigger:** The administrator installs into a cluster whose first start predates Author's database script (step 4)  
**Flow:**

1. The manual gives the script to run once by hand against the running cluster. The database image's initialization directory only runs on an empty data directory.
2. Use case continues at step 5.

### A3: Installing with the umbrella's compose file

**Trigger:** The administrator runs Elicit from the umbrella checkout with Docker Compose (step 4)  
**Flow:**

1. The manual states that the compose file already creates the `author` database on a fresh data directory, starts the preview Survey and Author in the right order, and that the umbrella's realm already carries the `elicit-author` client and its roles.
2. The administrator replaces the development secrets the manual names. The compose file's passwords and client secret are for development only (BR-004).
3. Use case continues at step 8.

### A4: A required setting is missing

**Trigger:** Author fails to start, naming a missing setting, or the System overview reports a required setting absent (step 11)  
**Flow:**

1. The manual's troubleshooting chapter names the setting and its environment-variable form.
2. The administrator supplies it and restarts Author.
3. Use case continues at step 9.

### A5: Sign-in succeeds but authoring is refused

**Trigger:** A user signs in and is shown the unauthorized page (step 10)  
**Flow:**

1. The manual sends the administrator to the OIDC diagnostics screen (UC-038), which shows the roles the identity provider actually sent.
2. The administrator grants `elicit_author` on the `elicit-author` client, not on Admin's client.
3. Use case continues at step 10.

### A6: The preview Survey is unreachable

**Trigger:** The preview Survey is not ready, or Author is configured with an address it cannot reach (step 12)  
**Flow:**

1. The manual states the symptom: authoring works, but deleting a survey is refused with the connection failure named (UC-047 A7). The manual gives the address check to run from Author's host.
2. The administrator corrects the preview's address or starts the preview.
3. Use case continues at step 12.

### A7: The service token is missing, too short, or differs

**Trigger:** Author or the preview does not report ready, does not start, or a survey delete is refused naming the service token (step 9)  
**Flow:**

1. The manual states the three symptoms. Without the token an application starts but reports not ready, through a health check named `service-token` whose reason names the variable. With a token shorter than 32 characters it does not start, and its log names the variable and the required length. With different tokens in Author and the preview, deleting a survey that has a reporting schema is refused, naming the token (UC-047 A7).
2. The administrator gives both containers the same token of at least 32 characters and restarts them.
3. Use case continues at step 9.

## Postconditions

### Success Postconditions

- Author is running against its own `author` database and its preview Survey. Authors sign in through the `elicit-author` client. A survey exported from Author has been applied at a site.

### Failure Postconditions

- Nothing at any site is changed by the installation. The only thing that ever crosses from Author to a site is a definition file an administrator applies. A partly installed Author can be removed by dropping the `author` database and the `elicit-author` client.

## Business Rules

### BR-001: The manual is read before Author exists

The installation manual is a release artifact, not part of the application: it is neither in the image nor served by Author, because the reader has no Author yet (C-027). Every procedure in it can be run from the manual alone (NFR-022).

### BR-002: Author's database is never a site's

Author works in a database of its own, named `author` by convention, which may share a cluster with a site but never its `survey` database. Its `survey` schema is built by the preview Survey's migrations, never by Author, which owns no migrations (C-005, C-006).

### BR-003: Two roles, on Author's own client

`elicit_author` grants authoring and `elicit_author_admin` grants the System section (C-021). Both are roles of the `elicit-author` client. Neither is implied by the other or by Admin's `elicit_admin`, so being an administrator of a site grants nothing in Author.

### BR-004: Development secrets are named as such

Every password and client secret the umbrella's compose file and realm carry is identified in the manual as a development value to replace, never as a default to keep.

### BR-005: Content languages follow the release

`author.content.languages` is set by the release to the curated languages and checked against them by the build (C-026). The manual tells the administrator to leave it, and explains that narrowing the languages a site offers is done with `i18n.offered.locales`, not with this setting.

### BR-006: The configuration reference is the code's

Every setting Author reads is in the manual's configuration reference with the default its sources carry, and the manual's build fails on any difference (NFR-021, UC-051 A3).

### BR-007: The images are the only distribution

A release publishes Author and Survey as container images and nothing else; no runnable Java artifact is distributed. The manual therefore gives two installation paths, the umbrella's compose file (A3) and the same images under the site's own container platform, and no procedure that runs either application on a bare Java runtime.

### BR-008: One service token for Author and its preview

Author and the preview Survey receive the same `ELICIT_SERVICE_TOKEN`: Author sends it on its call to the preview, which refuses a request without it (NFR-025). The token is generated for the deployment, at least 32 characters, and supplied like the database passwords. The value the umbrella's compose file carries is a development value (BR-004), and Author warns at startup and on the System overview while it is in use.

## Reference

Traces to FR-070, FR-072..FR-078. The manual is `docs/manual/elicit-author-installation-manual.tex`, built by UC-051. The platform's installation manual (umbrella UC-001 A4) names it for sites that author surveys.
