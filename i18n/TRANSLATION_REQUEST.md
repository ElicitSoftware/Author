# Translation request: Elicit Author

This document is generated from the application's English text file and is meant to be handed,
as is, to a translator or to an AI translation agent. It contains everything needed to produce a
complete language file for one target language. A finished translation is placed in this
repository's `i18n/` directory, alongside this document, and is packaged into the application at
build time, so it ships with the release and travels with the version of the code it was made
against.

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
| upstream, downstream | The direction a survey flows: a rule's upstream question is the one whose answer it reads, its downstream target the step, section or question it acts on, which comes later | Translate consistently; the same pair of words in every rule message |
| token | A placeholder inside question text such as `{<NAME>|default}`; the key is the part in angle brackets | Translate as "placeholder" if clearer; never as a credential |
| access code | The credential a respondent types (issued by the Admin console) | Never call it a token |
| export | Producing the definition file for the Admin console | Use "export", never "publish" |
| retire / restore | Removing an element from the current definition (kept in history) and bringing it back | Translate consistently |
| ontology, dimension, tag, reporting tag | Reporting metadata attached to questions | Translate consistently |
| content language | A language a survey's own questions and options are published in, as opposed to the language of the tool itself | Translate consistently; keep it distinct from the tool's language |
| base language | The language a survey's content is written in, and the one every untranslated string falls back to | Translate consistently |
| out of date (translation) | A translation whose original wording has changed since it was made, so it is no longer shown | Translate consistently; not "expired" or "invalid" |
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

## Strings to translate

Every row is one key in `translations.properties`. Return a file `translations_<tag>.properties` with exactly these keys in this order, one `key=translation` per line, UTF-8, no additions and no omissions.

