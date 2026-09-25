# Use Case: Manage Languages

## Overview

**Use Case ID:** UC-041  
**Use Case Name:** Manage Languages  
**Primary Actor:** Survey Author (with the `elicit_author` role)  
**Goal:** Add, replace or remove a language for the authoring tool by uploading a translation file through the tool, so that a site can offer a new language without anyone touching the server.  
**Status:** Draft  
**Requirements:** [FR-055, C-025](../requirements.md)

## Preconditions

- The author is authenticated (UC-001) and holds the `elicit_author` role; users without that role do not see the Languages page.
- The tool reads its translations from the deployment's translations directory (UC-040), which is writable by the tool.
- The tool ships English only; every other language is a file in that directory.

## Main Success Scenario

1. The author opens the Languages page from the navigation.
2. The system lists the languages currently available: English (built in) and every mounted language with its tag, its name in that language, its layout direction, how many texts it translates and when it was last changed.
3. The author downloads the translation request.
4. The author obtains a completed translation file (from a translator or an AI agent) and uploads it, choosing the language tag.
5. The system checks the file: it must be a UTF-8 properties file, every key must exist in the tool's English texts, every placeholder in a text must match the English placeholders, and parameterised texts must not contain a lone apostrophe.
6. The system shows a summary before anything is stored: how many texts are translated, how many are missing (they will fall back to English) and how many are identical to English.
7. The author chooses the layout direction (proposed from the language, right-to-left for Arabic, Hebrew, Persian, Urdu and similar) and confirms.
8. The system stores the file in the tool's sub-directory of the translations directory, records the direction, and reloads the translations.
9. The system lists the new language on the Languages page and offers it in the language selector of every new session, and the author has added a language without server access.

## Alternative Flows

### A1: The file fails validation

**Trigger:** The uploaded file has unknown keys, mismatched placeholders, a lone apostrophe in a parameterised text, or cannot be read (step 5)  
**Flow:**

1. The system lists every problem with its key and stores nothing.
2. The author corrects the file.
3. Use case continues at step 4.

### A2: The language already exists

**Trigger:** A file for the chosen tag is already mounted (step 4)  
**Flow:**

1. The system tells the author that the upload will replace the existing file and shows the summary for the new one.
2. The author confirms.
3. Use case continues at step 8.

### A3: The author removes a language

**Trigger:** The author chooses to remove a mounted language (step 2)  
**Flow:**

1. The system asks for confirmation, naming the language.
2. The system deletes the language file and its direction entry and reloads the translations.
3. Sessions currently using that language fall back to English on their next page.
4. Use case ends.

### A4: The translations directory is not writable

**Trigger:** The tool cannot write to the translations directory (step 8)  
**Flow:**

1. The system explains that the directory must be mounted writable and names the configured path.
2. Use case ends.

### A5: The user lacks the role

**Trigger:** A user without the `elicit_author` role opens the Languages address directly (step 1)  
**Flow:**

1. The system shows the unauthorized page; nothing is listed.
2. Use case ends.

## Postconditions

### Success Postconditions

- A `translations_<tag>.properties` file for the tool exists in the translations directory and the direction is recorded; the language is offered in every new session.
- The upload is recorded with who did it, when and which tag.

### Failure Postconditions

- The translations directory is unchanged; the languages available before the attempt remain available.

## Business Rules

### BR-001: Only the author role manages languages

The Languages page, its navigation entry and every upload or removal require the `elicit_author` role.

### BR-002: English is built in and cannot be removed

The English texts ship with the tool. Uploading a file for English overrides individual texts; it never removes the built-in ones, and English can never be removed from the selector.

### BR-003: A translation file may only use known keys

Every key in an uploaded file must exist in the tool's English texts. Unknown keys are rejected so that typos are noticed instead of silently ignored.

### BR-004: Placeholders are preserved

For every text, the set of `{n}` placeholders must equal the English set, and a text with placeholders must not contain a lone apostrophe.

### BR-005: A partial file is an override, not an error

A file that translates only some texts is accepted; the missing texts fall back to English (UC-040 BR-001). The summary makes the count visible before the file is stored.

### BR-006: The file is the unit of change

An upload replaces the whole file for that tag; there is no per-text editing in the tool.

### BR-007: Direction follows the language unless chosen

The proposed direction comes from the language; the author may override it, and the choice is stored in the translations directory's direction manifest.

### BR-008: The tool manages only its own languages

The tool writes only into its own `author` sub-directory of the translations directory; the survey application's and the console's languages are managed from the admin console (Admin UC-021).

### BR-009: Every change is attributed

Each upload and removal is logged with the author, the time and the tag.
