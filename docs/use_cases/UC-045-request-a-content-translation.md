# Use Case: Request a Content Translation

## Overview

**Use Case ID:** UC-045  
**Use Case Name:** Request a Content Translation  
**Primary Actor:** Survey Author  
**Goal:** Produce one file per survey and target language, carrying every string with the context, rules and length budget a translator or an AI agent needs, so that a whole survey can be translated outside the tool and returned in one document.  
**Status:** Draft  
**Requirements:** [FR-058](../requirements.md)

## Preconditions

- The author is authenticated (UC-001) and the survey is open on the Translations page (UC-043).
- The survey declares a base language and the target language (UC-044).

## Main Success Scenario

1. The author chooses "Request translation" for the target language.
2. The author chooses whether to include every string or only the missing and out-of-date ones.
3. The system writes a JSON document naming the survey by its key, the base language, the target language and the moment it was generated, with an instructions preamble, the glossary and one item per string.
4. Each item carries its element key, its field, the base text, a hash of that base text, its status, the context a translator needs to disambiguate it (the step, section and question it sits in, the question's type and, for a select question, its options), the token keys it uses, whether it contains HTML, and its length budget; an out-of-date item also carries the text its existing translation was made from and that translation.
5. The author sends the file to a translator or an AI agent, who fills in the translation of each item and returns the same document.
6. The author imports the returned file (UC-046), and the survey is translated without anyone opening the tool.

## Alternative Flows

### A1: Nothing needs translating

**Trigger:** The author asks for the missing and out-of-date strings of a complete language (step 2)  
**Flow:**

1. The system says the language is complete and writes no file.
2. Use case ends.

### A2: The base text changes while the file is out

**Trigger:** The author edits a question after sending the file (step 5)  
**Flow:**

1. The returned item's hash no longer matches the current base text.
2. The import accepts the item and reports it as out of date, so it is not served until it is translated again (UC-046 A2).
3. Use case ends.

## Postconditions

### Success Postconditions

- A file exists that round-trips by itself: every field the import needs is in it, and the only field the translator fills is the translation.

### Failure Postconditions

- No file is written; the survey is unchanged.

## Business Rules

### BR-001: The file is Author-local

The translation request never reaches the console or the survey application. Translations reach a site only inside the definition file (UC-008).

### BR-002: The instructions travel in the file

The preamble states the rules in the document itself, so that an agent needs nothing but the file: translate the translation field and change nothing else, do not add, remove or reorder items, keep every token key exactly as written while translating the phrase around it, keep HTML tags and translate only the text between them, respect the length budget, keep option labels of one question distinct from each other, and return the same document with the translations filled in.

### BR-003: A token is a key inside a phrase

A placeholder is written `{phrase containing a TOKEN|default text}`. The key is what the survey substitutes an earlier answer into; the phrase and the default around it are prose to be translated. The key must survive, the braces and the bar must survive, and the translation must use exactly the tokens the base text uses.

### BR-004: The length budget is not the column width

The budget in each item allows the target language to run longer than the source, because a translation is stored in a column with no width; what it protects is the space the string is drawn in. A translator is never given a limit the import will not accept.

### BR-005: An empty translation means "not translated"

An item returned with an empty translation is skipped on import and counted, never stored as a blank string.

### BR-006: The glossary is per survey

The terms the survey's own domain needs are maintained with the survey and exported into the preamble, alongside the platform terms that never change (Elicit, survey, step, section, question, access code).