| Key | English | Where it appears | Max length | Notes |
|-----|---------|------------------|------------|-------|
| `common.language` | Language | Header · language selector accessible name | 20 |  |
| `common.logoAlt` | {0} logo | Header · logo image alternative text | 40 | params: {0} = organization name |
| `common.appTitle` | {0} {1} | Header · application title | 40 | params: {0} = organization name, {1} = application type; identical in most languages (reorder only) |
| `common.appTitle.default` | Elicit {0} | Header · application title when no brand is mounted | 40 | params: {0} = application type; identical in most languages, Elicit stays |
| `common.appType.author` | Author | Header · the word appended to the organization name | 20 |  |
| `common.save` | Save | Any dialog · button | 15 |  |
| `common.cancel` | Cancel | Any dialog · button | 15 |  |
| `common.close` | Close | Any dialog · button | 15 |  |
| `common.remove` | Remove | Element menus · menu item that retires an element | 15 |  |
| `common.restore` | Restore | Element menus · menu item that restores a retired element | 15 |  |
| `common.ok` | OK | Dialogs · button | 10 | identical |
| `mainLayout.nav.surveys` | Surveys | Navigation drawer · menu item | 20 |  |
| `mainLayout.nav.guide` | How surveys work | Navigation drawer · menu item linking to the survey-model guide | 25 |  |
| `mainLayout.nav.manual` | Manual (PDF) | Navigation drawer · menu item opening the author's manual PDF in a new tab; PDF stays | 25 | identical |
| `mainLayout.header.manual` | Manual | Header · link opening the author's manual PDF in a new tab | 15 | identical |
| `mainLayout.header.manualTitle` | Open the author's manual (PDF) in a new tab | Header · tooltip of the manual link; PDF stays | 60 |  |
| `mainLayout.nav.aiDraft` | Draft with AI | Navigation drawer · menu item | 25 |  |
| `mainLayout.nav.importDefinition` | Import definition | Navigation drawer · menu item | 25 |  |
| `mainLayout.nav.logout` | Logout | Navigation drawer · menu item | 20 |  |
| `loginView.redirecting` | Redirecting to sign-in… | Sign-in redirect page (rarely visible) · paragraph | 40 |  |
| `logoutView.signingOut` | Signing out… | Sign-out redirect page (rarely visible) · paragraph | 40 |  |
| `unauthorizedView.title` | Access Restricted | Access restricted page · heading | 30 |  |
| `unauthorizedView.message` | You are authenticated but do not have the required permissions to access this application. Please contact your administrator to request the 'elicit_author' role for the Author application. | Access restricted page · paragraph; 'elicit_author' is a technical role name, keep it | 250 |  |
| `unauthorizedView.logout` | Logout | Access restricted page · button | 15 |  |
| `structureNavigationTree.showRemoved` | Show removed | Drawer structure tree · checkbox label | 20 |  |
| `structureNavigationTree.header` | Survey structure | Drawer structure tree · tree column header | 25 |  |
| `surveysView.pageTitle` | Surveys | Surveys list · browser tab title | 20 |  |
| `surveysView.title` | Surveys | Surveys list · heading | 20 |  |
| `surveysView.newSurvey` | New survey | Surveys list · primary button | 20 |  |
| `surveysView.aiDraft` | Draft with AI | Surveys list · secondary button beside New survey | 20 |  |
| `surveysView.aiDraftTitle` | Have an AI agent write a first draft of a survey, then import it here | Surveys list · tooltip on that button | 90 |  |
| `surveysView.grid.name` | Name | Surveys list · grid column header | 15 |  |
| `surveysView.grid.title` | Title | Surveys list · grid column header | 15 |  |
| `surveysView.grid.order` | Order | Surveys list · grid column header (display order) | 15 |  |
| `surveysView.grid.lastEditedBy` | Last edited by | Surveys list · grid column header | 20 |  |
| `surveysView.grid.releaseNote` | Release note | Surveys list · grid column header | 20 |  |
| `surveysView.open` | Open | Surveys list · row button opening the survey overview | 12 |  |
| `surveysView.editDetails` | Edit details | Surveys list · row button opening the metadata dialog | 15 |  |
| `surveysView.delete` | Delete | Surveys list · small button in each row, opens the delete confirmation | 15 |  |
| `deleteSurveyDialog.title` | Delete {0}? | Delete survey dialog · dialog title | 60 | params: {0} = survey name |
| `deleteSurveyDialog.permanent` | This permanently deletes the survey and everything in it: steps, sections, questions, choice lists, rules, analysis tags, reports, report marks and translations, removed elements included. It cannot be undone. | Delete survey dialog · paragraph | 250 |  |
| `deleteSurveyDialog.previewAnswers` | Preview respondents recorded for this survey in Author: {0}. They and their answers are deleted with it. | Delete survey dialog · paragraph shown only when previewing the survey recorded respondents; Author is the product name | 160 | params: {0} = number of respondents |
| `deleteSurveyDialog.exported` | This survey has been exported, last by {0}: {1}. Author holds the master copy. Sites that applied an export keep their copy, and deleting it here does not change them. To change those sites later, import the backup into Author, edit it and export again; a survey rebuilt from scratch cannot update them. | Delete survey dialog · paragraph for a survey already exported; Author stays (product name) | 450 | params: {0} = author name, {1} = release note |
| `deleteSurveyDialog.neverExported` | This survey has never been exported, so no site has a copy of it. | Delete survey dialog · paragraph for a survey never exported | 120 |  |
| `deleteSurveyDialog.advice` | Download a backup first. It is the only way to get the survey back: importing it restores the survey with the same keys. It holds the survey exactly as it stands, even with validation errors, and no site can install it. | Delete survey dialog · paragraph urging a backup before deleting | 260 |  |
| `deleteSurveyDialog.backup` | Download backup | Delete survey dialog · primary button downloading a backup file of the survey | 30 |  |
| `deleteSurveyDialog.confirmName` | Type {0} to confirm | Delete survey dialog · text field label; the author types the survey name exactly | 60 | params: {0} = survey name |
| `deleteSurveyDialog.delete` | Delete survey | Delete survey dialog · destructive button, enabled once the name is typed | 20 |  |
| `deleteSurveyDialog.deleted` | Survey {0} deleted | Surveys list · notification after a delete | 60 | params: {0} = survey name |
| `surveyEditorView.pageTitle` | Survey | Survey overview · browser tab title | 20 |  |
| `surveyEditorView.editDetails` | Edit details | Survey overview · toolbar button | 15 |  |
| `surveyEditorView.export` | Export… | Survey overview · toolbar button, opens the export dialog (keep the ellipsis) | 15 |  |
| `surveyEditorView.design` | Design… | Survey overview · toolbar button, opens the designer (keep the ellipsis) | 15 |  |
| `surveyEditorView.reporting` | Analysis… | Survey overview · toolbar button, opens the analysis tags page (keep the ellipsis) | 15 |  |
| `surveyEditorView.summary` | {0} — key {1} | Survey overview · paragraph under the heading | 80 | params: {0} = survey name, {1} = survey key |
| `surveyEditorView.neverExported` | Never exported. | Survey overview · attribution sentence | 30 |  |
| `surveyEditorView.lastEditedBy` | Last edited by {0}. | Survey overview · attribution sentence | 40 | params: {0} = user name |
| `surveyEditorView.lastExportedBy` | Last exported by {0}: {1} | Survey overview · attribution sentence | 80 | params: {0} = user name, {1} = release note |
| `surveyEditorView.validation` | Validation | Survey overview · heading of the validation panel | 20 |  |
| `surveyEditorView.readyToExport` | Ready to export. No problems found. | Survey overview · paragraph when validation finds nothing | 40 |  |
| `surveyEditorView.issue.error` | Error: {0} | Survey overview · validation list item | 100 | params: {0} = finding text identical |
| `surveyEditorView.issue.warning` | Warning: {0} | Survey overview · validation list item | 100 | params: {0} = finding text |
| `validation.finding.location` | — in {0} | Survey overview and export dialog · where a validation finding's element sits, appended to the finding | 40 | params: {0} = path of step/section/question names, already joined |
| `validation.where.step` | Step ''{0}'' | Validation finding · names a step; starts the sentence it is inserted into | 60 | params: {0} = step name |
| `validation.where.section` | Section ''{0}'' | Validation finding · names a section; starts the sentence it is inserted into | 60 | params: {0} = section name |
| `validation.where.sectionInStep` | Section ''{0}'' in step ''{1}'' | Validation finding · names a section as placed in one step; starts the sentence it is inserted into | 90 | params: {0} = section name, {1} = step name |
| `validation.where.question` | Question ''{0}'' | Validation finding · names a question; starts the sentence it is inserted into | 60 | params: {0} = question short text |
| `validation.where.questionInSection` | Question ''{0}'' in section ''{1}'' | Validation finding · names a question as placed in one section; starts the sentence it is inserted into | 90 | params: {0} = question short text, {1} = section name |
| `validation.where.rule` | Rule ''{0}'' | Validation finding · names a rule; starts the sentence it is inserted into | 60 | params: {0} = rule description |
| `validation.where.theRule` | The rule | Validation finding · the rule being saved in the rule dialog; starts the sentence it is inserted into | 30 |  |
| `validation.survey.noSteps` | This survey has no steps yet. Add a step to start. | Validation finding · warning: the survey is empty | 120 |  |
| `validation.mount.stepRemoved` | A section is placed in a step that has been removed. Move it to another step. | Validation finding · error: a section sits in a step that no longer exists | 160 |  |
| `validation.mount.sectionRemoved` | A step holds a section that has been removed. Take it out of the step. | Validation finding · error: a step still holds a section that no longer exists | 160 |  |
| `validation.assignment.questionRemoved` | A section holds a question that has been removed. Take it out of the section. | Validation finding · error: a section still holds a question that no longer exists | 160 |  |
| `validation.assignment.sectionRemoved` | A question is placed in a section that has been removed. Move it to another section. | Validation finding · error: a question sits in a section that no longer exists | 160 |  |
| `validation.question.listRemoved` | Question ''{0}'' uses a list of options that has been removed. Choose another list. | Validation finding · error: the question uses a list of options that no longer exists | 180 | params: {0} = question short text |
| `validation.question.noList` | Question ''{0}'' has no list of options. A {1} question needs one. | Validation finding · error: a choice question has no list of options | 180 | params: {0} = question short text, {1} = question type name, kept as written (RADIO, COMBOBOX) |
| `validation.question.emptyList` | Question ''{0}'' uses an empty list. Add options to the list. | Validation finding · error: the list a choice question uses has no options | 160 | params: {0} = question short text |
| `validation.question.noText` | Question ''{0}'' has no question text. Enter what respondents should read. | Validation finding · error: the question has no text for respondents | 180 | params: {0} = question short text |
| `validation.option.listRemoved` | Option ''{0}'' belongs to a list that has been removed. | Validation finding · error: an option belongs to a list that no longer exists | 140 | params: {0} = option text |
| `validation.step.empty` | Step ''{0}'' is empty. Add a section to it. | Validation finding · warning: the step holds no sections | 120 | params: {0} = step name |
| `validation.section.unplaced` | Section ''{0}'' is not used. Place it in a step, or remove it. | Validation finding · warning: the section is in no step | 150 | params: {0} = section name |
| `validation.section.empty` | Section ''{0}'' is empty. Add a question to it. | Validation finding · warning: the section holds no questions | 120 | params: {0} = section name |
| `validation.question.unplaced` | Question ''{0}'' is not used. Place it in a section, or remove it. | Validation finding · warning: the question is in no section | 150 | params: {0} = question short text |
| `validation.survey.startingPoint` | The survey''s starting point is not set, or points to a section that is no longer in the survey. Set "{0}" in "{1}". | Validation finding · error: the survey does not say which section it opens on | 260 | params: {0} = the field label surveyMetadataDialog.initialDisplayKey, {1} = the dialog title surveyMetadataDialog.editSurvey, both already translated |
| `validation.survey.nameSingleLine` | The survey''s name contains a line break or another control character. Correct it in "{0}". | Validation finding · error: the survey name is not a single line of text | 220 | params: {0} = the dialog title surveyMetadataDialog.editSurvey, already translated |
| `validation.survey.titleSingleLine` | The survey''s title contains a line break or another control character. Correct it in "{0}". | Validation finding · error: the survey title is not a single line of text | 220 | params: {0} = the dialog title surveyMetadataDialog.editSurvey, already translated |
| `validation.order.steps` | Two steps share position {0}. Reorder them. | Validation finding · warning: two steps have the same position | 120 | params: {0} = position number |
| `validation.order.sections` | Two sections in this step share position {0}. Reorder them. | Validation finding · warning: two sections of one step have the same position | 140 | params: {0} = position number |
| `validation.order.questions` | Two questions in this section share position {0}. Reorder them. | Validation finding · warning: two questions of one section have the same position | 140 | params: {0} = position number |
| `validation.rule.noTarget` | {0} has no downstream target. Choose the step, section or question it acts on. | Validation finding · error, also shown when saving a rule: the rule has no downstream target | 180 | params: {0} = the rule as the subject of the sentence ("Rule 'x'" or "The rule") |
| `validation.rule.sectionNotInStep` | {0} has downstream section ''{1}'' in step ''{2}'', but that section is not in that step. Choose the downstream target again. | Validation finding · error, also shown when saving a rule: the downstream section is not in the step the rule names | 240 | params: {0} = the rule as the subject of the sentence ("Rule 'x'" or "The rule"), {1} = section name, {2} = step name |
| `validation.rule.questionNotInSection` | {0} has downstream question ''{1}'' in section ''{2}'', but that question is not in that section. Choose the downstream target again. | Validation finding · error, also shown when saving a rule: the downstream question is not in the section the rule names | 240 | params: {0} = the rule as the subject of the sentence ("Rule 'x'" or "The rule"), {1} = question short text, {2} = section name |
| `validation.rule.questionWithoutPlace` | {0} has downstream question ''{1}'' in a different section from its upstream question, but does not say which step and section. Choose the downstream target again. | Validation finding · error, also shown when saving a rule: the downstream question is in another section than the upstream question and the rule does not say which step and section | 280 | params: {0} = the rule as the subject of the sentence ("Rule 'x'" or "The rule"), {1} = question short text |
| `validation.rule.readsRemovedQuestion` | {0} has an upstream question that has been removed. Choose another upstream question. | Validation finding · error, also shown when saving a rule: the upstream question no longer exists | 180 | params: {0} = the rule as the subject of the sentence ("Rule 'x'" or "The rule") |
| `validation.rule.readsRemovedStep` | {0} has an upstream step that has been removed. Choose the upstream question again. | Validation finding · error: the upstream step no longer exists | 180 | params: {0} = the rule as the subject of the sentence ("Rule 'x'" or "The rule") |
| `validation.rule.targetsRemovedStep` | {0} has a downstream step that has been removed. Choose another downstream target. | Validation finding · error, also shown when saving a rule: the downstream step no longer exists | 160 | params: {0} = the rule as the subject of the sentence ("Rule 'x'" or "The rule") |
| `validation.rule.targetsRemovedSection` | {0} has a downstream section that is no longer in that step. Choose another downstream target. | Validation finding · error, also shown when saving a rule: the downstream section is no longer in the step | 180 | params: {0} = the rule as the subject of the sentence ("Rule 'x'" or "The rule") |
| `validation.rule.targetsRemovedQuestion` | {0} has a downstream question that is no longer in that section. Choose another downstream target. | Validation finding · error, also shown when saving a rule: the downstream question is no longer in the section | 180 | params: {0} = the rule as the subject of the sentence ("Rule 'x'" or "The rule") |
| `validation.rule.incomplete` | {0} is incomplete. Choose its operator and its action. | Validation finding · error: the rule has no operator or no action | 140 | params: {0} = the rule as the subject of the sentence ("Rule 'x'" or "The rule") |
| `validation.rule.repeatSelectionOperator` | {0} repeats its downstream target once for each item selected in its upstream question, but its operator, {1}, is never true for a selection, so nothing would ever be repeated. Change the operator to "Field exists". | Validation finding · error: a Repeat rule reads a multi-select question with an operator that can never be true for it; upstream question and downstream target as in the other rule findings; Field exists as in the guide (guideView.rules.*) | 320 | params: {0} = the rule as the subject of the sentence ("Rule 'x'" or "The rule"), {1} = the operator name |
| `validation.rule.backwards` | {0} points backwards: {1}. | Validation finding · error, also shown when saving a rule: the downstream target comes before the upstream question; keep the same words for "points backwards" as surveyDesignerView.backward.one | 400 | params: {0} = the rule as the subject of the sentence ("Rule 'x'" or "The rule"), {1} = the reason, a validation.rule.backwards.* phrase |
| `validation.rule.backwards.question` | downstream question ''{0}'' in step ''{1}'' › ''{2}'' must come after upstream question ''{3}'' in step ''{4}'' › ''{5}'' | Validation finding · reason a rule points backwards when its downstream target is a question; continues a sentence, so no capital and no full stop | 320 | params: {0} = downstream question, {1} = its step, {2} = its section, {3} = the upstream question, {4} = its step, {5} = its section |
| `validation.rule.backwards.section` | downstream section ''{0}'' in step ''{1}'' must come after upstream question ''{2}'' in step ''{3}'' › ''{4}'' | Validation finding · reason a rule points backwards when its downstream target is a section; continues a sentence, so no capital and no full stop | 300 | params: {0} = downstream section, {1} = its step, {2} = the upstream question, {3} = its step, {4} = its section |
| `validation.rule.backwards.step` | downstream step ''{0}'' must come after upstream question ''{1}'' in step ''{2}'' › ''{3}'' | Validation finding · reason a rule points backwards when its downstream target is a step; continues a sentence, so no capital and no full stop | 280 | params: {0} = downstream step, {1} = the upstream question, {2} = its step, {3} = its section |
| `validation.rule.backwards.edge` | Points backwards: {0} | Validation finding · Designer board, tooltip of a rule arrow that points backwards | 360 | params: {0} = the reason, a validation.rule.backwards.* phrase |
| `validation.step.noBuildingRule` | Step ''{0}'' has no Show or Repeat rule. Every step after the first needs one. Add a Show rule on the step — usually "Field exists" on an upstream question. | Validation finding · error: a step after the first has no Show or Repeat rule; Show, Repeat and Field exists as in the guide (guideView.rules.*) | 320 | params: {0} = step name |
| `validation.section.noBuildingRule` | Section ''{0}'' in step ''{1}'' has no Show or Repeat rule. Everything after the first step needs one. Add a Show rule on the section in this step — usually "Field exists" on an upstream question. | Validation finding · error: a section after the first step has no Show or Repeat rule; Show, Repeat and Field exists as in the guide (guideView.rules.*) | 360 | params: {0} = section name, {1} = step name |
| `validation.question.noBuildingRule` | Question ''{0}'' in section ''{1}'' in step ''{2}'' has no Show or Repeat rule. Everything after the first step needs one. Add a Show rule on the question, its section or its step — usually "Field exists" on an upstream question. | Validation finding · error: a question after the first step has no Show or Repeat rule; Show, Repeat and Field exists as in the guide (guideView.rules.*) | 400 | params: {0} = question short text, {1} = section name, {2} = step name |
| `validation.token.unfilledWithDefault` | {0} uses {1}, but no rule fills it in here. Respondents will always see "{2}". | Validation finding · warning: no rule fills a placeholder, so its default always shows | 240 | params: {0} = the element as the subject of the sentence (a validation.where.* phrase), {1} = the placeholder as written, {2} = its default text |
| `validation.token.unfilledNoDefault` | {0} uses {1}, but no rule fills it in here and it has no default, so respondents would see {1} itself. Add a rule that fills it, or give it a default: {2}. | Validation finding · error: no rule fills a placeholder and it has no default | 340 | params: {0} = the element as the subject of the sentence (a validation.where.* phrase), {1} = the placeholder as written, {2} = the same placeholder with a default added |
| `validation.token.undelimited` | {0} uses {1} without angle brackets around the token. Write {2}. | Validation finding · error: a placeholder names its token without angle brackets | 220 | params: {0} = the element as the subject of the sentence (a validation.where.* phrase), {1} = the placeholder as written, {2} = the corrected placeholder |
| `validation.token.undelimitedWithDefault` | {0} uses {1} without angle brackets around the token. Write {2}. Until then respondents always see "{3}". | Validation finding · error: a placeholder names its token without angle brackets, so its default always shows | 300 | params: {0} = the element as the subject of the sentence (a validation.where.* phrase), {1} = the placeholder as written, {2} = the corrected placeholder, {3} = its default text |
| `validation.token.undefined` | {0} uses {1} in {2}, but no rule has a token named {3}, so respondents always see "{5}". Add the token to a rule. If {1} is formatting, move it outside the braces: {4} | Validation finding · error: a placeholder names a token no rule has, or holds formatting inside its braces | 420 | params: {0} = the element as the subject of the sentence (a validation.where.* phrase), {1} = the name in angle brackets, {2} = the placeholder as written, {3} = the token name, {4} = where formatting belongs, {5} = the default text |
| `validation.token.undefinedNoDefault` | {0} uses {1} in {2}, but no rule has a token named {3}, so respondents always see the text as written. Add the token to a rule. If {1} is formatting, move it outside the braces: {4} | Validation finding · error: a placeholder with no default names a token no rule has, or holds formatting inside its braces | 420 | params: {0} = the element as the subject of the sentence (a validation.where.* phrase), {1} = the name in angle brackets, {2} = the placeholder as written, {3} = the token name, {4} = where formatting belongs |
| `validation.rule.tokenUnused` | {0} has the token {1}, but nothing in its downstream target uses {2}. Add it to a text there, or remove the token from the rule. | Validation finding · warning: nothing in the rule downstream target uses the rule token | 260 | params: {0} = the rule ("Rule 'x'"), {1} = token name, {2} = the token as a text writes it |
| `validation.rule.displayOnlyNoValue` | {0} cannot fill {1}: its upstream question ''{2}'' only shows text ({3}) and has no answer. Choose an upstream question the respondent answers, or set the rule''s "{4}". | Validation finding · error: the upstream question only shows text and has no value of its own, so its token is never filled | 360 | params: {0} = the rule, {1} = the token as a text writes it, {2} = question short text, {3} = question type name, kept as written (HTML, MODAL), {4} = the field label relationshipDialog.defaultUpstreamValue, already translated |
| `validation.rule.displayOnlyConstant` | {0} has upstream question ''{2}'', which only shows text ({3}) and has no answer, so {1} is always the rule''s "{4}". To use a real answer, choose an upstream question the respondent answers. | Validation finding · warning: the upstream question only shows text, so its token is always the rule own value | 360 | params: {0} = the rule, {1} = the token as a text writes it, {2} = question short text, {3} = question type name, kept as written, {4} = the field label relationshipDialog.defaultUpstreamValue, already translated |
| `validation.rule.tokenBracketed` | {0} has the token {1}. Write it without angle brackets: {2}. Brackets belong only in the text, as {3}. | Validation finding · error: the rule token was stored with its angle brackets | 240 | params: {0} = the rule, {1} = the token as stored, {2} = the token name, {3} = the token as a text writes it |
| `validation.rule.tokenInvalid` | {0} has the token "{1}", which is not a valid name. Use only letters, digits and underscores. | Validation finding · error: the rule token holds a character a token name cannot have | 220 | params: {0} = the rule, {1} = the token as stored |
| `validation.rule.tokenLeadingDigit` | {0} has the token {1}. It works, but a token name should start with a letter. Rename it. | Validation finding · warning: the rule token does not start with a letter | 200 | params: {0} = the rule, {1} = the token as stored |
| `validation.rule.tokenCase` | {0} has the token {1}. It works, but write {2} so the token list does not show both. | Validation finding · warning: the rule token is not in upper case | 200 | params: {0} = the rule, {1} = the token as stored, {2} = the token in upper case |
| `validation.rename.step` | Step ''{0}'' was renamed to ''{1}''. Sites already using this survey will show the new name in data already recorded too. | Validation finding · warning that an edit also changes what sites already using the survey report: a step was renamed | 260 | params: {0} = old name, {1} = new name |
| `validation.rename.section` | Section ''{0}'' was renamed to ''{1}''. Sites already using this survey will show the new name in data already recorded too. | Validation finding · warning that an edit also changes what sites already using the survey report: a section was renamed | 260 | params: {0} = old name, {1} = new name |
| `validation.tag.redefined` | Tag {0} was changed to {1}. Answers already collected at sites using this survey will be recorded under the new tag. | Validation finding · warning that an edit also changes what sites already using the survey report: a tag in use was renamed or moved to another dimension | 280 | params: {0} = the tag before, quoted with its table, {1} = the tag after |
| `validation.tag.reassigned` | {0}: tag {1} was changed to {2}. Answers already collected at sites using this survey will be recorded under the new tag. | Validation finding · warning that an edit also changes what sites already using the survey report: an element was given another tag or value | 300 | params: {0} = element name, {1} = the tag and value before, quoted, {2} = the tag and value after |
| `validation.tag.unassigned` | {0}: tag {1} was removed. Answers already collected at sites using this survey will no longer be recorded under it. | Validation finding · warning that an edit also changes what sites already using the survey report: a tag was taken off an element | 300 | params: {0} = element name, {1} = the tag and value, quoted |
| `validation.tag.assignmentBroken` | Tag ''{0}'' is assigned, but not to one section or question. Remove the assignment and add it again. | Validation finding · Analysis tags: error, a tag assignment does not point at exactly one element | 240 | params: {0} = tag |
| `validation.tag.assignedToRemoved.section` | Tag ''{0}'' is assigned to a section that has been removed{1}. Remove the assignment. | Validation finding · Analysis tags: warning, a tag is assigned to a section that no longer exists | 220 | params: {0} = tag, {1} = empty, or a space and the section name in brackets |
| `validation.tag.assignedToRemoved.question` | Tag ''{0}'' is assigned to a question that has been removed{1}. Remove the assignment. | Validation finding · Analysis tags: warning, a tag is assigned to a question that no longer exists | 220 | params: {0} = tag, {1} = empty, or a space and the question name in brackets |
| `validation.tag.assignedTwice` | Tag ''{0}'' is assigned to {1} {2} times with different values. Keep one. | Validation finding · Analysis tags: warning, one tag is on one element more than once with different values | 220 | params: {0} = tag, {1} = element name, {2} = how many times |
| `validation.tag.table.empty` | Tag ''{0}'' gives the dimension table an empty name. Rename the tag or its dimension; sites could not build their analysis schema with this name. | Validation finding · Analysis tags: error, a tag produces an empty dimension table name | 280 | params: {0} = tag; dynamic |
| `validation.tag.table.characters` | Tag ''{0}'' gives the dimension table the name ''{1}'', which may only use a–z, 0–9 and _. Rename the tag or its dimension; sites could not build their analysis schema with this name. | Validation finding · Analysis tags: error, a tag produces a dimension table name with characters a table name cannot have | 340 | params: {0} = tag, {1} = table name; dynamic |
| `validation.tag.table.tooLong` | Tag ''{0}'' gives the dimension table the name ''{1}'', which is longer than {2} characters. Rename the tag or its dimension; sites could not build their analysis schema with this name. | Validation finding · Analysis tags: error, a tag produces a dimension table name that is too long | 340 | params: {0} = tag, {1} = table name, {2} = the limit; dynamic |
| `validation.tag.column.empty` | Tag ''{0}'' gives the column an empty name. Rename the tag; sites could not build their analysis schema with this name. | Validation finding · Analysis tags: error, a tag produces an empty column name | 240 | params: {0} = tag; dynamic |
| `validation.tag.column.characters` | Tag ''{0}'' gives the column the name ''{1}'', which may only use a–z, 0–9 and _. Rename the tag; sites could not build their analysis schema with this name. | Validation finding · Analysis tags: error, a tag produces a column name with characters a column name cannot have | 300 | params: {0} = tag, {1} = column name; dynamic |
| `validation.tag.column.tooLong` | Tag ''{0}'' gives the column the name ''{1}'', which is longer than {2} characters. Rename the tag; sites could not build their analysis schema with this name. | Validation finding · Analysis tags: error, a tag produces a column name that is too long | 300 | params: {0} = tag, {1} = column name, {2} = the limit; dynamic |
| `validation.tag.column.reserved` | Tag ''{0}'' gives the column the name ''{1}'', which is a fixed column of every survey''s fact table (step_key, section_key, question_key, item_key). Rename the tag; the site''s analysis build refuses it. | Validation finding · Analysis tags: error, a tag produces a column name reserved for a fixed column; step_key, section_key, question_key, item_key are technical names, keep them | 300 | params: {0} = tag, {1} = column name |
| `validation.tag.unused` | Tag ''{0}'' is not used. Assign it to a section or question, or delete it. | Validation finding · Analysis tags: warning, a tag is on no element | 180 | params: {0} = tag |
| `validation.tag.sameColumn` | Tags {0} produce the same column, ''{1}''. Rename one of them. | Validation finding · Analysis tags: error, several tags produce one column | 220 | params: {0} = the tags, quoted and comma separated, {1} = column name |
| `validation.tag.tableClash` | Tag ''{0}'' has no dimension, so its reporting table is ''{1}'' — the same table dimension ''{2}'' uses. Rename the tag or give it a dimension. | Validation finding · Analysis tags: error, a tag without a dimension produces the table a dimension already uses | 300 | params: {0} = tag, {1} = table name, {2} = dimension name |
| `validation.translation.missing.one` | {0}: 1 text is not translated yet. Respondents will see it in the survey''s own language. | Validation finding · warning: one text of a published language is not translated | 220 | params: {0} = language tag |
| `validation.translation.missing.many` | {0}: {1} texts are not translated yet. Respondents will see them in the survey''s own language. | Validation finding · warning: several texts of a published language are not translated | 220 | params: {0} = language tag, {1} = how many |
| `validation.translation.stale.one` | {0}: 1 translation is out of date because the original text changed. Respondents will see the original until it is translated again. | Validation finding · warning: one translation is out of date | 260 | params: {0} = language tag |
| `validation.translation.stale.many` | {0}: {1} translations are out of date because the original text changed. Respondents will see the original until they are translated again. | Validation finding · warning: several translations are out of date | 260 | params: {0} = language tag, {1} = how many |
| `surveyMetadataDialog.newSurvey` | New survey | Survey details dialog · dialog title when creating | 20 |  |
| `surveyMetadataDialog.editSurvey` | Edit survey | Survey details dialog · dialog title when editing | 20 |  |
| `surveyMetadataDialog.name` | Name | Survey details dialog · field label | 20 |  |
| `surveyMetadataDialog.title` | Title | Survey details dialog · field label | 20 |  |
| `surveyMetadataDialog.description` | Description | Survey details dialog · field label | 20 |  |
| `surveyMetadataDialog.initialDisplayKey` | Initial display key | Survey details dialog · field label | 30 |  |
| `surveyMetadataDialog.postSurveyUrl` | Post-survey address | Survey details dialog · field label (URL shown after the survey) | 30 |  |
| `surveyMetadataDialog.surveyKey` | Survey key | Survey details dialog · read-only field label | 20 |  |
| `surveyMetadataDialog.nameRequired` | Name is required | Survey details dialog · field error | 30 |  |
| `surveyMetadataDialog.titleRequired` | Title is required | Survey details dialog · field error | 30 |  |
| `surveyMetadataDialog.created` | Survey created | Survey details dialog · success notification | 30 |  |
| `surveyMetadataDialog.updated` | Survey updated | Survey details dialog · success notification | 30 |  |
| `exportDialog.title` | Export {0} | Export dialog · dialog title | 40 | params: {0} = survey title |
| `exportDialog.releaseNote` | Release note | Export dialog · text area label | 20 |  |
| `exportDialog.releaseNote.placeholder` | What changed in this revision (recorded on the survey and in the file) | Export dialog · text area placeholder | 80 |  |
| `exportDialog.releaseNote.required` | Enter a release note | Export dialog · field error | 30 |  |
| `exportDialog.export` | Export | Export dialog · primary button | 15 |  |
| `exportDialog.issue.error` | Error: {0} | Export dialog · validation list item | 100 | params: {0} = finding text identical |
| `exportDialog.issue.warning` | Warning: {0} | Export dialog · validation list item | 100 | params: {0} = finding text |
| `exportDialog.errorsBlock` | Fix {0} error(s) before you can export. | Export dialog · paragraph | 80 | params: {0} = number of errors |
| `exportDialog.warningsDoNotBlock` | You can export with warnings. | Export dialog · paragraph | 50 |  |
| `exportDialog.retroactiveReminder` | Reminder: renaming a step or section, or changing a question's tag, also relabels answers already recorded at sites already using this survey. | Export dialog · secondary paragraph | 200 |  |
| `exportDialog.download` | Download {0} | Export dialog · download link | 40 | params: {0} = file name |
| `exportDialog.revisionSummary` | Revision {0}: {1} | Export dialog · paragraph after export | 80 | params: {0} = revision number, {1} = list of table names with counts |
| `importDefinitionView.pageTitle` | Import definition | Import page · browser tab title | 25 |  |
| `importDefinitionView.title` | Import definition | Import page · heading | 25 |  |
| `importDefinitionView.intro` | Upload an ELICIT_SURVEY_EXPORT_V1 (.elicit) file to open it in Author as a working copy. Every survey and element key in the file is kept exactly as it is, so a later export updates the same rows at every site that already applied it. A survey whose key is already open here is refused. | Import page · paragraph; ELICIT_SURVEY_EXPORT_V1 and .elicit are technical names | 400 |  |
| `importDefinitionView.introDraft` | An ELICIT_SURVEY_DRAFT_V1 file is accepted too -- a definition drafted outside Author, which leaves its keys blank for Author to mint. Only Author reads that marker, so a draft cannot be installed at a site until it passes validation here and is exported. | Import page · second intro paragraph; ELICIT_SURVEY_DRAFT_V1 is a technical name | 300 |  |
| `importDefinitionView.aiDraftLink` | No draft yet? Have an AI agent write one | Import page · link to the page that hands out the AI specification | 60 |  |
| `importDefinitionView.imported` | Imported {0} | Import page · heading after import | 40 | params: {0} = survey name |
| `importDefinitionView.draftNotice` | This came from an {0} draft, so its keys were minted here. Review it, fix anything validation reports, then export it to get a file a site can apply. | Import page · paragraph shown only when the imported file was a draft | 220 | params: {0} = the draft format marker |
| `importDefinitionView.surveyKey` | Survey key {0} | Import page · first part of the result sentence | 40 | params: {0} = survey key |
| `importDefinitionView.fileRevision` | , file revision {0} | Import page · optional clause appended to the result sentence (keep the leading comma) | 30 | params: {0} = revision number |
| `importDefinitionView.recordsImported` | {0} records imported from {1} | Import page · second sentence of the result | 60 | params: {0} = count, {1} = file name |
| `importDefinitionView.retiredSkipped` | , {0} retired record(s) skipped | Import page · optional clause appended to the result sentence (keep the leading comma) | 40 | params: {0} = count |
| `importDefinitionView.openInEditor` | Open in editor | Import page · primary button | 20 |  |
| `importDefinitionView.importFailed` | Import failed: {0} | Import page · error paragraph | 80 | params: {0} = reason |
| `importDefinitionView.validation` | Validation | Import page · heading of the validation findings shown after an import | 20 |  |
| `importDefinitionView.validationClean` | No problems found. This survey is ready to export. | Import page · paragraph when validation finds nothing in the imported definition | 70 |  |
| `importDefinitionView.issue.error` | Error: {0} | Import page · validation list item | 100 | params: {0} = finding text identical |
| `importDefinitionView.issue.warning` | Warning: {0} | Import page · validation list item | 100 | params: {0} = finding text |
| `aiDraftView.pageTitle` | Draft with AI | AI draft page · browser tab title | 25 |  |
| `aiDraftView.title` | Draft a survey with an AI agent | AI draft page · heading | 45 |  |
| `aiDraftView.intro` | A survey can start from a draft written somewhere else. Take the specification to any AI session, describe the survey you want, and bring back the file it writes. Nothing leaves this installation on its own: you carry the specification out, and you carry the draft back. | AI draft page · intro paragraph | 350 |  |
| `aiDraftView.step1` | Copy the prompt into an AI session, or download the specification and attach the file. | AI draft page · numbered step 1 of 4 | 110 |  |
| `aiDraftView.step2` | Describe the survey you want. The agent asks about whatever decides structure: the topics to cover, what repeats per person or item, what has to be conditional. | AI draft page · numbered step 2 of 4 | 200 |  |
| `aiDraftView.step3` | Save the .elicit file the agent writes. | AI draft page · numbered step 3 of 4; .elicit is a file extension, keep it | 60 |  |
| `aiDraftView.step4` | Import it here. Author mints every key on import, so the draft arrives as an ordinary working copy. | AI draft page · numbered step 4 of 4 | 130 |  |
| `aiDraftView.copy` | Copy prompt to clipboard | AI draft page · primary button; copies the instruction plus the specification | 30 |  |
| `aiDraftView.download` | Download the specification | AI draft page · link that downloads the specification file | 30 |  |
| `aiDraftView.copied` | Prompt copied. Paste it into your AI session. | AI draft page · notification after a successful copy | 60 |  |
| `aiDraftView.copyFailed` | Could not copy to the clipboard. Download the specification instead. | AI draft page · notification when the browser refused the clipboard write | 80 |  |
| `aiDraftView.unavailable` | This build does not carry the specification, so there is nothing to copy or download. | AI draft page · paragraph shown only by a build that packaged no specification | 100 |  |
| `aiDraftView.whatComesBack.h` | What comes back | AI draft page · section heading | 25 |  |
| `aiDraftView.whatComesBack.p1` | A draft is not a deployable definition. It carries a marker only Author reads, so it cannot be installed at a site until you have reviewed it here and exported it. | AI draft page · paragraph | 220 |  |
| `aiDraftView.whatComesBack.p2` | Expect to fix things. Open the imported survey, read the validation panel and correct what it reports, or send those messages back to the agent and import a corrected draft. | AI draft page · paragraph | 220 |  |
| `aiDraftView.goToImport` | Go to Import definition | AI draft page · link to the import page; match the wording of mainLayout.nav.importDefinition | 30 |  |
| `aiDraftView.promptPreamble` | I want to write a survey definition for the Elicit survey platform. The specification below tells you the survey model and the exact file format. Read it, ask me whatever you need to know about the survey I want, and then write me a complete .elicit draft file. | AI draft page · NOT shown on screen: the sentence copied to the clipboard in front of the specification, addressed by the author to an AI agent. The specification that follows it stays in English. | 300 |  |
| `guideView.pageTitle` | How a survey is put together | Guide page · browser tab title | 40 |  |
| `guideView.title` | How a survey is put together | Guide page · heading | 40 |  |
| `guideView.diagram.alt` | The designer board for a small survey called Family history: two step cards, About you and Each child, each holding section cards (You and Children, then Child) with question rows inside them, and a Show rule drawn from the question 'Has children' to the question 'Child count'. | Guide page · alternative text of the designer screenshot; Family history, About you, Each child, You, Children, Child, Has children and Child count are the names shown in the picture and stay in English | 400 |  |
| `guideView.surveysLink` | Go to the Surveys list | Guide page · link at the end of the page | 30 |  |
| `guideView.intro.p1` | An Elicit survey is a decision tree the respondent walks through. This page shows the pieces a survey is made of and how the designer draws them, using the same names and glyphs you will see on the board and in the structure tree. | Guide page · introduction paragraph | 300 |  |
| `guideView.tree.h` | Elicit is a decision tree | Guide page · section heading | 40 |  |
| `guideView.tree.p1` | A respondent moves through the survey one page at a time. What they answer decides what they see next: a question can open a section, repeat a group of questions, or change the wording of a later question. Designing a survey means designing that path. | Guide page · paragraph | 350 |  |
| `guideView.containers.h` | Containers nest | Guide page · section heading | 40 |  |
| `guideView.containers.p1` | Everything sits inside something else. A survey holds steps, a step holds sections, and a section holds questions. | Guide page · paragraph | 150 |  |
| `guideView.containers.step.term` | Step | Guide page · bold term starting a bullet | 15 |  |
| `guideView.containers.step.desc` | — a page in the respondent's progress bar. For you it is a lane on the board that groups related sections. | Guide page · rest of the bullet after the term (keep the leading dash) | 150 |  |
| `guideView.containers.section.term` | Section | Guide page · bold term starting a bullet | 15 |  |
| `guideView.containers.section.desc` | — a titled group of questions on that page. For you it is a card inside a lane. | Guide page · rest of the bullet after the term (keep the leading dash) | 120 |  |
| `guideView.containers.question.term` | Question | Guide page · bold term starting a bullet | 15 |  |
| `guideView.containers.question.desc` | — one input the respondent answers. For you it is a row inside a card, with a glyph that shows its type. | Guide page · rest of the bullet after the term (keep the leading dash) | 150 |  |
| `guideView.order.h` | Order is the default path | Guide page · section heading | 40 |  |
| `guideView.order.p1` | Steps run in order, sections run in order within their step, and questions run in order within their section. Unless a rule says otherwise, that display order is exactly what the respondent experiences. Dragging an element to a new position changes it. | Guide page · paragraph | 300 |  |
| `guideView.order.p2` | Only the first step is built when a respondent signs in. Every step, section and question after it is built just in time, by a Show or Repeat rule, when the respondent's answers call for it — so the first page appears quickly, and someone who leaves in the first step or two leaves nothing saved for questions they never saw. Give every later element such a rule; for a step that always follows, the usual one is Field exists → Show on the step, read from a question the respondent has already passed. Validation reports any later element no rule builds. | Guide page · paragraph | 600 |  |
| `guideView.rules.h` | Rules are the arrows | Guide page · section heading | 40 |  |
| `guideView.rules.p1` | A rule reads the answer to one upstream question, compares it with a reference value using an operator, and when the comparison holds performs an action on one downstream target: a step, a section, or a question. Written out, every rule is the same sentence: | Guide page · paragraph ending with a colon that introduces the example sentence | 300 |  |
| `guideView.rules.example.question` | How many children? | Guide page · example question in the rule sentence | 30 |  |
| `guideView.rules.example.action` | Repeat | Guide page · example action name in the rule sentence (same word as the Repeat rule) | 15 |  |
| `guideView.rules.example.section` | Child | Guide page · example section name in the rule sentence | 15 |  |
| `guideView.rules.example.thatManyTimes` | that many times | Guide page · end of the rule sentence | 25 |  |
| `guideView.rules.example.ariaLabel` | Example rule: question 'How many children?' greater than 0 repeats section 'Child' that many times | Guide page · accessible name of the rule sentence | 120 |  |
| `guideView.rules.show.term` | Show | Guide page · bold rule name starting a bullet | 15 |  |
| `guideView.rules.show.desc` | — the target appears only when the rule holds. Until then the respondent never sees it. | Guide page · rest of the bullet (keep the leading dash) | 120 |  |
| `guideView.rules.repeat.term` | Repeat | Guide page · bold rule name starting a bullet | 15 |  |
| `guideView.rules.repeat.desc` | — the target section or question is repeated as many times as the answer says, for example one 'Child' section per child. When the answer is a selection from a list, the target is repeated once for each item selected and the rule's token is filled with that item's text, for example one 'Game' section per game attended. | Guide page · rest of the bullet (keep the leading dash) | 380 |  |
| `guideView.rules.text.term` | Text | Guide page · bold rule name starting a bullet | 15 |  |
| `guideView.rules.text.desc` | — the answer is written into the target's text wherever a {<TOKEN>\|default} placeholder appears, so a later question can say 'your daughter' instead of 'your child'. The rule reaches everything inside its target: one Text rule on a step fills the token in every section and question of that step. A token is a slot name, not a question — several rules may fill the same token, each where it is used. | Guide page · rest of the bullet (keep the leading dash); {<TOKEN>\|default} is a literal placeholder syntax, keep it | 450 |  |
| `guideView.rules.p2` | There is no Hide rule. Hiding is the same as not showing: give the element a Show rule and it stays hidden until that rule holds. | Guide page · paragraph | 150 |  |
| `guideView.rules.p3` | Operators are Boolean, Greater than, Equal, Not equal, Field exists and Contains. When several rules point at the same target, all of them must hold. | Guide page · paragraph; operator names should match the rule editor | 150 |  |
| `guideView.designer.h` | The designer draws all of this | Guide page · section heading | 40 |  |
| `guideView.designer.p1` | On the board, lanes are steps, cards are sections, rows are questions and arrows are rules. A card starts closed; one click opens it and selects it, a double click on a lane, card or row opens its editor, and the ⋮ menu on each element lists everything you can do to it. A breadcrumb at the top of the page shows where you are (survey, step, section, question), and the structure tree at the bottom of the drawer follows your selection. Every element carries its glyph, and the glyph is its handle: drag a glyph from the Controls panel to create an element, drag it within its container to reorder, or drag it into another container to show the same element there as well (a section in another step, a question in another section); Copy to step… in a card's menu makes a separate section instead. Drag a rule glyph onto a question to start a rule, then drag the arrowhead onto the target. Everything a drag can do, the element's menu can do too. | Guide page · long paragraph; menu item names (Copy to step…) should match the designer | 900 |  |
| `guideView.designing.h` | Designing a survey | Guide page · section heading | 40 |  |
| `guideView.designing.p1` | Open a survey from the Surveys list and choose Design…. The drawer on the left is your toolbox; the board on the right is the survey. Work top-down: steps, then sections, then questions, then the lists, rules and analysis tags that give them meaning. | Guide page · paragraph; Design… must match the toolbar button | 300 |  |
| `guideView.designing.search.term` | Search | Guide page · bold panel name starting a bullet | 15 |  |
| `guideView.designing.search.desc` | — each of the Lists, Sections and Questions panels has its own search box that narrows that panel as you type: lists by name, description and option text, sections by name and description, questions by name, text and type. Handy once a survey has dozens of each. | Guide page · rest of the bullet (keep the leading dash) | 300 |  |
| `guideView.designing.controls.term` | Controls | Guide page · bold panel name starting a bullet | 15 |  |
| `guideView.designing.controls.desc` | — the glyphs for a step, a section, a question of the chosen type, and the three rules. Drag one onto the board, or use Add step and the Add… items in the menus. | Guide page · rest of the bullet (keep the leading dash) | 200 |  |
| `guideView.designing.lists.term` | Lists | Guide page · bold panel name starting a bullet | 15 |  |
| `guideView.designing.lists.desc` | — the lists of options a choice question (radio, combo box, multi-select, checkbox group) offers. New list… defines one with its options in order; drag a list's glyph onto a question to give it those options, or pick the list in the question's editor. | Guide page · rest of the bullet (keep the leading dash) | 300 |  |
| `guideView.designing.sections.term` | Sections | Guide page · bold panel name starting a bullet | 15 |  |
| `guideView.designing.sections.desc` | — every section in the survey, once, with the number of steps it is mounted in. Drag one into a lane, or use Mount in step…, to show the same section in another step; it stays one section, so a change to its name or questions reaches every step. | Guide page · rest of the bullet (keep the leading dash) | 300 |  |
| `guideView.designing.questions.term` | Questions | Guide page · bold panel name starting a bullet | 15 |  |
| `guideView.designing.questions.desc` | — every question in the survey, once, with the number of sections it appears in. Drag one into a card, or use Add to section…, to ask the same question again elsewhere; it stays one question, so a change to its text reaches every section it is in. | Guide page · rest of the bullet (keep the leading dash) | 300 |  |
| `guideView.designing.reportingTags.term` | Analysis tags | Guide page · bold panel name starting a bullet | 20 |  |
| `guideView.designing.reportingTags.desc` | — the ⋮ menu of a question or section offers Analysis tags…; the survey's Analysis… page lists them all with the tables and columns they produce. Tagged elements show a tag badge on the board, one entry per tag. | Guide page · rest of the bullet (keep the leading dash) | 300 |  |
| `guideView.designing.structure.term` | Survey structure | Guide page · bold panel name starting a bullet | 20 |  |
| `guideView.designing.structure.desc` | — the tree at the bottom of the drawer; click an element to select it on the board. Show removed reveals retired elements so they can be restored. | Guide page · rest of the bullet (keep the leading dash) | 200 |  |
| `guideView.designing.p2` | When the structure is in place, add rules for anything conditional, tag what the site's analysis schema needs, mark what the reports show, check the validation panel on the survey overview, and Export… the .elicit file for the Admin app to install. | Guide page · paragraph; Export… must match the toolbar button | 250 |  |
| `guideView.reportingTags.h` | Analysis tags | Guide page · section heading | 40 |  |
| `guideView.reportingTags.p1` | An analysis tag names a column of the star schema a site builds from the answers for analysis. Tag a question and its answers fill that column; tag a section as it appears in a step with a fixed value, and every respondent who reaches it is classified by that value. Tags that share a dimension share one table of values. Open a survey's Analysis page, or the 'Analysis tags…' item on a question or section, to manage them. Changing or removing an assigned tag is retroactive: answers already recorded at deployed sites are reclassified. Tags are not reports: what a respondent or an administrator reads is decided by the report marks on the survey's Reports page. | Guide page · paragraph | 500 |  |
| `guideView.identity.h` | Identity and exporting | Guide page · section heading | 40 |  |
| `guideView.identity.p1` | Every element gets a key the moment it is created and keeps it for life. Removing an element retires it rather than deleting it, so answers already recorded at a site keep their meaning. When the survey is ready you export a .elicit file; the Admin app installs it at a site the first time and updates it in place after that, matching elements by key. | Guide page · paragraph | 400 |  |
| `guideView.identity.p2` | Deleting a whole survey, from the Surveys list, is the one thing that does remove rows, and it cannot be undone. Download the backup it offers first: importing that file brings the survey back with every key. Sites that applied an export are not touched; to change them later, import the backup, edit and export again. | Guide page · paragraph on deleting a whole survey | 420 |  |
| `guideView.legend.h` | Glyph legend | Guide page · heading of the glyph legend | 20 |  |
| `guideView.legend.step` | Step, numbered by its display order | Guide page · legend entry | 50 |  |
| `guideView.legend.section` | Section | Guide page · legend entry | 20 |  |
| `guideView.legend.question` | Text question — the icon changes with the question type (number, choice, date, …) | Guide page · legend entry | 90 |  |
| `guideView.legend.showRule` | Show rule | Guide page · legend entry | 20 |  |
| `guideView.legend.repeatRule` | Repeat rule | Guide page · legend entry | 20 |  |
| `guideView.legend.textRule` | Text rule | Guide page · legend entry | 20 |  |
| `designer.guideLink` | How surveys work | Designer board and toolbox · link to the survey-model guide | 30 |  |
| `designer.menu.edit` | Edit… | Designer node menus (step, section, question, list) · menu item opening the edit dialog | 20 |  |
| `designer.menu.moveUp` | Move up | Designer node menus · menu item | 20 |  |
| `designer.menu.moveDown` | Move down | Designer node menus · menu item | 20 |  |
| `designer.menu.openDetails` | Open details | Designer node menus · menu item opening the element details page | 25 |  |
| `designer.menu.reportingTags` | Analysis tags… | Designer node menus · menu item opening the analysis-tags dialog | 25 |  |
| `designer.needsStep` | Add a step first | Designer palette and drawer panels · tooltip on a glyph that cannot be dragged until the survey has a step | 40 |  |
| `designer.needsSection` | Add a section first | Designer palette and drawer panels · tooltip on a glyph that cannot be dragged until a step holds a section | 40 |  |
| `designer.needsQuestion` | Add a question first | Designer palette and drawer panels · tooltip on a glyph that cannot be dragged until a section holds a question | 40 |  |
| `designerPalette.aria` | Palette: drag a glyph onto the board to add an element | Designer palette · accessible name of the palette | 80 |  |
| `designerPalette.step` | Step | Designer palette · row caption next to the step glyph | 15 |  |
| `designerPalette.section` | Section | Designer palette · row caption next to the section glyph | 15 |  |
| `designerPalette.question` | Question | Designer palette · row caption next to the question glyph | 15 |  |
| `designerPalette.rule` | Rule | Designer palette · row caption next to the rule glyphs | 15 |  |
| `designerPalette.roledescription` | draggable palette item | Designer palette · aria-roledescription of each glyph (lower case) | 40 |  |
| `designerToolbox.controls` | Controls | Designer toolbox · collapsible panel title | 20 |  |
| `designerToolbox.lists` | Lists | Designer toolbox · collapsible panel title | 20 |  |
| `designerToolbox.sections` | Sections | Designer toolbox · collapsible panel title | 20 |  |
| `designerToolbox.questions` | Questions | Designer toolbox · collapsible panel title | 20 |  |
| `designerToolbox.listsCount` | Lists ({0}) | Designer toolbox · panel title while a search is active | 25 | params: {0} = number of matches |
| `designerToolbox.sectionsCount` | Sections ({0}) | Designer toolbox · panel title while a search is active | 25 | params: {0} = number of matches |
| `designerToolbox.questionsCount` | Questions ({0}) | Designer toolbox · panel title while a search is active | 25 | params: {0} = number of matches |
| `glyph.step` | Step | Designer glyph · accessible name and tooltip of a step glyph without a number | 15 |  |
| `glyph.stepOrder` | Step {0} | Designer glyph · accessible name and tooltip of a numbered step glyph | 15 | params: {0} = step number |
| `glyph.section` | Section | Designer glyph · accessible name and tooltip of a section glyph | 15 |  |
| `glyph.question` | Question | Designer glyph · accessible name and tooltip of a question glyph with no type | 15 |  |
| `glyph.questionOfType` | {0} question | Designer glyph · accessible name and tooltip of a typed question glyph | 30 | params: {0} = question type name in lower case (e.g. "radio") |
| `glyph.rule.show` | Show rule | Designer glyph · accessible name and tooltip | 20 |  |
| `glyph.rule.repeat` | Repeat rule | Designer glyph · accessible name and tooltip | 20 |  |
| `glyph.rule.text` | Text rule | Designer glyph · accessible name and tooltip | 20 |  |
| `glyph.rule` | Rule | Designer glyph · accessible name and tooltip of a rule glyph of unknown kind | 15 |  |
| `listPanel.newList` | New list… | Designer toolbox, Lists panel · button | 20 |  |
| `listPanel.search` | Search lists | Designer toolbox, Lists panel · search field placeholder | 25 |  |
| `listPanel.search.aria` | Search lists by name, description or option text | Designer toolbox, Lists panel · search field accessible name | 70 |  |
| `listPanel.roledescription` | draggable list | Designer toolbox, Lists panel · aria-roledescription of a list glyph (lower case) | 30 |  |
| `listPanel.optionCount.one` | 1 option | Designer toolbox, Lists panel · count under a list name | 15 |  |
| `listPanel.optionCount.many` | {0} options | Designer toolbox, Lists panel · count under a list name | 15 | params: {0} = number of options (0 or 2+) |
| `listPanel.questionCount.one` | 1 question | Designer toolbox, Lists panel · count of questions using the list | 15 |  |
| `listPanel.questionCount.many` | {0} questions | Designer toolbox, Lists panel · count of questions using the list | 15 | params: {0} = number of questions (2+) |
| `listPanel.menu.aria` | Actions for list {0} | Designer toolbox, Lists panel · accessible name of the row menu button | 40 | params: {0} = list name |
| `listPanel.empty` | No lists yet. A choice question (radio, combo box, multi-select, checkbox group) needs one. | Designer toolbox, Lists panel · empty-state text | 120 |  |
| `listPanel.noMatch` | No list matches ''{0}''. | Designer toolbox, Lists panel · empty-state text while a search has no hits | 50 | params: {0} = the search text |
| `pickerDialog.chooseOne` | Choose one | Step/section picker dialog · validation message under the combo box | 25 |  |
| `previewFields.htmlPlaceholder` | <em>HTML content preview</em> | Question preview · placeholder shown for an HTML question with no content | 40 | html |
| `previewFields.invalidHtml` | Enter valid HTML to preview it. | Question preview · message replacing an HTML question whose markup cannot be parsed | 50 |  |
| `questionBankPanel.search` | Search questions | Designer toolbox, Questions panel · search field placeholder | 25 |  |
| `questionBankPanel.search.aria` | Search questions by name, text or type | Designer toolbox, Questions panel · search field accessible name | 60 |  |
| `questionBankPanel.roledescription` | draggable question | Designer toolbox, Questions panel · aria-roledescription of a question glyph (lower case) | 30 |  |
| `questionBankPanel.uses.one` | in 1 section | Designer toolbox, Questions panel · where the question is placed | 20 |  |
| `questionBankPanel.uses.many` | in {0} sections | Designer toolbox, Questions panel · where the question is placed | 20 | params: {0} = number of sections (0 or 2+) |
| `questionBankPanel.uses.none` | in no section | Designer toolbox, Questions panel · badge on a question no section holds | 20 |  |
| `questionBankPanel.menu.aria` | Actions for question {0} | Designer toolbox, Questions panel · accessible name of the row menu button | 40 | params: {0} = question name |
| `questionBankPanel.menu.addToSection` | Add to section… | Designer toolbox, Questions panel · menu item | 25 |  |
| `questionBankPanel.empty` | No questions yet. Questions you add appear here and can be reused in other sections. | Designer toolbox, Questions panel · empty-state text | 110 |  |
| `questionBankPanel.noMatch` | No question matches ''{0}''. | Designer toolbox, Questions panel · empty-state text while a search has no hits | 50 | params: {0} = the search text |
| `questionRow.aria` | Question {0} | Designer board, question row · accessible name of the row | 40 | params: {0} = question name |
| `questionRow.aria.removed` | Question {0} (removed) | Designer board, question row · accessible name of a removed row | 50 | params: {0} = question name |
| `questionRow.menu.aria` | Actions for question {0} | Designer board, question row · accessible name of the row menu button | 40 | params: {0} = question name |
| `questionRow.menu.alsoAddToSection` | Also add to section… | Designer board, question row · menu item | 30 |  |
| `questionRow.menu.addRule` | Add rule… | Designer board, question row · menu item | 20 |  |
| `questionRow.menu.removeFromSection` | Remove from this section | Designer board, question row · menu item | 30 |  |
| `sectionBankPanel.search` | Search sections | Designer toolbox, Sections panel · search field placeholder | 25 |  |
| `sectionBankPanel.search.aria` | Search sections by name or description | Designer toolbox, Sections panel · search field accessible name | 60 |  |
| `sectionBankPanel.roledescription` | draggable section | Designer toolbox, Sections panel · aria-roledescription of a section glyph (lower case) | 30 |  |
| `sectionBankPanel.uses.one` | in 1 step | Designer toolbox, Sections panel · where the section is mounted | 20 |  |
| `sectionBankPanel.uses.many` | in {0} steps | Designer toolbox, Sections panel · where the section is mounted | 20 | params: {0} = number of steps (0 or 2+) |
| `sectionBankPanel.uses.none` | in no step | Designer toolbox, Sections panel · badge on a section no step holds | 20 |  |
| `sectionBankPanel.menu.aria` | Actions for section {0} | Designer toolbox, Sections panel · accessible name of the row menu button | 40 | params: {0} = section name |
| `sectionBankPanel.menu.mountInStep` | Mount in step… | Designer toolbox, Sections panel · menu item | 25 |  |
| `sectionBankPanel.empty` | No sections yet. Sections you add appear here and can be mounted in other steps. | Designer toolbox, Sections panel · empty-state text | 110 |  |
| `sectionBankPanel.noMatch` | No section matches ''{0}''. | Designer toolbox, Sections panel · empty-state text while a search has no hits | 50 | params: {0} = the search text |
| `sectionCard.questionCount.one` | 1 question | Designer board, section card · count in the card header | 15 |  |
| `sectionCard.questionCount.many` | {0} questions | Designer board, section card · count in the card header | 15 | params: {0} = number of questions (0 or 2+) |
| `sectionCard.mounted` | in {0} steps | Designer board, section card · badge shown when the section is mounted in several steps | 20 | params: {0} = number of steps (2+) |
| `sectionCard.mounted.title` | This section is mounted in {0} steps; editing it changes all of them | Designer board, section card · tooltip of the mounted badge | 90 | params: {0} = number of steps |
| `sectionCard.menu.aria` | Actions for section {0} | Designer board, section card · accessible name of the card menu button | 40 | params: {0} = section name |
| `sectionCard.menu.addQuestion` | Add question | Designer board, section card · menu item | 25 |  |
| `sectionCard.menu.copyToStep` | Copy to step… | Designer board, section card · menu item | 25 |  |
| `sectionCard.menu.alsoMountInStep` | Also mount in step… | Designer board, section card · menu item | 30 |  |
| `sectionCard.menu.removeFromStep` | Remove from this step | Designer board, section card · menu item | 30 |  |
| `sectionCard.aria` | Section {0}, {1} | Designer board, section card · accessible name of the card header | 60 | params: {0} = section name, {1} = the question count text |
| `sectionCard.aria.removed` | Section {0}, {1}, removed | Designer board, section card · accessible name of a removed card header | 70 | params: {0} = section name, {1} = the question count text |
| `sectionCard.empty` | No questions yet — drop a question glyph here or use Add question. | Designer board, section card · empty-state text inside an empty card | 90 | mentions the menu item sectionCard.menu.addQuestion |
| `sectionPreview.heading` | Preview | Designer preview pane · pane heading | 20 |  |
| `sectionPreview.aria` | Preview of the selected section as a respondent sees it | Designer preview pane · accessible name of the pane | 80 |  |
| `sectionPreview.hint.select` | Select a step, section or question on the board to see it as a respondent will. | Designer preview pane · hint when nothing is selected | 110 |  |
| `sectionPreview.hint.noSections` | Step ''{0}'' has no sections yet, so respondents would see nothing here. | Designer preview pane · hint for an empty step | 100 | params: {0} = step name |
| `sectionPreview.hint.step` | Respondents see one section per page. Answers typed here are not saved. | Designer preview pane · hint when a step is selected | 100 |  |
| `sectionPreview.hint.section` | What a respondent sees on this page. Answers typed here are not saved. | Designer preview pane · hint when a section or question is selected | 100 |  |
| `sectionPreview.note.stepRuleShown` | Step ''{0}'' is shown only when a rule reveals it. | Designer preview pane · note above the pages of a rule-shown step | 70 | params: {0} = step name |
| `sectionPreview.page` | Page {0} of {1} | Designer preview pane · caption above each page | 20 | params: {0} = page number, {1} = page count |
| `sectionPreview.note.removed` | Removed: respondents do not see this section. | Designer preview pane · note in a removed section | 60 |  |
| `sectionPreview.note.ruleShown` | Shown only when a rule reveals it. | Designer preview pane · note under a rule-shown section or question | 50 |  |
| `sectionPreview.note.noQuestions` | No questions yet. | Designer preview pane · note in a section without questions | 30 |  |
| `stepLane.aria` | Step {0}: {1} | Designer board, step lane · accessible name of the lane | 50 | params: {0} = step number, {1} = step name |
| `stepLane.sectionCount.one` | 1 section | Designer board, step lane · count in the lane header | 15 |  |
| `stepLane.sectionCount.many` | {0} sections | Designer board, step lane · count in the lane header | 15 | params: {0} = number of sections (0 or 2+) |
| `stepLane.menu.aria` | Actions for step {0} | Designer board, step lane · accessible name of the lane menu button | 40 | params: {0} = step name |
| `stepLane.menu.addStepAfter` | Add step after | Designer board, step lane · menu item | 25 |  |
| `stepLane.menu.addSection` | Add section | Designer board, step lane · menu item | 25 |  |
| `stepLane.menu.removeStep` | Remove step | Designer board, step lane · menu item | 20 |  |
| `stepLane.header.aria` | Step {0}: {1}, {2} | Designer board, step lane · accessible name of the lane header | 70 | params: {0} = step number, {1} = step name, {2} = the section count text |
| `stepLane.header.aria.removed` | Step {0}: {1}, {2}, removed | Designer board, step lane · accessible name of a removed lane header | 80 | params: {0} = step number, {1} = step name, {2} = the section count text |
| `stepLane.empty` | No sections yet — drop a section glyph here or use Add section. | Designer board, step lane · empty-state text inside an empty lane | 90 | mentions the menu item stepLane.menu.addSection |
| `tagBadge.title` | Analysis tags: {0} | Designer board, analysis-tag badge · tooltip and accessible name | 60 | params: {0} = the tag names |
| `surveyDesignerView.pageTitle` | Designer | Designer · browser tab title | 20 |  |
| `surveyDesignerView.showRemoved` | Show removed | Designer toolbar · checkbox label | 20 |  |
| `surveyDesignerView.showRules` | Show rules | Designer toolbar · checkbox label | 20 |  |
| `surveyDesignerView.showPreview` | Show preview | Designer toolbar · checkbox label | 20 |  |
| `surveyDesignerView.overview` | Overview | Designer toolbar · button back to the survey overview | 20 |  |
| `surveyDesignerView.addStep` | Add step | Designer toolbar · button | 20 |  |
| `surveyDesignerView.breadcrumb.aria` | Where you are in the survey | Designer breadcrumb · accessible name of the breadcrumb | 40 |  |
| `surveyDesignerView.noSurvey` | No survey with id {0} | Designer · error page message for an unknown survey id | 40 | params: {0} = survey id |
| `surveyDesignerView.emptyBoard` | This survey has no steps yet. Drag the Step glyph here or use Add step. | Designer board · empty-state text, followed by the guide link | 100 |  |
| `surveyDesignerView.crumb.step` | Step: {0} | Designer breadcrumb · crumb label | 40 | params: {0} = step name |
| `surveyDesignerView.crumb.section` | Section: {0} | Designer breadcrumb · crumb label | 40 | params: {0} = section name |
| `surveyDesignerView.crumb.question` | Question: {0} | Designer breadcrumb · crumb label | 40 | params: {0} = question name |
| `surveyDesignerView.sectionAlsoInStep` | Section ''{0}'' is now also in step ''{1}'' | Designer · success notification after mounting a section | 80 | params: {0} = section name, {1} = step name |
| `surveyDesignerView.questionAlsoInSection` | Question ''{0}'' is now also in section ''{1}'' | Designer · success notification after placing a question | 80 | params: {0} = question name, {1} = section name |
| `surveyDesignerView.movedStep` | Moved step ''{0}'' | Designer · notification after a move | 50 | params: {0} = step name |
| `surveyDesignerView.movedSection` | Moved section ''{0}'' | Designer · notification after a move | 50 | params: {0} = section name |
| `surveyDesignerView.movedQuestion` | Moved question ''{0}'' | Designer · notification after a move | 50 | params: {0} = question name |
| `surveyDesignerView.refused.step` | A step goes on the board. Drop it between two steps or after the last one. | Designer · warning when a step is dropped where it cannot go | 120 |  |
| `surveyDesignerView.refused.section` | A section goes in a step. Drop it on a step or between two of its sections. | Designer · warning when a section is dropped outside a step | 120 |  |
| `surveyDesignerView.refused.question` | A question goes in a section. Drop it on a section card, or add a section to the step first. | Designer · warning when a question is dropped outside a section | 140 |  |
| `surveyDesignerView.refused.rule` | A rule starts at the question whose answer it reads. Drop it on that question's row. | Designer · warning when a rule glyph is dropped anywhere but a question row | 130 |  |
| `surveyDesignerView.refused.list` | A list gives a question its options. Drop it on that question's row. | Designer · warning when a list is dropped anywhere but a question row | 110 |  |
| `surveyDesignerView.refused.removed` | ''{0}'' has been removed. Restore it before dropping anything on it. | Designer · warning when something is dropped on a removed element | 110 | params: {0} = element name |
| `surveyDesignerView.copySection.title` | Copy section ''{0}'' | Designer, copy-section picker dialog · dialog title | 50 | params: {0} = section name |
| `surveyDesignerView.copySection.label` | Into step | Designer, copy-section picker dialog · combo box label | 20 |  |
| `surveyDesignerView.mountSection.title` | Mount section ''{0}'' | Designer, mount-section picker dialog · dialog title | 50 | params: {0} = section name |
| `surveyDesignerView.mountSection.label` | Also in step | Designer, mount-section picker dialog · combo box label | 20 |  |
| `surveyDesignerView.questionUsesList` | Question ''{0}'' now offers the list ''{1}'' | Designer · success notification after dropping a list on a question | 80 | params: {0} = question name, {1} = list name |
| `surveyDesignerView.removeList.title` | Remove list | Designer, remove-list confirmation · dialog title | 25 |  |
| `surveyDesignerView.removeList.text` | Remove the list ''{0}'' and its options? | Designer, remove-list confirmation · dialog text | 60 | params: {0} = list name |
| `surveyDesignerView.removedList` | Removed list ''{0}'' | Designer · success notification | 50 | params: {0} = list name |
| `surveyDesignerView.addQuestion.title` | Add question ''{0}'' | Designer, add-question picker dialog · dialog title | 50 | params: {0} = question name |
| `surveyDesignerView.addQuestion.label` | To section | Designer, add-question picker dialog · combo box label | 20 |  |
| `surveyDesignerView.kind.step` | step | Designer, remove confirmation · the word inserted into removeAssignment sentences (lower case) | 15 |  |
| `surveyDesignerView.kind.section` | section | Designer, remove confirmation · the word inserted into removeAssignment sentences (lower case) | 15 |  |
| `surveyDesignerView.kind.question` | question | Designer, remove confirmation · the word inserted into removeAssignment sentences (lower case) | 15 |  |
| `surveyDesignerView.removeAssignment.title` | Remove from {0} | Designer, remove confirmation · dialog title | 30 | params: {0} = surveyDesignerView.kind.step or .section |
| `surveyDesignerView.removeAssignment.text` | Remove {0} ''{1}'' from {2} ''{3}''? | Designer, remove confirmation · dialog text, first sentence | 80 | params: {0} = kind word (section/question), {1} = its name, {2} = parent kind word (step/section), {3} = parent name |
| `surveyDesignerView.removeAssignment.last` | This is its only place in the survey, so the {0} itself is removed (it can be restored from its details page). | Designer, remove confirmation · second sentence when this is the element's only placement | 120 | params: {0} = kind word (section/question) |
| `surveyDesignerView.removeAssignment.shared` | It stays in the other places it is used. | Designer, remove confirmation · second sentence when the element is used elsewhere | 60 |  |
| `surveyDesignerView.removedAssignment` | Removed {0} ''{1}'' from {2} ''{3}'' | Designer · success notification | 80 | params: {0} = kind word (section/question), {1} = its name, {2} = parent kind word (step/section), {3} = parent name |
| `surveyDesignerView.removeStep.title` | Remove step | Designer, remove-step confirmation · dialog title | 25 |  |
| `surveyDesignerView.removeStep.text` | Remove step ''{0}''?{1} It can be restored from its details page. | Designer, remove-step confirmation · dialog text | 100 | params: {0} = step name, {1} = empty or a space plus surveyDesignerView.removeStep.dependents |
| `surveyDesignerView.removeStep.dependents` | Removed with it: {0}. | Designer, remove-step confirmation · sentence listing what goes with the step | 60 | params: {0} = list of dependents separated by "; " |
| `surveyDesignerView.removedStep` | Removed step ''{0}'' | Designer · success notification | 50 | params: {0} = step name |
| `surveyDesignerView.removeUnplaced.title` | Remove element | Designer, remove-element confirmation · dialog title | 25 |  |
| `surveyDesignerView.removeUnplaced.text` | ''{0}'' is in no step or section. Remove it from the survey? | Designer, remove-element confirmation · dialog body | 120 | params: {0} = element name |
| `surveyDesignerView.removedUnplaced` | Removed ''{0}'' | Designer · success notification | 50 | params: {0} = element name |
| `surveyDesignerView.revealedUnplacedSection` | This section is in no step, so the board cannot show it. It is marked in the Sections panel. | Designer · notification after following a validation finding | 140 |  |
| `surveyDesignerView.revealedUnplacedQuestion` | This question is in no section, so the board cannot show it. It is marked in the Questions panel. | Designer · notification after following a validation finding | 140 |  |
| `surveyDesignerView.dropArrowHint` | Drop the arrow on a step, a section or a question | Designer · info notification when a rule arrow is dropped nowhere useful | 70 |  |
| `surveyDesignerView.ruleGone` | This rule no longer exists | Designer · info notification | 40 |  |
| `surveyDesignerView.ruleRepointed` | Rule re-pointed | Designer · success notification after moving a rule arrow end | 30 |  |
| `surveyDesignerView.undo` | Undo | Designer · inline button in the copied-section notification | 15 |  |
| `surveyDesignerView.copiedSection` | Copied section ''{0}'' into step ''{1}''. | Designer · notification text before the Undo button | 70 | params: {0} = new section name, {1} = step name |
| `surveyDesignerView.backward.one` | {0} — 1 rule now points backwards (shown in red). Move the downstream target after the upstream question, or move the elements again. | Designer · warning after a move that turned one rule backwards | 200 | params: {0} = the move message (e.g. surveyDesignerView.movedStep) |
| `surveyDesignerView.backward.many` | {0} — {1} rules now point backwards (shown in red). Move the downstream target after the upstream question, or move the elements again. | Designer · warning after a move that turned several rules backwards | 200 | params: {0} = the move message, {1} = number of rules |
| `constraint.surveys_name_un` | Another survey already uses this name | Any save · notification or field error from a database constraint | 80 |  |
| `constraint.surveys_survey_key_un` | A survey with this key already exists in Author | Import · notification | 80 |  |
| `constraint.steps_survey_name_un` | Another step in this survey already uses this name | Step dialog · field error | 80 |  |
| `constraint.steps_survey_display_order_un` | Another step already has this position | Designer · notification | 80 |  |
| `constraint.sections_survey_display_order_un` | Another section already has this position | Designer · notification | 80 |  |
| `constraint.select_groups_name_un` | Another list in this survey already uses this name | List dialog · field error | 80 |  |
| `constraint.select_items_display_text_un` | This list already has an option with that text | List dialog · field error | 80 |  |
| `constraint.steps_sections_display_key_un` | Another assignment already uses this display key | Designer · field error | 80 |  |
| `constraint.dimensions_un` | A dimension with this name already exists | Tag dialog · field error | 80 |  |
| `constraint.ontology_un` | A tag with this name already exists in this survey's namespace | Tag dialog · field error | 80 |  |
| `constraint.metadata_un` | This element already carries this tag with the same value | Tag assignment dialog · notification | 80 |  |
| `constraint.sqlstate.23503` | This element is still referenced by another element | Any removal · notification | 80 |  |
| `constraint.sqlstate.23505` | A row with the same unique value already exists ({0}) | Any save · notification | 80 | params: {0} = technical constraint name |
| `elementView.pageTitle` | Element | Element detail page · browser tab title | 20 |  |
| `elementView.surveyOverview` | Survey overview | Element detail page · back button | 25 |  |
| `elementView.openInDesigner` | Open in designer | Element detail page · button | 25 |  |
| `elementView.kind.step` | step | Element detail page · badge next to the heading, lower case | 15 | dynamic |
| `elementView.kind.section` | section | Element detail page · badge next to the heading, lower case | 15 | dynamic |
| `elementView.kind.question` | question | Element detail page · badge next to the heading, lower case | 15 | dynamic |
| `elementView.badge.removed` | {0} (removed) | Element detail page · badge for a retired element | 25 | params: {0} = step/section/question word |
| `elementView.question.untitled` | Question {0} | Element detail page · heading when the question has no short text | 25 | params: {0} = question number |
| `elementView.fact.displayOrder` | Display order | Element detail page · fact label | 25 |  |
| `elementView.fact.dimensionName` | Dimension name | Element detail page · fact label | 25 |  |
| `elementView.fact.description` | Description | Element detail page · fact label | 25 |  |
| `elementView.fact.type` | Type | Element detail page · fact label | 25 |  |
| `elementView.fact.text` | Text | Element detail page · fact label | 25 |  |
| `elementView.fact.required` | Required | Element detail page · fact label | 25 |  |
| `elementView.fact.tooltip` | Tooltip | Element detail page · fact label | 25 |  |
| `elementView.fact.minMax` | Min / max | Element detail page · fact label (minimum / maximum) | 25 |  |
| `elementView.fact.validationText` | Validation text | Element detail page · fact label | 25 |  |
| `elementView.fact.list` | List (durable id) | Element detail page · fact label; the answer list and its technical id | 25 |  |
| `elementView.fact.mask` | Mask | Element detail page · fact label (input mask) | 25 |  |
| `elementView.fact.placeholder` | Placeholder | Element detail page · fact label | 25 |  |
| `elementView.fact.defaultValue` | Default value | Element detail page · fact label | 25 |  |
| `elementView.fact.variant` | Variant | Element detail page · fact label | 25 |  |
| `elementView.fact.tagValue` | value "{0}" | Element detail page · part of the analysis-tags fact | 30 | params: {0} = fixed value |
| `elementView.fact.reportingTags` | Analysis tags | Element detail page · fact label | 25 |  |
| `elementView.fact.elementKey` | Element key | Element detail page · fact label | 25 |  |
| `elementView.fact.durableId` | Durable id | Element detail page · fact label | 25 |  |
| `elementView.fact.removedAt` | Removed at | Element detail page · fact label (date) | 25 |  |
| `elementView.remove.header` | Remove {0}? | Remove confirmation · dialog header | 60 | params: {0} = element description, e.g. Question "Age" |
| `elementView.remove.explanation` | The element is retired, not deleted: sites that apply the next export close it, respondents who already started keep it, and recorded answers stay valid. | Remove confirmation · paragraph | 200 |  |
| `elementView.remove.dependents` | Removed together with it: | Remove confirmation · paragraph before the list of dependents | 40 |  |
| `elementView.undo` | Undo | Removal notice · button | 10 |  |
| `elementView.removed` | {0} removed | Removal notice · text | 60 | params: {0} = element description |
| `elementView.restore.blocked` | Cannot restore {0}: {1} | Element detail page · error notification | 120 | params: {0} = element description, {1} = reasons |
| `elementView.restore.header` | Restore {0}? | Restore confirmation · dialog header | 60 | params: {0} = element description |
| `elementView.restore.explanation` | The element reopens in its former position, keeping its element key. | Restore confirmation · paragraph | 120 |  |
| `elementView.restore.dependents` | Restored together with it: | Restore confirmation · paragraph before the list of dependents | 40 |  |
| `elementView.restored` | Restored | Element detail page · success notification | 20 |  |
| `reportingView.pageTitle` | Analysis tags | Analysis tags page · browser tab title | 25 |  |
| `reportingView.heading` | Analysis tags — {0} | Analysis tags page · heading | 60 | params: {0} = survey title |
| `reportingView.surveyOverview` | Survey overview | Analysis tags page · back button | 25 |  |
| `reportingView.openInDesigner` | Open in designer | Analysis tags page · button | 25 |  |
| `reportingView.newTag` | New tag… | Analysis tags page · button, opens a dialog | 20 |  |
| `reportingView.assignTag` | Assign tag… | Analysis tags page · button, opens a dialog | 20 |  |
| `reportingView.intro` | An analysis tag names a column of the survey's analysis schema at a site. Assign it to a question and the answers fill that column; assign it to a section with a fixed value and every respondent who reaches the section is classified by it. Tags on the same dimension share one table of values. | Analysis tags page · explanatory paragraph | 400 |  |
| `reportingView.retro` | Changing or removing an assigned tag is retroactive: answers already recorded at deployed sites are reclassified when the survey is exported and applied. | Analysis tags page · explanatory paragraph | 200 |  |
| `reportingView.grid.tag` | Tag | Analysis tags page · tags grid header | 15 |  |
| `reportingView.grid.dimension` | Dimension | Analysis tags page · tags grid header | 15 |  |
| `reportingView.grid.ownTable` | (own table) | Analysis tags page · tags grid cell when the tag has no dimension | 15 |  |
| `reportingView.grid.table` | Table | Analysis tags page · tags grid header | 15 |  |
| `reportingView.grid.reportColumn` | Column | Analysis tags page · tags grid header | 20 |  |
| `reportingView.grid.assignments` | Assignments | Analysis tags page · tags grid header | 15 |  |
| `reportingView.edit` | Edit… | Analysis tags page · small grid button, opens a dialog | 10 |  |
| `reportingView.removeTag.tooltip` | Remove this tag | Analysis tags page · tooltip on the remove button | 30 |  |
| `reportingView.removeTag.blocked` | Remove its assignments first | Analysis tags page · tooltip on the disabled remove button | 40 |  |
| `reportingView.assignmentsGrid.element` | Element | Analysis tags page · assignments grid header | 15 |  |
| `reportingView.assignmentsGrid.where` | Where | Analysis tags page · assignments grid header | 15 |  |
| `reportingView.assignmentsGrid.appliesTo` | Applies to | Analysis tags page · assignments grid header | 15 |  |
| `reportingView.assignmentsGrid.tag` | Tag | Analysis tags page · assignments grid header | 15 |  |
| `reportingView.assignmentsGrid.value` | Value | Analysis tags page · assignments grid header | 15 |  |
| `reportingView.removedElement` | removed element | Analysis tags page · badge on an assignment whose element was removed | 20 |  |
| `reportingView.tags` | Tags | Analysis tags page · section heading | 15 |  |
| `reportingView.assignments` | Assignments | Analysis tags page · section heading | 15 |  |
| `reportingView.impact.title` | Dimensional impact | Analysis tags page · section heading | 25 |  |
| `reportingView.impact.intro` | When the exported survey is applied and answers arrive, the site's reporting build creates these objects in the survey's own reporting schema. | Analysis tags page · paragraph; each survey gets a reporting schema of its own at the site | 200 |  |
| `reportingView.impact.none` | No tags yet: the survey adds nothing to the reporting schema. | Analysis tags page · paragraph | 80 |  |
| `reportingView.impact.sharedDimension` | shared dimension "{0}" | Analysis tags page · fragment of a list item | 40 | params: {0} = dimension name |
| `reportingView.impact.ownTable` | own table of tag "{0}" | Analysis tags page · fragment of a list item | 40 | params: {0} = tag name |
| `reportingView.impact.assignmentOne` | {0} assignment | Analysis tags page · count fragment, singular | 20 | params: {0} = 1 |
| `reportingView.impact.assignmentMany` | {0} assignments | Analysis tags page · count fragment, plural | 20 | params: {0} = count |
| `reportingView.impact.table` | {0} — {1} — tags: {2} — {3} | Analysis tags page · list item | 120 | params: {0} = table name, {1} = shared/own fragment, {2} = tag list, {3} = count fragment |
| `reportingView.impact.dimensionTables` | Dimension tables | Analysis tags page · list caption | 25 |  |
| `reportingView.impact.column` | {0} → {1} (tag "{2}") | Analysis tags page · list item | 80 | params: {0} = column name, {1} = table name, {2} = tag name |
| `reportingView.impact.reportColumns` | Columns | Analysis tags page · list caption | 25 |  |
| `reportingView.impact.perItemColumns` | Fixed columns filled for a section repeated per selected item | Analysis tags page · list caption | 80 |  |
| `reportingView.impact.perItemColumn` | {0} → {1} (the question the Repeat rule reads, and the item each instance is about) | Analysis tags page · list item | 120 | params: {0} = column name, {1} = table name |
| `reportingView.impact.error` | Error: {0} | Analysis tags page · list item prefix | 40 | params: {0} = finding text; identical |
| `reportingView.impact.warning` | Warning: {0} | Analysis tags page · list item prefix | 40 | params: {0} = finding text |
| `reportingView.impact.findings` | Problems | Analysis tags page · list caption | 25 |  |
| `reportingView.removeTag.header` | Remove tag | Remove tag confirmation · dialog header | 25 |  |
| `reportingView.removeTag.text` | Remove the tag "{0}"? It has no assignments. | Remove tag confirmation · dialog text | 80 | params: {0} = tag name |
| `reportingView.removedTag` | Removed tag "{0}" | Analysis tags page · success notification | 40 | params: {0} = tag name |
| `reportingView.removeAssignment.header` | Remove analysis tag | Remove assignment confirmation · dialog header | 30 |  |
| `reportingView.removeAssignment.text` | Remove tag "{0}" from {1}? Answers already recorded at deployed sites stop being classified by it once the survey is exported and applied. | Remove assignment confirmation · dialog text | 200 | params: {0} = tag name, {1} = element description |
| `elementTagsDialog.title` | Analysis tags — {0} | Element tags dialog (designer) · dialog header | 60 | params: {0} = element name |
| `elementTagsDialog.addTag` | Add tag… | Element tags dialog · button, opens a dialog | 15 |  |
| `elementTagsDialog.empty` | No analysis tags yet. Without a tag, answers to this element are not recorded in the site's analysis schema. | Element tags dialog · paragraph when no tag is assigned | 120 |  |
| `elementTagsDialog.grid.tag` | Tag | Element tags dialog · grid header | 15 |  |
| `elementTagsDialog.grid.appliesTo` | Applies to | Element tags dialog · grid header | 15 |  |
| `elementTagsDialog.grid.value` | Value | Element tags dialog · grid header | 15 |  |
| `elementTagsDialog.grid.reportColumn` | Column | Element tags dialog · grid header | 20 |  |
| `elementTagsDialog.edit` | Edit… | Element tags dialog · small grid button, opens a dialog | 10 |  |
| `elementTagsDialog.removeAssignment.header` | Remove analysis tag | Remove assignment confirmation · dialog header | 30 |  |
| `elementTagsDialog.removeAssignment.text` | Remove tag "{0}" from {1}? Answers already recorded at deployed sites stop being classified by it once the survey is exported and applied. | Remove assignment confirmation · dialog text | 200 | params: {0} = tag name, {1} = element name |
| `elementTagsDialog.removedTag` | Removed tag "{0}" | Element tags dialog · success notification | 40 | params: {0} = tag name |
| `tagAssignmentDialog.title.assign` | Assign analysis tag | Assign tag dialog · dialog header | 30 |  |
| `tagAssignmentDialog.title.edit` | Edit analysis tag | Assign tag dialog · dialog header when editing | 30 |  |
| `tagAssignmentDialog.element` | Element | Assign tag dialog · element picker label | 15 |  |
| `tagAssignmentDialog.element.section` | Section {0} — {1} | Assign tag dialog · element picker item | 80 | params: {0} = section name, {1} = path in the survey |
| `tagAssignmentDialog.element.question` | Question {0} — {1} | Assign tag dialog · element picker item | 80 | params: {0} = question name, {1} = path in the survey |
| `tagAssignmentDialog.appliesTo` | Applies to | Assign tag dialog · scope radio group label | 15 |  |
| `tagAssignmentDialog.tag` | Tag | Assign tag dialog · tag picker label | 15 |  |
| `tagAssignmentDialog.newTag` | New tag… | Assign tag dialog · small button, opens a dialog | 15 |  |
| `tagAssignmentDialog.reportedValue` | Recorded value | Assign tag dialog · radio group label | 20 |  |
| `tagAssignmentDialog.useAnswer` | Use the respondent's answer | Assign tag dialog · radio option | 40 |  |
| `tagAssignmentDialog.fixedValue` | Fixed value | Assign tag dialog · radio option and text field label | 20 |  |
| `tagAssignmentDialog.value.helper` | Stored lowercased and trimmed whenever the element is answered | Assign tag dialog · helper text under the fixed value field | 80 |  |
| `tagAssignmentDialog.acknowledge` | I understand that answers already recorded at deployed sites are reclassified | Assign tag dialog · checkbox label | 100 |  |
| `tagAssignmentDialog.help.tag` | The analysis tag. Its column in the analysis schema is filled from this element. | Assign tag dialog · help tooltip | 100 |  |
| `tagAssignmentDialog.help.value` | A constant recorded instead of the answer, e.g. "Female" on the section about the respondent's mother. | Assign tag dialog · help tooltip | 120 |  |
| `tagAssignmentDialog.warning` | This assignment is exported already. Changing its tag or value is retroactive: answers already recorded at deployed sites are reclassified, not only answers recorded from now on. | Assign tag dialog · warning shown when editing an exported assignment | 200 |  |
| `tagAssignmentDialog.scope.section` | This section as it appears in step "{0}" | Assign tag dialog · scope option | 60 | params: {0} = step name |
| `tagAssignmentDialog.scope.question` | This question wherever it appears | Assign tag dialog · scope option | 40 |  |
| `tagAssignmentDialog.scope.questionInSection` | This question only in section "{0}" | Assign tag dialog · scope option | 60 | params: {0} = section name |
| `tagAssignmentDialog.impact.none` | Choose a tag to see the table and column it fills. | Assign tag dialog · preview text | 60 |  |
| `tagAssignmentDialog.impact.text` | Values go to {0} in the survey''s reporting schema · column {1}{2} | Assign tag dialog · preview text; {0} is a technical table name | 100 | params: {0} = table name, {1} = column name, {2} = constant/from-answer fragment |
| `tagAssignmentDialog.impact.constant` | · constant | Assign tag dialog · preview fragment, keep the leading separator | 15 |  |
| `tagAssignmentDialog.impact.fromAnswer` | · from the answer | Assign tag dialog · preview fragment, keep the leading separator | 20 |  |
| `tagAssignmentDialog.error.chooseTag` | Choose a tag | Assign tag dialog · field error | 30 |  |
| `tagAssignmentDialog.error.fixedValue` | Enter the fixed value or record the answer instead | Assign tag dialog · field error | 60 |  |
| `tagAssignmentDialog.error.chooseElement` | Choose an element | Assign tag dialog · field error | 30 |  |
| `tagAssignmentDialog.error.chooseScope` | Choose what the tag applies to | Assign tag dialog · field error | 40 |  |
| `tagAssignmentDialog.updated` | Updated tag "{0}" | Assign tag dialog · success notification | 40 | params: {0} = tag name |
| `tagAssignmentDialog.assigned` | Assigned tag "{0}" | Assign tag dialog · success notification | 40 | params: {0} = tag name |
| `tagDialog.title.new` | New analysis tag | Tag dialog · dialog header | 30 |  |
| `tagDialog.title.edit` | Edit analysis tag | Tag dialog · dialog header when editing | 30 |  |
| `tagDialog.tag` | Tag | Tag dialog · text field label | 15 |  |
| `tagDialog.tag.helper` | Letters, digits, spaces and hyphens. Becomes a column of the analysis schema. | Tag dialog · helper text | 80 |  |
| `tagDialog.dimension` | Dimension | Tag dialog · combo box label | 15 |  |
| `tagDialog.dimension.placeholder` | None: the tag gets its own table | Tag dialog · combo box placeholder | 40 |  |
| `tagDialog.dimension.helper` | Type a new name to create a dimension. Letters, digits and underscores. | Tag dialog · helper text | 80 |  |
| `tagDialog.namespace` | Namespace | Tag dialog · read-only field label | 15 |  |
| `tagDialog.namespace.helper` | Groups this survey's tags; a site keeps one row per namespace and tag | Tag dialog · helper text | 80 |  |
| `tagDialog.help.tag` | The tag's name. Answers of the tagged elements are recorded under a column named after it. | Tag dialog · help tooltip | 100 |  |
| `tagDialog.help.dimension` | A shared category such as "age" or "cancer". Tags on the same dimension share one table of values; leave it empty to give this tag a table of its own. | Tag dialog · help tooltip | 160 |  |
| `tagDialog.acknowledge` | I understand that answers already recorded at deployed sites are reclassified | Tag dialog · checkbox label | 100 |  |
| `tagDialog.warning` | This tag is assigned. Changing its name or dimension is retroactive: answers already recorded at deployed sites are reclassified, not only answers recorded from now on. | Tag dialog · warning shown when editing an assigned tag | 200 |  |
| `tagDialog.advanced` | Advanced | Tag dialog · collapsible section title | 15 |  |
| `tagDialog.error.tagRequired` | Tag is required | Tag dialog · field error | 30 |  |
| `tagDialog.created` | Created tag "{0}" | Tag dialog · success notification | 40 | params: {0} = tag name |
| `tagDialog.updated` | Updated tag "{0}" | Tag dialog · success notification | 40 | params: {0} = tag name |
| `tagDialog.preview.none` | Choose a tag to see the table and column it produces. | Tag dialog · preview text | 60 |  |
| `tagDialog.preview.text` | Values go to {0} in the survey''s reporting schema · column {1} | Tag dialog · preview text; {0} is a technical table name | 80 | params: {0} = table name, {1} = column name |
| `elementDialog.kind.step` | step | Step/section dialog · the word "step" as used inside sentences (lower case) | 15 | dynamic (referenced through ElementDialog.KIND_STEP) |
| `elementDialog.kind.section` | section | Step/section dialog · the word "section" as used inside sentences (lower case) | 15 | dynamic (referenced through ElementDialog.KIND_SECTION) |
| `elementDialog.title.new` | New {0} | Step/section dialog · dialog title | 30 | params: {0} = "step" or "section" |
| `elementDialog.title.edit` | Edit {0} | Step/section dialog · dialog title | 30 | params: {0} = "step" or "section" |
| `elementDialog.name` | Name | Step/section dialog · field label | 20 |  |
| `elementDialog.dimensionName` | Dimension name | Step/section dialog · field label | 25 |  |
| `elementDialog.description` | Description | Step/section dialog · field label | 20 |  |
| `elementDialog.acknowledge` | I understand that the new name relabels data already recorded at deployed sites | Step/section dialog · checkbox shown when renaming | 120 |  |
| `elementDialog.dimensionName.helper` | Analysis dimension; defaults to the name. Changing it is an analysis change, not a rename. | Step/section dialog · field helper text | 120 |  |
| `elementDialog.help.name` | The {0}''s name. Shown to respondents and used as its label in the analysis schema; renaming it is retroactive. | Step/section dialog · field tooltip | 140 | params: {0} = "step" or "section" |
| `elementDialog.help.dimensionName` | The name this {0} is recorded under in the analysis schema. Defaults to the name; change it to relabel recorded data without renaming. | Step/section dialog · field tooltip | 140 | params: {0} = "step" or "section" |
| `elementDialog.help.description` | An optional note for authors. It is not shown to respondents. | Step/section dialog · field tooltip | 80 |  |
| `elementDialog.error.nameRequired` | Name is required | Step/section dialog · field error | 30 |  |
| `elementDialog.renameWarning` | Renaming is retroactive: data already recorded at deployed sites will show the new name, not only data recorded from now on. | Step/section dialog · warning shown when the name changes | 200 |  |
| `elementDialog.placed` | The {0} is placed where you dropped it; you can move it afterwards. | Step/section dialog · note when creating | 100 | params: {0} = "step" or "section" |
| `elementDialog.saved.created` | Created {0} ''{1}'' | Step/section dialog · notification | 60 | params: {0} = "step" or "section", {1} = the element name |
| `elementDialog.saved.updated` | Updated {0} ''{1}'' | Step/section dialog · notification | 60 | params: {0} = "step" or "section", {1} = the element name |
| `questionDialog.title.new` | New question | Question dialog · dialog title | 30 |  |
| `questionDialog.title.edit` | Edit question | Question dialog · dialog title | 30 |  |
| `questionDialog.type` | Type | Question dialog · field label | 20 |  |
| `questionDialog.text` | Question text | Question dialog · field label | 25 |  |
| `questionDialog.shortText` | Short text | Question dialog · field label | 20 |  |
| `questionDialog.toolTip` | Tooltip | Question dialog · field label | 20 |  |
| `questionDialog.required` | Required | Question dialog · checkbox label | 20 |  |
| `questionDialog.minimum` | Minimum | Question dialog · field label | 20 |  |
| `questionDialog.maximum` | Maximum | Question dialog · field label | 20 |  |
| `questionDialog.minimumLength` | Minimum length | Question dialog · field label, shown instead of Minimum for text types where the bound counts characters | 20 |  |
| `questionDialog.maximumLength` | Maximum length | Question dialog · field label, shown instead of Maximum for text types where the bound counts characters | 20 |  |
| `questionDialog.validationText` | Validation message | Question dialog · field label | 25 |  |
| `questionDialog.selectGroup` | List of options | Question dialog · field label | 25 |  |
| `questionDialog.newList` | New list… | Question dialog · small button next to the list picker | 15 |  |
| `questionDialog.mask` | Input mask | Question dialog · field label | 20 |  |
| `questionDialog.placeholder` | Placeholder | Question dialog · field label | 20 |  |
| `questionDialog.defaultValue` | Default value | Question dialog · field label | 20 |  |
| `questionDialog.sample` | Sample answer | Question dialog · field label | 20 |  |
| `questionDialog.variant` | Variant | Question dialog · field label | 20 |  |
| `questionDialog.shortText.helper` | Shown in the tree and on the board | Question dialog · field helper text | 60 |  |
| `questionDialog.sample.helper` | Previews the texts that Text rules fill with this answer | Question dialog · field helper text | 80 |  |
| `questionDialog.variant.helper` | Vaadin theme variants applied to the field | Question dialog · field helper text | 60 |  |
| `questionDialog.help.type` | The kind of input the respondent uses to answer. | Question dialog · field tooltip | 80 |  |
| `questionDialog.help.shortText` | A short machine-friendly name, shown in the tree and on the board. | Question dialog · field tooltip | 100 |  |
| `questionDialog.help.text` | The question as the respondent sees it. May contain {<TOKEN>} placeholders that TEXT rules fill in. | Question dialog · field tooltip; {<TOKEN>} is a literal placeholder example, keep it | 140 |  |
| `questionDialog.help.toolTip` | Help text shown to the respondent when they hover the field. | Question dialog · field tooltip | 100 |  |
| `questionDialog.help.required` | The respondent must answer this before continuing. | Question dialog · field tooltip | 80 |  |
| `questionDialog.help.minimum` | The smallest number the respondent may enter. | Question dialog · field tooltip | 80 |  |
| `questionDialog.help.maximum` | The largest number the respondent may enter. | Question dialog · field tooltip | 80 |  |
| `questionDialog.help.minimumLength` | The fewest characters the respondent may type. Set a maximum too: the length is checked only when both are given. | Question dialog · field tooltip | 120 |  |
| `questionDialog.help.maximumLength` | The most characters the respondent may type. Set a minimum too: the length is checked only when both are given. | Question dialog · field tooltip | 120 |  |
| `questionDialog.help.selectGroup` | The list of choices the respondent picks from. | Question dialog · field tooltip | 80 |  |
| `questionDialog.help.mask` | A regular expression matched against every character the respondent types; anything that does not match cannot be entered. It limits which characters are allowed, not the shape of the answer. Digits only: [0-9]. Phone number: [0-9 ()+-]. Letters and spaces: [A-Za-z ]. | Question dialog · field tooltip; the bracketed patterns are literal regular expressions, keep them as they are | 320 |  |
| `questionDialog.help.placeholder` | Faint example text shown in the empty field. | Question dialog · field tooltip | 80 |  |
| `questionDialog.help.defaultValue` | The value the field starts with. | Question dialog · field tooltip | 60 |  |
| `questionDialog.help.sample` | An example answer, used only in previews. Wherever a Text rule writes this question's answer into another text, the preview shows this instead: sample "Bob" previews "Hello {<PNAME>}" as "Hello Bob". Respondents never see it. | Question dialog · field tooltip; "Bob", "Hello {<PNAME>}" and "Hello Bob" are literal examples | 300 |  |
| `questionDialog.help.variant` | Visual style variants applied to the field: alignment, size, or layout. | Question dialog · field tooltip | 100 |  |
| `questionDialog.error.typeRequired` | Choose a question type | Question dialog · field error | 40 |  |
| `questionDialog.error.textRequired` | A question needs its text | Question dialog · field error | 40 |  |
| `questionDialog.preview` | Preview | Question dialog · heading of the live preview box | 20 |  |
| `questionDialog.preview.chooseType` | Choose a type to preview the field. | Question dialog · placeholder text in the preview box | 60 |  |
| `questionDialog.saved.created` | Question created | Question dialog · notification | 40 |  |
| `questionDialog.saved.updated` | Question updated | Question dialog · notification | 40 |  |
| `relationshipDialog.title.new` | New rule | Rule dialog · dialog title | 30 |  |
| `relationshipDialog.title.edit` | Edit rule | Rule dialog · dialog title | 30 |  |
| `relationshipDialog.upstream` | Reads the answer to | Rule dialog · field label (the question whose answer is checked) | 30 |  |
| `relationshipDialog.operator` | Operator | Rule dialog · field label | 20 |  |
| `relationshipDialog.value` | Value | Rule dialog · field label | 20 |  |
| `relationshipDialog.action` | Action | Rule dialog · field label | 20 |  |
| `relationshipDialog.targetKind` | Target | Rule dialog · radio group label | 20 |  |
| `relationshipDialog.target` | Target element | Rule dialog · field label | 25 |  |
| `relationshipDialog.token` | Token | Rule dialog · field label (a text placeholder name) | 20 | identical |
| `relationshipDialog.description` | Description | Rule dialog · field label | 20 |  |
| `relationshipDialog.defaultUpstreamValue` | Default upstream value | Rule dialog · field label | 30 |  |
| `relationshipDialog.overrideUpstreamValue` | Override upstream value | Rule dialog · field label | 30 |  |
| `relationshipDialog.removeRule` | Remove rule | Rule dialog · button | 20 |  |
| `relationshipDialog.token.helper` | Optional for Show and Repeat, required for Text. Fills {<TOKEN>\|default} — or a phrase holding it, {Has <TOKEN> been\|Were you} — in the target's text and in every text inside it | Rule dialog · field helper text; {<TOKEN>\|default} and {Has <TOKEN> been\|Were you} are literal examples, keep them | 180 |  |
| `relationshipDialog.token.helper.displayOnly` | This question shows text and collects no answer, so only the rule's own default upstream value can fill the token. Read from a question the respondent answers to fill it with their answer | Rule dialog · field helper text shown when the chosen source question is display-only (HTML or MODAL) | 200 |  |
| `relationshipDialog.operator.helper.perItem` | The upstream question is answered by selecting items, so the downstream target repeats once for each item selected and the token is filled with that item's text. Use FIELD_EXIST | Rule dialog · helper text under the operator field, shown when a Repeat rule reads a multi-select or checkbox-group question; FIELD_EXIST is an operator name as the dropdown shows it, keep it | 260 |  |
| `relationshipDialog.targetKind.step` | Step | Rule dialog · radio option | 15 |  |
| `relationshipDialog.targetKind.section` | Section | Rule dialog · radio option | 15 |  |
| `relationshipDialog.targetKind.question` | Question | Rule dialog · radio option | 15 |  |
| `relationshipDialog.help.upstream` | The question whose answer this rule checks. | Rule dialog · field tooltip | 80 |  |
| `relationshipDialog.help.operator` | How the answer is compared to the value — equals, greater than, contains, or just whether an answer exists. | Rule dialog · field tooltip | 140 |  |
| `relationshipDialog.help.value` | The value the answer is compared against. Leave empty for operators that only test whether an answer exists. | Rule dialog · field tooltip | 140 |  |
| `relationshipDialog.help.action` | What the rule does when it matches: SHOW reveals the target, REPEAT repeats it as many times as the answer says, or once for each item selected when the answer is a selection from a list, TEXT substitutes the answer into the target's text. A SHOW or REPEAT rule may also carry a token naming the slot the upstream answer fills in the target's texts; a TEXT rule must. | Rule dialog · field tooltip; SHOW, REPEAT and TEXT are action names, keep them | 420 |  |
| `relationshipDialog.help.targetKind` | Whether the rule acts on a whole step, a section, or a single question. A rule with a token on a step fills it in every section and question of that step; on a section, in every question of it. | Rule dialog · field tooltip | 220 |  |
| `relationshipDialog.help.target` | The element the action applies to. | Rule dialog · field tooltip | 60 |  |
| `relationshipDialog.help.token` | The slot written as {<TOKEN>} in the target's texts that the upstream answer fills. Any rule may carry one — a SHOW or REPEAT rule that also names a token fills it, as the runtime does — and a TEXT rule must. Pick one the survey already uses or type a new name (up to 10 characters). The same token may be filled by several rules, one per place it is used. The preview shows the upstream question's sample answer in its place. | Rule dialog · field tooltip; TEXT is an action name and {<TOKEN>} a literal example, keep them | 320 |  |
| `relationshipDialog.help.description` | An optional note for authors. It is not shown to respondents. | Rule dialog · field tooltip | 80 |  |
| `relationshipDialog.help.defaultUpstreamValue` | The value to assume for the upstream answer when the respondent has not answered it yet. | Rule dialog · field tooltip | 120 |  |
| `relationshipDialog.help.overrideUpstreamValue` | A fixed value to use for the upstream answer instead of the respondent's actual answer. | Rule dialog · field tooltip | 120 |  |
| `relationshipDialog.advanced` | Advanced | Rule dialog · collapsible section title | 20 |  |
| `relationshipDialog.saved.created` | Rule created | Rule dialog · notification | 40 |  |
| `relationshipDialog.saved.updated` | Rule updated | Rule dialog · notification | 40 |  |
| `relationshipDialog.removed` | Rule removed | Rule dialog · notification | 40 |  |
| `relationshipDialog.sentence` | Question ''{0}'' {1}{2} → {3}{4} ''{5}'' | Rule dialog · the rule read back as one sentence | 120 | params: {0} = upstream question label, {1} = operator name, {2} = compared value (empty or leading space), {3} = action name, {4} = token (empty or leading space), {5} = target label |
| `relationshipDialog.sentence.noOperator` | (operator) | Rule dialog · placeholder inside the sentence when no operator is chosen | 20 |  |
| `relationshipDialog.sentence.noAction` | (action) | Rule dialog · placeholder inside the sentence when no action is chosen | 20 |  |
| `selectGroupDialog.title.new` | New list of options | Options-list dialog · dialog title | 30 |  |
| `selectGroupDialog.title.edit` | Edit list of options | Options-list dialog · dialog title | 30 |  |
| `selectGroupDialog.name` | Name | Options-list dialog · field label | 20 |  |
| `selectGroupDialog.description` | Description | Options-list dialog · field label | 20 |  |
| `selectGroupDialog.dataType` | Data type | Options-list dialog · field label | 20 |  |
| `selectGroupDialog.addOption` | Add option | Options-list dialog · button | 20 |  |
| `selectGroupDialog.dataType.helper` | Informational; 'Text' unless the site's analysis expects otherwise | Options-list dialog · field helper text; 'Text' is a stored value, keep it | 100 |  |
| `selectGroupDialog.help.name` | The list's name, shown when picking options for a question. Unique within the survey. | Options-list dialog · field tooltip | 120 |  |
| `selectGroupDialog.help.description` | An optional note for authors. | Options-list dialog · field tooltip | 60 |  |
| `selectGroupDialog.help.dataType` | A label carried with the list; the Survey runtime does not interpret it. | Options-list dialog · field tooltip | 100 |  |
| `selectGroupDialog.error.nameRequired` | Name is required | Options-list dialog · field error | 30 |  |
| `selectGroupDialog.option.displayText` | Display text | Options-list dialog · option row placeholder | 20 |  |
| `selectGroupDialog.option.displayText.aria` | Option display text | Options-list dialog · option row accessible name | 30 |  |
| `selectGroupDialog.option.codedValue` | Coded value (defaults to the text) | Options-list dialog · option row placeholder | 40 |  |
| `selectGroupDialog.option.codedValue.aria` | Option coded value | Options-list dialog · option row accessible name | 30 |  |
| `selectGroupDialog.option.moveUp` | Move option up | Options-list dialog · icon button accessible name | 20 |  |
| `selectGroupDialog.option.moveDown` | Move option down | Options-list dialog · icon button accessible name | 20 |  |
| `selectGroupDialog.option.remove` | Remove option | Options-list dialog · icon button accessible name | 20 |  |
| `selectGroupDialog.optionsLabel` | Options, in the order respondents see them | Options-list dialog · heading above the option rows | 60 |  |
| `selectGroupDialog.noOptions` | No options yet. | Options-list dialog · hint when the list has no options | 30 |  |
| `selectGroupDialog.saved.created` | Created list ''{0}'' | Options-list dialog · notification | 40 | params: {0} = the list name |
| `selectGroupDialog.saved.updated` | Updated list ''{0}'' | Options-list dialog · notification | 40 | params: {0} = the list name |
| `mainLayout.resizer.title` | Drag to resize the drawer | Drawer resize handle · tooltip | 40 |  |
| `mainLayout.resizer.aria` | Resize drawer | Drawer resize handle · accessible name | 30 |  |
| `mainLayout.smallScreen.heading` | A bigger screen is needed | Full-screen notice on narrow screens · heading | 40 |  |
| `mainLayout.smallScreen.text` | Elicit Author is built for designing surveys on a tablet or larger screen. Please switch to a wider device. | Full-screen notice on narrow screens · paragraph | 160 |  |
| `common.none` | none | System screens · value shown when nothing is set | 15 |  |
| `common.unknown` | unknown | System screens · value shown when a brand field is missing | 15 |  |
| `mainLayout.nav.system` | System | Navigation drawer · section heading for setup and diagnostics pages | 20 |  |
| `mainLayout.nav.systemOverview` | Overview | Navigation drawer · menu item | 25 |  |
| `mainLayout.nav.systemDatabase` | Database | Navigation drawer · menu item | 25 |  |
| `mainLayout.nav.systemBranding` | Branding | Navigation drawer · menu item | 25 |  |
| `mainLayout.nav.systemConnections` | Connections | Navigation drawer · menu item | 25 |  |
| `mainLayout.nav.systemOidc` | OIDC | Navigation drawer · menu item (the sign-in diagnostics page; OIDC is a protocol name) | 25 | identical |
| `system.grid.item` | Item | System pages · grid column header (the name of a row) | 20 |  |
| `system.grid.value` | Value | System pages · grid column header | 20 |  |
| `system.grid.state` | State | System pages · grid column header (a status badge) | 20 |  |
| `system.grid.detail` | Detail | System pages · grid column header | 20 |  |
| `system.badge.ok` | OK | System pages · status badge for a passed check | 12 | identical |
| `system.badge.failed` | Failed | System pages · status badge for a failed check | 12 |  |
| `system.badge.notChecked` | Not checked | System pages · status badge for a check not yet run | 15 |  |
| `system.badge.present` | Present | System pages · status badge for a setting that is set | 12 |  |
| `system.badge.absent` | Absent | System pages · status badge for a setting that is missing | 12 |  |
| `system.milliseconds` | {0} ms | System pages · duration in milliseconds | 12 | params: {0} = number; identical where "ms" is the usual abbreviation |
| `systemOverviewView.pageTitle` | System Overview | System overview · browser tab title | 40 |  |
| `systemOverviewView.title` | System Overview | System overview · page heading | 40 |  |
| `systemOverviewView.intro` | What this deployment is running, whether it is healthy, and whether every required setting is present. Settings are shown as present or absent, never by value; they are startup configuration and cannot be changed here. | System overview · introductory paragraph | 300 |  |
| `systemOverviewView.running` | Running | System overview · section heading (what is running) | 30 |  |
| `systemOverviewView.row.application` | Application | System overview · row label (application name) | 25 |  |
| `systemOverviewView.row.version` | Version | System overview · row label | 25 |  |
| `systemOverviewView.row.built` | Built | System overview · row label (build time) | 25 |  |
| `systemOverviewView.row.profile` | Profile | System overview · row label (runtime profile) | 25 |  |
| `systemOverviewView.row.started` | Started | System overview · row label (start time) | 25 |  |
| `systemOverviewView.row.uptime` | Uptime | System overview · row label | 25 |  |
| `systemOverviewView.uptime.days` | {0} d {1} h {2} min | System overview · uptime value | 30 | params: {0} = days, {1} = hours, {2} = minutes; identical where the abbreviations are the same |
| `systemOverviewView.uptime.hours` | {0} h {1} min | System overview · uptime value | 30 | params: {0} = hours, {1} = minutes; identical where the abbreviations are the same |
| `systemOverviewView.uptime.minutes` | {0} min {1} s | System overview · uptime value | 30 | params: {0} = minutes, {1} = seconds; identical where the abbreviations are the same |
| `systemOverviewView.health` | Health | System overview · section heading | 30 |  |
| `systemOverviewView.area.database` | Database connection | System overview · health row, link to the Database page | 40 |  |
| `systemOverviewView.area.surveySchema` | Survey schema | System overview · health row, link to the Database page | 40 |  |
| `systemOverviewView.area.surveySchema.notChecked` | not checked: the connection failed | System overview · health detail when the database connection failed | 60 |  |
| `systemOverviewView.area.branding` | Branding | System overview · health row, link to the Branding page | 40 |  |
| `systemOverviewView.area.connections` | Connections | System overview · health row, link to the Connections page | 40 |  |
| `systemOverviewView.connectionsSummary.one` | {0} outbound target configured; open Connections to check it | System overview · health detail when exactly one outbound target is configured | 80 | params: {0} = the number 1 |
| `systemOverviewView.connectionsSummary.many` | {0} outbound targets configured; open Connections to check them | System overview · health detail for several (or zero) outbound targets | 80 | params: {0} = count |
| `systemOverviewView.grid.area` | Area | System overview · grid column header | 20 |  |
| `systemOverviewView.requiredSettings` | Required settings | System overview · section heading | 30 |  |
| `systemOverviewView.grid.setting` | Setting | System overview · grid column header (property name) | 20 |  |
| `systemOverviewView.grid.suppliedBy` | Supplied by | System overview · grid column header (environment variable) | 20 |  |
| `systemOverviewView.grid.purpose` | Purpose | System overview · grid column header | 20 |  |
| `systemOverviewView.setting.datasourcePassword` | password of the database user Author connects as | System overview · purpose of a required setting | 60 |  |
| `systemOverviewView.setting.oidcSecret` | client secret registered with the identity provider | System overview · purpose of a required setting | 60 |  |
| `systemOverviewView.setting.serviceToken` | service token Author sends to the preview Survey | System overview · purpose of a required setting; the service token is the secret Elicit's applications authenticate each other with | 70 |  |
| `systemOverviewView.serviceToken.developmentValue` | {0} is the development value. Replace it before this Author is used at a real site. | System overview · warning below the required settings | 140 | params: {0} = environment variable name |
| `systemDatabaseView.pageTitle` | System Database | System database · browser tab title | 40 |  |
| `systemDatabaseView.title` | Database | System database · page heading | 30 |  |
| `systemDatabaseView.intro` | Author owns no migrations. The survey schema in its database is created by the Survey preview instance that shares the database, so a missing schema means that instance has not started against this database yet. | System database · introductory paragraph | 300 |  |
| `systemDatabaseView.connection` | Connection | System database · section heading | 30 |  |
| `systemDatabaseView.row.state` | State | System database · row label | 25 |  |
| `systemDatabaseView.row.configuredUser` | Configured user | System database · row label (database user from configuration) | 25 |  |
| `systemDatabaseView.row.connectedAs` | Connected as | System database · row label (database user reported by the server) | 25 |  |
| `systemDatabaseView.row.database` | Database | System database · row label (database name) | 25 |  |
| `systemDatabaseView.row.server` | Server | System database · row label (server version) | 25 |  |
| `systemDatabaseView.row.roundTrip` | Round trip | System database · row label (probe duration) | 25 |  |
| `systemDatabaseView.surveySchema` | Survey schema | System database · section heading | 30 |  |
| `systemDatabaseView.row.schemaPresent` | Schema present | System database · row label | 40 |  |
| `systemDatabaseView.row.surveyMigration` | Survey migration (survey.flyway_history) | System database · row label; keep the table name in parentheses | 50 |  |
| `systemDatabaseView.migration.notInstalled` | not installed | System database · migration value when the history table is missing | 20 |  |
| `systemDatabaseView.migration.empty` | empty | System database · migration value when the history table has no rows | 20 |  |
| `systemDatabaseView.migration.applied` | {0} applied {1} | System database · migration value | 60 | params: {0} = version, {1} = date applied |
| `systemDatabaseView.migration.failed` | {0} FAILED {1} | System database · migration value for a failed migration | 60 | params: {0} = version, {1} = date attempted |
| `systemDatabaseView.migration.unreadable` | present, but not readable by {0}: {1} | System database · migration value when the table cannot be read | 120 | params: {0} = database user, {1} = technical error text |
| `systemDatabaseView.row.durableSequences` | Kimball durable-key sequences | System database · row label; Kimball is a data-warehouse design name | 40 |  |
| `systemDatabaseView.row.surveysAuthored` | Surveys authored | System database · row label | 40 |  |
| `systemDatabaseView.schema.exists` | survey.surveys exists | System database · detail of a passed check; keep the table name | 40 |  |
| `systemDatabaseView.schema.missing` | survey.surveys does not exist: the Survey preview instance has not created the schema in this database yet | System database · detail of a failed check; keep the table name | 160 |  |
| `systemDatabaseView.durableSequences.exists` | survey.{0} exists | System database · detail of a passed check | 60 | params: {0} = sequence name; keep the survey. prefix |
| `systemDatabaseView.durableSequences.missing` | survey.{0} is missing: the schema in this database predates the V3 migrations | System database · detail of a failed check | 120 | params: {0} = sequence name; keep the survey. prefix |
| `systemBrandingView.pageTitle` | System Branding | System branding · browser tab title | 40 |  |
| `systemBrandingView.title` | Branding | System branding · page heading | 30 |  |
| `systemBrandingView.intro` | The brand resolves from the mounted directory first, then the local directory, then the default packaged with the application. Each asset is listed on its own, because a partial mount renders with the wrong fonts and no error. | System branding · introductory paragraph | 300 |  |
| `systemBrandingView.reload` | Reload brand | System branding · button | 20 |  |
| `systemBrandingView.reloaded` | Brand cache discarded; the result below is freshly resolved. | System branding · notification after reloading | 80 |  |
| `systemBrandingView.resolution` | Resolution | System branding · section heading (how the brand was found) | 30 |  |
| `systemBrandingView.row.configuredPath` | Configured path (brand.file.system.path) | System branding · row label; keep the property name in parentheses | 50 |  |
| `systemBrandingView.row.localPath` | Local path (brand.local.path) | System branding · row label; keep the property name in parentheses | 50 |  |
| `systemBrandingView.path.exists` | {0} (exists) | System branding · path value | 100 | params: {0} = directory path |
| `systemBrandingView.path.notFound` | {0} (not found) | System branding · path value | 100 | params: {0} = directory path |
| `systemBrandingView.row.metadataFile` | Metadata file | System branding · row label | 30 |  |
| `systemBrandingView.row.brandName` | Brand name | System branding · row label | 30 |  |
| `systemBrandingView.row.organization` | Organization | System branding · row label | 30 |  |
| `systemBrandingView.row.brandVersion` | Brand version | System branding · row label | 30 |  |
| `systemBrandingView.row.inUse` | In use (cached) | System branding · row label (the brand currently applied) | 30 |  |
| `systemBrandingView.inUse` | {0} [{1}] | System branding · value | 60 | params: {0} = brand display name, {1} = brand key; identical (punctuation only) |
| `systemBrandingView.row.summary` | Summary | System branding · row label | 30 |  |
| `systemBrandingView.summary.mounted` | Mounted brand at {0} | System branding · summary line | 80 | params: {0} = directory path |
| `systemBrandingView.summary.mountedNamed` | Mounted brand: {0} at {1} | System branding · summary line | 80 | params: {0} = brand name, {1} = directory path |
| `systemBrandingView.summary.local` | Local brand directory {0} | System branding · summary line | 80 | params: {0} = directory path |
| `systemBrandingView.summary.embedded` | Embedded default theme; no brand directory at {0} | System branding · summary line | 80 | params: {0} = directory path |
| `systemBrandingView.assets` | Assets | System branding · section heading | 30 |  |
| `systemBrandingView.grid.asset` | Asset | System branding · grid column header | 20 |  |
| `systemBrandingView.grid.path` | Path | System branding · grid column header | 20 |  |
| `systemBrandingView.grid.source` | Source | System branding · grid column header (where the asset came from) | 20 |  |
| `systemBrandingView.grid.location` | Location | System branding · grid column header | 20 |  |
| `systemBrandingView.source.mounted` | Mounted | System branding · source badge (from the mounted directory) | 15 |  |
| `systemBrandingView.source.local` | Local | System branding · source badge (from the local directory) | 15 | identical in Romance languages |
| `systemBrandingView.source.embedded` | Embedded default | System branding · source badge (packaged default) | 20 |  |
| `systemBrandingView.source.absent` | Absent | System branding · source badge | 15 |  |
| `systemBrandingView.source.unreadable` | Unreadable | System branding · source badge | 15 |  |
| `systemBrandingView.asset.colorStylesheet` | color stylesheet | System branding · what an asset is for | 30 |  |
| `systemBrandingView.asset.typographyStylesheet` | typography stylesheet | System branding · what an asset is for | 30 |  |
| `systemBrandingView.asset.themeStylesheet` | theme stylesheet | System branding · what an asset is for | 30 |  |
| `systemBrandingView.asset.horizontalLogo` | horizontal logo | System branding · what an asset is for | 30 |  |
| `systemBrandingView.asset.headerIcon` | header icon | System branding · what an asset is for | 30 |  |
| `systemBrandingView.asset.favicon` | favicon | System branding · what an asset is for (the browser tab icon) | 30 | identical |
| `systemBrandingView.asset.notFound` | not found in any location | System branding · asset location when nothing was found | 40 |  |
| `systemBrandingView.asset.unreadable` | {0} exists but cannot be read | System branding · asset location when the file cannot be read | 80 | params: {0} = file path |
| `systemBrandingView.logoPreview` | Logo preview | System branding · section heading | 30 |  |
| `systemBrandingView.logoAlt` | Brand logo | System branding · logo image alternative text | 30 |  |
| `systemConnectionsView.pageTitle` | System Connections | System connections · browser tab title | 40 |  |
| `systemConnectionsView.title` | Connections | System connections · page heading | 30 |  |
| `systemConnectionsView.intro` | Each outbound dependency Author will call, read from the same settings the runtime uses. A check sends one read-only request and gives up after {0} seconds. Author''s only other dependency is the database, checked on its own page. | System connections · introductory paragraph | 300 | params: {0} = timeout in seconds; double any apostrophe |
| `systemConnectionsView.empty` | No outbound targets are configured. | System connections · text when there is nothing to check | 60 |  |
| `systemConnectionsView.checkAll` | Check all | System connections · button | 15 |  |
| `systemConnectionsView.check` | Check | System connections · button in a grid row | 12 |  |
| `systemConnectionsView.grid.dependency` | Dependency | System connections · grid column header (kind of dependency) | 20 |  |
| `systemConnectionsView.grid.source` | Source | System connections · grid column header (where the address comes from) | 20 |  |
| `systemConnectionsView.grid.address` | Address | System connections · grid column header | 20 |  |
| `systemConnectionsView.grid.time` | Time | System connections · grid column header (probe duration) | 20 |  |
| `systemConnectionsView.group.identityProvider` | Identity provider | System connections · kind of dependency | 25 |  |
| `systemConnectionsView.group.telemetryCollector` | Telemetry collector | System connections · kind of dependency | 25 |  |
| `systemConnectionsView.result.timedOut` | timed out after {0} s | System connections · check detail | 40 | params: {0} = seconds |
| `systemConnectionsView.result.interrupted` | interrupted | System connections · check detail | 20 |  |
| `systemConnectionsView.result.discoveryServed` | discovery document served at {0} | System connections · check detail | 80 | params: {0} = address |
| `systemConnectionsView.result.notDiscovery` | HTTP {0} from {1} is not an OIDC discovery document; check the realm address | System connections · check detail; OIDC is a protocol name | 120 | params: {0} = HTTP status code, {1} = address |
| `systemConnectionsView.result.reachable` | reachable, HTTP {0} | System connections · check detail | 40 | params: {0} = HTTP status code |
| `systemConnectionsView.result.noHostPort` | address {0} has no host and port | System connections · check detail | 60 | params: {0} = address |
| `systemConnectionsView.result.connected` | connected to {0}:{1} | System connections · check detail | 40 | params: {0} = host, {1} = port |
| `oidcDiagnosticsView.pageTitle` | OIDC Diagnostics | OIDC diagnostics page · browser tab title | 40 |  |
| `oidcDiagnosticsView.title` | OIDC | OIDC diagnostics page · page heading; OIDC is a protocol name | 20 | identical |
| `oidcDiagnosticsView.intro` | The identity the provider handed this session: who signed in, which roles arrived, and whether the authoring role is among them. Tokens are masked. | OIDC diagnostics page · introductory paragraph | 200 |  |
| `oidcDiagnosticsView.user` | User: {0} | OIDC diagnostics page · label in a monospace block | 40 | params: {0} = user name |
| `oidcDiagnosticsView.isAnonymous` | Is Anonymous: {0} | OIDC diagnostics page · label in a monospace block | 40 | params: {0} = true or false |
| `oidcDiagnosticsView.roles` | Roles: {0} | OIDC diagnostics page · label in a monospace block | 40 | params: {0} = list of role names; identical in Romance languages |
| `oidcDiagnosticsView.roleSource` | Role Source: {0} | OIDC diagnostics page · label in a monospace block (where the roles were read from) | 40 | params: {0} = source name |
| `oidcDiagnosticsView.hasRole` | Has {0}: {1} | OIDC diagnostics page · label in a monospace block | 40 | params: {0} = role name, {1} = true or false |
| `oidcDiagnosticsView.issuer` | Issuer: {0} | OIDC diagnostics page · label in a monospace block | 40 | params: {0} = issuer address |
| `oidcDiagnosticsView.email` | Email: {0} | OIDC diagnostics page · label in a monospace block | 40 | params: {0} = email address |
| `oidcDiagnosticsView.idToken` | ID Token: {0} | OIDC diagnostics page · label in a monospace block | 40 | params: {0} = masked token |
| `oidcDiagnosticsView.idTokenUnavailable` | ID Token: Not available or resolvable | OIDC diagnostics page · label in a monospace block | 60 |  |
| `oidcDiagnosticsView.idTokenError` | ID Token Error: {0} | OIDC diagnostics page · label in a monospace block | 60 | params: {0} = technical error text |
| `oidcDiagnosticsView.accessToken` | Access Token: {0} | OIDC diagnostics page · label in a monospace block | 40 | params: {0} = masked token |
| `oidcDiagnosticsView.accessTokenUnavailable` | Access Token: Not available or resolvable | OIDC diagnostics page · label in a monospace block | 60 |  |
| `oidcDiagnosticsView.accessTokenError` | Access Token Error: {0} | OIDC diagnostics page · label in a monospace block | 60 | params: {0} = technical error text |
| `surveyEditorView.translations` | Translations… | Survey overview · toolbar button opening the translations page | 20 |  |
| `surveyMetadataDialog.baseLanguage` | Content language | Survey details dialog · field label: the language the content is written in | 25 |  |
| `surveyMetadataDialog.baseLanguage.helper` | The language this survey's questions are written in. | Survey details dialog · helper text under that field | 80 |  |
| `surveyMetadataDialog.contentLanguages` | Published translations | Survey details dialog · field label: the languages the survey is published in | 25 |  |
| `surveyMetadataDialog.contentLanguages.helper` | The languages this survey is published in. Elicit carries these; contact ElicitSoftware for another and it arrives in the next release. | Survey details dialog · helper text under the published-languages picker; "ElicitSoftware" is a company name and stays as it is | 200 |  |
| `surveyMetadataDialog.contentLanguages.option` | {0} — {1} | Survey details dialog · published-languages picker item: the tag then the language name | 40 | params: {0} = language tag, {1} = language name; identical (punctuation only) |
| `translationsView.pageTitle` | Translations | Translations page · browser tab title | 20 |  |
| `translationsView.heading` | Translations — {0} | Translations page · heading | 40 | params: {0} = the survey's title, untranslated |
| `translationsView.backToOverview` | Back to the survey overview | Translations page · tooltip on the back button | 40 |  |
| `translationsView.intro` | Every string a respondent reads, beside its translation. A string with no translation, or one whose wording changed after it was translated, is shown to respondents in the survey's own language. | Translations page · introductory paragraph | 250 |  |
| `translationsView.language` | Language | Translations page · label of the language selector | 15 |  |
| `translationsView.language.option` | {0} — {1} | Translations page · language picker item: the tag then the language name | 40 | params: {0} = language tag, {1} = language name; identical (punctuation only) |
| `translationsView.language.option.new` | {0} — {1} · not published yet | Translations page · language picker item for a language the survey does not publish yet | 60 | params: {0} = language tag, {1} = language name |
| `translationsView.language.option.unsupported` | {0} — {1} · not carried by Elicit | Translations page · language picker item for a language the survey publishes but Elicit has no application text for | 60 | params: {0} = language tag, {1} = language name |
| `translationsView.language.placeholder` | Choose a language | Translations page · language picker placeholder | 30 |  |
| `translationsView.language.helper` | Elicit carries these languages. Need another? Contact ElicitSoftware and it arrives in the next release. | Translations page · helper text under the language picker; "ElicitSoftware" is a company name and stays as it is | 120 |  |
| `translationsView.language.published` | {0} is now one of this survey''s published languages; {1} string(s) still need translating. | Translations page · notification after a language is added to the published set | 110 | params: {0} = language tag, {1} = how many strings are still untranslated |
| `translationsView.onlyOutstanding` | Only what needs work | Translations page · checkbox filtering to missing and out-of-date rows | 25 |  |
| `translationsView.counts` | {0} translated, {1} missing, {2} out of date | Translations page · summary line | 60 | params: {0} = translated, {1} = missing, {2} = out of date |
| `translationsView.request` | Request translation | Translations page · button producing the file for a translator | 25 |  |
| `translationsView.import` | Import a returned file | Translations page · upload button for the returned file | 25 |  |
| `translationsView.noLanguages` | This survey publishes no translations yet. Choose a language to start one. | Translations page · shown in place of the counts when no language is chosen yet | 80 |  |
| `translationsView.grid.element` | Where | Translations page · grid column header: which step, section or question | 12 |  |
| `translationsView.grid.field` | Text | Translations page · grid column header: which text of it | 12 |  |
| `translationsView.grid.baseText` | Original | Translations page · grid column header: the text as authored | 12 | identical in Spanish |
| `translationsView.grid.translation` | Translation | Translations page · grid column header: the editable translation | 15 |  |
| `translationsView.grid.status` | Status | Translations page · grid column header | 12 |  |
| `translationsView.status.translated` | Translated | Translations page · status badge | 12 |  |
| `translationsView.status.missing` | Missing | Translations page · status badge | 12 |  |
| `translationsView.status.stale` | Out of date | Translations page · status badge: the wording changed after translation | 15 |  |
| `translationsView.status.staleTooltip` | Translated from: {0} | Translations page · tooltip on the out-of-date badge | 40 | params: {0} = the wording it was translated from |
| `translationImport.title` | Translations imported | Import result dialog · title | 30 |  |
| `translationImport.counts` | {0} imported, {1} unchanged, {2} left untranslated, {3} rejected. | Import result dialog · summary line | 80 | params: {0} = imported, {1} = unchanged, {2} = skipped, {3} = rejected |
| `translationImport.stale` | {0} of them were translated from wording that has since changed, so they will not be shown until they are translated again. | Import result dialog · note under the summary | 150 | params: {0} = how many |
| `translationImport.rejected` | These were not imported: | Import result dialog · heading above the list of rejected items | 40 |  |
| `translationImport.warnings` | Worth checking: | Import result dialog · heading above the list of warnings | 40 |  |
| `designer.staleTranslations` | The {0} changed. Its {1} translation(s) ({2}) are now out of date and will not be shown to respondents until they are translated again. | Designer · notification after a wording change | 180 | params: {0} = what changed (question, step name, ...), {1} = how many languages, {2} = the language tags |
| `designer.staleTranslations.open` | Open translations | Designer · button in that notification | 20 |  |
| `error.name.required` | Name is required | Error message · a name field was left empty | 60 |  |
| `error.name.tooLong` | Name must be 255 characters or fewer | Error message · a name is too long | 80 |  |
| `error.name.singleLine` | Name must be a single line of text | Error message · a name holds a line break, a tab or another control character | 80 |  |
| `error.list.gone` | The list no longer exists | Error message · the list of options was removed meanwhile | 60 |  |
| `error.list.descriptionTooLong` | Description must be 255 characters or fewer | Error message · list of options dialog | 90 |  |
| `error.list.dataTypeTooLong` | Data type must be 50 characters or fewer | Error message · list of options dialog | 90 |  |
| `error.list.optionTextRequired` | Every option needs its display text | Error message · list of options dialog, under the options | 80 |  |
| `error.list.optionTextTooLong` | Option text must be 255 characters or fewer | Error message · list of options dialog, under the options | 90 |  |
| `error.list.optionTwice` | Option ''{0}'' appears twice | Error message · list of options dialog, under the options | 90 | params: {0} = option text |
| `error.list.codedValueTooLong` | Coded value must be 255 characters or fewer | Error message · list of options dialog, under the options | 90 |  |
| `error.survey.gone` | Survey {0} no longer exists | Error message · the survey was deleted meanwhile | 80 | params: {0} = survey id |
| `error.survey.delete.previewUnreachable` | The survey was not deleted: its reporting schema could not be dropped because the preview Survey at {0} could not be reached ({1}). Start it and try again. | Error message · Delete survey dialog; the preview Survey (a service) was down | 200 | params: {0} = address, {1} = technical reason |
| `error.survey.delete.previewRefused` | The survey was not deleted: the preview Survey did not drop its reporting schema (answered {0}: {1}). | Error message · Delete survey dialog; the preview Survey answered but refused | 160 | params: {0} = HTTP status code, {1} = its answer |
| `error.survey.delete.serviceTokenMissing` | The survey was not deleted: service token not configured. Set {0} for Author and the preview Survey, then try again. | Error message · Delete survey dialog; Author was started without its service token | 200 | params: {0} = environment variable name |
| `error.survey.delete.previewUnauthorized` | The survey was not deleted: the preview Survey refused the request. Check that Author and the preview Survey are given the same service token ({0}). | Error message · Delete survey dialog; the preview Survey did not accept Author's service token | 220 | params: {0} = environment variable name |
| `error.survey.notLanguageTag` | ''{0}'' is not a language tag. Choose one of the languages Elicit carries. | Error message · survey details and Translations page | 160 | params: {0} = what was typed |
| `error.survey.baseLanguage` | {0} is the language this survey is written in; it needs no translation. | Error message · Translations page, publishing the survey own language | 160 | params: {0} = language tag |
| `error.survey.languagePublished` | {0} is already one of this survey''s published languages. | Error message · Translations page | 140 | params: {0} = language tag |
| `error.survey.languagesFull` | This survey already publishes as many languages as it can hold. | Error message · Translations page | 140 |  |
| `error.survey.languageUnsupported` | {0} is not one of the languages Elicit carries, so no site could show it. Contact ElicitSoftware to have it translated; it becomes available in the next release. | Error message · survey details and Translations page; ElicitSoftware stays | 320 | params: {0} = language tag |
| `error.survey.exportNeedsAuthor` | An export must be attributed to a signed-in author | Error message · export dialog | 140 |  |
| `error.survey.titleRequired` | Title is required | Error message · survey details dialog | 60 |  |
| `error.survey.titleTooLong` | Title must be 255 characters or fewer | Error message · survey details dialog | 90 |  |
| `error.survey.titleSingleLine` | Title must be a single line of text | Error message · survey details dialog; the title holds a line break, a tab or another control character | 90 |  |
| `error.survey.descriptionTooLong` | Description must be 2000 characters or fewer | Error message · survey details dialog | 90 |  |
| `error.survey.initialDisplayKeyTooLong` | Initial display key must be 255 characters or fewer | Error message · survey details dialog; same field name as surveyMetadataDialog.initialDisplayKey | 120 |  |
| `error.survey.postSurveyUrlTooLong` | Post-survey address must be 255 characters or fewer | Error message · survey details dialog; same field name as surveyMetadataDialog.postSurveyUrl | 120 |  |
| `error.designer.sectionAlreadyInStep` | Step ''{0}'' already contains this section; a section can be mounted once per step | Error message · designer, dropping a section | 200 | params: {0} = step name |
| `error.designer.questionAlreadyInSection` | Section ''{0}'' already contains this question | Error message · designer, dropping a question | 140 | params: {0} = section name |
| `error.designer.sectionAssignmentGone` | Section assignment not found | Error message · designer, the section was taken out of the step meanwhile | 90 |  |
| `error.designer.questionAssignmentGone` | Question assignment not found | Error message · designer, the question was taken out of the section meanwhile | 90 |  |
| `error.step.notFound` | Step not found | Error message · designer and element page | 60 |  |
| `error.step.goneOrRemoved` | Step not found or removed | Error message · designer | 80 |  |
| `error.section.goneOrRemoved` | Section not found or removed | Error message · designer | 80 |  |
| `error.question.goneOrRemoved` | Question not found or removed | Error message · designer | 80 |  |
| `error.designer.noListForType` | A {0} question does not take a list of options | Error message · designer, giving a list to a question that takes none | 140 | params: {0} = question type name, kept as written |
| `error.designer.noListForThisType` | A question of this type does not take a list of options | Error message · designer, giving a list to a question that takes none | 140 |  |
| `error.step.nameRequired` | A step needs a name | Error message · step dialog | 60 |  |
| `error.step.nameTooLong` | Step names are at most 255 characters | Error message · step dialog | 90 |  |
| `error.section.nameRequired` | A section needs a name | Error message · section dialog | 60 |  |
| `error.section.nameTooLong` | Section names are at most 255 characters | Error message · section dialog | 90 |  |
| `error.question.typeRequired` | Choose a question type | Error message · question dialog | 60 |  |
| `error.question.textRequired` | A question needs its text | Error message · question dialog | 60 |  |
| `error.question.listRequired` | A {0} question needs a list of options | Error message · question dialog | 140 | params: {0} = question type name, kept as written |
| `error.question.minOverMax` | The minimum cannot exceed the maximum | Error message · question dialog | 90 |  |
| `designer.stale.stepName` | step name | Designer · what changed, inserted into designer.staleTranslations as {0} | 40 |  |
| `designer.stale.sectionName` | section name | Designer · what changed, inserted into designer.staleTranslations as {0} | 40 |  |
| `designer.stale.question` | question | Designer · what changed, inserted into designer.staleTranslations as {0} | 40 |  |
| `designer.stale.shortText` | short text | Designer · what changed, inserted into designer.staleTranslations as {0} | 40 |  |
| `designer.stale.tooltip` | tooltip | Designer · what changed, inserted into designer.staleTranslations as {0} | 40 |  |
| `designer.stale.placeholder` | placeholder | Designer · what changed, inserted into designer.staleTranslations as {0} | 40 |  |
| `designer.stale.validationText` | validation message | Designer · what changed, inserted into designer.staleTranslations as {0} | 40 |  |
| `error.rule.gone` | Rule not found or removed | Error message · rule dialog and designer | 80 |  |
| `error.rule.dropOnElement` | Drop the arrow on a step, a section or a question | Error message · designer, dragging a rule arrow | 120 |  |
| `error.rule.readsFromQuestion` | A rule reads from a question | Error message · designer, dragging the upstream end of a rule arrow | 80 |  |
| `error.rule.upstreamRequired` | Choose the question the rule reads from | Error message · rule dialog, no upstream question chosen | 100 |  |
| `error.rule.selfTarget` | A rule cannot target the question it reads from | Error message · rule dialog, upstream and downstream are the same question | 120 |  |
| `error.rule.operatorRequired` | Choose an operator | Error message · rule dialog | 50 |  |
| `error.rule.actionRequired` | Choose an action | Error message · rule dialog | 50 |  |
| `error.rule.textNeedsToken` | A Text rule needs the token it replaces | Error message · rule dialog; Text is the rule action | 100 |  |
| `error.rule.tokenTooLong` | Token names are at most {0} characters | Error message · rule dialog | 100 | params: {0} = the limit |
| `error.rule.tokenInvalid` | A token name starts with a letter and holds letters, digits and underscores only, so no text could ever name ''{0}'' | Error message · rule dialog | 260 | params: {0} = the token as typed |
| `error.rule.displayOnlySource` | A {0} question collects no answer, so {1} would stay unfilled. Read from a question the respondent answers, or give the rule a default upstream value to fill the token with | Error message · rule dialog; default upstream value as in relationshipDialog.defaultUpstreamValue | 380 | params: {0} = question type name, kept as written, {1} = the token as a text writes it |
| `error.rule.displayOnlyType` | display-only | Error message · stands in for a question type name that cannot be read | 40 |  |
| `error.rule.repeatNotStep` | Repeat applies to a section or a question, not a step | Error message · rule dialog; Repeat is the rule action | 140 |  |
| `error.rule.repeatSelectionOperator` | The upstream question is answered by selecting items, so a Repeat rule repeats its downstream target once for each item selected, and {0} is never true for a selection. Choose FIELD_EXIST | Error message · rule dialog, on the operator field; Repeat is the rule action; FIELD_EXIST is an operator name as the dropdown shows it, keep it | 240 | params: {0} = the operator name as the dropdown shows it |
| `error.element.noSurvey` | An element must belong to a survey | Error message · internal guard | 80 |  |
| `error.question.notFound` | Question not found | Error message · element page | 60 |  |
| `error.section.notFound` | Section not found | Error message · element page | 60 |  |
| `error.element.notFound` | Element not found | Error message · element page | 60 |  |
| `error.option.notFound` | Option not found | Error message · list of options | 60 |  |
| `error.element.unsupportedKind` | Unsupported element kind {0} | Error message · internal guard | 80 | params: {0} = element kind |
| `element.dependent.questionInSection` | assignment of the question to section {0} | Remove and restore confirmation · list item: what else is removed or restored with a question | 120 | params: {0} = section name, already quoted |
| `element.dependent.sectionInStep` | assignment of the section to step {0} | Remove and restore confirmation · list item: what else is removed or restored with a section | 120 | params: {0} = step name, already quoted |
| `element.dependent.questionOfSection` | assignment of question {0} to the section | Remove and restore confirmation · list item: what else is removed or restored with a section | 120 | params: {0} = question name, already quoted |
| `element.dependent.sectionOfStep` | assignment of section {0} to the step | Remove and restore confirmation · list item: what else is removed or restored with a step | 120 | params: {0} = section name, already quoted |
| `element.dependent.rule` | rule ''{0}'' | Remove and restore confirmation · list item: a rule removed or restored with the element | 100 | params: {0} = rule description |
| `element.blocker.sectionRemoved` | section {0} is removed; restore it first | Remove and restore confirmation · why a question cannot be restored | 140 | params: {0} = section name, already quoted |
| `element.blocker.stepRemoved` | step {0} is removed; restore it first | Remove and restore confirmation · why a section cannot be restored | 140 | params: {0} = step name, already quoted |
| `error.element.cannotRemove` | Cannot remove {0}: {1} | Error message · element page | 300 | params: {0} = the element (a validation.where.* phrase), {1} = the reasons |
| `error.element.cannotRestore` | Cannot restore {0}: {1} | Error message · element page | 300 | params: {0} = the element (a validation.where.* phrase), {1} = the reasons |
| `error.list.inUse` | This list is still used by {0} question(s); change or remove them first | Error message · designer, removing a list of options | 180 | params: {0} = number of questions |
| `error.survey.notFound` | Survey not found | Error message · internal guard | 60 |  |
| `error.survey.exportedKeepsRemoved` | This survey has been exported; removed elements are kept so sites can retire them | Error message · purging removed elements of an exported survey | 200 |  |
| `reporting.scope.invalid` | invalid | Analysis tags page · Applies-to column, an assignment that points at nothing | 30 |  |
| `reporting.scope.sectionInStep` | Section in step | Analysis tags page · Applies-to column and tag dialogs | 40 |  |
| `reporting.scope.questionEverywhere` | Question everywhere | Analysis tags page · Applies-to column and tag dialogs | 40 |  |
| `reporting.scope.questionInSection` | Question in section | Analysis tags page · Applies-to column and tag dialogs | 40 |  |
| `reporting.value.theAnswer` | (the answer) | Analysis tags page · Value column when the respondent answer is reported | 30 |  |
| `reporting.where.everywhere` | wherever it appears | Analysis tags page · Where column for a tag on a question in every place it is used | 40 |  |
| `error.tag.gone` | The tag no longer exists | Error message · Analysis tags page | 60 |  |
| `error.tag.inUse.one` | Tag ''{0}'' is assigned to 1 element; remove those assignments first | Error message · removing a tag that is assigned once | 160 | params: {0} = tag |
| `error.tag.inUse.many` | Tag ''{0}'' is assigned to {1} elements; remove those assignments first | Error message · removing a tag that is assigned | 160 | params: {0} = tag, {1} = how many elements |
| `error.tag.scopeRequired` | Choose what the tag applies to | Error message · tag assignment dialog | 80 |  |
| `error.tag.required` | Choose a tag | Error message · tag assignment dialog | 40 |  |
| `error.tag.elementGone` | The element no longer exists in this survey | Error message · tag assignment dialog | 100 |  |
| `error.tag.alreadyAssigned` | This element already carries the tag ''{0}'' | Error message · tag assignment dialog | 120 | params: {0} = tag |
| `error.tag.assignmentGone` | The assignment no longer exists | Error message · Analysis tags page | 80 |  |
| `error.tag.nameRequired` | Tag is required | Error message · tag dialog | 40 |  |
| `error.tag.tooLong` | Tag must be 255 characters or fewer | Error message · tag dialog | 90 |  |
| `error.tag.characters` | Tag may contain only letters, digits, spaces and hyphens, and must start with a letter or digit | Error message · tag dialog | 220 |  |
| `error.tag.hyphenNoDimension` | A tag without a dimension names its own table, and a hyphen is not allowed in a table name; choose a dimension or remove the hyphen | Error message · tag dialog | 300 |  |
| `error.tag.column.empty` | The column name is empty | Error message · tag dialog | 80 |  |
| `error.tag.column.characters` | The column ''{0}'' contains characters other than a-z, 0-9 and _ | Error message · tag dialog | 200 | params: {0} = column name; dynamic |
| `error.tag.column.tooLong` | The column ''{0}'' is longer than {1} characters | Error message · tag dialog | 200 | params: {0} = column name, {1} = the limit; dynamic |
| `error.tag.table.empty` | The dimension table is empty | Error message · tag dialog | 80 |  |
| `error.tag.table.characters` | The dimension table ''{0}'' contains characters other than a-z, 0-9 and _ | Error message · tag dialog | 200 | params: {0} = table name; dynamic |
| `error.tag.table.tooLong` | The dimension table ''{0}'' is longer than {1} characters | Error message · tag dialog | 200 | params: {0} = table name, {1} = the limit; dynamic |
| `error.dimension.tooLong` | Dimension must be 50 characters or fewer | Error message · tag dialog | 90 |  |
| `error.dimension.characters` | Dimension may contain only letters, digits and underscores (it names a table) | Error message · tag dialog | 200 |  |
| `error.tag.valueTooLong` | Value must be 255 characters or fewer | Error message · tag assignment dialog | 90 |  |
| `error.translation.notTranslatable` | {0} carries no respondent-facing text | Error message · Translations page | 140 | params: {0} = table and field name, kept as written |
| `error.translation.textGone` | The element this translation belongs to no longer has that text | Error message · Translations page | 160 |  |
| `error.handoff.tooLarge` | That file is larger than {0} MB | Error message · Translations page, importing a file | 80 | params: {0} = size limit in MB |
| `error.handoff.notJson` | That file is not readable JSON: {0} | Error message · Translations page, importing a file | 300 | params: {0} = the parser message, in English |
| `error.handoff.notRequest` | That is not a translation request: the file does not carry {0} | Error message · Translations page, importing a file | 200 | params: {0} = the format marker, kept as written |
| `error.handoff.otherSurvey` | That file belongs to another survey | Error message · Translations page, importing a file | 80 |  |
| `error.handoff.noLanguage` | That file names no language | Error message · Translations page, importing a file | 80 |  |
| `translationImport.item.unnamed` | (unnamed) | Translations page, result of importing a returned translation file · stands in for an item that has no id | 30 |  |
| `translationImport.reject.notUuid` | {0}: element_key is not a UUID | Translations page, result of importing a returned translation file · rejected item; element_key and UUID stay | 120 | params: {0} = item id |
| `translationImport.reject.notTranslatable` | {0}: {1} carries no respondent-facing text | Translations page, result of importing a returned translation file · rejected item | 160 | params: {0} = item id, {1} = table and field name, kept as written |
| `translationImport.reject.elementGone` | {0}: that element no longer exists, or has been retired | Translations page, result of importing a returned translation file · rejected item | 160 | params: {0} = item id |
| `translationImport.reject.placeholderCount` | {0}: the source has {1} placeholder(s) and the translation has {2}; each one fills in an earlier answer | Translations page, result of importing a returned translation file · rejected item | 240 | params: {0} = item id, {1} = placeholders in the source, {2} = placeholders in the translation |
| `translationImport.reject.tokens` | {0}: tokens {1} do not match the source''s {2} | Translations page, result of importing a returned translation file · rejected item | 200 | params: {0} = item id, {1} = tokens in the translation, {2} = tokens in the source |
| `translationImport.reject.unbalanced` | {0}: unbalanced braces or bars in the translation | Translations page, result of importing a returned translation file · rejected item | 140 | params: {0} = item id |
| `translationImport.reject.tooLong` | {0}: {1} characters, over the {2} allowed | Translations page, result of importing a returned translation file · rejected item | 140 | params: {0} = item id, {1} = its length, {2} = the limit |
| `translationImport.reject.duplicateOption` | {0}: another option of this question is translated the same way | Translations page, result of importing a returned translation file · rejected item | 180 | params: {0} = item id |
| `translationImport.warn.tagsDiffer` | {0}: the HTML tags differ from the source''s | Translations page, result of importing a returned translation file · warning on an imported item | 140 | params: {0} = item id |
| `translationImport.warn.baseChanged` | {0}: the base text changed since the file was exported | Translations page, result of importing a returned translation file · warning on an imported item | 160 | params: {0} = item id |
| `error.import.line` | Line {0}: {1} | Import page, why a definition file was refused · prefix naming the line of the file | 600 | params: {0} = line number, {1} = what is wrong on that line |
| `error.import.notExport` | This is not a survey definition export: the file does not start with ''# {0}'' | Import page, why a definition file was refused · the file lacks its first line | 220 | params: {0} = the format marker, kept as written |
| `error.import.noColon` | invalid record, missing colon separator | Import page, why a definition file was refused · continues error.import.line | 120 |  |
| `error.import.recordBeforeSurvey` | a {0} record appears before the surveys record | Import page, why a definition file was refused · continues error.import.line; surveys is a record name and stays | 160 | params: {0} = record name, kept as written |
| `error.import.multipleSurveys` | multiple surveys records; only one survey per file | Import page, why a definition file was refused · continues error.import.line; surveys is a record name and stays | 160 |  |
| `error.import.unknownRecord` | unknown record type ''{0}'' | Import page, why a definition file was refused · continues error.import.line | 120 | params: {0} = record name |
| `error.import.readFailed` | Failed to read the file: {0} | Import page, why a definition file was refused · the file could not be read | 300 | params: {0} = the system message, in English |
| `error.import.notDefinition` | This is not a survey definition: the file must start with ''# {0}'' or ''# {1}'' | Import page, why a definition file was refused · the file lacks its first line | 260 | params: {0}, {1} = the two format markers, kept as written |
| `error.import.noSurveysRecord` | This file contains no surveys record, so it is not a survey definition export | Import page, why a definition file was refused · surveys is a record name and stays | 220 |  |
| `error.import.alreadyOpen` | This survey is already open in Author as ''{0}'' (id {1}). Open it from the Surveys list instead of importing it again. | Import page, why a definition file was refused · Author is the product name | 300 | params: {0} = survey name, {1} = survey id |
| `error.import.translationNotTranslatable` | translations row targets a field that carries no respondent-facing text: {0} | Import page, why a definition file was refused · continues error.import.line; translations is a record name and stays | 240 | params: {0} = table and field name, kept as written |
| `error.import.translationEmpty` | translations row for {0} has an empty value; a removed translation is a closed effective_to, not a blank | Import page, why a definition file was refused · continues error.import.line; translations and effective_to stay | 300 | params: {0} = table and field name, kept as written |
| `error.import.translationMissing` | translations row has no {0} | Import page, why a definition file was refused · continues error.import.line; translations is a record name and stays | 140 | params: {0} = field name, kept as written |
| `error.import.invalidValue` | Invalid {0} in file: {1} | Import page, why a definition file was refused · continues error.import.line | 300 | params: {0} = field name, kept as written, {1} = the value found |
| `error.import.tokenTooLong` | the rule''s token ''{0}'' is {1} characters; a token name is at most {2}, the width of the column that carries it. Shorten it here and in every text that names it | Import page, why a definition file was refused · continues error.import.line | 420 | params: {0} = token, {1} = its length, {2} = the limit |
| `error.import.tokenInvalid` | the rule''s token ''{0}'' is not a name a text could write between angle brackets, so no placeholder could ever name it | Import page, why a definition file was refused · continues error.import.line | 320 | params: {0} = token as written in the file |
| `error.import.emptyHeader` | File carries an empty "{0}" header | Import page, why a definition file was refused · continues error.import.line | 140 | params: {0} = header name, kept as written |
| `error.import.badHeader` | Unparseable "{0}" header value: {1} | Import page, why a definition file was refused · continues error.import.line | 240 | params: {0} = header name, kept as written, {1} = the value found |
| `error.import.noSourceId` | Expected non-null source id for {0} | Import page, why a definition file was refused · continues error.import.line | 200 | params: {0} = record and reference name, kept as written |
| `error.import.noIdMapping` | No ID mapping found for {0} source_id={1} | Import page, why a definition file was refused · continues error.import.line; source_id stays | 240 | params: {0} = record and reference name, kept as written, {1} = the id in the file |
| `error.import.fieldCount` | {0} requires {1} fields, got {2} | Import page, why a definition file was refused · continues error.import.line | 160 | params: {0} = record name, kept as written, {1} = fields required, {2} = fields found |
| `error.export.hasErrors` | The definition has {0} error(s) that must be fixed before export | Error message · export dialog | 180 | params: {0} = number of errors |
| `ruleLabel.show` | Show {0} | Designer board · label on a rule arrow | 80 | params: {0} = target name |
| `ruleLabel.repeat` | Repeat {0} | Designer board · label on a rule arrow | 80 | params: {0} = target name |
| `ruleLabel.replace` | Replace {0} | Designer board · label on a Text rule arrow | 80 | params: {0} = the token, or the target name |
| `ruleLabel.rule` | Rule | Designer board · label on a rule arrow whose action is not set | 30 |  |
| `ruleLabel.target` | target | Designer board · stands in for a target that has no name | 30 |  |
| `ruleCondition.isTrue` | is true | Designer board · rule condition in an arrow tooltip, Boolean operator | 30 |  |
| `ruleCondition.exists` | exists | Designer board · rule condition in an arrow tooltip, Field exists operator | 30 |  |
| `ruleArrows.backwards` | points backwards | Designer board · appended to the label of a rule arrow that points backwards; same words as validation.rule.backwards | 40 |  |
| `ruleArrows.dragSource` | Drag to read from another question | Designer board · tooltip on the upstream end of a rule arrow | 80 |  |
| `ruleArrows.dragTarget` | Drag to another target | Designer board · tooltip on the downstream end of a rule arrow | 60 |  |
| `ruleArrows.when` | when | Designer board · joins a rule label and its condition in the arrow tooltip | 20 |  |
| `elementView.notFound` | No {0} {1} in survey {2} | Element page · error page when the element does not exist | 120 | params: {0} = element kind as in the address (step, section, question), {1} = its id, {2} = survey id |
| `previewFields.sampleOption` | Option {0} | Question dialog · sample option in the preview of a choice question that has no list yet | 30 | params: {0} = a letter |
| `system.build.unknown` | unknown | System overview · version or build time when the build did not record it | 30 |  |
| `report.type.respondent` | Respondent report | Report marks · name of the default report respondents see | 30 |  |
| `report.type.administrator` | Administrator report | Report marks · name of the default report administrators see | 30 |  |
| `error.report.gone` | The report no longer exists | Error message · reports page | 60 |  |
| `error.report.nameRequired` | Name is required | Error message · report service dialog | 40 |  |
| `error.report.nameTooLong` | Name must be 255 characters or fewer | Error message · report service dialog | 60 |  |
| `error.report.nameTaken` | This survey already has a report named ''{0}'' | Error message · report service dialog | 80 | params: {0} = report name |
| `error.report.descriptionRequired` | Description is required | Error message · report service dialog | 40 |  |
| `error.report.descriptionTooLong` | Description must be 255 characters or fewer | Error message · report service dialog | 60 |  |
| `error.report.urlRequired` | Address is required | Error message · report service dialog | 40 |  |
| `error.report.urlTooLong` | Address must be 255 characters or fewer | Error message · report service dialog | 60 |  |
| `error.report.urlInvalid` | Enter a path starting with / for a report built into Survey, or a full http(s) address of an external service | Error message · report service dialog | 160 |  |
| `error.mark.scopeRequired` | Choose what the mark applies to | Error message · report marks dialog | 60 |  |
| `error.mark.reportRequired` | Choose a report | Error message · report marks dialog | 40 |  |
| `error.mark.elementGone` | The element no longer exists in this survey | Error message · report marks dialog | 100 |  |
| `error.mark.notMarkable` | A {0} question has no value to show on a report | Error message · report marks dialog | 100 | params: {0} = question type (HTML, PASSWORD) |
| `error.mark.alreadyMarked` | This element is already on the {0} | Error message · report marks dialog | 100 | params: {0} = report name |
| `error.mark.gone` | The mark no longer exists | Error message · reports page | 60 |  |
| `error.mark.labelTooLong` | Label must be 255 characters or fewer | Error message · report marks dialog | 60 |  |
| `validation.mark.removedElement` | {0}: ''{1}'' is marked but has been removed. Unmark it or restore the element. | Validation finding · export warning | 200 | params: {0} = report name, {1} = element name |
| `surveyEditorView.reports` | Reports… | Survey overview · toolbar button, opens the reports page (keep the ellipsis) | 15 |  |
| `designer.menu.reportMarks` | Report marks… | Designer node menus · menu item opening the report-marks dialog | 25 |  |
| `reportBadge.title` | Report marks: {0} | Designer board, report-mark badge · tooltip and accessible name | 60 | params: {0} = the report names |
| `elementView.fact.reportMarks` | Report marks | Element detail page · fact label | 25 |  |
| `elementView.fact.markLabel` | shown as "{0}" | Element detail page · part of the report-marks fact | 40 | params: {0} = the mark's label |
| `reportsView.pageTitle` | Reports | Reports page · browser tab title | 25 |  |
| `reportsView.heading` | Reports — {0} | Reports page · heading | 60 | params: {0} = survey title |
| `reportsView.surveyOverview` | Survey overview | Reports page · back button | 25 |  |
| `reportsView.openInDesigner` | Open in designer | Reports page · button | 25 |  |
| `reportsView.intro` | What respondents see after they finish, and what the administrator report monitors. Report services are called in the order listed, and every new survey starts with the built-in respondent report. Report marks say which questions and sections each report shows. | Reports page · explanatory paragraph | 400 |  |
| `reportsView.dragHint` | Drag a row to change the order. | Reports page · hint under the heading | 60 |  |
| `reportsView.services` | Report services | Reports page · section heading | 25 |  |
| `reportsView.addService` | Add report service… | Reports page · button, opens a dialog | 30 |  |
| `reportsView.markElement` | Mark element… | Reports page · button, opens a dialog | 25 |  |
| `reportsView.servicesGrid.order` | Order | Reports page · services grid header | 10 |  |
| `reportsView.servicesGrid.name` | Name | Reports page · services grid header | 15 |  |
| `reportsView.servicesGrid.description` | Description | Reports page · services grid header | 15 |  |
| `reportsView.servicesGrid.url` | Address | Reports page · services grid header; the URL or path of the service | 15 |  |
| `reportsView.builtIn` | Built-in | Reports page · badge on the respondent report row | 15 |  |
| `reportsView.builtIn.title` | The respondent report built into Survey; it needs no service of your own | Reports page · tooltip on the built-in badge | 100 |  |
| `reportsView.services.empty` | No report services. Respondents of this survey go straight to the post-survey address when they finish. | Reports page · paragraph shown when the survey lists no report | 160 |  |
| `reportsView.edit` | Edit… | Reports page · small grid button, opens a dialog | 10 |  |
| `reportsView.removeService.header` | Remove report service | Remove report confirmation · dialog header | 30 |  |
| `reportsView.removeService.text` | Remove the report "{0}"? Sites applying the next export stop offering it. | Remove report confirmation · dialog text | 120 | params: {0} = report name |
| `reportsView.removeService.builtInText` | Remove the built-in respondent report "{0}"? Respondents of this survey will see no report. Its Respondent marks stay, but reach no report until a report service lists them. | Remove report confirmation · dialog text for the built-in respondent report | 240 | params: {0} = report name |
| `reportsView.removedService` | Removed report "{0}" | Reports page · success notification | 40 | params: {0} = report name |
| `reportsView.marks` | Report marks | Reports page · section heading | 25 |  |
| `reportsView.tab.respondent` | Respondent | Reports page · tab, also the badge text on the board | 20 |  |
| `reportsView.tab.administrator` | Administrator | Reports page · tab, also the badge text on the board | 20 |  |
| `reportsView.marksGrid.order` | Order | Reports page · marks grid header | 10 |  |
| `reportsView.marksGrid.element` | Element | Reports page · marks grid header | 15 |  |
| `reportsView.marksGrid.where` | Where | Reports page · marks grid header | 15 |  |
| `reportsView.marksGrid.appliesTo` | Applies to | Reports page · marks grid header | 15 |  |
| `reportsView.marksGrid.label` | Shown as | Reports page · marks grid header; the text the report shows for the element | 15 |  |
| `reportsView.marks.empty` | Nothing is marked for this report yet. Use Mark element…, or Report marks… in an element's menu on the board. | Reports page · paragraph shown when the report has no marks | 160 |  |
| `reportsView.editLabel` | Edit label… | Reports page · small grid button, opens a dialog | 15 |  |
| `reportsView.unmark` | Unmark | Reports page · small grid button and confirm button | 10 |  |
| `reportsView.unmark.header` | Unmark element | Unmark confirmation · dialog header | 25 |  |
| `reportsView.unmark.text` | Take "{0}" off the {1}? | Unmark confirmation · dialog text | 80 | params: {0} = element name, {1} = report name |
| `reportsView.unmarked` | Unmarked "{0}" | Reports page · success notification | 40 | params: {0} = element name |
| `reportsView.removedElement` | removed element | Reports page · badge on a mark whose element was removed | 20 |  |
| `reportServiceDialog.title.add` | Add report service | Report service dialog · dialog header | 30 |  |
| `reportServiceDialog.title.edit` | Edit report service | Report service dialog · dialog header | 30 |  |
| `reportServiceDialog.name` | Name | Report service dialog · field label | 15 |  |
| `reportServiceDialog.description` | Description | Report service dialog · field label | 15 |  |
| `reportServiceDialog.url` | Address | Report service dialog · field label; the URL or path | 15 |  |
| `reportServiceDialog.url.helper` | A path such as /api/reports/respondent-summary for a report built into Survey, or the full http(s) address of a service the site runs | Report service dialog · helper text under the address field | 200 |  |
| `reportServiceDialog.help.name` | Heads the report's card; translatable on the Translations page. | Report service dialog · help popover | 100 |  |
| `reportServiceDialog.help.url` | A path is resolved by each site against its own Survey; a full address is used as written. | Report service dialog · help popover | 120 |  |
| `reportServiceDialog.added` | Added report "{0}" | Report service dialog · success notification | 40 | params: {0} = report name |
| `reportServiceDialog.updated` | Updated report "{0}" | Report service dialog · success notification | 40 | params: {0} = report name |
| `surveyEditorView.postSurveyActions` | Post-survey actions… | Survey overview · toolbar button opening the post-survey actions page | 30 |  |
| `postSurveyActionsView.pageTitle` | Post-survey actions | Post-survey actions page · browser tab title | 40 |  |
| `postSurveyActionsView.heading` | Post-survey actions — {0} | Post-survey actions page · heading | 80 | params: {0} = survey title |
| `postSurveyActionsView.surveyOverview` | Survey overview | Post-survey actions page · button back to the survey overview | 25 |  |
| `postSurveyActionsView.intro` | What the survey's site does when a respondent finishes: each action is an address the site's Survey calls, in the order listed, with the respondent's id. Respondents never see them. Use one to file a respondent's results in another system. | Post-survey actions page · introduction paragraph; Survey is the application name, keep it | 300 |  |
| `postSurveyActionsView.notTranslated` | Names and descriptions are notes for authors and site operators, so they are not translated. | Post-survey actions page · secondary paragraph | 150 |  |
| `postSurveyActionsView.dragHint` | Drag a row to change the order. | Post-survey actions page · hint under the introduction | 60 |  |
| `postSurveyActionsView.addAction` | Add action… | Post-survey actions page · primary button | 25 |  |
| `postSurveyActionsView.grid.order` | Order | Post-survey actions grid · column header | 12 |  |
| `postSurveyActionsView.grid.name` | Name | Post-survey actions grid · column header | 20 |  |
| `postSurveyActionsView.grid.description` | Description | Post-survey actions grid · column header | 20 |  |
| `postSurveyActionsView.grid.url` | Address | Post-survey actions grid · column header for the web address | 20 |  |
| `postSurveyActionsView.empty` | No post-survey actions. The site calls nothing when a respondent finishes. | Post-survey actions page · shown instead of an empty grid | 120 |  |
| `postSurveyActionsView.edit` | Edit… | Post-survey actions grid · row button | 15 |  |
| `postSurveyActionsView.remove.header` | Remove post-survey action | Remove action confirmation · dialog title | 40 |  |
| `postSurveyActionsView.remove.text` | Remove the action "{0}"? Sites applying the next export stop calling it, and delete their record of which respondents it was called for. | Remove action confirmation · dialog text | 200 | params: {0} = action name |
| `postSurveyActionsView.removed` | Removed action "{0}" | Post-survey actions page · notification | 60 | params: {0} = action name |
| `postSurveyActionDialog.title.add` | Add post-survey action | Post-survey action dialog · dialog title | 40 |  |
| `postSurveyActionDialog.title.edit` | Edit post-survey action | Post-survey action dialog · dialog title | 40 |  |
| `postSurveyActionDialog.name` | Name | Post-survey action dialog · field label | 20 |  |
| `postSurveyActionDialog.description` | Description | Post-survey action dialog · field label | 20 |  |
| `postSurveyActionDialog.url` | Address | Post-survey action dialog · field label for the web address | 20 |  |
| `postSurveyActionDialog.url.helper` | The full http(s) address the site's Survey calls, for example http://fhhs:8080/familyhistory/generate. It may hold {respondentId}, and {externalId} when the action sends the External ID. | Post-survey action dialog · helper text under the address field; keep the example address and the two placeholders unchanged | 250 |  |
| `postSurveyActionDialog.help.name` | Identifies the action in this list and on a site's Connections screen; not shown to respondents. | Post-survey action dialog · help tooltip of the name field; Connections is a screen name in the Admin application | 150 |  |
| `postSurveyActionDialog.help.url` | Called exactly as written, from the site's Survey; it must be reachable from there. | Post-survey action dialog · help tooltip of the address field | 150 |  |
| `postSurveyActionDialog.added` | Added action "{0}" | Post-survey action dialog · notification | 60 | params: {0} = action name |
| `postSurveyActionDialog.updated` | Updated action "{0}" | Post-survey action dialog · notification | 60 | params: {0} = action name |
| `postSurveyActionDialog.method` | Method | Post-survey action dialog · field label for the HTTP method; POST, GET and PUT stay | 20 |  |
| `postSurveyActionDialog.method.helper` | How the site's Survey calls the address; POST unless the service expects GET or PUT | Post-survey action dialog · helper text under the method field; POST, GET and PUT stay | 120 |  |
| `postSurveyActionDialog.sendsExternalId` | Send the subject's External ID | Post-survey action dialog · checkbox label | 50 |  |
| `postSurveyActionDialog.sendsExternalId.warning` | The External ID usually identifies the person in another system, such as a medical record number. The site sends it, with the department's code, on every call of this action, and only to an https address. | Post-survey action dialog · warning shown while the checkbox is ticked; https stays | 250 |  |
| `postSurveyActionsView.grid.method` | Method | Post-survey actions grid · column header | 12 |  |
| `postSurveyActionsView.grid.externalId` | External ID | Post-survey actions grid · column header: whether the action sends the External ID | 20 |  |
| `postSurveyActionsView.grid.externalId.yes` | Sent | Post-survey actions grid · cell text when the action sends the External ID | 12 |  |
| `error.postSurveyAction.methodInvalid` | Unsupported method {0}; choose POST, GET or PUT | Error message · post-survey action dialog; POST, GET and PUT stay | 100 | params: {0} = the method entered |
| `error.postSurveyAction.placeholderUnknown` | The address may hold only the placeholders respondentId and externalId, not {0} | Error message · post-survey action dialog; respondentId and externalId are placeholder names, keep them | 140 | params: {0} = the placeholder found, with its braces |
| `error.postSurveyAction.externalIdPlaceholderNeedsFlag` | {externalId} needs the action to send the External ID; tick the box or remove the placeholder | Error message · post-survey action dialog; keep the placeholder exactly as written | 140 |  |
| `error.postSurveyAction.externalIdNeedsHttps` | An action that sends the External ID must use an https address | Error message · post-survey action dialog; https stays | 100 |  |
| `error.postSurveyAction.gone` | The post-survey action no longer exists | Error message · post-survey actions | 80 |  |
| `error.postSurveyAction.nameRequired` | Name is required | Error message · post-survey action dialog | 60 |  |
| `error.postSurveyAction.nameTooLong` | Name must be 255 characters or fewer | Error message · post-survey action dialog | 80 |  |
| `error.postSurveyAction.nameTaken` | This survey already has an action named ''{0}'' | Error message · post-survey action dialog | 100 | params: {0} = action name |
| `error.postSurveyAction.descriptionRequired` | Description is required | Error message · post-survey action dialog | 60 |  |
| `error.postSurveyAction.descriptionTooLong` | Description must be 255 characters or fewer | Error message · post-survey action dialog | 80 |  |
| `error.postSurveyAction.urlRequired` | Address is required | Error message · post-survey action dialog | 60 |  |
| `error.postSurveyAction.urlTooLong` | Address must be 255 characters or fewer | Error message · post-survey action dialog | 80 |  |
| `error.postSurveyAction.urlInvalid` | Enter the full http(s) address the site's Survey calls; a path is not enough for an action | Error message · post-survey action dialog; Survey is the application name, keep it | 160 |  |
| `reportMarkDialog.title.mark` | Mark for a report | Report mark dialog · dialog header | 30 |  |
| `reportMarkDialog.title.label` | Edit label | Report mark dialog · dialog header when only the label is edited | 25 |  |
| `reportMarkDialog.element` | Element | Report mark dialog · field label | 15 |  |
| `reportMarkDialog.element.section` | Section {0} — {1} | Report mark dialog · the element line | 60 | params: {0} = section name, {1} = path |
| `reportMarkDialog.element.question` | Question {0} — {1} | Report mark dialog · the element line | 60 | params: {0} = question name, {1} = path |
| `reportMarkDialog.appliesTo` | Applies to | Report mark dialog · radio group label | 20 |  |
| `reportMarkDialog.scope.question` | Wherever the question appears | Report mark dialog · radio option | 40 |  |
| `reportMarkDialog.scope.questionInSection` | Only in section {0} | Report mark dialog · radio option | 40 | params: {0} = section name |
| `reportMarkDialog.scope.section` | The section as mounted in step {0} | Report mark dialog · radio option | 40 | params: {0} = step name |
| `reportMarkDialog.report` | Report | Report mark dialog · radio group label | 15 |  |
| `reportMarkDialog.label` | Label | Report mark dialog · field label | 15 |  |
| `reportMarkDialog.label.helper` | Optional. Replaces the element's own text on the report; translatable on the Translations page. | Report mark dialog · helper text | 120 |  |
| `reportMarkDialog.disclosure` | The respondent report is reachable again with the access code, so a marked answer is exactly as private as that code. | Report mark dialog · note shown when the respondent report is chosen | 160 |  |
| `reportMarkDialog.error.chooseElement` | Choose an element | Report mark dialog · field error | 30 |  |
| `reportMarkDialog.error.chooseScope` | Choose what the mark applies to | Report mark dialog · field error | 40 |  |
| `reportMarkDialog.error.chooseReport` | Choose a report | Report mark dialog · field error | 30 |  |
| `reportMarkDialog.marked` | Marked "{0}" for the {1} | Report mark dialog · success notification | 60 | params: {0} = element name, {1} = report name |
| `reportMarkDialog.relabeled` | Updated the label | Report mark dialog · success notification | 30 |  |
| `reportMarkDialog.notMarkable` | A {0} question has no value to show on a report, so it cannot be marked. | Element marks dialog · paragraph for an HTML or PASSWORD question | 100 | params: {0} = question type (HTML, PASSWORD) |
| `elementMarksDialog.title` | Report marks — {0} | Element marks dialog · dialog header | 40 | params: {0} = element name |
| `elementMarksDialog.addMark` | Add mark… | Element marks dialog · button, opens a dialog | 15 |  |
| `elementMarksDialog.empty` | Not on any report yet. | Element marks dialog · paragraph when the element carries no mark | 40 |  |
| `elementMarksDialog.grid.report` | Report | Element marks dialog · grid header | 15 |  |
| `elementMarksDialog.grid.appliesTo` | Applies to | Element marks dialog · grid header | 15 |  |
| `elementMarksDialog.grid.label` | Shown as | Element marks dialog · grid header | 15 |  |
| `reportsView.preview` | Preview in Survey… | Reports page · link to the preview Survey, opens a new tab (keep the ellipsis) | 25 |  |
| `reportsView.preview.hint` | Opens the preview Survey. Finish the survey there as a preview respondent to see the respondent report with the marks as they stand. | Reports page · line under the preview link | 160 |  |

