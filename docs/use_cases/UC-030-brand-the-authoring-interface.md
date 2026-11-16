# Use Case: Brand the Authoring Interface

## Overview

**Use Case ID:** UC-030  
**Use Case Name:** Brand the Authoring Interface  
**Primary Actor:** System Administrator  
**Goal:** Have the deployed Author interface carry the site's brand — logo, display name and theme — resolved from a mounted brand directory with a sensible default when none is present, so that Author looks like the rest of the platform at a site and never serves a file from outside the brand directory.  
**Status:** Implemented  
**Requirements:** [FR-037, NFR-010](../requirements.md)

## Preconditions

- Author is deployed and configured with a brand mount location and a local brand directory location.

## Main Success Scenario

1. The administrator mounts a brand directory for the Author deployment, holding a brand configuration and its assets (logo, theme styling).
2. Author starts and detects the active brand, reading its key, display name and logo from the mounted configuration.
3. The system applies the brand to the application shell so every authoring page shows the site's theme and logo.
4. A page requests a brand asset by name, and the system serves it from the mounted brand directory with a caching header.
5. The author works throughout the application and sees the site's brand.

## Alternative Flows

### A1: No brand mounted

**Trigger:** Neither the external mount nor the local brand directory exists (step 2)  
**Flow:**

1. The system falls back to the brand embedded in the application (display name "Elicit Software").
2. Use case continues at step 3 with the embedded brand.

### A2: Brand configuration is malformed

**Trigger:** The mounted brand configuration cannot be read as valid configuration (step 2)  
**Flow:**

1. The system falls back to the embedded brand rather than leaving the interface unbranded.
2. Use case continues at step 3 with the embedded brand.

### A3: Requested asset absent from the mount

**Trigger:** A requested brand asset is not present on the external mount (step 4)  
**Flow:**

1. The system serves the asset from the brand resources embedded in the application.
2. Use case continues at step 5.

### A4: Requested path escapes the brand root

**Trigger:** A requested brand-file path contains a parent-directory segment or an empty segment (step 4)  
**Flow:**

1. The system rejects the request as a bad request and serves nothing.
2. Use case ends.

## Postconditions

### Success Postconditions

- Every authoring page carries the resolved brand.
- Requested brand assets are served only from within the brand root.

### Failure Postconditions

- A request for a path outside the brand root is rejected and no file is served; the interface stays on its resolved brand.

## Business Rules

### BR-001: Fixed brand-resolution order

The active brand is resolved in a fixed order — the external mount first, then a local brand directory, then the brand embedded in the application — and the first source that resolves wins.

### BR-002: The interface is never left unbranded

A missing or malformed brand configuration falls back to the embedded default brand, so the application always presents a complete, valid brand.

### BR-003: Brand files are served only from within the brand root

A requested brand-file path that contains a parent-directory segment or an empty segment is rejected, so no file outside the brand directory can ever be served.

### BR-004: An SVG brand asset is served as an image only

A brand asset whose name ends in `.svg` is served with `Content-Security-Policy: sandbox`. Used as an image — the logo in the application shell — it renders as before; opened as a page of its own it runs no script. Every brand asset also carries the headers every response carries (NFR-024).

### BR-005: A brand's stylesheets name the brand's own files

A relative `url()` in a brand stylesheet names a file of the brand, relative to that stylesheet: `url("fonts/Face.woff2")` in `typography/brand-typography.css` is `typography/fonts/Face.woff2`. The stylesheets are inlined into the page, so the system resolves each such URL to the address the file is served from before the page is sent. That is how a brand carries its own fonts, and the embedded default brand carries IBM Plex Sans and Mono this way, so no font is requested from a third party. A URL that is absolute, or that would leave the brand root, is left as written.
