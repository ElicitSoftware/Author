# Translation request: Elicit Author

This document is generated from the application's English text file and is meant to be handed,
as is, to a translator or to an AI translation agent. It contains everything needed to produce a
complete language file for one target language.

## About the application

Elicit Author is the survey-authoring tool of the Elicit survey platform. Survey authors
(researchers, educators, analysts) use it to build questionnaires: they arrange steps, sections
and questions on a visual designer board, define answer options, draw branching rules between
questions, tag questions for reporting, and export the finished definition as a file that the
Admin console installs. The texts below are the tool's own words: navigation, buttons, menus,
grid headers, dialog labels and help texts, notifications, and the built-in guide that explains
the survey model. The survey content authors write is not included and stays as entered.

**Audience and tone:** professional users who design surveys; some pages (the guide, the rule
editor) explain concepts in plain prose. Use clear, precise, gender-neutral language and the
formal register where the language distinguishes one (for example *usted* in Spanish). Keep
button and menu labels short.

## Glossary and words to keep

| Term | Meaning | Rule |
|------|---------|------|
| Elicit | Product name | Never translate or transliterate |
| survey, step, section, question | The structural levels of a survey (a survey has steps, a step has sections, a section has questions) | Translate consistently; the same word everywhere |
| element | Any of step, section or question | Translate consistently |
| rule, relationship | A condition on one answer that shows, hides or repeats a downstream element | Translate consistently |
| token | A placeholder inside question text such as `{NAME|default}` | Translate as "placeholder" if clearer; never as a credential |
| access code | The credential a respondent types (issued by the Admin console) | Never call it a token |
| export | Producing the definition file for the Admin console | Use "export", never "publish" |
| retire / restore | Removing an element from the current definition (kept in history) and bringing it back | Translate consistently |
| ontology, dimension, tag, reporting tag | Reporting metadata attached to questions | Translate consistently |
| PDF, CSV, JSON, HTTP, OIDC, ID, URL, `.elicit` | Technical names | Keep as written |

## Rules for the translation

1. Keep every placeholder such as `{0}`, `{1}` exactly as written; move it inside the sentence
   where the language needs it, but never remove, rename or reorder it with another placeholder.
2. Where a row is flagged **params**, the value is processed by Java MessageFormat: any apostrophe
   in your translation must be doubled (`l''accès`). Rows without the flag may use a single
   apostrophe normally.
3. Where a row is flagged **html**, keep the HTML tags and translate only the text between them.
4. Respect the *Max length* column where given; these strings sit in buttons, menus and grid headers.
5. Do not translate the keys (the first column). Do not add, remove or reorder keys.
6. For right-to-left languages, write the text naturally; the tool mirrors the layout.
7. Return exactly one file named `translations_<tag>.properties` (for example
   `translations_es_419.properties`, `translations_ar.properties`), UTF-8 encoded, with the same
   keys in the same order as the English file at the end of this document, one `key=translation`
   per line, and a first line `# Reviewed by <name or agent>, <date>`.

## Brand strings

Deployments mount their own brand. The organization name shown in the header comes from the
brand's own files and is translated there, not in this file, through a `localized` block keyed
by language tag in `brand-config.json` (`name`, `organization`) and `brand-info.json`
(`description`); the base `name` also derives a technical identifier and must stay as it is.
