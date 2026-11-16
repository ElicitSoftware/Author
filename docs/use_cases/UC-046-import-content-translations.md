# Use Case: Import Content Translations

## Overview

**Use Case ID:** UC-046  
**Use Case Name:** Import Content Translations  
**Primary Actor:** Survey Author  
**Goal:** Load a returned translation file in one step, with every item checked before it is stored, so that a good file lands complete and a bad item is reported by name instead of failing the file.  
**Status:** Implemented  
**Requirements:** [FR-059](../requirements.md)

## Preconditions

- The author is authenticated (UC-001) and the survey is open on the Translations page (UC-043).
- The author holds a file produced by UC-045 for this survey, with translations filled in.

## Main Success Scenario

1. The author uploads the returned file.
2. The system checks the envelope: the format marker, the survey key against the open survey, and the language against the survey's content languages.
3. The system checks each item: that the element exists and is not retired, that the field is translatable for that element, that the translation uses exactly the tokens the current base text uses, that its braces and bars are balanced, that it is within the item's length budget, and that two options of one question have not been translated to the same text.
4. The system stores every accepted item against its element key and field, recording the current base text and its hash, in one transaction.
5. The system reports the counts — imported, unchanged, skipped as empty, imported but out of date, rejected — and lists every rejected item with its reason.
6. The grid refreshes, and the author sees the language's remaining gaps.

## Alternative Flows

### A1: Some items are rejected

**Trigger:** An item fails one of the checks in step 3  
**Flow:**

1. The system keeps the rest of the file and stores it.
2. The rejected items are listed with their ids and reasons; nothing about them is stored.
3. The author has the file corrected and repeats the use case; re-importing an unchanged item is counted as unchanged.

### A2: The base text moved while the file was out

**Trigger:** An item's hash does not match the element's current base text (step 3)  
**Flow:**

1. The system stores the translation and reports it as "base text changed since export".
2. The row is out of date, so the runtime shows the base text until it is translated again (Survey UC-009 BR-008).
3. Use case continues at step 5.

### A3: The file is for another survey or another language

**Trigger:** The survey key does not match the open survey, or the language is not one the survey publishes (step 2)  
**Flow:**

1. For the wrong survey, the system rejects the file and stores nothing.
2. For an unpublished language, the system offers to publish it (UC-044) and continues at step 3 on confirmation.
3. Use case ends.

### A4: The file is not the document that was sent

**Trigger:** The upload is not the expected format, is not valid JSON, is larger than the accepted size, or has had items added, removed or reordered (step 2)  
**Flow:**

1. The system rejects the file, naming what is wrong, and stores nothing. The accepted size is 5 MB: the upload refuses a larger file before sending it, and the server checks the size again on what arrives (NFR-023).
2. Use case ends.

## Postconditions

### Success Postconditions

- Every accepted item has one current translation row carrying the base text it was checked against and that text's hash.
- The result is reported in enough detail to act on: what landed, what was skipped, what was rejected and why.

### Failure Postconditions

- A rejected envelope stores nothing at all; a rejected item leaves that string as it was.

## Business Rules

### BR-001: One broken item never fails the file

A translator's file of eight hundred strings is not refused for one unbalanced brace. Acceptance is per item; the accepted set is stored as one transaction.

### BR-002: The hash is recomputed from the current base text

What is stored with a translation is the base text as it stands at import, not the text the file was generated from. An item made from unchanged text therefore lands current and is served; one made from since-changed text lands out of date and is not.

### BR-003: Token sets must match exactly

A translation that drops, adds or renames a token is rejected, because the survey substitutes an answer into it at runtime and a missing key silently loses that substitution.

### BR-004: Option labels stay distinguishable

Two options of the same question translated to the same text are both rejected; a respondent must be able to tell the answers apart.

### BR-005: HTML is warned, not rejected

An item whose tags do not match the source's is imported with a warning, because a translator may legitimately move emphasis, and a broken fragment is visible in the preview rather than dangerous.

### BR-006: A retired element is never translated

An item naming an element that has been retired since the file was generated is rejected; its translations were closed with it (UC-043 A3).
