# Use Case: Manage Content Languages

## Overview

**Use Case ID:** UC-044  
**Use Case Name:** Manage Content Languages  
**Primary Actor:** Survey Author  
**Goal:** Declare the language a survey's content is written in and the languages it is published in, so that a finished language reaches every site and a half-finished one is held back.  
**Status:** Implemented  
**Requirements:** [FR-057, C-024](../requirements.md)

## Preconditions

- The author is authenticated (UC-001) and the survey is open in the tool.
- The survey has a base language; a survey created before this feature has `en`.

## Main Success Scenario

1. The author opens the survey details form (UC-004) to see or change the base language, or the Translations page (UC-043) to manage the published set.
2. The survey details form shows the base language, the language the survey's content is written in, beside the survey's name, title and description. It is not asked for when a survey is created, which is written in `en` (UC-003 BR-006).
3. The Translations page shows the content languages, the set the survey is published in, in its language picker, which also offers the curated languages the survey is not published in. Each is named rather than only tagged. This is the only place the set is managed (BR-007).
4. The author adds a language to the published set by choosing it, or typing its tag, on the Translations page.
5. The system records it with the survey and shows its outstanding strings (UC-043), reporting how many the new language leaves untranslated.
6. The author translates the survey (UC-043, UC-045, UC-046) and exports it (UC-008); every site that mounts that language for its own chrome now serves the survey in it.

## Alternative Flows

### A1: The author publishes an incomplete language

**Trigger:** The author adds a language with missing or out-of-date strings to the published set (step 4)  
**Flow:**

1. The system accepts it and warns how many strings will fall back to the base language at every site. The warning is repeated at export (UC-008).
2. Use case continues at step 5.

### A2: The author withdraws a language

**Trigger:** The author chooses Withdraw for a published language on the Translations page (step 3)  
**Flow:**

1. The system explains that the translations are kept, not deleted, and that no site will serve them until the language is published again.
2. The author confirms; the language leaves the published set at once, is offered again as not published, and leaves the sites at the next export. If the author declines, nothing changes.
3. Use case ends.

### A3: The author changes the base language

**Trigger:** The author saves the survey details with a different base language (step 2, UC-004 A4)  
**Flow:**

1. Before anything is stored, the system warns that every existing translation was made from text in the old base language and that changing it does not translate the content.
2. The author confirms; the base language changes and every translation of the new base language's own tag, if any, is redundant and reported. If the author declines, the details form stays open and nothing is stored.
3. Use case ends.

## Postconditions

### Success Postconditions

- The survey records its base language and its published set; both travel in the definition file and are applied at every site.

### Failure Postconditions

- A rejected tag leaves both settings unchanged.

## Business Rules

### BR-001: The published set is the author's decision, not a count

A language is published because the author says so, not because translations exist for it. A language may be fully translated and unpublished while work is reviewed, or published and incomplete with the missing strings falling back.

The per-language counts live on the Translations page, where the translating is done, and are reported again when a language is added (A1) and at export. The published set is decided there too, beside them (BR-007).

### BR-002: A site serves a language only when its own chrome has it

The published set says what the survey offers; what a respondent is offered at a given site is the intersection of that set with the languages mounted for the application's own texts there (Survey UC-009 BR-009). A site that has not adopted a language holds its rows without serving them, so one file suits every site.

### BR-003: Every site gets the same file

The definition file carries every language the survey publishes. There is no per-site export: one revision is one file, distributed to every site, and a site's own mount decides what its respondents see.

### BR-004: Only a language Elicit carries may be published

A content language must be one of the languages the applications themselves are translated into, named by `author.content.languages` and curated by ElicitSoftware. The reason is BR-002: a language the chrome does not have is never served, so content published in one would reach no respondent at any site. Offering the author a free choice invites work that is accepted, exported, distributed and then silently never shown.

A language outside that set is refused wherever it is chosen or typed, with the route to getting one: ElicitSoftware translates the applications into it and adds it to the curated translations (the umbrella's `elicit-i18n/`), which sites mount. An author cannot get ahead of the curated set, which is the cost of never producing unshowable work.

One exception, and it is about removal rather than authoring: a survey that already publishes a language Elicit does not carry — from an imported definition, or from before the set was curated — keeps showing it, marked, so that an author can see what the survey publishes and take it back out. Saving such a survey is allowed; adding such a language is not.

A tag is written in the same form the chrome bundles use (`es-419`, `ar`), so the two sets can be intersected without guessing.

### BR-005: The base language is the fallback for every string

Whatever a survey is published in, every untranslated and every out-of-date string is shown in the base language (Survey UC-009 BR-005). A survey is therefore never partly unreadable.

### BR-006: A tag is validated and stored canonically

A tag is accepted only when BCP-47 can parse it and its primary subtag names a real language: `zzz` is well-formed and means nothing. What is stored is the canonical form, so `ES-419` and `es-419` are one language rather than two, and the published set can be compared with a site's own languages by equality.

The underscore spelling is read rather than refused: `es_419` is not BCP-47, but it is how the translation bundle files are named, so it is the form an author is most likely to have in front of them. It is normalized on the way in, not stored.

The base language is refused as a target wherever it is offered or typed, however it was itself written.

### BR-007: One place manages the published set

The published set is added to and withdrawn from on the Translations page only. Neither the form that creates a survey nor the one that edits its details shows it: a language chosen there is chosen without the counts that say what publishing it would mean, and a second control for one setting is a second place for it to be changed by mistake. Saving the survey details leaves the set as stored.
