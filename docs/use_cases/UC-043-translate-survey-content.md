# Use Case: Translate Survey Content

## Overview

**Use Case ID:** UC-043  
**Use Case Name:** Translate Survey Content  
**Primary Actor:** Survey Author  
**Goal:** Review and correct a survey's content in one published language, string by string, seeing for every string whether it is translated, missing or out of date, so that respondents at every site read the survey in their own language.  
**Status:** Implemented  
**Requirements:** [FR-056, FR-060, C-024](../requirements.md)

## Preconditions

- The author is authenticated (UC-001) and the survey is open in the tool.
- The survey declares a base language. It need not yet publish any content language: one can be added from this page (UC-044).
- The survey's structure exists; there is nothing to translate before there are questions.

## Main Success Scenario

1. The author opens the Translations page for the survey, from the survey editor or the designer menu.
2. The author chooses a language: one the survey already publishes, one of the common languages the page offers, or a tag typed in. Choosing one the survey does not publish yet adds it to the published set (UC-044) before the strings are listed. The survey's base language is never offered, because every string already falls back to it.
3. The system lists every translatable string of the survey's current structure: the element it belongs to (step, section and question, or list and option), the field, the base text, the translation in the chosen language, and the status — translated, missing, or out of date because the base text was edited after the translation was made.
4. The system shows the counts for the language (translated, missing, out of date) and offers filters for the missing and the out-of-date rows.
5. The author edits a translation in place.
6. The system stores it against the element's key and the field, records the base text it was translated from and a hash of that text, and moves the row to translated.
7. The author works through the missing and out-of-date rows, and the survey is ready to export with that language complete.

## Alternative Flows

### A1: The author edits the base text instead

**Trigger:** The author changes the wording of a question, option, step or section that already has translations (step 5, or in any editor dialog)  
**Flow:**

1. The system saves the change and tells the author, in a notification that does not dismiss itself, which languages that wording change invalidated and how many strings are affected, with a link to this page filtered to that element.
2. The affected rows show as out of date, and the runtime shows the base text for them until they are translated again (Survey UC-009 BR-008).
3. Use case continues at step 5.

### A2: The author removes a translation

**Trigger:** The author decides one string should fall back to the base language in this language (step 5)  
**Flow:**

1. The system closes that translation rather than storing an empty value; the row returns to missing.
2. The closure travels in the definition file, and each site closes its own row.
3. Use case continues at step 5.

### A3: An element is retired while it has translations

**Trigger:** The author retires a question, section, step, select item, relationship or report (UC-009)  
**Flow:**

1. The system closes every current translation of that element at the same instant, in the same transaction.
2. The strings disappear from this page; restoring the element (UC-010) reopens them.
3. Use case ends.

### A4: The survey publishes no languages yet

**Trigger:** The author opens the page for a survey whose content languages are empty (step 2)  
**Flow:**

1. The system offers the languages the survey could be translated into and selects none, explaining that choosing one starts a translation.
2. The author chooses one; the system adds it to the published set (UC-044) and reports how many strings it leaves untranslated.
3. Use case continues at step 3.

### A5: The author types a tag that is not a language

**Trigger:** The author types a language tag the offered list does not carry (step 2)  
**Flow:**

1. The system checks the tag and, if it is not well-formed BCP-47 or names no language, refuses it with a message and keeps the language that was selected before.
2. A tag the survey already publishes is simply selected rather than added twice.
3. Use case continues at step 2.

## Postconditions

### Success Postconditions

- Every edited string has one current translation row for that survey, element, field and language, carrying the base text it was translated from and its hash.
- The counts on the page and the warnings shown at export (UC-007) reflect what is missing and what is out of date.

### Failure Postconditions

- A string that could not be saved keeps its previous translation; nothing is stored half-written.

## Business Rules

### BR-001: A translation is attached by element key, never by id

A translation names the element it belongs to by that element's `*_key` UUID and the column it translates. Surrogate ids change on every version and are reallocated on every import; the key is the identity that survives both.

### BR-002: Only whitelisted fields are translatable

The translatable fields are the respondent-facing ones: a survey's title and description; a step's and section's name and description; a question's text, short text, tooltip, placeholder and validation text; a select item's display text; a relationship's default upstream value; a report service's name and description (UC-048 BR-003); a report mark's label (UC-049 BR-006). Identifiers, coded values, dimension names, tags, URLs, masks and a question's default value and sample are never translated, because they are read by machines or analyzed as data.

### BR-003: Editing base text never edits its translations

A rewrite of a question or a rename of a step leaves the translation rows untouched and current; what changes is that they no longer match the base text they were made from, so they are out of date. Retirement is the opposite: it applies to every language at once (A3).

### BR-004: Out of date is decided by the base text, not by a date

A translation carries a hash of the base text it was made from. It is out of date when that hash no longer matches the element's current base text, however long ago either was written.

### BR-005: A language is offered by name, and accepted only if it is a language

The picker shows a tag beside the language's name, because the name is what an author reads and the tag is what a site's chrome mount has to match (UC-044 BR-004). It offers the languages the survey publishes first, then the most used languages of the web, and a tag outside that list is typed in. A typed tag is accepted only when BCP-47 can parse it and it names a real language, and it is stored canonically, so `ES-419` and `es-419` are one language rather than two.

### BR-006: The Translations page is the only surface that shows a translation

Every other view — the designer, the structure tree, the editor dialogs, the previews and the element details page — shows the survey's base language, so that an author always sees the text they are editing. Showing a content language in those surfaces as well is proposed in `docs/research/i18n_survey.md` and is not adopted here; if it is, this rule is amended rather than contradicted, because that proposal also leaves the base text where it is stored.

### BR-007: Removal is a closed row, never an empty value

A translation is removed by closing it, as any structural element is. There is no empty current translation, so a site can tell "no translation" from "translated to nothing".

### BR-008: One working copy, one version

Author holds one version-0 row per translation, as it does for every structural element; versions are created at each site by the console when a changed file is applied (Admin UC-017).
