# Use Case: Build the Author Installation Manual

## Overview

**Use Case ID:** UC-051  
**Use Case Name:** Build the Author Installation Manual  
**Primary Actor:** Release Engineer  
**Goal:** Typeset the Author installation manual from its LaTeX sources, check its configuration reference against Author's code, stamp it with Author's version and a build date, and publish it beside each tagged release.  
**Status:** Draft  
**Requirements:** [FR-071, NFR-020, NFR-021, NFR-022, C-027, C-028](../requirements.md)

## Preconditions

- Author is checked out inside the Elicit umbrella checkout, which carries the shared brand package (`../docs/manual/elicit-brand.sty`).
- A container runtime is available and running.

## Main Success Scenario

1. The release engineer runs the manual build for the installation manual, optionally giving the version and the build date. Alternatively, a tagged release runs it.
2. The system checks the configuration reference: every configuration key Author reads is documented in the manual, and every documented default equals the value in `application.properties` (BR-003).
3. The system determines the version: the argument if given, otherwise the version in `pom.xml`, otherwise "unknown". It determines the build date: the argument if given, otherwise the current UTC time.
4. The system writes the stamp to a generated file the brand package reads, so a hand-run build and a release build use the same sources.
5. The system typesets the manual with LuaLaTeX through `latexmk` inside a TeX Live container, with the umbrella's brand package (BR-002).
6. The system writes the PDF beside the sources, where it is not packaged into the image, and reports where it landed.
7. For a tagged release, the system attaches the PDF to that release, next to the image of the same version (BR-004).

## Alternative Flows

### A1: No container runtime

**Trigger:** No container runtime is available (step 5)  
**Flow:**

1. The system says so, names the variable that skips the build, and exits non-zero.
2. Use case ends.

### A2: The build is skipped deliberately

**Trigger:** The release engineer sets the skip variable (step 1)  
**Flow:**

1. The system says the installation manual is not being built and exits zero, so a containing build is not failed.
2. Use case ends.

### A3: The configuration reference has drifted from the code

**Trigger:** The check finds a key Author reads that the manual does not document, a documented key Author no longer reads, or a default that disagrees (step 2)  
**Flow:**

1. The system lists each difference and exits non-zero. Nothing is typeset.
2. The release engineer corrects the manual.
3. Use case continues at step 1.

### A4: The umbrella checkout is absent

**Trigger:** The shared brand package is not at `../docs/manual/` (step 5)  
**Flow:**

1. The system says Author must sit inside the umbrella checkout and exits non-zero. No PDF is written.
2. Use case ends.

### A5: The sources do not typeset

**Trigger:** LaTeX reports an error (step 5)  
**Flow:**

1. The system halts on the first error with the file and line, and exits non-zero. A previously built PDF is left untouched.
2. The release engineer fixes the source.
3. Use case continues at step 1.

### A6: A tagged release cannot typeset the manual

**Trigger:** Step 2 or step 5 fails during a tagged release (step 7)  
**Flow:**

1. The release fails. A release is never published without its installation manual.
2. Use case ends.

## Postconditions

### Success Postconditions

- A PDF exists carrying Author's version and the build date on its title page and in every page footer. For a tagged release, it is attached to that release.

### Failure Postconditions

- No PDF is written, or a previously built PDF is left untouched. The generated stamp file is regenerated on the next build and is not committed.

## Business Rules

### BR-001: Unknown is written, not omitted

A build that supplies neither version nor date renders both as "unknown". The fields are never left blank (NFR-020).

### BR-002: One brand package for every manual

The installation manual is typeset with the umbrella's `docs/manual/elicit-brand.sty`, the package the platform's installation manual uses, read in place rather than copied (C-028). It is typeset in the Elicit default brand regardless of which brand any deployment mounts. Its command and SQL listings use the package's copy-safe verbatim environments (NFR-022).

### BR-003: Drift from the code fails the build

Every key Author reads — through `@ConfigProperty`, a programmatic lookup, or the required-settings check behind the System overview (UC-034) — must appear in the configuration reference. Every default the manual prints must be the value in `application.properties`, and a default cell that states no value claims nothing. The check runs before typesetting, so a stale reference is never produced (NFR-021).

### BR-004: The manual is published, not packaged

The installation manual is attached to the release it describes. It is never packaged into the image and never served by the application, unlike the author's manual (UC-039 BR-004), because it is read before any Author service exists (C-027).

## Reference

Traces to FR-071. Modelled on the platform manual's build (umbrella UC-002) and on the author's manual build (`docs/manual/build-manual.sh`, UC-039).
