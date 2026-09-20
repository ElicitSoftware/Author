# Use Case: Author Surveys in My Language

## Overview

**Use Case ID:** UC-034  
**Use Case Name:** Author Surveys in My Language  
**Primary Actor:** Survey Author  
**Goal:** See every page of the authoring tool — navigation, buttons, menus, grid headers, dialogs, messages, page titles, the survey-model guide and the organization name — in a language they read, laid out right-to-left when that language requires it, so that they can design and export surveys without understanding English.  
**Status:** Implemented  
**Requirements:** [FR-042, FR-043, FR-044, FR-045, FR-046, NFR-014, NFR-015, C-020, C-021](../requirements.md)

## Preconditions

- The author is authenticated and has reached the authoring tool (UC-001).
- The tool ships English texts and at least one additional language (Latin American Spanish and Arabic in the reference deployment).
- The deployment may have mounted further language files or text overrides; if so, those languages are also available.

## Main Success Scenario

1. The author opens the tool.
2. The system determines the language to use: the language chosen earlier in this browser session, otherwise the browser's preferred language, otherwise English.
3. The system displays the page with every tool text in that language, and with the page — including the designer board and its lanes — laid out right-to-left when the language is written right-to-left.
4. The author chooses a different language from the language selector shown in the header of every page.
5. The system redisplays the current page in the chosen language, applies the matching layout direction, and remembers the choice for the rest of the browser session.
6. The author works through the tool — the survey list, the designer, dialogs for questions, sections and rules, reporting tags, import and export.
7. The system presents every page in the chosen language while the survey content the author writes (question text, options, step and section names, tags) is stored and exported exactly as entered, and the author completes their work in their language.

## Alternative Flows

### A1: A text is missing in the chosen language

**Trigger:** A language file lacks the translation for one of the texts on the page (step 3)  
**Flow:**

1. The system shows the English text for that item and the chosen language for everything else.
2. Use case continues at step 4.

### A2: Deployment mounted an additional language

**Trigger:** The system administrator has mounted a language file the tool does not ship (step 4)  
**Flow:**

1. The system lists the mounted language in the language selector alongside the shipped ones.
2. The author chooses the mounted language.
3. Use case continues at step 5.

### A3: Deployment overrides individual texts

**Trigger:** The system administrator has mounted a language file containing only some texts for a shipped language (step 3)  
**Flow:**

1. The system shows the mounted text for the overridden items and the shipped text for all others.
2. Use case continues at step 4.

### A4: The sign-in page is shown by the identity provider

**Trigger:** The author is not yet authenticated when opening the tool (step 1)  
**Flow:**

1. The identity provider presents its own sign-in page in its own language.
2. After sign-in, use case continues at step 2.

## Postconditions

### Success Postconditions

- The chosen language is remembered for the browser session and applied to every page until the author logs out or closes the browser.
- Authored survey content and exported definition files are unaffected by the language choice; only the tool's own texts and layout direction differ.

### Failure Postconditions

- If no language can be determined, the tool is shown in English; the author can still do everything.

## Business Rules

### BR-001: English is the fallback language

English is the default language and the fallback for any text missing from another language file. A text missing from every language file is shown as a visible marker (`!key!`) rather than blank, so the omission is noticed and fixed.

### BR-002: Available languages are shipped plus mounted

The languages offered are the union of the languages shipped with the tool and the language files present in the deployment's mounted translations directory. A mounted file for a shipped language overrides only the texts it contains.

### BR-003: Language precedence

Session choice, then browser preference, then English. A language that is not available is skipped, never partially applied.

### BR-004: Layout direction follows the language

Languages written right-to-left (Arabic, Hebrew, Persian, Urdu and similar) mirror the whole page, including the designer board, its lanes and the drawer; all other languages are laid out left-to-right. A deployment may declare the direction of a mounted language explicitly.

### BR-005: Authored content keeps its entered language

Question text, options, step and section names, tags and the exported definition file are stored and exported as entered, whatever language the tool chrome uses.

### BR-006: Brand names are localized by the brand

The organization name supplied by the mounted brand may carry per-language variants; the tool shows the variant for the current language and falls back to the brand's base text. The brand's technical identifiers are never translated.

### BR-007: The language choice never crosses sessions

The choice is held for the browser session only; nothing about the author's language is stored.