## English source file

```properties
common.language=Language
common.logoAlt={0} logo
common.appTitle={0} {1}
common.appTitle.default=Elicit {0}
common.appType.author=Author
common.save=Save
common.cancel=Cancel
common.close=Close
common.remove=Remove
common.restore=Restore
common.ok=OK
mainLayout.nav.surveys=Surveys
mainLayout.nav.guide=How surveys work
mainLayout.nav.manual=Manual (PDF)
mainLayout.header.manual=Manual
mainLayout.header.manualTitle=Open the author's manual (PDF) in a new tab
mainLayout.nav.aiDraft=Draft with AI
mainLayout.nav.importDefinition=Import definition
mainLayout.nav.logout=Logout
loginView.redirecting=Redirecting to sign-in…
logoutView.signingOut=Signing out…
unauthorizedView.title=Access Restricted
unauthorizedView.message=You are authenticated but do not have the required permissions to access this application. Please contact your administrator to request the 'elicit_author' role for the Author application.
unauthorizedView.logout=Logout
structureNavigationTree.showRemoved=Show removed
structureNavigationTree.header=Survey structure
surveysView.pageTitle=Surveys
surveysView.title=Surveys
surveysView.newSurvey=New survey
surveysView.aiDraft=Draft with AI
surveysView.aiDraftTitle=Have an AI agent write a first draft of a survey, then import it here
surveysView.grid.name=Name
surveysView.grid.title=Title
surveysView.grid.order=Order
surveysView.grid.lastEditedBy=Last edited by
surveysView.grid.releaseNote=Release note
surveysView.open=Open
surveysView.editDetails=Edit details
surveysView.delete=Delete
deleteSurveyDialog.title=Delete {0}?
deleteSurveyDialog.permanent=This permanently deletes the survey and everything in it: steps, sections, questions, choice lists, rules, analysis tags, reports, report marks and translations, removed elements included. It cannot be undone.
deleteSurveyDialog.previewAnswers=Preview respondents recorded for this survey in Author: {0}. They and their answers are deleted with it.
deleteSurveyDialog.exported=This survey has been exported, last by {0}: {1}. Author holds the master copy. Sites that applied an export keep their copy, and deleting it here does not change them. To change those sites later, import the backup into Author, edit it and export again; a survey rebuilt from scratch cannot update them.
deleteSurveyDialog.neverExported=This survey has never been exported, so no site has a copy of it.
deleteSurveyDialog.advice=Download a backup first. It is the only way to get the survey back: importing it restores the survey with the same keys. It holds the survey exactly as it stands, even with validation errors, and no site can install it.
deleteSurveyDialog.backup=Download backup
deleteSurveyDialog.confirmName=Type {0} to confirm
deleteSurveyDialog.delete=Delete survey
deleteSurveyDialog.deleted=Survey {0} deleted
surveyEditorView.pageTitle=Survey
surveyEditorView.editDetails=Edit details
surveyEditorView.export=Export…
surveyEditorView.design=Design…
surveyEditorView.reporting=Analysis…
surveyEditorView.summary={0} — key {1}
surveyEditorView.neverExported=Never exported.
surveyEditorView.lastEditedBy=Last edited by {0}.
surveyEditorView.lastExportedBy=Last exported by {0}: {1}
surveyEditorView.validation=Validation
surveyEditorView.readyToExport=Ready to export. No problems found.
surveyEditorView.issue.error=Error: {0}
surveyEditorView.issue.warning=Warning: {0}
validation.finding.location=— in {0}
validation.where.step=Step ''{0}''
validation.where.section=Section ''{0}''
validation.where.sectionInStep=Section ''{0}'' in step ''{1}''
validation.where.question=Question ''{0}''
validation.where.questionInSection=Question ''{0}'' in section ''{1}''
validation.where.rule=Rule ''{0}''
validation.where.theRule=The rule
validation.survey.noSteps=This survey has no steps yet. Add a step to start.
validation.mount.stepRemoved=A section is placed in a step that has been removed. Move it to another step.
validation.mount.sectionRemoved=A step holds a section that has been removed. Take it out of the step.
validation.assignment.questionRemoved=A section holds a question that has been removed. Take it out of the section.
validation.assignment.sectionRemoved=A question is placed in a section that has been removed. Move it to another section.
validation.question.listRemoved=Question ''{0}'' uses a list of options that has been removed. Choose another list.
validation.question.noList=Question ''{0}'' has no list of options. A {1} question needs one.
validation.question.emptyList=Question ''{0}'' uses an empty list. Add options to the list.
validation.question.noText=Question ''{0}'' has no question text. Enter what respondents should read.
validation.option.listRemoved=Option ''{0}'' belongs to a list that has been removed.
validation.step.empty=Step ''{0}'' is empty. Add a section to it.
validation.section.unplaced=Section ''{0}'' is not used. Place it in a step, or remove it.
validation.section.empty=Section ''{0}'' is empty. Add a question to it.
validation.question.unplaced=Question ''{0}'' is not used. Place it in a section, or remove it.
validation.survey.startingPoint=The survey''s starting point is not set, or points to a section that is no longer in the survey. Set "{0}" in "{1}".
validation.survey.nameSingleLine=The survey''s name contains a line break or another control character. Correct it in "{0}".
validation.survey.titleSingleLine=The survey''s title contains a line break or another control character. Correct it in "{0}".
validation.order.steps=Two steps share position {0}. Reorder them.
validation.order.sections=Two sections in this step share position {0}. Reorder them.
validation.order.questions=Two questions in this section share position {0}. Reorder them.
validation.rule.noTarget={0} has no downstream target. Choose the step, section or question it acts on.
validation.rule.sectionNotInStep={0} has downstream section ''{1}'' in step ''{2}'', but that section is not in that step. Choose the downstream target again.
validation.rule.questionNotInSection={0} has downstream question ''{1}'' in section ''{2}'', but that question is not in that section. Choose the downstream target again.
validation.rule.questionWithoutPlace={0} has downstream question ''{1}'' in a different section from its upstream question, but does not say which step and section. Choose the downstream target again.
validation.rule.readsRemovedQuestion={0} has an upstream question that has been removed. Choose another upstream question.
validation.rule.readsRemovedStep={0} has an upstream step that has been removed. Choose the upstream question again.
validation.rule.targetsRemovedStep={0} has a downstream step that has been removed. Choose another downstream target.
validation.rule.targetsRemovedSection={0} has a downstream section that is no longer in that step. Choose another downstream target.
validation.rule.targetsRemovedQuestion={0} has a downstream question that is no longer in that section. Choose another downstream target.
validation.rule.incomplete={0} is incomplete. Choose its operator and its action.
validation.rule.repeatSelectionOperator={0} repeats its downstream target once for each item selected in its upstream question, but its operator, {1}, is never true for a selection, so nothing would ever be repeated. Change the operator to "Field exists".
validation.rule.backwards={0} points backwards: {1}.
validation.rule.backwards.question=downstream question ''{0}'' in step ''{1}'' › ''{2}'' must come after upstream question ''{3}'' in step ''{4}'' › ''{5}''
validation.rule.backwards.section=downstream section ''{0}'' in step ''{1}'' must come after upstream question ''{2}'' in step ''{3}'' › ''{4}''
validation.rule.backwards.step=downstream step ''{0}'' must come after upstream question ''{1}'' in step ''{2}'' › ''{3}''
validation.rule.backwards.edge=Points backwards: {0}
validation.step.noBuildingRule=Step ''{0}'' has no Show or Repeat rule. Every step after the first needs one. Add a Show rule on the step — usually "Field exists" on an upstream question.
validation.section.noBuildingRule=Section ''{0}'' in step ''{1}'' has no Show or Repeat rule. Everything after the first step needs one. Add a Show rule on the section in this step — usually "Field exists" on an upstream question.
validation.question.noBuildingRule=Question ''{0}'' in section ''{1}'' in step ''{2}'' has no Show or Repeat rule. Everything after the first step needs one. Add a Show rule on the question, its section or its step — usually "Field exists" on an upstream question.
validation.token.unfilledWithDefault={0} uses {1}, but no rule fills it in here. Respondents will always see "{2}".
validation.token.unfilledNoDefault={0} uses {1}, but no rule fills it in here and it has no default, so respondents would see {1} itself. Add a rule that fills it, or give it a default: {2}.
validation.token.undelimited={0} uses {1} without angle brackets around the token. Write {2}.
validation.token.undelimitedWithDefault={0} uses {1} without angle brackets around the token. Write {2}. Until then respondents always see "{3}".
validation.token.undefined={0} uses {1} in {2}, but no rule has a token named {3}, so respondents always see "{5}". Add the token to a rule. If {1} is formatting, move it outside the braces: {4}
validation.token.undefinedNoDefault={0} uses {1} in {2}, but no rule has a token named {3}, so respondents always see the text as written. Add the token to a rule. If {1} is formatting, move it outside the braces: {4}
validation.rule.tokenUnused={0} has the token {1}, but nothing in its downstream target uses {2}. Add it to a text there, or remove the token from the rule.
validation.rule.displayOnlyNoValue={0} cannot fill {1}: its upstream question ''{2}'' only shows text ({3}) and has no answer. Choose an upstream question the respondent answers, or set the rule''s "{4}".
validation.rule.displayOnlyConstant={0} has upstream question ''{2}'', which only shows text ({3}) and has no answer, so {1} is always the rule''s "{4}". To use a real answer, choose an upstream question the respondent answers.
validation.rule.tokenBracketed={0} has the token {1}. Write it without angle brackets: {2}. Brackets belong only in the text, as {3}.
validation.rule.tokenInvalid={0} has the token "{1}", which is not a valid name. Use only letters, digits and underscores.
validation.rule.tokenLeadingDigit={0} has the token {1}. It works, but a token name should start with a letter. Rename it.
validation.rule.tokenCase={0} has the token {1}. It works, but write {2} so the token list does not show both.
validation.rename.step=Step ''{0}'' was renamed to ''{1}''. Sites already using this survey will show the new name in data already recorded too.
validation.rename.section=Section ''{0}'' was renamed to ''{1}''. Sites already using this survey will show the new name in data already recorded too.
validation.tag.redefined=Tag {0} was changed to {1}. Answers already collected at sites using this survey will be recorded under the new tag.
validation.tag.reassigned={0}: tag {1} was changed to {2}. Answers already collected at sites using this survey will be recorded under the new tag.
validation.tag.unassigned={0}: tag {1} was removed. Answers already collected at sites using this survey will no longer be recorded under it.
validation.tag.assignmentBroken=Tag ''{0}'' is assigned, but not to one section or question. Remove the assignment and add it again.
validation.tag.assignedToRemoved.section=Tag ''{0}'' is assigned to a section that has been removed{1}. Remove the assignment.
validation.tag.assignedToRemoved.question=Tag ''{0}'' is assigned to a question that has been removed{1}. Remove the assignment.
validation.tag.assignedTwice=Tag ''{0}'' is assigned to {1} {2} times with different values. Keep one.
validation.tag.table.empty=Tag ''{0}'' gives the dimension table an empty name. Rename the tag or its dimension; sites could not build their analysis schema with this name.
validation.tag.table.characters=Tag ''{0}'' gives the dimension table the name ''{1}'', which may only use a–z, 0–9 and _. Rename the tag or its dimension; sites could not build their analysis schema with this name.
validation.tag.table.tooLong=Tag ''{0}'' gives the dimension table the name ''{1}'', which is longer than {2} characters. Rename the tag or its dimension; sites could not build their analysis schema with this name.
validation.tag.column.empty=Tag ''{0}'' gives the column an empty name. Rename the tag; sites could not build their analysis schema with this name.
validation.tag.column.characters=Tag ''{0}'' gives the column the name ''{1}'', which may only use a–z, 0–9 and _. Rename the tag; sites could not build their analysis schema with this name.
validation.tag.column.tooLong=Tag ''{0}'' gives the column the name ''{1}'', which is longer than {2} characters. Rename the tag; sites could not build their analysis schema with this name.
validation.tag.column.reserved=Tag ''{0}'' gives the column the name ''{1}'', which is a fixed column of every survey''s fact table (step_key, section_key, question_key, item_key). Rename the tag; the site''s analysis build refuses it.
validation.tag.unused=Tag ''{0}'' is not used. Assign it to a section or question, or delete it.
validation.tag.sameColumn=Tags {0} produce the same column, ''{1}''. Rename one of them.
validation.tag.tableClash=Tag ''{0}'' has no dimension, so its reporting table is ''{1}'' — the same table dimension ''{2}'' uses. Rename the tag or give it a dimension.
validation.translation.missing.one={0}: 1 text is not translated yet. Respondents will see it in the survey''s own language.
validation.translation.missing.many={0}: {1} texts are not translated yet. Respondents will see them in the survey''s own language.
validation.translation.stale.one={0}: 1 translation is out of date because the original text changed. Respondents will see the original until it is translated again.
validation.translation.stale.many={0}: {1} translations are out of date because the original text changed. Respondents will see the original until they are translated again.
surveyMetadataDialog.newSurvey=New survey
surveyMetadataDialog.editSurvey=Edit survey
surveyMetadataDialog.name=Name
surveyMetadataDialog.title=Title
surveyMetadataDialog.description=Description
surveyMetadataDialog.initialDisplayKey=Initial display key
surveyMetadataDialog.postSurveyUrl=Post-survey address
surveyMetadataDialog.surveyKey=Survey key
surveyMetadataDialog.nameRequired=Name is required
surveyMetadataDialog.titleRequired=Title is required
surveyMetadataDialog.created=Survey created
surveyMetadataDialog.updated=Survey updated
exportDialog.title=Export {0}
exportDialog.releaseNote=Release note
exportDialog.releaseNote.placeholder=What changed in this revision (recorded on the survey and in the file)
exportDialog.releaseNote.required=Enter a release note
exportDialog.export=Export
exportDialog.issue.error=Error: {0}
exportDialog.issue.warning=Warning: {0}
exportDialog.errorsBlock=Fix {0} error(s) before you can export.
exportDialog.warningsDoNotBlock=You can export with warnings.
exportDialog.retroactiveReminder=Reminder: renaming a step or section, or changing a question's tag, also relabels answers already recorded at sites already using this survey.
exportDialog.download=Download {0}
exportDialog.revisionSummary=Revision {0}: {1}
importDefinitionView.pageTitle=Import definition
importDefinitionView.title=Import definition
importDefinitionView.intro=Upload an ELICIT_SURVEY_EXPORT_V1 (.elicit) file to open it in Author as a working copy. Every survey and element key in the file is kept exactly as it is, so a later export updates the same rows at every site that already applied it. A survey whose key is already open here is refused.
importDefinitionView.introDraft=An ELICIT_SURVEY_DRAFT_V1 file is accepted too -- a definition drafted outside Author, which leaves its keys blank for Author to mint. Only Author reads that marker, so a draft cannot be installed at a site until it passes validation here and is exported.
importDefinitionView.aiDraftLink=No draft yet? Have an AI agent write one
importDefinitionView.imported=Imported {0}
importDefinitionView.draftNotice=This came from an {0} draft, so its keys were minted here. Review it, fix anything validation reports, then export it to get a file a site can apply.
importDefinitionView.surveyKey=Survey key {0}
importDefinitionView.fileRevision=, file revision {0}
importDefinitionView.recordsImported={0} records imported from {1}
importDefinitionView.retiredSkipped=, {0} retired record(s) skipped
importDefinitionView.openInEditor=Open in editor
importDefinitionView.importFailed=Import failed: {0}
importDefinitionView.validation=Validation
importDefinitionView.validationClean=No problems found. This survey is ready to export.
importDefinitionView.issue.error=Error: {0}
importDefinitionView.issue.warning=Warning: {0}
aiDraftView.pageTitle=Draft with AI
aiDraftView.title=Draft a survey with an AI agent
aiDraftView.intro=A survey can start from a draft written somewhere else. Take the specification to any AI session, describe the survey you want, and bring back the file it writes. Nothing leaves this installation on its own: you carry the specification out, and you carry the draft back.
aiDraftView.step1=Copy the prompt into an AI session, or download the specification and attach the file.
aiDraftView.step2=Describe the survey you want. The agent asks about whatever decides structure: the topics to cover, what repeats per person or item, what has to be conditional.
aiDraftView.step3=Save the .elicit file the agent writes.
aiDraftView.step4=Import it here. Author mints every key on import, so the draft arrives as an ordinary working copy.
aiDraftView.copy=Copy prompt to clipboard
aiDraftView.download=Download the specification
aiDraftView.copied=Prompt copied. Paste it into your AI session.
aiDraftView.copyFailed=Could not copy to the clipboard. Download the specification instead.
aiDraftView.unavailable=This build does not carry the specification, so there is nothing to copy or download.
aiDraftView.whatComesBack.h=What comes back
aiDraftView.whatComesBack.p1=A draft is not a deployable definition. It carries a marker only Author reads, so it cannot be installed at a site until you have reviewed it here and exported it.
aiDraftView.whatComesBack.p2=Expect to fix things. Open the imported survey, read the validation panel and correct what it reports, or send those messages back to the agent and import a corrected draft.
aiDraftView.goToImport=Go to Import definition
aiDraftView.promptPreamble=I want to write a survey definition for the Elicit survey platform. The specification below tells you the survey model and the exact file format. Read it, ask me whatever you need to know about the survey I want, and then write me a complete .elicit draft file.
guideView.pageTitle=How a survey is put together
guideView.title=How a survey is put together
guideView.diagram.alt=The designer board for a small survey called Family history: two step cards, About you and Each child, each holding section cards (You and Children, then Child) with question rows inside them, and a Show rule drawn from the question 'Has children' to the question 'Child count'.
guideView.surveysLink=Go to the Surveys list
guideView.intro.p1=An Elicit survey is a decision tree the respondent walks through. This page shows the pieces a survey is made of and how the designer draws them, using the same names and glyphs you will see on the board and in the structure tree.
guideView.tree.h=Elicit is a decision tree
guideView.tree.p1=A respondent moves through the survey one page at a time. What they answer decides what they see next: a question can open a section, repeat a group of questions, or change the wording of a later question. Designing a survey means designing that path.
guideView.containers.h=Containers nest
guideView.containers.p1=Everything sits inside something else. A survey holds steps, a step holds sections, and a section holds questions.
guideView.containers.step.term=Step
guideView.containers.step.desc=— a page in the respondent's progress bar. For you it is a lane on the board that groups related sections.
guideView.containers.section.term=Section
guideView.containers.section.desc=— a titled group of questions on that page. For you it is a card inside a lane.
guideView.containers.question.term=Question
guideView.containers.question.desc=— one input the respondent answers. For you it is a row inside a card, with a glyph that shows its type.
guideView.order.h=Order is the default path
guideView.order.p1=Steps run in order, sections run in order within their step, and questions run in order within their section. Unless a rule says otherwise, that display order is exactly what the respondent experiences. Dragging an element to a new position changes it.
guideView.order.p2=Only the first step is built when a respondent signs in. Every step, section and question after it is built just in time, by a Show or Repeat rule, when the respondent's answers call for it — so the first page appears quickly, and someone who leaves in the first step or two leaves nothing saved for questions they never saw. Give every later element such a rule; for a step that always follows, the usual one is Field exists → Show on the step, read from a question the respondent has already passed. Validation reports any later element no rule builds.
guideView.rules.h=Rules are the arrows
guideView.rules.p1=A rule reads the answer to one upstream question, compares it with a reference value using an operator, and when the comparison holds performs an action on one downstream target: a step, a section, or a question. Written out, every rule is the same sentence:
guideView.rules.example.question=How many children?
guideView.rules.example.action=Repeat
guideView.rules.example.section=Child
guideView.rules.example.thatManyTimes=that many times
guideView.rules.example.ariaLabel=Example rule: question 'How many children?' greater than 0 repeats section 'Child' that many times
guideView.rules.show.term=Show
guideView.rules.show.desc=— the target appears only when the rule holds. Until then the respondent never sees it.
guideView.rules.repeat.term=Repeat
guideView.rules.repeat.desc=— the target section or question is repeated as many times as the answer says, for example one 'Child' section per child. When the answer is a selection from a list, the target is repeated once for each item selected and the rule's token is filled with that item's text, for example one 'Game' section per game attended.
guideView.rules.text.term=Text
guideView.rules.text.desc=— the answer is written into the target's text wherever a {<TOKEN>|default} placeholder appears, so a later question can say 'your daughter' instead of 'your child'. The rule reaches everything inside its target: one Text rule on a step fills the token in every section and question of that step. A token is a slot name, not a question — several rules may fill the same token, each where it is used.
guideView.rules.p2=There is no Hide rule. Hiding is the same as not showing: give the element a Show rule and it stays hidden until that rule holds.
guideView.rules.p3=Operators are Boolean, Greater than, Equal, Not equal, Field exists and Contains. When several rules point at the same target, all of them must hold.
guideView.designer.h=The designer draws all of this
guideView.designer.p1=On the board, lanes are steps, cards are sections, rows are questions and arrows are rules. A card starts closed; one click opens it and selects it, a double click on a lane, card or row opens its editor, and the ⋮ menu on each element lists everything you can do to it. A breadcrumb at the top of the page shows where you are (survey, step, section, question), and the structure tree at the bottom of the drawer follows your selection. Every element carries its glyph, and the glyph is its handle: drag a glyph from the Controls panel to create an element, drag it within its container to reorder, or drag it into another container to show the same element there as well (a section in another step, a question in another section); Copy to step… in a card's menu makes a separate section instead. Drag a rule glyph onto a question to start a rule, then drag the arrowhead onto the target. Everything a drag can do, the element's menu can do too.
guideView.designing.h=Designing a survey
guideView.designing.p1=Open a survey from the Surveys list and choose Design…. The drawer on the left is your toolbox; the board on the right is the survey. Work top-down: steps, then sections, then questions, then the lists, rules and analysis tags that give them meaning.
guideView.designing.search.term=Search
guideView.designing.search.desc=— each of the Lists, Sections and Questions panels has its own search box that narrows that panel as you type: lists by name, description and option text, sections by name and description, questions by name, text and type. Handy once a survey has dozens of each.
guideView.designing.controls.term=Controls
guideView.designing.controls.desc=— the glyphs for a step, a section, a question of the chosen type, and the three rules. Drag one onto the board, or use Add step and the Add… items in the menus.
guideView.designing.lists.term=Lists
guideView.designing.lists.desc=— the lists of options a choice question (radio, combo box, multi-select, checkbox group) offers. New list… defines one with its options in order; drag a list's glyph onto a question to give it those options, or pick the list in the question's editor.
guideView.designing.sections.term=Sections
guideView.designing.sections.desc=— every section in the survey, once, with the number of steps it is mounted in. Drag one into a lane, or use Mount in step…, to show the same section in another step; it stays one section, so a change to its name or questions reaches every step.
guideView.designing.questions.term=Questions
guideView.designing.questions.desc=— every question in the survey, once, with the number of sections it appears in. Drag one into a card, or use Add to section…, to ask the same question again elsewhere; it stays one question, so a change to its text reaches every section it is in.
guideView.designing.reportingTags.term=Analysis tags
guideView.designing.reportingTags.desc=— the ⋮ menu of a question or section offers Analysis tags…; the survey's Analysis… page lists them all with the tables and columns they produce. Tagged elements show a tag badge on the board, one entry per tag.
guideView.designing.structure.term=Survey structure
guideView.designing.structure.desc=— the tree at the bottom of the drawer; click an element to select it on the board. Show removed reveals retired elements so they can be restored.
guideView.designing.p2=When the structure is in place, add rules for anything conditional, tag what the site's analysis schema needs, mark what the reports show, check the validation panel on the survey overview, and Export… the .elicit file for the Admin app to install.
guideView.reportingTags.h=Analysis tags
guideView.reportingTags.p1=An analysis tag names a column of the star schema a site builds from the answers for analysis. Tag a question and its answers fill that column; tag a section as it appears in a step with a fixed value, and every respondent who reaches it is classified by that value. Tags that share a dimension share one table of values. Open a survey's Analysis page, or the 'Analysis tags…' item on a question or section, to manage them. Changing or removing an assigned tag is retroactive: answers already recorded at deployed sites are reclassified. Tags are not reports: what a respondent or an administrator reads is decided by the report marks on the survey's Reports page.
guideView.identity.h=Identity and exporting
guideView.identity.p1=Every element gets a key the moment it is created and keeps it for life. Removing an element retires it rather than deleting it, so answers already recorded at a site keep their meaning. When the survey is ready you export a .elicit file; the Admin app installs it at a site the first time and updates it in place after that, matching elements by key.
guideView.identity.p2=Deleting a whole survey, from the Surveys list, is the one thing that does remove rows, and it cannot be undone. Download the backup it offers first: importing that file brings the survey back with every key. Sites that applied an export are not touched; to change them later, import the backup, edit and export again.
guideView.legend.h=Glyph legend
guideView.legend.step=Step, numbered by its display order
guideView.legend.section=Section
guideView.legend.question=Text question — the icon changes with the question type (number, choice, date, …)
guideView.legend.showRule=Show rule
guideView.legend.repeatRule=Repeat rule
guideView.legend.textRule=Text rule
designer.guideLink=How surveys work
designer.menu.edit=Edit…
designer.menu.moveUp=Move up
designer.menu.moveDown=Move down
designer.menu.openDetails=Open details
designer.menu.reportingTags=Analysis tags…
designer.needsStep=Add a step first
designer.needsSection=Add a section first
designer.needsQuestion=Add a question first
designerPalette.aria=Palette: drag a glyph onto the board to add an element
designerPalette.step=Step
designerPalette.section=Section
designerPalette.question=Question
designerPalette.rule=Rule
designerPalette.roledescription=draggable palette item
designerToolbox.controls=Controls
designerToolbox.lists=Lists
designerToolbox.sections=Sections
designerToolbox.questions=Questions
designerToolbox.listsCount=Lists ({0})
designerToolbox.sectionsCount=Sections ({0})
designerToolbox.questionsCount=Questions ({0})
glyph.step=Step
glyph.stepOrder=Step {0}
glyph.section=Section
glyph.question=Question
glyph.questionOfType={0} question
glyph.rule.show=Show rule
glyph.rule.repeat=Repeat rule
glyph.rule.text=Text rule
glyph.rule=Rule
listPanel.newList=New list…
listPanel.search=Search lists
listPanel.search.aria=Search lists by name, description or option text
listPanel.roledescription=draggable list
listPanel.optionCount.one=1 option
listPanel.optionCount.many={0} options
listPanel.questionCount.one=1 question
listPanel.questionCount.many={0} questions
listPanel.menu.aria=Actions for list {0}
listPanel.empty=No lists yet. A choice question (radio, combo box, multi-select, checkbox group) needs one.
listPanel.noMatch=No list matches ''{0}''.
pickerDialog.chooseOne=Choose one
previewFields.htmlPlaceholder=<em>HTML content preview</em>
previewFields.invalidHtml=Enter valid HTML to preview it.
questionBankPanel.search=Search questions
questionBankPanel.search.aria=Search questions by name, text or type
questionBankPanel.roledescription=draggable question
questionBankPanel.uses.one=in 1 section
questionBankPanel.uses.many=in {0} sections
questionBankPanel.uses.none=in no section
questionBankPanel.menu.aria=Actions for question {0}
questionBankPanel.menu.addToSection=Add to section…
questionBankPanel.empty=No questions yet. Questions you add appear here and can be reused in other sections.
questionBankPanel.noMatch=No question matches ''{0}''.
questionRow.aria=Question {0}
questionRow.aria.removed=Question {0} (removed)
questionRow.menu.aria=Actions for question {0}
questionRow.menu.alsoAddToSection=Also add to section…
questionRow.menu.addRule=Add rule…
questionRow.menu.removeFromSection=Remove from this section
sectionBankPanel.search=Search sections
sectionBankPanel.search.aria=Search sections by name or description
sectionBankPanel.roledescription=draggable section
sectionBankPanel.uses.one=in 1 step
sectionBankPanel.uses.many=in {0} steps
sectionBankPanel.uses.none=in no step
sectionBankPanel.menu.aria=Actions for section {0}
sectionBankPanel.menu.mountInStep=Mount in step…
sectionBankPanel.empty=No sections yet. Sections you add appear here and can be mounted in other steps.
sectionBankPanel.noMatch=No section matches ''{0}''.
sectionCard.questionCount.one=1 question
sectionCard.questionCount.many={0} questions
sectionCard.mounted=in {0} steps
sectionCard.mounted.title=This section is mounted in {0} steps; editing it changes all of them
sectionCard.menu.aria=Actions for section {0}
sectionCard.menu.addQuestion=Add question
sectionCard.menu.copyToStep=Copy to step…
sectionCard.menu.alsoMountInStep=Also mount in step…
sectionCard.menu.removeFromStep=Remove from this step
sectionCard.aria=Section {0}, {1}
sectionCard.aria.removed=Section {0}, {1}, removed
sectionCard.empty=No questions yet — drop a question glyph here or use Add question.
sectionPreview.heading=Preview
sectionPreview.aria=Preview of the selected section as a respondent sees it
sectionPreview.hint.select=Select a step, section or question on the board to see it as a respondent will.
sectionPreview.hint.noSections=Step ''{0}'' has no sections yet, so respondents would see nothing here.
sectionPreview.hint.step=Respondents see one section per page. Answers typed here are not saved.
sectionPreview.hint.section=What a respondent sees on this page. Answers typed here are not saved.
sectionPreview.note.stepRuleShown=Step ''{0}'' is shown only when a rule reveals it.
sectionPreview.page=Page {0} of {1}
sectionPreview.note.removed=Removed: respondents do not see this section.
sectionPreview.note.ruleShown=Shown only when a rule reveals it.
sectionPreview.note.noQuestions=No questions yet.
stepLane.aria=Step {0}: {1}
stepLane.sectionCount.one=1 section
stepLane.sectionCount.many={0} sections
stepLane.menu.aria=Actions for step {0}
stepLane.menu.addStepAfter=Add step after
stepLane.menu.addSection=Add section
stepLane.menu.removeStep=Remove step
stepLane.header.aria=Step {0}: {1}, {2}
stepLane.header.aria.removed=Step {0}: {1}, {2}, removed
stepLane.empty=No sections yet — drop a section glyph here or use Add section.
tagBadge.title=Analysis tags: {0}
surveyDesignerView.pageTitle=Designer
surveyDesignerView.showRemoved=Show removed
surveyDesignerView.showRules=Show rules
surveyDesignerView.showPreview=Show preview
surveyDesignerView.overview=Overview
surveyDesignerView.addStep=Add step
surveyDesignerView.breadcrumb.aria=Where you are in the survey
surveyDesignerView.noSurvey=No survey with id {0}
surveyDesignerView.emptyBoard=This survey has no steps yet. Drag the Step glyph here or use Add step.
surveyDesignerView.crumb.step=Step: {0}
surveyDesignerView.crumb.section=Section: {0}
surveyDesignerView.crumb.question=Question: {0}
surveyDesignerView.sectionAlsoInStep=Section ''{0}'' is now also in step ''{1}''
surveyDesignerView.questionAlsoInSection=Question ''{0}'' is now also in section ''{1}''
surveyDesignerView.movedStep=Moved step ''{0}''
surveyDesignerView.movedSection=Moved section ''{0}''
surveyDesignerView.movedQuestion=Moved question ''{0}''
surveyDesignerView.refused.step=A step goes on the board. Drop it between two steps or after the last one.
surveyDesignerView.refused.section=A section goes in a step. Drop it on a step or between two of its sections.
surveyDesignerView.refused.question=A question goes in a section. Drop it on a section card, or add a section to the step first.
surveyDesignerView.refused.rule=A rule starts at the question whose answer it reads. Drop it on that question's row.
surveyDesignerView.refused.list=A list gives a question its options. Drop it on that question's row.
surveyDesignerView.refused.removed=''{0}'' has been removed. Restore it before dropping anything on it.
surveyDesignerView.copySection.title=Copy section ''{0}''
surveyDesignerView.copySection.label=Into step
surveyDesignerView.mountSection.title=Mount section ''{0}''
surveyDesignerView.mountSection.label=Also in step
surveyDesignerView.questionUsesList=Question ''{0}'' now offers the list ''{1}''
surveyDesignerView.removeList.title=Remove list
surveyDesignerView.removeList.text=Remove the list ''{0}'' and its options?
surveyDesignerView.removedList=Removed list ''{0}''
surveyDesignerView.addQuestion.title=Add question ''{0}''
surveyDesignerView.addQuestion.label=To section
surveyDesignerView.kind.step=step
surveyDesignerView.kind.section=section
surveyDesignerView.kind.question=question
surveyDesignerView.removeAssignment.title=Remove from {0}
surveyDesignerView.removeAssignment.text=Remove {0} ''{1}'' from {2} ''{3}''?
surveyDesignerView.removeAssignment.last=This is its only place in the survey, so the {0} itself is removed (it can be restored from its details page).
surveyDesignerView.removeAssignment.shared=It stays in the other places it is used.
surveyDesignerView.removedAssignment=Removed {0} ''{1}'' from {2} ''{3}''
surveyDesignerView.removeStep.title=Remove step
surveyDesignerView.removeStep.text=Remove step ''{0}''?{1} It can be restored from its details page.
surveyDesignerView.removeStep.dependents=Removed with it: {0}.
surveyDesignerView.removedStep=Removed step ''{0}''
surveyDesignerView.removeUnplaced.title=Remove element
surveyDesignerView.removeUnplaced.text=''{0}'' is in no step or section. Remove it from the survey?
surveyDesignerView.removedUnplaced=Removed ''{0}''
surveyDesignerView.revealedUnplacedSection=This section is in no step, so the board cannot show it. It is marked in the Sections panel.
surveyDesignerView.revealedUnplacedQuestion=This question is in no section, so the board cannot show it. It is marked in the Questions panel.
surveyDesignerView.dropArrowHint=Drop the arrow on a step, a section or a question
surveyDesignerView.ruleGone=This rule no longer exists
surveyDesignerView.ruleRepointed=Rule re-pointed
surveyDesignerView.undo=Undo
surveyDesignerView.copiedSection=Copied section ''{0}'' into step ''{1}''.
surveyDesignerView.backward.one={0} — 1 rule now points backwards (shown in red). Move the downstream target after the upstream question, or move the elements again.
surveyDesignerView.backward.many={0} — {1} rules now point backwards (shown in red). Move the downstream target after the upstream question, or move the elements again.
constraint.surveys_name_un=Another survey already uses this name
constraint.surveys_survey_key_un=A survey with this key already exists in Author
constraint.steps_survey_name_un=Another step in this survey already uses this name
constraint.steps_survey_display_order_un=Another step already has this position
constraint.sections_survey_display_order_un=Another section already has this position
constraint.select_groups_name_un=Another list in this survey already uses this name
constraint.select_items_display_text_un=This list already has an option with that text
constraint.steps_sections_display_key_un=Another assignment already uses this display key
constraint.dimensions_un=A dimension with this name already exists
constraint.ontology_un=A tag with this name already exists in this survey's namespace
constraint.metadata_un=This element already carries this tag with the same value
constraint.sqlstate.23503=This element is still referenced by another element
constraint.sqlstate.23505=A row with the same unique value already exists ({0})
elementView.pageTitle=Element
elementView.surveyOverview=Survey overview
elementView.openInDesigner=Open in designer
elementView.kind.step=step
elementView.kind.section=section
elementView.kind.question=question
elementView.badge.removed={0} (removed)
elementView.question.untitled=Question {0}
elementView.fact.displayOrder=Display order
elementView.fact.dimensionName=Dimension name
elementView.fact.description=Description
elementView.fact.type=Type
elementView.fact.text=Text
elementView.fact.required=Required
elementView.fact.tooltip=Tooltip
elementView.fact.minMax=Min / max
elementView.fact.validationText=Validation text
elementView.fact.list=List (durable id)
elementView.fact.mask=Mask
elementView.fact.placeholder=Placeholder
elementView.fact.defaultValue=Default value
elementView.fact.variant=Variant
elementView.fact.tagValue=value "{0}"
elementView.fact.reportingTags=Analysis tags
elementView.fact.elementKey=Element key
elementView.fact.durableId=Durable id
elementView.fact.removedAt=Removed at
elementView.remove.header=Remove {0}?
elementView.remove.explanation=The element is retired, not deleted: sites that apply the next export close it, respondents who already started keep it, and recorded answers stay valid.
elementView.remove.dependents=Removed together with it:
elementView.undo=Undo
elementView.removed={0} removed
elementView.restore.blocked=Cannot restore {0}: {1}
elementView.restore.header=Restore {0}?
elementView.restore.explanation=The element reopens in its former position, keeping its element key.
elementView.restore.dependents=Restored together with it:
elementView.restored=Restored
reportingView.pageTitle=Analysis tags
reportingView.heading=Analysis tags — {0}
reportingView.surveyOverview=Survey overview
reportingView.openInDesigner=Open in designer
reportingView.newTag=New tag…
reportingView.assignTag=Assign tag…
reportingView.intro=An analysis tag names a column of the survey's analysis schema at a site. Assign it to a question and the answers fill that column; assign it to a section with a fixed value and every respondent who reaches the section is classified by it. Tags on the same dimension share one table of values.
reportingView.retro=Changing or removing an assigned tag is retroactive: answers already recorded at deployed sites are reclassified when the survey is exported and applied.
reportingView.grid.tag=Tag
reportingView.grid.dimension=Dimension
reportingView.grid.ownTable=(own table)
reportingView.grid.table=Table
reportingView.grid.reportColumn=Column
reportingView.grid.assignments=Assignments
reportingView.edit=Edit…
reportingView.removeTag.tooltip=Remove this tag
reportingView.removeTag.blocked=Remove its assignments first
reportingView.assignmentsGrid.element=Element
reportingView.assignmentsGrid.where=Where
reportingView.assignmentsGrid.appliesTo=Applies to
reportingView.assignmentsGrid.tag=Tag
reportingView.assignmentsGrid.value=Value
reportingView.removedElement=removed element
reportingView.tags=Tags
reportingView.assignments=Assignments
reportingView.impact.title=Dimensional impact
reportingView.impact.intro=When the exported survey is applied and answers arrive, the site's reporting build creates these objects in the survey's own reporting schema.
reportingView.impact.none=No tags yet: the survey adds nothing to the reporting schema.
reportingView.impact.sharedDimension=shared dimension "{0}"
reportingView.impact.ownTable=own table of tag "{0}"
reportingView.impact.assignmentOne={0} assignment
reportingView.impact.assignmentMany={0} assignments
reportingView.impact.table={0} — {1} — tags: {2} — {3}
reportingView.impact.dimensionTables=Dimension tables
reportingView.impact.column={0} → {1} (tag "{2}")
reportingView.impact.reportColumns=Columns
reportingView.impact.perItemColumns=Fixed columns filled for a section repeated per selected item
reportingView.impact.perItemColumn={0} → {1} (the question the Repeat rule reads, and the item each instance is about)
reportingView.impact.error=Error: {0}
reportingView.impact.warning=Warning: {0}
reportingView.impact.findings=Problems
reportingView.removeTag.header=Remove tag
reportingView.removeTag.text=Remove the tag "{0}"? It has no assignments.
reportingView.removedTag=Removed tag "{0}"
reportingView.removeAssignment.header=Remove analysis tag
reportingView.removeAssignment.text=Remove tag "{0}" from {1}? Answers already recorded at deployed sites stop being classified by it once the survey is exported and applied.
elementTagsDialog.title=Analysis tags — {0}
elementTagsDialog.addTag=Add tag…
elementTagsDialog.empty=No analysis tags yet. Without a tag, answers to this element are not recorded in the site's analysis schema.
elementTagsDialog.grid.tag=Tag
elementTagsDialog.grid.appliesTo=Applies to
elementTagsDialog.grid.value=Value
elementTagsDialog.grid.reportColumn=Column
elementTagsDialog.edit=Edit…
elementTagsDialog.removeAssignment.header=Remove analysis tag
elementTagsDialog.removeAssignment.text=Remove tag "{0}" from {1}? Answers already recorded at deployed sites stop being classified by it once the survey is exported and applied.
elementTagsDialog.removedTag=Removed tag "{0}"
tagAssignmentDialog.title.assign=Assign analysis tag
tagAssignmentDialog.title.edit=Edit analysis tag
tagAssignmentDialog.element=Element
tagAssignmentDialog.element.section=Section {0} — {1}
tagAssignmentDialog.element.question=Question {0} — {1}
tagAssignmentDialog.appliesTo=Applies to
tagAssignmentDialog.tag=Tag
tagAssignmentDialog.newTag=New tag…
tagAssignmentDialog.reportedValue=Recorded value
tagAssignmentDialog.useAnswer=Use the respondent's answer
tagAssignmentDialog.fixedValue=Fixed value
tagAssignmentDialog.value.helper=Stored lowercased and trimmed whenever the element is answered
tagAssignmentDialog.acknowledge=I understand that answers already recorded at deployed sites are reclassified
tagAssignmentDialog.help.tag=The analysis tag. Its column in the analysis schema is filled from this element.
tagAssignmentDialog.help.value=A constant recorded instead of the answer, e.g. "Female" on the section about the respondent's mother.
tagAssignmentDialog.warning=This assignment is exported already. Changing its tag or value is retroactive: answers already recorded at deployed sites are reclassified, not only answers recorded from now on.
tagAssignmentDialog.scope.section=This section as it appears in step "{0}"
tagAssignmentDialog.scope.question=This question wherever it appears
tagAssignmentDialog.scope.questionInSection=This question only in section "{0}"
tagAssignmentDialog.impact.none=Choose a tag to see the table and column it fills.
tagAssignmentDialog.impact.text=Values go to {0} in the survey''s reporting schema · column {1}{2}
tagAssignmentDialog.impact.constant=· constant
tagAssignmentDialog.impact.fromAnswer=· from the answer
tagAssignmentDialog.error.chooseTag=Choose a tag
tagAssignmentDialog.error.fixedValue=Enter the fixed value or record the answer instead
tagAssignmentDialog.error.chooseElement=Choose an element
tagAssignmentDialog.error.chooseScope=Choose what the tag applies to
tagAssignmentDialog.updated=Updated tag "{0}"
tagAssignmentDialog.assigned=Assigned tag "{0}"
tagDialog.title.new=New analysis tag
tagDialog.title.edit=Edit analysis tag
tagDialog.tag=Tag
tagDialog.tag.helper=Letters, digits, spaces and hyphens. Becomes a column of the analysis schema.
tagDialog.dimension=Dimension
tagDialog.dimension.placeholder=None: the tag gets its own table
tagDialog.dimension.helper=Type a new name to create a dimension. Letters, digits and underscores.
tagDialog.namespace=Namespace
tagDialog.namespace.helper=Groups this survey's tags; a site keeps one row per namespace and tag
tagDialog.help.tag=The tag's name. Answers of the tagged elements are recorded under a column named after it.
tagDialog.help.dimension=A shared category such as "age" or "cancer". Tags on the same dimension share one table of values; leave it empty to give this tag a table of its own.
tagDialog.acknowledge=I understand that answers already recorded at deployed sites are reclassified
tagDialog.warning=This tag is assigned. Changing its name or dimension is retroactive: answers already recorded at deployed sites are reclassified, not only answers recorded from now on.
tagDialog.advanced=Advanced
tagDialog.error.tagRequired=Tag is required
tagDialog.created=Created tag "{0}"
tagDialog.updated=Updated tag "{0}"
tagDialog.preview.none=Choose a tag to see the table and column it produces.
tagDialog.preview.text=Values go to {0} in the survey''s reporting schema · column {1}
elementDialog.kind.step=step
elementDialog.kind.section=section
elementDialog.title.new=New {0}
elementDialog.title.edit=Edit {0}
elementDialog.name=Name
elementDialog.dimensionName=Dimension name
elementDialog.description=Description
elementDialog.acknowledge=I understand that the new name relabels data already recorded at deployed sites
elementDialog.dimensionName.helper=Analysis dimension; defaults to the name. Changing it is an analysis change, not a rename.
elementDialog.help.name=The {0}''s name. Shown to respondents and used as its label in the analysis schema; renaming it is retroactive.
elementDialog.help.dimensionName=The name this {0} is recorded under in the analysis schema. Defaults to the name; change it to relabel recorded data without renaming.
elementDialog.help.description=An optional note for authors. It is not shown to respondents.
elementDialog.error.nameRequired=Name is required
elementDialog.renameWarning=Renaming is retroactive: data already recorded at deployed sites will show the new name, not only data recorded from now on.
elementDialog.placed=The {0} is placed where you dropped it; you can move it afterwards.
elementDialog.saved.created=Created {0} ''{1}''
elementDialog.saved.updated=Updated {0} ''{1}''
questionDialog.title.new=New question
questionDialog.title.edit=Edit question
questionDialog.type=Type
questionDialog.text=Question text
questionDialog.shortText=Short text
questionDialog.toolTip=Tooltip
questionDialog.required=Required
questionDialog.minimum=Minimum
questionDialog.maximum=Maximum
questionDialog.minimumLength=Minimum length
questionDialog.maximumLength=Maximum length
questionDialog.validationText=Validation message
questionDialog.selectGroup=List of options
questionDialog.newList=New list…
questionDialog.mask=Input mask
questionDialog.placeholder=Placeholder
questionDialog.defaultValue=Default value
questionDialog.sample=Sample answer
questionDialog.variant=Variant
questionDialog.shortText.helper=Shown in the tree and on the board
questionDialog.sample.helper=Previews the texts that Text rules fill with this answer
questionDialog.variant.helper=Vaadin theme variants applied to the field
questionDialog.help.type=The kind of input the respondent uses to answer.
questionDialog.help.shortText=A short machine-friendly name, shown in the tree and on the board.
questionDialog.help.text=The question as the respondent sees it. May contain {<TOKEN>} placeholders that TEXT rules fill in.
questionDialog.help.toolTip=Help text shown to the respondent when they hover the field.
questionDialog.help.required=The respondent must answer this before continuing.
questionDialog.help.minimum=The smallest number the respondent may enter.
questionDialog.help.maximum=The largest number the respondent may enter.
questionDialog.help.minimumLength=The fewest characters the respondent may type. Set a maximum too: the length is checked only when both are given.
questionDialog.help.maximumLength=The most characters the respondent may type. Set a minimum too: the length is checked only when both are given.
questionDialog.help.selectGroup=The list of choices the respondent picks from.
questionDialog.help.mask=A regular expression matched against every character the respondent types; anything that does not match cannot be entered. It limits which characters are allowed, not the shape of the answer. Digits only: [0-9]. Phone number: [0-9 ()+-]. Letters and spaces: [A-Za-z ].
questionDialog.help.placeholder=Faint example text shown in the empty field.
questionDialog.help.defaultValue=The value the field starts with.
questionDialog.help.sample=An example answer, used only in previews. Wherever a Text rule writes this question's answer into another text, the preview shows this instead: sample "Bob" previews "Hello {<PNAME>}" as "Hello Bob". Respondents never see it.
questionDialog.help.variant=Visual style variants applied to the field: alignment, size, or layout.
questionDialog.error.typeRequired=Choose a question type
questionDialog.error.textRequired=A question needs its text
questionDialog.preview=Preview
questionDialog.preview.chooseType=Choose a type to preview the field.
questionDialog.saved.created=Question created
questionDialog.saved.updated=Question updated
relationshipDialog.title.new=New rule
relationshipDialog.title.edit=Edit rule
relationshipDialog.upstream=Reads the answer to
relationshipDialog.operator=Operator
relationshipDialog.value=Value
relationshipDialog.action=Action
relationshipDialog.targetKind=Target
relationshipDialog.target=Target element
relationshipDialog.token=Token
relationshipDialog.description=Description
relationshipDialog.defaultUpstreamValue=Default upstream value
relationshipDialog.overrideUpstreamValue=Override upstream value
relationshipDialog.removeRule=Remove rule
relationshipDialog.token.helper=Optional for Show and Repeat, required for Text. Fills {<TOKEN>|default} — or a phrase holding it, {Has <TOKEN> been|Were you} — in the target's text and in every text inside it
relationshipDialog.token.helper.displayOnly=This question shows text and collects no answer, so only the rule's own default upstream value can fill the token. Read from a question the respondent answers to fill it with their answer
relationshipDialog.operator.helper.perItem=The upstream question is answered by selecting items, so the downstream target repeats once for each item selected and the token is filled with that item's text. Use FIELD_EXIST
relationshipDialog.targetKind.step=Step
relationshipDialog.targetKind.section=Section
relationshipDialog.targetKind.question=Question
relationshipDialog.help.upstream=The question whose answer this rule checks.
relationshipDialog.help.operator=How the answer is compared to the value — equals, greater than, contains, or just whether an answer exists.
relationshipDialog.help.value=The value the answer is compared against. Leave empty for operators that only test whether an answer exists.
relationshipDialog.help.action=What the rule does when it matches: SHOW reveals the target, REPEAT repeats it as many times as the answer says, or once for each item selected when the answer is a selection from a list, TEXT substitutes the answer into the target's text. A SHOW or REPEAT rule may also carry a token naming the slot the upstream answer fills in the target's texts; a TEXT rule must.
relationshipDialog.help.targetKind=Whether the rule acts on a whole step, a section, or a single question. A rule with a token on a step fills it in every section and question of that step; on a section, in every question of it.
relationshipDialog.help.target=The element the action applies to.
relationshipDialog.help.token=The slot written as {<TOKEN>} in the target's texts that the upstream answer fills. Any rule may carry one — a SHOW or REPEAT rule that also names a token fills it, as the runtime does — and a TEXT rule must. Pick one the survey already uses or type a new name (up to 10 characters). The same token may be filled by several rules, one per place it is used. The preview shows the upstream question's sample answer in its place.
relationshipDialog.help.description=An optional note for authors. It is not shown to respondents.
relationshipDialog.help.defaultUpstreamValue=The value to assume for the upstream answer when the respondent has not answered it yet.
relationshipDialog.help.overrideUpstreamValue=A fixed value to use for the upstream answer instead of the respondent's actual answer.
relationshipDialog.advanced=Advanced
relationshipDialog.saved.created=Rule created
relationshipDialog.saved.updated=Rule updated
relationshipDialog.removed=Rule removed
relationshipDialog.sentence=Question ''{0}'' {1}{2} → {3}{4} ''{5}''
relationshipDialog.sentence.noOperator=(operator)
relationshipDialog.sentence.noAction=(action)
selectGroupDialog.title.new=New list of options
selectGroupDialog.title.edit=Edit list of options
selectGroupDialog.name=Name
selectGroupDialog.description=Description
selectGroupDialog.dataType=Data type
selectGroupDialog.addOption=Add option
selectGroupDialog.dataType.helper=Informational; 'Text' unless the site's analysis expects otherwise
selectGroupDialog.help.name=The list's name, shown when picking options for a question. Unique within the survey.
selectGroupDialog.help.description=An optional note for authors.
selectGroupDialog.help.dataType=A label carried with the list; the Survey runtime does not interpret it.
selectGroupDialog.error.nameRequired=Name is required
selectGroupDialog.option.displayText=Display text
selectGroupDialog.option.displayText.aria=Option display text
selectGroupDialog.option.codedValue=Coded value (defaults to the text)
selectGroupDialog.option.codedValue.aria=Option coded value
selectGroupDialog.option.moveUp=Move option up
selectGroupDialog.option.moveDown=Move option down
selectGroupDialog.option.remove=Remove option
selectGroupDialog.optionsLabel=Options, in the order respondents see them
selectGroupDialog.noOptions=No options yet.
selectGroupDialog.saved.created=Created list ''{0}''
selectGroupDialog.saved.updated=Updated list ''{0}''
mainLayout.resizer.title=Drag to resize the drawer
mainLayout.resizer.aria=Resize drawer
mainLayout.smallScreen.heading=A bigger screen is needed
mainLayout.smallScreen.text=Elicit Author is built for designing surveys on a tablet or larger screen. Please switch to a wider device.
common.none=none
common.unknown=unknown
mainLayout.nav.system=System
mainLayout.nav.systemOverview=Overview
mainLayout.nav.systemDatabase=Database
mainLayout.nav.systemBranding=Branding
mainLayout.nav.systemConnections=Connections
mainLayout.nav.systemOidc=OIDC
system.grid.item=Item
system.grid.value=Value
system.grid.state=State
system.grid.detail=Detail
system.badge.ok=OK
system.badge.failed=Failed
system.badge.notChecked=Not checked
system.badge.present=Present
system.badge.absent=Absent
system.milliseconds={0} ms
systemOverviewView.pageTitle=System Overview
systemOverviewView.title=System Overview
systemOverviewView.intro=What this deployment is running, whether it is healthy, and whether every required setting is present. Settings are shown as present or absent, never by value; they are startup configuration and cannot be changed here.
systemOverviewView.running=Running
systemOverviewView.row.application=Application
systemOverviewView.row.version=Version
systemOverviewView.row.built=Built
systemOverviewView.row.profile=Profile
systemOverviewView.row.started=Started
systemOverviewView.row.uptime=Uptime
systemOverviewView.uptime.days={0} d {1} h {2} min
systemOverviewView.uptime.hours={0} h {1} min
systemOverviewView.uptime.minutes={0} min {1} s
systemOverviewView.health=Health
systemOverviewView.area.database=Database connection
systemOverviewView.area.surveySchema=Survey schema
systemOverviewView.area.surveySchema.notChecked=not checked: the connection failed
systemOverviewView.area.branding=Branding
systemOverviewView.area.connections=Connections
systemOverviewView.connectionsSummary.one={0} outbound target configured; open Connections to check it
systemOverviewView.connectionsSummary.many={0} outbound targets configured; open Connections to check them
systemOverviewView.grid.area=Area
systemOverviewView.requiredSettings=Required settings
systemOverviewView.grid.setting=Setting
systemOverviewView.grid.suppliedBy=Supplied by
systemOverviewView.grid.purpose=Purpose
systemOverviewView.setting.datasourcePassword=password of the database user Author connects as
systemOverviewView.setting.oidcSecret=client secret registered with the identity provider
systemOverviewView.setting.serviceToken=service token Author sends to the preview Survey
systemOverviewView.serviceToken.developmentValue={0} is the development value. Replace it before this Author is used at a real site.
systemDatabaseView.pageTitle=System Database
systemDatabaseView.title=Database
systemDatabaseView.intro=Author owns no migrations. The survey schema in its database is created by the Survey preview instance that shares the database, so a missing schema means that instance has not started against this database yet.
systemDatabaseView.connection=Connection
systemDatabaseView.row.state=State
systemDatabaseView.row.configuredUser=Configured user
systemDatabaseView.row.connectedAs=Connected as
systemDatabaseView.row.database=Database
systemDatabaseView.row.server=Server
systemDatabaseView.row.roundTrip=Round trip
systemDatabaseView.surveySchema=Survey schema
systemDatabaseView.row.schemaPresent=Schema present
systemDatabaseView.row.surveyMigration=Survey migration (survey.flyway_history)
systemDatabaseView.migration.notInstalled=not installed
systemDatabaseView.migration.empty=empty
systemDatabaseView.migration.applied={0} applied {1}
systemDatabaseView.migration.failed={0} FAILED {1}
systemDatabaseView.migration.unreadable=present, but not readable by {0}: {1}
systemDatabaseView.row.durableSequences=Kimball durable-key sequences
systemDatabaseView.row.surveysAuthored=Surveys authored
systemDatabaseView.schema.exists=survey.surveys exists
systemDatabaseView.schema.missing=survey.surveys does not exist: the Survey preview instance has not created the schema in this database yet
systemDatabaseView.durableSequences.exists=survey.{0} exists
systemDatabaseView.durableSequences.missing=survey.{0} is missing: the schema in this database predates the V3 migrations
systemBrandingView.pageTitle=System Branding
systemBrandingView.title=Branding
systemBrandingView.intro=The brand resolves from the mounted directory first, then the local directory, then the default packaged with the application. Each asset is listed on its own, because a partial mount renders with the wrong fonts and no error.
systemBrandingView.reload=Reload brand
systemBrandingView.reloaded=Brand cache discarded; the result below is freshly resolved.
systemBrandingView.resolution=Resolution
systemBrandingView.row.configuredPath=Configured path (brand.file.system.path)
systemBrandingView.row.localPath=Local path (brand.local.path)
systemBrandingView.path.exists={0} (exists)
systemBrandingView.path.notFound={0} (not found)
systemBrandingView.row.metadataFile=Metadata file
systemBrandingView.row.brandName=Brand name
systemBrandingView.row.organization=Organization
systemBrandingView.row.brandVersion=Brand version
systemBrandingView.row.inUse=In use (cached)
systemBrandingView.inUse={0} [{1}]
systemBrandingView.row.summary=Summary
systemBrandingView.summary.mounted=Mounted brand at {0}
systemBrandingView.summary.mountedNamed=Mounted brand: {0} at {1}
systemBrandingView.summary.local=Local brand directory {0}
systemBrandingView.summary.embedded=Embedded default theme; no brand directory at {0}
systemBrandingView.assets=Assets
systemBrandingView.grid.asset=Asset
systemBrandingView.grid.path=Path
systemBrandingView.grid.source=Source
systemBrandingView.grid.location=Location
systemBrandingView.source.mounted=Mounted
systemBrandingView.source.local=Local
systemBrandingView.source.embedded=Embedded default
systemBrandingView.source.absent=Absent
systemBrandingView.source.unreadable=Unreadable
systemBrandingView.asset.colorStylesheet=color stylesheet
systemBrandingView.asset.typographyStylesheet=typography stylesheet
systemBrandingView.asset.themeStylesheet=theme stylesheet
systemBrandingView.asset.horizontalLogo=horizontal logo
systemBrandingView.asset.headerIcon=header icon
systemBrandingView.asset.favicon=favicon
systemBrandingView.asset.notFound=not found in any location
systemBrandingView.asset.unreadable={0} exists but cannot be read
systemBrandingView.logoPreview=Logo preview
systemBrandingView.logoAlt=Brand logo
systemConnectionsView.pageTitle=System Connections
systemConnectionsView.title=Connections
systemConnectionsView.intro=Each outbound dependency Author will call, read from the same settings the runtime uses. A check sends one read-only request and gives up after {0} seconds. Author''s only other dependency is the database, checked on its own page.
systemConnectionsView.empty=No outbound targets are configured.
systemConnectionsView.checkAll=Check all
systemConnectionsView.check=Check
systemConnectionsView.grid.dependency=Dependency
systemConnectionsView.grid.source=Source
systemConnectionsView.grid.address=Address
systemConnectionsView.grid.time=Time
systemConnectionsView.group.identityProvider=Identity provider
systemConnectionsView.group.telemetryCollector=Telemetry collector
systemConnectionsView.result.timedOut=timed out after {0} s
systemConnectionsView.result.interrupted=interrupted
systemConnectionsView.result.discoveryServed=discovery document served at {0}
systemConnectionsView.result.notDiscovery=HTTP {0} from {1} is not an OIDC discovery document; check the realm address
systemConnectionsView.result.reachable=reachable, HTTP {0}
systemConnectionsView.result.noHostPort=address {0} has no host and port
systemConnectionsView.result.connected=connected to {0}:{1}
oidcDiagnosticsView.pageTitle=OIDC Diagnostics
oidcDiagnosticsView.title=OIDC
oidcDiagnosticsView.intro=The identity the provider handed this session: who signed in, which roles arrived, and whether the authoring role is among them. Tokens are masked.
oidcDiagnosticsView.user=User: {0}
oidcDiagnosticsView.isAnonymous=Is Anonymous: {0}
oidcDiagnosticsView.roles=Roles: {0}
oidcDiagnosticsView.roleSource=Role Source: {0}
oidcDiagnosticsView.hasRole=Has {0}: {1}
oidcDiagnosticsView.issuer=Issuer: {0}
oidcDiagnosticsView.email=Email: {0}
oidcDiagnosticsView.idToken=ID Token: {0}
oidcDiagnosticsView.idTokenUnavailable=ID Token: Not available or resolvable
oidcDiagnosticsView.idTokenError=ID Token Error: {0}
oidcDiagnosticsView.accessToken=Access Token: {0}
oidcDiagnosticsView.accessTokenUnavailable=Access Token: Not available or resolvable
oidcDiagnosticsView.accessTokenError=Access Token Error: {0}
surveyEditorView.translations=Translations…
surveyMetadataDialog.baseLanguage=Content language
surveyMetadataDialog.baseLanguage.helper=The language this survey's questions are written in.
surveyMetadataDialog.contentLanguages=Published translations
surveyMetadataDialog.contentLanguages.helper=The languages this survey is published in. Elicit carries these; contact ElicitSoftware for another and it arrives in the next release.
surveyMetadataDialog.contentLanguages.option={0} — {1}
translationsView.pageTitle=Translations
translationsView.heading=Translations — {0}
translationsView.backToOverview=Back to the survey overview
translationsView.intro=Every string a respondent reads, beside its translation. A string with no translation, or one whose wording changed after it was translated, is shown to respondents in the survey's own language.
translationsView.language=Language
translationsView.language.option={0} — {1}
translationsView.language.option.new={0} — {1} · not published yet
translationsView.language.option.unsupported={0} — {1} · not carried by Elicit
translationsView.language.placeholder=Choose a language
translationsView.language.helper=Elicit carries these languages. Need another? Contact ElicitSoftware and it arrives in the next release.
translationsView.language.published={0} is now one of this survey''s published languages; {1} string(s) still need translating.
translationsView.onlyOutstanding=Only what needs work
translationsView.counts={0} translated, {1} missing, {2} out of date
translationsView.request=Request translation
translationsView.import=Import a returned file
translationsView.noLanguages=This survey publishes no translations yet. Choose a language to start one.
translationsView.grid.element=Where
translationsView.grid.field=Text
translationsView.grid.baseText=Original
translationsView.grid.translation=Translation
translationsView.grid.status=Status
translationsView.status.translated=Translated
translationsView.status.missing=Missing
translationsView.status.stale=Out of date
translationsView.status.staleTooltip=Translated from: {0}
translationImport.title=Translations imported
translationImport.counts={0} imported, {1} unchanged, {2} left untranslated, {3} rejected.
translationImport.stale={0} of them were translated from wording that has since changed, so they will not be shown until they are translated again.
translationImport.rejected=These were not imported:
translationImport.warnings=Worth checking:
designer.staleTranslations=The {0} changed. Its {1} translation(s) ({2}) are now out of date and will not be shown to respondents until they are translated again.
designer.staleTranslations.open=Open translations
error.name.required=Name is required
error.name.tooLong=Name must be 255 characters or fewer
error.name.singleLine=Name must be a single line of text
error.list.gone=The list no longer exists
error.list.descriptionTooLong=Description must be 255 characters or fewer
error.list.dataTypeTooLong=Data type must be 50 characters or fewer
error.list.optionTextRequired=Every option needs its display text
error.list.optionTextTooLong=Option text must be 255 characters or fewer
error.list.optionTwice=Option ''{0}'' appears twice
error.list.codedValueTooLong=Coded value must be 255 characters or fewer
error.survey.gone=Survey {0} no longer exists
error.survey.delete.previewUnreachable=The survey was not deleted: its reporting schema could not be dropped because the preview Survey at {0} could not be reached ({1}). Start it and try again.
error.survey.delete.previewRefused=The survey was not deleted: the preview Survey did not drop its reporting schema (answered {0}: {1}).
error.survey.delete.serviceTokenMissing=The survey was not deleted: service token not configured. Set {0} for Author and the preview Survey, then try again.
error.survey.delete.previewUnauthorized=The survey was not deleted: the preview Survey refused the request. Check that Author and the preview Survey are given the same service token ({0}).
error.survey.notLanguageTag=''{0}'' is not a language tag. Choose one of the languages Elicit carries.
error.survey.baseLanguage={0} is the language this survey is written in; it needs no translation.
error.survey.languagePublished={0} is already one of this survey''s published languages.
error.survey.languagesFull=This survey already publishes as many languages as it can hold.
error.survey.languageUnsupported={0} is not one of the languages Elicit carries, so no site could show it. Contact ElicitSoftware to have it translated; it becomes available in the next release.
error.survey.exportNeedsAuthor=An export must be attributed to a signed-in author
error.survey.titleRequired=Title is required
error.survey.titleTooLong=Title must be 255 characters or fewer
error.survey.titleSingleLine=Title must be a single line of text
error.survey.descriptionTooLong=Description must be 2000 characters or fewer
error.survey.initialDisplayKeyTooLong=Initial display key must be 255 characters or fewer
error.survey.postSurveyUrlTooLong=Post-survey address must be 255 characters or fewer
error.designer.sectionAlreadyInStep=Step ''{0}'' already contains this section; a section can be mounted once per step
error.designer.questionAlreadyInSection=Section ''{0}'' already contains this question
error.designer.sectionAssignmentGone=Section assignment not found
error.designer.questionAssignmentGone=Question assignment not found
error.step.notFound=Step not found
error.step.goneOrRemoved=Step not found or removed
error.section.goneOrRemoved=Section not found or removed
error.question.goneOrRemoved=Question not found or removed
error.designer.noListForType=A {0} question does not take a list of options
error.designer.noListForThisType=A question of this type does not take a list of options
error.step.nameRequired=A step needs a name
error.step.nameTooLong=Step names are at most 255 characters
error.section.nameRequired=A section needs a name
error.section.nameTooLong=Section names are at most 255 characters
error.question.typeRequired=Choose a question type
error.question.textRequired=A question needs its text
error.question.listRequired=A {0} question needs a list of options
error.question.minOverMax=The minimum cannot exceed the maximum
designer.stale.stepName=step name
designer.stale.sectionName=section name
designer.stale.question=question
designer.stale.shortText=short text
designer.stale.tooltip=tooltip
designer.stale.placeholder=placeholder
designer.stale.validationText=validation message
error.rule.gone=Rule not found or removed
error.rule.dropOnElement=Drop the arrow on a step, a section or a question
error.rule.readsFromQuestion=A rule reads from a question
error.rule.upstreamRequired=Choose the question the rule reads from
error.rule.selfTarget=A rule cannot target the question it reads from
error.rule.operatorRequired=Choose an operator
error.rule.actionRequired=Choose an action
error.rule.textNeedsToken=A Text rule needs the token it replaces
error.rule.tokenTooLong=Token names are at most {0} characters
error.rule.tokenInvalid=A token name starts with a letter and holds letters, digits and underscores only, so no text could ever name ''{0}''
error.rule.displayOnlySource=A {0} question collects no answer, so {1} would stay unfilled. Read from a question the respondent answers, or give the rule a default upstream value to fill the token with
error.rule.displayOnlyType=display-only
error.rule.repeatNotStep=Repeat applies to a section or a question, not a step
error.rule.repeatSelectionOperator=The upstream question is answered by selecting items, so a Repeat rule repeats its downstream target once for each item selected, and {0} is never true for a selection. Choose FIELD_EXIST
error.element.noSurvey=An element must belong to a survey
error.question.notFound=Question not found
error.section.notFound=Section not found
error.element.notFound=Element not found
error.option.notFound=Option not found
error.element.unsupportedKind=Unsupported element kind {0}
element.dependent.questionInSection=assignment of the question to section {0}
element.dependent.sectionInStep=assignment of the section to step {0}
element.dependent.questionOfSection=assignment of question {0} to the section
element.dependent.sectionOfStep=assignment of section {0} to the step
element.dependent.rule=rule ''{0}''
element.blocker.sectionRemoved=section {0} is removed; restore it first
element.blocker.stepRemoved=step {0} is removed; restore it first
error.element.cannotRemove=Cannot remove {0}: {1}
error.element.cannotRestore=Cannot restore {0}: {1}
error.list.inUse=This list is still used by {0} question(s); change or remove them first
error.survey.notFound=Survey not found
error.survey.exportedKeepsRemoved=This survey has been exported; removed elements are kept so sites can retire them
reporting.scope.invalid=invalid
reporting.scope.sectionInStep=Section in step
reporting.scope.questionEverywhere=Question everywhere
reporting.scope.questionInSection=Question in section
reporting.value.theAnswer=(the answer)
reporting.where.everywhere=wherever it appears
error.tag.gone=The tag no longer exists
error.tag.inUse.one=Tag ''{0}'' is assigned to 1 element; remove those assignments first
error.tag.inUse.many=Tag ''{0}'' is assigned to {1} elements; remove those assignments first
error.tag.scopeRequired=Choose what the tag applies to
error.tag.required=Choose a tag
error.tag.elementGone=The element no longer exists in this survey
error.tag.alreadyAssigned=This element already carries the tag ''{0}''
error.tag.assignmentGone=The assignment no longer exists
error.tag.nameRequired=Tag is required
error.tag.tooLong=Tag must be 255 characters or fewer
error.tag.characters=Tag may contain only letters, digits, spaces and hyphens, and must start with a letter or digit
error.tag.hyphenNoDimension=A tag without a dimension names its own table, and a hyphen is not allowed in a table name; choose a dimension or remove the hyphen
error.tag.column.empty=The column name is empty
error.tag.column.characters=The column ''{0}'' contains characters other than a-z, 0-9 and _
error.tag.column.tooLong=The column ''{0}'' is longer than {1} characters
error.tag.table.empty=The dimension table is empty
error.tag.table.characters=The dimension table ''{0}'' contains characters other than a-z, 0-9 and _
error.tag.table.tooLong=The dimension table ''{0}'' is longer than {1} characters
error.dimension.tooLong=Dimension must be 50 characters or fewer
error.dimension.characters=Dimension may contain only letters, digits and underscores (it names a table)
error.tag.valueTooLong=Value must be 255 characters or fewer
error.translation.notTranslatable={0} carries no respondent-facing text
error.translation.textGone=The element this translation belongs to no longer has that text
error.handoff.tooLarge=That file is larger than {0} MB
error.handoff.notJson=That file is not readable JSON: {0}
error.handoff.notRequest=That is not a translation request: the file does not carry {0}
error.handoff.otherSurvey=That file belongs to another survey
error.handoff.noLanguage=That file names no language
translationImport.item.unnamed=(unnamed)
translationImport.reject.notUuid={0}: element_key is not a UUID
translationImport.reject.notTranslatable={0}: {1} carries no respondent-facing text
translationImport.reject.elementGone={0}: that element no longer exists, or has been retired
translationImport.reject.placeholderCount={0}: the source has {1} placeholder(s) and the translation has {2}; each one fills in an earlier answer
translationImport.reject.tokens={0}: tokens {1} do not match the source''s {2}
translationImport.reject.unbalanced={0}: unbalanced braces or bars in the translation
translationImport.reject.tooLong={0}: {1} characters, over the {2} allowed
translationImport.reject.duplicateOption={0}: another option of this question is translated the same way
translationImport.warn.tagsDiffer={0}: the HTML tags differ from the source''s
translationImport.warn.baseChanged={0}: the base text changed since the file was exported
error.import.line=Line {0}: {1}
error.import.notExport=This is not a survey definition export: the file does not start with ''# {0}''
error.import.noColon=invalid record, missing colon separator
error.import.recordBeforeSurvey=a {0} record appears before the surveys record
error.import.multipleSurveys=multiple surveys records; only one survey per file
error.import.unknownRecord=unknown record type ''{0}''
error.import.readFailed=Failed to read the file: {0}
error.import.notDefinition=This is not a survey definition: the file must start with ''# {0}'' or ''# {1}''
error.import.noSurveysRecord=This file contains no surveys record, so it is not a survey definition export
error.import.alreadyOpen=This survey is already open in Author as ''{0}'' (id {1}). Open it from the Surveys list instead of importing it again.
error.import.translationNotTranslatable=translations row targets a field that carries no respondent-facing text: {0}
error.import.translationEmpty=translations row for {0} has an empty value; a removed translation is a closed effective_to, not a blank
error.import.translationMissing=translations row has no {0}
error.import.invalidValue=Invalid {0} in file: {1}
error.import.tokenTooLong=the rule''s token ''{0}'' is {1} characters; a token name is at most {2}, the width of the column that carries it. Shorten it here and in every text that names it
error.import.tokenInvalid=the rule''s token ''{0}'' is not a name a text could write between angle brackets, so no placeholder could ever name it
error.import.emptyHeader=File carries an empty "{0}" header
error.import.badHeader=Unparseable "{0}" header value: {1}
error.import.noSourceId=Expected non-null source id for {0}
error.import.noIdMapping=No ID mapping found for {0} source_id={1}
error.import.fieldCount={0} requires {1} fields, got {2}
error.export.hasErrors=The definition has {0} error(s) that must be fixed before export
ruleLabel.show=Show {0}
ruleLabel.repeat=Repeat {0}
ruleLabel.replace=Replace {0}
ruleLabel.rule=Rule
ruleLabel.target=target
ruleCondition.isTrue=is true
ruleCondition.exists=exists
ruleArrows.backwards=points backwards
ruleArrows.dragSource=Drag to read from another question
ruleArrows.dragTarget=Drag to another target
ruleArrows.when=when
elementView.notFound=No {0} {1} in survey {2}
previewFields.sampleOption=Option {0}
system.build.unknown=unknown
report.type.respondent=Respondent report
report.type.administrator=Administrator report
error.report.gone=The report no longer exists
error.report.nameRequired=Name is required
error.report.nameTooLong=Name must be 255 characters or fewer
error.report.nameTaken=This survey already has a report named ''{0}''
error.report.descriptionRequired=Description is required
error.report.descriptionTooLong=Description must be 255 characters or fewer
error.report.urlRequired=Address is required
error.report.urlTooLong=Address must be 255 characters or fewer
error.report.urlInvalid=Enter a path starting with / for a report built into Survey, or a full http(s) address of an external service
error.mark.scopeRequired=Choose what the mark applies to
error.mark.reportRequired=Choose a report
error.mark.elementGone=The element no longer exists in this survey
error.mark.notMarkable=A {0} question has no value to show on a report
error.mark.alreadyMarked=This element is already on the {0}
error.mark.gone=The mark no longer exists
error.mark.labelTooLong=Label must be 255 characters or fewer
validation.mark.removedElement={0}: ''{1}'' is marked but has been removed. Unmark it or restore the element.
surveyEditorView.reports=Reports…
designer.menu.reportMarks=Report marks…
reportBadge.title=Report marks: {0}
elementView.fact.reportMarks=Report marks
elementView.fact.markLabel=shown as "{0}"
reportsView.pageTitle=Reports
reportsView.heading=Reports — {0}
reportsView.surveyOverview=Survey overview
reportsView.openInDesigner=Open in designer
reportsView.intro=What respondents see after they finish, and what the administrator report monitors. Report services are called in the order listed, and every new survey starts with the built-in respondent report. Report marks say which questions and sections each report shows.
reportsView.dragHint=Drag a row to change the order.
reportsView.services=Report services
reportsView.addService=Add report service…
reportsView.markElement=Mark element…
reportsView.servicesGrid.order=Order
reportsView.servicesGrid.name=Name
reportsView.servicesGrid.description=Description
reportsView.servicesGrid.url=Address
reportsView.builtIn=Built-in
reportsView.builtIn.title=The respondent report built into Survey; it needs no service of your own
reportsView.services.empty=No report services. Respondents of this survey go straight to the post-survey address when they finish.
reportsView.edit=Edit…
reportsView.removeService.header=Remove report service
reportsView.removeService.text=Remove the report "{0}"? Sites applying the next export stop offering it.
reportsView.removeService.builtInText=Remove the built-in respondent report "{0}"? Respondents of this survey will see no report. Its Respondent marks stay, but reach no report until a report service lists them.
reportsView.removedService=Removed report "{0}"
reportsView.marks=Report marks
reportsView.tab.respondent=Respondent
reportsView.tab.administrator=Administrator
reportsView.marksGrid.order=Order
reportsView.marksGrid.element=Element
reportsView.marksGrid.where=Where
reportsView.marksGrid.appliesTo=Applies to
reportsView.marksGrid.label=Shown as
reportsView.marks.empty=Nothing is marked for this report yet. Use Mark element…, or Report marks… in an element's menu on the board.
reportsView.editLabel=Edit label…
reportsView.unmark=Unmark
reportsView.unmark.header=Unmark element
reportsView.unmark.text=Take "{0}" off the {1}?
reportsView.unmarked=Unmarked "{0}"
reportsView.removedElement=removed element
reportServiceDialog.title.add=Add report service
reportServiceDialog.title.edit=Edit report service
reportServiceDialog.name=Name
reportServiceDialog.description=Description
reportServiceDialog.url=Address
reportServiceDialog.url.helper=A path such as /api/reports/respondent-summary for a report built into Survey, or the full http(s) address of a service the site runs
reportServiceDialog.help.name=Heads the report's card; translatable on the Translations page.
reportServiceDialog.help.url=A path is resolved by each site against its own Survey; a full address is used as written.
reportServiceDialog.added=Added report "{0}"
reportServiceDialog.updated=Updated report "{0}"
surveyEditorView.postSurveyActions=Post-survey actions…
postSurveyActionsView.pageTitle=Post-survey actions
postSurveyActionsView.heading=Post-survey actions — {0}
postSurveyActionsView.surveyOverview=Survey overview
postSurveyActionsView.intro=What the survey's site does when a respondent finishes: each action is an address the site's Survey calls, in the order listed, with the respondent's id. Respondents never see them. Use one to file a respondent's results in another system.
postSurveyActionsView.notTranslated=Names and descriptions are notes for authors and site operators, so they are not translated.
postSurveyActionsView.dragHint=Drag a row to change the order.
postSurveyActionsView.addAction=Add action…
postSurveyActionsView.grid.order=Order
postSurveyActionsView.grid.name=Name
postSurveyActionsView.grid.description=Description
postSurveyActionsView.grid.url=Address
postSurveyActionsView.empty=No post-survey actions. The site calls nothing when a respondent finishes.
postSurveyActionsView.edit=Edit…
postSurveyActionsView.remove.header=Remove post-survey action
postSurveyActionsView.remove.text=Remove the action "{0}"? Sites applying the next export stop calling it, and delete their record of which respondents it was called for.
postSurveyActionsView.removed=Removed action "{0}"
postSurveyActionDialog.title.add=Add post-survey action
postSurveyActionDialog.title.edit=Edit post-survey action
postSurveyActionDialog.name=Name
postSurveyActionDialog.description=Description
postSurveyActionDialog.url=Address
postSurveyActionDialog.url.helper=The full http(s) address the site's Survey calls, for example http://fhhs:8080/familyhistory/generate. It may hold {respondentId}, and {externalId} when the action sends the External ID.
postSurveyActionDialog.help.name=Identifies the action in this list and on a site's Connections screen; not shown to respondents.
postSurveyActionDialog.help.url=Called exactly as written, from the site's Survey; it must be reachable from there.
postSurveyActionDialog.added=Added action "{0}"
postSurveyActionDialog.updated=Updated action "{0}"
postSurveyActionDialog.method=Method
postSurveyActionDialog.method.helper=How the site's Survey calls the address; POST unless the service expects GET or PUT
postSurveyActionDialog.sendsExternalId=Send the subject's External ID
postSurveyActionDialog.sendsExternalId.warning=The External ID usually identifies the person in another system, such as a medical record number. The site sends it, with the department's code, on every call of this action, and only to an https address.
postSurveyActionsView.grid.method=Method
postSurveyActionsView.grid.externalId=External ID
postSurveyActionsView.grid.externalId.yes=Sent
error.postSurveyAction.methodInvalid=Unsupported method {0}; choose POST, GET or PUT
error.postSurveyAction.placeholderUnknown=The address may hold only the placeholders respondentId and externalId, not {0}
error.postSurveyAction.externalIdPlaceholderNeedsFlag={externalId} needs the action to send the External ID; tick the box or remove the placeholder
error.postSurveyAction.externalIdNeedsHttps=An action that sends the External ID must use an https address
error.postSurveyAction.gone=The post-survey action no longer exists
error.postSurveyAction.nameRequired=Name is required
error.postSurveyAction.nameTooLong=Name must be 255 characters or fewer
error.postSurveyAction.nameTaken=This survey already has an action named ''{0}''
error.postSurveyAction.descriptionRequired=Description is required
error.postSurveyAction.descriptionTooLong=Description must be 255 characters or fewer
error.postSurveyAction.urlRequired=Address is required
error.postSurveyAction.urlTooLong=Address must be 255 characters or fewer
error.postSurveyAction.urlInvalid=Enter the full http(s) address the site's Survey calls; a path is not enough for an action
reportMarkDialog.title.mark=Mark for a report
reportMarkDialog.title.label=Edit label
reportMarkDialog.element=Element
reportMarkDialog.element.section=Section {0} — {1}
reportMarkDialog.element.question=Question {0} — {1}
reportMarkDialog.appliesTo=Applies to
reportMarkDialog.scope.question=Wherever the question appears
reportMarkDialog.scope.questionInSection=Only in section {0}
reportMarkDialog.scope.section=The section as mounted in step {0}
reportMarkDialog.report=Report
reportMarkDialog.label=Label
reportMarkDialog.label.helper=Optional. Replaces the element's own text on the report; translatable on the Translations page.
reportMarkDialog.disclosure=The respondent report is reachable again with the access code, so a marked answer is exactly as private as that code.
reportMarkDialog.error.chooseElement=Choose an element
reportMarkDialog.error.chooseScope=Choose what the mark applies to
reportMarkDialog.error.chooseReport=Choose a report
reportMarkDialog.marked=Marked "{0}" for the {1}
reportMarkDialog.relabeled=Updated the label
reportMarkDialog.notMarkable=A {0} question has no value to show on a report, so it cannot be marked.
elementMarksDialog.title=Report marks — {0}
elementMarksDialog.addMark=Add mark…
elementMarksDialog.empty=Not on any report yet.
elementMarksDialog.grid.report=Report
elementMarksDialog.grid.appliesTo=Applies to
elementMarksDialog.grid.label=Shown as
reportsView.preview=Preview in Survey…
reportsView.preview.hint=Opens the preview Survey. Finish the survey there as a preview respondent to see the respondent report with the marks as they stand.
```
