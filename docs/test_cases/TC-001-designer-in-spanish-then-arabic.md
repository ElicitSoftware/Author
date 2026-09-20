# Test Case: Designer in Spanish then Arabic

## Overview

**ID:** TC-001  
**Goal:** An author whose browser prefers Spanish opens the tool, switches it to Arabic, and adds a question to a survey in the designer with every tool text in the chosen language and the board mirrored right-to-left — verifying language negotiation, switching, layout direction and session persistence end-to-end.  
**Priority:** High  
**Status:** Draft

## Roles

- Survey Author (signs in, chooses the language, edits a survey)

## Preconditions

- An author account exists with the `elicit_author` role (Keycloak realm import `keycloak/elicit-realm.json`).
- Spanish (`es-419`) and Arabic (`ar`) language files are mounted from the Elicit umbrella repository (`elicit-i18n/author/`, reached as `../elicit-i18n` in the test and dev profiles); the tool itself ships English only.
- The browser's preferred language is set to Spanish (Latin America).

## Flow

| Step | Name                      | Description                                                                                                        | Test Data            | Use Case                                                          |
|------|---------------------------|--------------------------------------------------------------------------------------------------------------------|----------------------|-------------------------------------------------------------------|
| 1    | Sign in                   | The author signs in through the identity provider and reaches the survey list                                      | author / author      | [UC-001](../use_cases/UC-001-authenticate-via-oidc.md) |
| 2    | Verify Spanish tool       | The navigation items, header title, grid headers and page title are Spanish and the page is laid out left-to-right | -                    | -                                                                 |
| 3    | Switch to Arabic          | The author chooses Arabic in the header's language selector                                                        | ar                   | [UC-034](../use_cases/UC-034-author-surveys-in-my-language.md)     |
| 4    | Verify Arabic tool        | The same texts are Arabic, the page direction is right-to-left and the document language is Arabic                 | -                    | -                                                                 |
| 5    | Create and open a survey  | The author creates "TC Survey" and opens it in the designer                                                        | TC Survey            | [UC-003](../use_cases/UC-003-create-survey.md)                     |
| 6    | Verify mirrored board     | The palette, lanes and drawer are mirrored right-to-left and their labels are Arabic; survey names stay as entered | -                    | -                                                                 |
| 7    | Add a question            | The author adds a text question to the first section                                                               | ¿Cuál es su nombre?  | [UC-011](../use_cases/UC-011-author-question.md)                      |
| 8    | Verify language kept      | The question dialog, its help texts and the confirmation are Arabic without choosing again; the question text is stored as typed | - | -                                                        |

## Validation

1. **Session language persists**: After step 8, opening any other page in the same browser session shows Arabic; a new browser session falls back to the browser's Spanish preference.
2. **No untranslated text**: No tool text on any visited page is shown as an English fallback or a `!key!` marker.
3. **Content untouched**: The question added in step 7 keeps the text "¿Cuál es su nombre?" and the exported definition carries it unchanged.

## Postconditions

- Survey "TC Survey" holds one additional text question "¿Cuál es su nombre?" in its first section.
- No language information is stored; the question must be removed before the survey if the survey is cleaned up.
