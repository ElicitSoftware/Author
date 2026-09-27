# Showing Translated Survey Content Where the Author Edits It

**Status:** research / proposal, 2026-09-25. Nothing here is implemented. This
document depends on `Survey/docs/research/i18n_survey.md` ("Translating Survey
Content in the Database", revised 2026-09-25) and adds nothing to the schema, to
the `.elicit` file, or to the Survey runtime. It is a user-interface proposal for
Author alone.

Source: the Survey proposal named above (sections 3.2, 4.1, 5.1–5.5, 10, 11.2);
this module's `docs/requirements.md` (FR-035, FR-036, FR-038, FR-049 to FR-053,
NFR-001, NFR-012, NFR-013, NFR-018, NFR-019, C-015, C-017, C-018, C-019, C-023,
C-024), `docs/scd-contract.md`, UC-006, UC-011, UC-014, UC-016, UC-018, UC-019,
UC-021, UC-040; and the view and service layer under
`src/main/java/com/elicitsoftware/author/` — in particular
`survey/SurveyStructureQuery.java`, `survey/StructureNode.java`,
`survey/ListService.java`, `survey/ElementService.java`,
`flow/designer/SurveyDesignerView.java`, `flow/designer/SectionPreview.java`,
`flow/designer/QuestionDialog.java`, `flow/designer/ElementDialog.java`,
`flow/designer/SelectGroupDialog.java`, `flow/designer/RelationshipDialog.java`,
`flow/SurveyMetadataDialog.java`, `flow/ElementView.java`,
`flow/StructureNavigationTree.java`, and the `i18n` package
(`Translations.java`, `LocaleSelection.java`, `LocaleLayout.java`,
`LocaleDirectionConfig.java`, `LanguageSwitcher.java`).

Sibling proposals: `docs/research/undo-redo.md`,
`docs/research/session-discard.md`, `docs/research/rule-order-validation.md`.

## Question

The Survey proposal stores other languages of a survey's content in
`survey.translations` and gives Author a **Translations view** — one grid per
language over every translatable string — plus a JSON hand-off file for a
translator or an AI agent. That is the bulk surface. It leaves the rest of Author
monolingual: the designer board, the structure tree, the five editor dialogs, the
two previews and the element details page all keep showing the base-language
text, whatever language the author is working on.

The ask is the other half: **when the author is working in a content language,
show that language's text in the places where the author edits text.** What does
that take, and what must it deliberately not do?

## Short Answer

Add a **content language** to Author — a per-survey, per-session display mode,
independent of the chrome locale — and resolve every authored string through one
read model at the point of display.

- **One selector**, in the survey header and the designer toolbar, offering the
  survey's base language plus its `content_languages`. Hidden entirely when a
  survey has no content language, so a single-language survey looks exactly as it
  does today.
- **One read model, `ContentText`**, loaded once per view render (the way
  `optionTexts` and `tagLabels` already are) and passed down, never queried per
  node. It answers `text(elementKey, field, base)` and `status(elementKey,
  field)`. Author's copy differs from Survey's `ContentTranslator` in two ways
  that matter: there is no as-of resolution (Author holds working copies only),
  and **a stale translation is still shown** in the editing surfaces, because the
  author's job is to see it and fix it.
- **The display sites are already inventoried.** Every place Author renders
  authored content carries `data-i18n-content` (26 sites in 16 classes, put
  there for `DisplayedStringsSweepTest`). That attribute set is the work list,
  minus the reporting surfaces, whose strings are SQL identifiers.
- **The dialogs get a paired field, not a language tab.** Each translatable field
  grows a second input labeled with the content language, with the base string
  shown beneath it; the element's `Binder`, its working bean and its
  caller-supplied persist function are untouched, and the translations are
  written by a separate call keyed on the element key. This answers the objection
  in Survey §5.3 rather than overriding it: the cost there was a second bean and
  a second persist path for the *element* in five dialogs, and there is neither.
- **The two previews follow the runtime, not the editor.** `SectionPreview` and
  `QuestionDialog`'s live preview show what Survey would actually serve —
  base text where the translation is missing or stale — with a caption saying how
  many strings fell back and why, and with `dir` taken from the content language
  so an Arabic preview reads right-to-left inside an English left-to-right
  chrome. This is the surface that catches a translation that is faithful but
  three times too long for its field.
- **An invariant with a test behind it:** a translated string is never assigned
  to a mapped element field. The content language changes what Author *shows* and
  where a *second* string is stored; it never changes where the base text lives.
- **A list of surfaces that stay base-language whatever is selected**: validation
  messages, removal and restore plans, the export dialog and file, the rename and
  reporting ledgers, the reporting view, and anything that names an element to
  someone who will act on it outside the respondent's language.

The rest of this document works through each of those.

## 1. What the Survey proposal leaves Author with

### 1.1 The prerequisites this proposal assumes

Nothing here is buildable until the Survey change set lands. It assumes, verbatim
from that document:

| From Survey §| What Author gets |
| --- | --- |
| 3.2 | `survey.translations` (element-key keyed, Type 2, `source_hash`), `surveys.base_language`, `surveys.content_languages` |
| 3.2 | `model/Translation.java` in Author extending `StructuralElement`, so retire/restore and `@PrePersist ensureElementKey()` apply unchanged |
| 5.1 | `author/survey/TranslatableFields.java`, the whitelist of `(element_type, field)` pairs, agreeing with Admin's `SurveyDefinitionFileFields` and Survey's copy by test |
| 5.2 | `base_language` and `content_languages` edited in `SurveyMetadataDialog` |
| 5.3 | `TranslationService.rows(...)` / `.upsert(surveyId, elementType, elementKey, field, language, value)`, computing `source_hash` and `source_text` from the current base text |
| 5.3 | `TranslationsView`, route `survey/:surveyId/translations` — the bulk grid |
| 5.4 | `ElementService.remove` / `restore` cascading to translations; the stale-wording notification on base edits; `ExportValidation` warnings |
| 5.5 | The JSON hand-off export and `TranslationImportService` |

This proposal adds **no table, no column, no `.elicit` record, no migration and
no change to Admin or Survey**. It is Author view code, one read model, one
session-scoped selection, and the write path the dialogs need. That is worth
stating plainly, because it means this work can be scheduled independently of
anything that has to be deployed to a site (Survey §10 phase 4b, section 12
below).

### 1.2 Why this is not a reversal of Survey §5.3

Survey §5.3 rejected "language tabs inside the five Binder-bound editor dialogs",
and the reasons were specific: each dialog is a `Binder` over a **detached
working bean** with a **caller-supplied persist function**
(`QuestionDialog.java:85`, `:170-185`; `ElementDialog.java:91-93`;
`SelectGroupDialog.java:59-98` with its hand-rolled `OptionRow`s and
`OptionDraft`), so per-language fields would mean a second bean and a second
persist path in five dialogs; and a translator working through a whole survey
would have to open every dialog.

Both objections survive, and neither is contradicted here:

- **No second bean, no second persist path for the element.** The paired field
  (section 5.2) is not bound to the element bean at all. It is read from and
  written to `survey.translations` by element key. `Question`, `Step`, `Section`,
  `SelectGroup` and `Survey` beans, their binders and their persist functions are
  byte-for-byte what they are today.
- **This is not the translator's workflow.** A translator still gets the hand-off
  file (Survey §5.5) or the grid (§5.3). The surface proposed here serves a
  different person doing a different thing: the **author**, who is looking at a
  question for some other reason and wants to see — or fix — what it says in
  Spanish. Its unit of work is one element, not one survey.

The two surfaces write through the same `TranslationService.upsert`, show the
same three-word status vocabulary, and are reachable from each other.

## 2. The surfaces, exactly

Author already marks every subtree that carries authored survey content with
`data-i18n-content`, so that `DisplayedStringsSweepTest` can skip it when it
asserts that every *chrome* string went through the provider. That attribute is
therefore a complete, maintained inventory of the places this feature has to
touch. As of this writing there are 26 of them:

| Class:line | What it renders | In scope? |
| --- | --- | --- |
| `designer/StepLane.java:78` | step name on the lane header | **Yes** — `steps.name` |
| `designer/SectionCard.java:77` | section name on the card header | **Yes** — `sections.name` |
| `designer/QuestionRow.java:67` | question label in a card | **Yes** — `questions.short_text` or `text` |
| `designer/QuestionRow.java:70` | question type word | No — a lookup name, not authored |
| `designer/SectionPreview.java:111` | section name as the page title | **Yes**, under runtime rules (5.3) |
| `designer/SectionPreview.java:154` | the question as the respondent sees it | **Yes**, under runtime rules (5.3) |
| `designer/QuestionDialog.java:268` | the live field preview | **Yes**, under runtime rules (5.3) |
| `designer/SelectGroupDialog.java:147` | the option rows | **Yes** — `select_items.display_text` only |
| `designer/ListPanel.java:73` | list name in the drawer | No — `select_groups.name` is author-facing |
| `designer/QuestionBankPanel.java:66` | question label in the drawer | **Yes** — same field as the row |
| `designer/SectionBankPanel.java:66` | section name in the drawer | **Yes** — `sections.name` |
| `designer/TagBadge.java:42` | reporting tag names | No — SQL identifiers (`ReportingNames`) |
| `flow/SurveyEditorView.java:79` | the survey title | **Yes** — `surveys.title` |
| `flow/ElementView.java:101` | the element heading | **Yes** |
| `flow/ElementView.java:209` | every authored fact value | **Yes**, for whitelisted fields only |
| `flow/ElementView.java:231`, `:290` | removal / restore dependents | No — identity (section 5.6) |
| `flow/ImportDefinitionView.java:73` | the imported survey name | No — `surveys.name` is an identifier |
| `flow/reporting/*` (8 sites) | tags, dimensions, table and column names | No — identifiers |

Two surfaces that are *not* marked also display authored text and must be
included: `StructureNavigationTree` (the drawer tree, which labels nodes from
`StructureNode.label()`) and `RelationshipDialog`'s `defaultUpstreamValue` field
(`RelationshipDialog.java:64`), which is the one translatable field on
`relationships`. Adding `data-i18n-content` to both is part of this work.

## 3. Two languages at once

### 3.1 The chrome locale and the content language are different things

Author already has a chrome locale: `LanguageSwitcher` in the header, resolved by
`LocaleSelection` into the Vaadin session, applied to `UI.getLocale()` and to the
page direction by `LocaleLayout` (UC-040). That locale decides what *Author's own
labels* say. It must not decide what the *survey* says, for three reasons:

1. An author whose tool is in English routinely reviews Spanish content, and an
   author whose tool is in Spanish routinely authors English content. Conflating
   the two makes one of those impossible.
2. **Author is the master, so it is not bound by the site rule.** Survey §11.2
   offers a content language only where it is both in `content_languages` and
   mounted for the chrome at that site. That rule is right for a deployed site
   and wrong for Author: the whole point of §11.1 is that the master holds and
   edits `ja` for a Japanese site whose chrome file never comes near Author. So
   Author's content-language list is `surveys.content_languages` (union any
   language that already has rows, so a language removed from the set is still
   reachable to clean up), **independent of
   `ElicitI18NProvider.getProvidedLocales()`**. Proposed as C-026.
3. The pseudo-locale (`ElicitI18NProvider.PSEUDO_LOCALE`) that
   `DisplayedStringsSweepTest` runs under is a chrome concept. Content must not
   follow it, or the sweep would have nothing to skip.

### 3.2 The selection

`author/i18n/ContentLanguageSelection`, an `@ApplicationScoped` bean storing the
choice in the Vaadin session under `elicit.contentLanguage.<surveyId>`, modeled
exactly on `LocaleSelection` (`LocaleSelection.java:28`, `:62-70`). Per survey,
because a session may hold two surveys with different content languages; in the
session, because like the chrome choice it is a working preference and not
something to store against the author (UC-040 BR-007).

`author/flow/ContentLanguageSelector`, a `Select<Locale>` whose items are "Base —
English" plus the survey's content languages, labeled in their own language by
the same `LanguageSwitcher.displayName` helper (`LanguageSwitcher.java:78-84`).
It appears in two places: the `SurveyEditorView` toolbar beside **Design** and
**Reporting** (`SurveyEditorView.java:70`), and the designer toolbar beside the
three view checkboxes (`SurveyDesignerView.java:176`). Changing it re-renders the
current view — `SurveyDesignerView.render()` for the designer, `load(id)` for the
editor — and never reloads the page, which is what the chrome switcher has to do.
Views that carry no selector of their own — the element details page, every
dialog, and the Translations view's default language — read the same session
value, so the choice follows the author from the board into an element and back.

**When a survey has no content language the selector is not added at all**, so
every existing view test, screenshot and manual figure stays valid, and a
single-language deployment sees no new control. This mirrors
`LanguageSwitcher.setVisible(locales.size() > 1)` (`LanguageSwitcher.java:42`).

### 3.3 Direction and the `lang` attribute

Content in a language is not content in the chrome's direction. Every subtree
that renders translated text gets, on the element that already carries
`data-i18n-content`:

- `lang` = the content language's tag, so screen readers and spell-checkers
  switch voice and dictionary mid-page (WCAG 3.1.2, under NFR-007); and
- `dir` = `LocaleLayout.directionFor(contentLocale).getClientName()`
  (`LocaleLayout.java:45-47`), which already consults `LocaleDirectionConfig` and
  therefore honors a deployment's `i18n-config.json` override.

Both are dropped when the base language is selected. The designer board itself
stays in the chrome's direction — an Arabic *preview* inside an English
left-to-right board is correct and is what the author asked to see; a board that
flipped to RTL because the *content* is Arabic would be wrong, and is what
FR-052 already does for the chrome when the chrome is Arabic.

## 4. The read model

```java
@ApplicationScoped
public class ContentText {

    /** Everything one survey has in one language, read once per render. */
    public record Bundle(Locale language, boolean base, Map<Target, Entry> entries) {

        public record Target(UUID elementKey, String field) { }
        public record Entry(String value, String sourceHash, boolean stale) { }

        /** The string to show an author: the translation when there is one, else the base text. */
        public String text(UUID elementKey, String field, String base) { ... }

        /** What the runtime would serve: base text when missing *or* stale (Survey §3.2). */
        public String asServed(UUID elementKey, String field, String base) { ... }

        public Status status(UUID elementKey, String field) { ... } // TRANSLATED | STALE | MISSING
    }

    public Bundle load(int surveyId, Locale language);   // one query; base() when language is null
    public Counts counts(int surveyId, Locale language); // translated / missing / stale, for badges
}
```

Four decisions inside it:

- **`text` shows a stale translation; `asServed` does not.** Survey's
  `ContentTranslator` never serves stale text, because a rewritten clinical
  question shown with its pre-rewrite translation is worse than English. Author's
  editing surfaces have the opposite need: the stale string is the thing the
  author is there to look at. So the editors call `text` and decorate it with the
  status; the two previews call `asServed`, because their contract is "what the
  respondent will see" (section 5.3). One bundle, two accessors, and the
  difference is the whole of the runtime/editor distinction.
- **No as-of.** Author holds one row per element, `version = 0`, sentinel
  `effective_to` (C-018). The load predicate is `surveyId = ?1 and language = ?2
  and effectiveTo = ?3` against `translations_target_current_un`, and nothing in
  Author ever resolves a translation as of an instant. Versions are created by
  Admin at the site (Survey §7).
- **No cache.** The bundle is loaded once per render and threaded down exactly as
  `SurveyDesignerView` already threads `questions`, `optionTexts` and `tagLabels`
  (`SurveyDesignerView.java:600-610`). A 200-question survey has on the order of
  1,500 translatable strings per language; one indexed query returning small rows
  is cheaper than any invalidation scheme, and it cannot go stale after a write
  the way a cached map can. Section 8 says what to measure before revisiting
  this.
- **Staleness is computed in Java**, comparing the stored `source_hash` against
  the hash of the current base text with the shared `ContentHash` utility (Survey
  §3.2, "Hash rule"), never in SQL. The bundle already holds both.

`StructureNode` needs one addition to be usable here. A question node's label is
*either* `short_text` or a flattened `text` (`SurveyStructureQuery.java:100`), so
a display site cannot know which translation to look up. The record gains a
`labelField` component, set by the query, and one helper:

```java
public String labelIn(ContentText.Bundle b) {
    String s = b.text(elementKey, labelField, label);
    return "text".equals(labelField) ? SurveyStructureQuery.plain(s) : s;
}
```

so the flattening that `plain()` does to the base text (`:119-125`) is applied to
the translation too, and no call site repeats the rule.

**`label()` keeps meaning the base text.** That is deliberate. It is read by
notifications, aria labels, menus, removal plans, the rule dialog's paths and the
ledgers, and only some of those should change language. Making `label()` return
the translation would silently translate all of them; making display sites call
`labelIn(bundle)` forces each of the 26 sites to make the choice on purpose, and
section 5.6 is the list of the ones that answer "no".

## 5. Each surface

### 5.1 The board, the banks and the tree

`StepLane.name`, `SectionCard.name`, `QuestionRow.label`, the two bank panels'
labels and `StructureNavigationTree`'s node captions all switch to
`node.labelIn(bundle)`. Each gains a small status affordance next to the text:
nothing when translated, a muted dot with the tooltip "Not translated — showing
English" when missing, and a warning dot with "Out of date — respondents see
English" when stale. The board is dense (NFR-013 already restricts it to tablet
width and up), so this is a dot and a `title`, not a badge with words.

Two knock-on details:

- **The drawer search must match both languages.** Each panel row builds a
  lower-cased `searchText` and filters against it: label, question text and type
  in `QuestionBankPanel` (`:53-54`), label and description in `SectionBankPanel`
  (`:54`), and name, description and *option texts* in `ListPanel` (`:59-60`).
  Each gains the translated strings for its translatable parts — including the
  option texts in `ListPanel`, even though the list's own name is never
  translated — so an author who types a Spanish word finds the question.
- **Drag payloads keep the base label.** `BankQuestion` and `PaletteList` carry a
  label into a drop (`QuestionBankPanel.java:60`, `ListPanel.java:67`); it is used
  for the drop's notification and, in `PaletteList`, is the list *name*, which is
  never translated anyway. Leave both as they are.

`SectionCard`'s `questionCount`, `mountBadge` and the counts in the panels are
chrome and stay translated by the chrome locale, as they are now.

### 5.2 The five dialogs: a paired field

A new `flow/designer/TranslatedField` component. Given the base field, the
element type, the field name and the bundle, it renders under (or beside, on a
wide dialog) the base input:

```
┌ Question text ────────────────────────────────────────┐
│ Has {S1's mother|your mother} ever been diagnosed …    │   ← existing, bound to the bean
└───────────────────────────────────────────────────────┘
┌ Spanish (es-419) ─────────────────────────── ⚠ out of date ┐
│ ¿{La madre de S1|Su madre} ha tenido cáncer?          │   ← new, not bound to the bean
└───────────────────────────────────────────────────────┘
  Was translated from: "Has {S1's mother|your mother} had cancer?"        ⟨Open in Translations⟩
```

Mechanics:

- The component is **added only when the content language is not the base
  language**, and only for fields in `TranslatableFields`. With the base language
  selected, every dialog is exactly the dialog that exists today.
- It carries the same per-item validation the hand-off import applies (Survey
  §5.5): the token set must equal the token set of the *base field's current
  value in the dialog* (not the stored one, so an author changing both at once is
  checked against what they are actually saving), braces and bars balanced via
  `Tokens.PLACEHOLDER`, and length within `TranslationBudget.of(field, source)`.
  Failures mark the field invalid and block Save, as any other field does.
- For a stale row it shows `source_text` as the "was translated from" line — the
  authoring-only snapshot that Survey §3.2 stores and §6 does not export. That
  line is the single best argument for this whole feature: it is the only place
  an author can see the old wording, the new wording and the translation at once.
- The dialogs collect their components into a `List<TranslatedField>` and expose
  `translationEdits()`.

Per dialog:

| Dialog | Paired fields |
| --- | --- |
| `QuestionDialog` | `text`, `short_text`, `tool_tip`, `placeholder`, `validation_text` — and they follow the same `updateChoiceFields()` visibility rules (`QuestionDialog.java:317-346`), so a hidden base field never shows a visible translation |
| `ElementDialog` (step, section) | `name`, `description` — not `dimensionName`, which is a reporting identifier |
| `SelectGroupDialog` | each `OptionRow`'s `displayText` — not `codedValue`, not the group's own name or description |
| `RelationshipDialog` | `defaultUpstreamValue` only, inside the existing "Advanced" `Details` (`RelationshipDialog.java:206`) |
| `SurveyMetadataDialog` | `title`, `description` — never `name`, which names files and Admin lists |

### 5.3 The two previews follow the runtime

`SectionPreview` (FR-038) and `QuestionDialog`'s live preview (FR-035) have a
different contract from every other surface: they promise what the *respondent*
sees. So they call `asServed`, which means **a missing or stale translation shows
the base text**, exactly as Survey would serve it, and the author sees the real
consequence of leaving a translation stale instead of a comforting Spanish
sentence that no respondent will ever be shown.

- A caption above the pane names the language and the fallbacks: "Preview in
  Spanish (es-419) — 3 of 14 strings show English: 2 not translated, 1 out of
  date." Clicking it opens the Translations view filtered to that section.
- Token substitution runs on the translated string with the same values
  (`PreviewFields.substituteTokens`, `SectionPreview.java:111`, `:150-152`). The
  import rule that a translation must use exactly the source's token set (Survey
  §5.5) is what makes this safe. Note the known wart: Survey's `replaceTokens`
  applies English possessive handling (`QuestionManager.java:99-101`), so a
  possessive inside a token phrase may render wrongly in other languages at
  runtime; the preview will faithfully reproduce whatever the runtime does only
  once that is fixed or documented, and this proposal does not fix it.
- `optionTexts` must carry keys. `ListService.optionTexts` returns
  `Map<Integer, List<String>>` (`ListService.java:73-76`), which has thrown away
  the `select_item_key` the bundle needs. It becomes
  `Map<Integer, List<Option>>` with `record Option(UUID selectItemKey, String
  displayText)`, and its three call sites (`SurveyDesignerView.java:594`,
  `:610`, `ListPanel`) follow.
- The preview pane carries `lang` and `dir` from section 3.3. This is where a
  50 % longer Spanish option label or an RTL Arabic radio group becomes visible,
  which is the reason the length budget in Survey §5.5 exists at all.

### 5.4 The element details page

`ElementView` shows one element's facts (`ElementView.java:140-180`). Each
whitelisted fact — step/section `name` and `description`, question `text`,
`tool_tip`, `validation_text`, `placeholder`, and the heading — shows the
translated value with the base value beneath it in the muted secondary style, and
the status word. Non-whitelisted facts (`dimensionName`, `mask`, `variant`,
`defaultValue`, the reporting tags, the element key, the durable id) are
unchanged. The removal and restore dependent lists stay base-language (5.6).

### 5.5 The survey editor

`SurveyEditorView.heading` shows the translated `surveys.title`. Beside the
existing **Design** / **Reporting** / **Edit details** / **Export** buttons, the
selector and a per-language progress line: "Spanish (es-419): 412 translated, 18
missing, 3 out of date — Open translations." The validation panel
(`SurveyEditorView.java:107-120`) already renders `ExportValidation` issues and
will show the two warnings Survey §5.4 adds; no change here beyond them.

### 5.6 What never changes language

A translated string must not appear where it would mislead someone acting outside
the respondent's language. The rule: **if the string names an element to a human
who will act on it in Author, in Admin, in the file or in a log, it is the base
language.** Concretely:

- `ExportValidation` messages and the export dialog — the file they describe is
  base-language plus translation rows, and a site operator reading Admin sees
  base-language names.
- `ElementService.RemovalPlan` / `RestorePlan` dependent lists
  (`ElementService.java:169`, `:279`; `ElementView.java:231`, `:290`).
- `RenameLedger` and `ReportingChangeLedger` entries.
- The whole reporting section (`ReportingView`, `TagDialog`,
  `TagAssignmentDialog`, `ElementTagsDialog`, `TagBadge`): tags, dimensions and
  derived table and column names are SQL identifiers via `ReportingNames`.
- `surveys.name`, `select_groups.name`, `steps_sections.display_key`,
  `questions.default_value`, `mask`, `variant`, `sample`, `select_items.coded_value`,
  `relationships.token`, `reference_value` and `description` — none of which is on
  the whitelist (Survey §5.1), and `default_value` in particular is written into
  `answers.text_value` and analyzed, so translating it would fork stored data by
  language.
- Notifications that confirm a save quote the element by its base name, because
  the next thing the author does with that name is find it in a file or a grid.

Proposed as NFR-021 and covered by a test that renders the reporting view and the
validation panel under a content language and asserts nothing moved.

## 6. Writing a translation from an editing surface

Saving a dialog now has two effects: the element (unchanged) and its translations
(new). Two shapes were weighed.

**A. Compose at the call site.** The dialog exposes `translationEdits()`; the
view that supplied the persist function composes it:

```java
// today (SurveyDesignerView.java:503-504):
//   new QuestionDialog(survey.id, current, null, q -> designer.update(survey.id, q), saved -> load(), listService)
QuestionDialog dialog = new QuestionDialog(survey.id, current, null, q -> {
    Question saved = designer.update(survey.id, q);                    // unchanged, @Transactional
    translations.upsertAll(survey.id, "questions", saved.questionKey,  // second transaction
            contentLanguage(), pending.translationEdits());
    return saved;
}, saved -> load(), listService);
```

(`pending` is the dialog's edits holder, handed to the view before the dialog is
constructed so the persist lambda can close over it; the same shape works for
`ElementDialog.step` / `.section`, `RelationshipDialog` and
`SurveyMetadataDialog`, whose persist functions are supplied the same way at
`SurveyDesignerView.java:499-502`, `SurveyEditorView.java:77` and
`SurveyDesignerView.java:934`.)

Each service method is already its own transactional unit (C-018; see
`docs/research/undo-redo.md` §1), so this adds one more of them. The failure mode
is benign and namable: the element is saved, the translation is not, and the
author is told "The question was saved. Its Spanish text was not — open
Translations to retry." Nothing is corrupted, because a translation is an
independent row keyed on an element key that now exists.

**B. A transactional façade** — `ElementService.saveWithTranslations(...)` — one
transaction, both writes. Cleaner semantics, but it means a new method per
element kind wrapping five existing services, and it re-introduces the
"second persist path" that Survey §5.3 objected to.

**A is recommended**, with one exception: `SelectGroupDialog`, where the option
rows are saved as a whole by `ListService.create` / `update`
(`ListService.java:86`, `:102`) and a new option has no `select_item_key` until
that method runs. There, `OptionDraft` gains a `translation` component and
`ListService` writes the translation inside its own existing `@Transactional`
method, which is both simpler and atomic. So: composition everywhere except
lists, where the draft already exists to carry exactly this kind of thing.

Two properties of `upsert` make the whole thing well-behaved:

- It recomputes `source_hash` from the **current** base text (Survey §5.5), so an
  author who rewrites a question and its Spanish in one Save gets a translation
  that is current, not stale. That is the single most common editing motion this
  feature enables, and it is free.
- Clearing a paired field is a **retirement**, not an empty value: the row's
  `effective_to` is closed through the same retire path `ElementService` uses for
  any other structural element (Survey §5.4). `translations.value` is `NOT NULL` and there is no "blank
  current row". The dialog asks once ("Remove the Spanish text for this field?
  Respondents will see English.") before doing it.

## 7. Stale wording, in context

Survey §5.4 has each editor notify the author when a base edit strands a
translation: "The wording of this question changed. Its 2 translations (es-419,
ar) are now out of date…" That notification is right when the author is working
in the base language and cannot see the damage.

With a content language selected the damage is on screen, so the rule sharpens:

- If the save changed a base field **and** the paired translation for the current
  content language was edited in the same save, no notification for that language
  — nothing went stale.
- If the save changed a base field and the pair was left alone, the notification
  fires, naming only the languages actually stranded.
- The board's status dots and the preview caption update on the same render, so
  the author sees the consequence in the place they made the change.

This also removes the worst version of the problem, which is an author rewriting
twenty questions in one sitting and learning at export time that a language went
entirely stale.

## 8. Performance

NFR-001 gives any authoring view 2 seconds for a 200-question survey. The board
already rebuilds wholly on every edit (`SurveyDesignerView.render()`,
`:216-227`), running the structure query, the tag collection, the token samples
and the option texts. This feature adds exactly one query per render, on
`translations_target_current_un`, returning at most a few thousand short rows,
and a hash comparison per row in Java.

Rules that keep it there:

- **One load per render**, threaded down like `optionTexts`. No display site may
  query. A `ContentTextQueryCountTest` in the style of the existing service tests
  asserts one statement per board render.
- **Do not fetch `source_text`** in the board's load — it is only needed by the
  dialogs' "was translated from" line and by the Translations view, and it is the
  one wide column on the table. Two projections: a lean one for rendering, a full
  one for a single element.
- Revisit caching only if a profile shows the query mattering; the invalidation
  bug it would introduce is worse than the query it would save (section 4).

## 9. Accessibility

- `lang` and `dir` per content subtree (3.3), which is WCAG 3.1.2 and is the part
  of this feature most likely to be forgotten.
- The status dots are not color alone: each carries a `title` and an
  `aria-label` with the status word, and the aria labels that already interpolate
  a node's label (`QuestionRow.java:44-45`, `SectionCard.java:101-103`) get the
  displayed label plus the status, so a screen-reader user hears "Question, ¿Ha
  tenido cáncer?, translation out of date".
- The selector is keyboard reachable in the toolbar and does not reload the page,
  so focus is not lost (NFR-012's spirit, though it is not a drag action).
- Paired fields are ordinary Vaadin fields with labels; nothing here bypasses
  component APIs (C-003).

## 10. Tests

New:

- `ContentTextTest` — fallback to base, `text` shows a stale row, `asServed`
  does not, status classification, base bundle is empty and free.
- `SurveyStructureQueryTest` additions — `labelField` is recorded; `labelIn`
  picks `short_text` over `text` and re-flattens a translated `text`.
- `SurveyDesignerViewContentLanguageTest` (browserless) — board, banks and tree
  labels switch; a missing translation shows the base text and a dot; the
  selector is absent when `content_languages` is empty.
- `SectionPreviewContentTest` — a stale translation previews as base text with
  the caption counting it; `dir="rtl"` and `lang="ar"` on the pane for Arabic;
  translated option labels reach the field.
- `QuestionDialogTranslationTest` — paired fields appear only with a content
  language and follow `updateChoiceFields` visibility; token-set, brace and
  budget validation block Save; editing base and translation in one save leaves
  the translation current (hash taken from the new base); clearing retires.
- `SelectGroupDialogTranslationTest` — option translations round-trip through
  `OptionDraft` and land in one transaction with the options.
- `ContentLanguageDoesNotLeakTest` — NFR-021: with a content language selected,
  the reporting view, the validation panel, the removal plan and an export's
  bytes are identical to the base-language run, and no mapped element field ever
  holds a translated string.
- `ContentTextQueryCountTest` — one statement per render (section 8).

Existing, which will need updating:

- `DisplayedStringsSweepTest` — must still pass: the new chrome strings
  (selector label, status words, captions, the notification) go through the
  provider, and the content subtrees keep `data-i18n-content` so the sweep skips
  them. The two newly marked surfaces (`StructureNavigationTree`,
  `RelationshipDialog.defaultUpstreamValue`) change what it skips.
- `DisplayedStringsCoverageTest` and `TranslationBundleConsistencyTest` — new
  keys in `translations.properties`, context lines in
  `translations.context.properties`, and `es-419` and `ar` files under
  `../elicit-i18n/author/`, with `i18n/TRANSLATION_REQUEST.md` regenerated.
- `ElementViewTest`, `SurveyEditorViewTest`, `MainLayoutTest`,
  `designer/*Test` — unchanged in the base language, which is the point; add one
  content-language case each rather than rewriting them.

## 11. Proposed catalog entries

**ID allocation.** The Survey change set is expected to claim FR-056 to FR-059
and UC-043 to UC-046 in Author's catalog for "Translate survey content", "Manage
content languages", "Request a content translation" and "Import content
translations" (Survey §9). This proposal therefore takes **FR-060 to FR-062,
NFR-020, NFR-021, C-026 and UC-047**, and the numbers must be re-checked against
whatever actually lands first.

Two existing entries are rewritten by the Survey change set, not by this one, and
are noted here so the dependency is visible:

- **C-024 Authored Content Language** currently says "Only the tool's own chrome
  is localized… content translation is tracked on a separate branch." It becomes
  the statement that content is authored in the survey's base language and
  translated through `survey.translations`. (Survey §9 calls this "C-021", which
  is its number on `main` before the i18n branch renumbering; on this branch it
  is C-024.)
- **UC-040 BR-005 "Authored content keeps its entered language"** likewise.
  (Survey §9 calls this "UC-034 BR-005", the pre-renumbering id.)

### Requirements

| ID | Title | User Story | Priority | Status |
| --- | --- | --- | --- | --- |
| FR-060 | See Authored Content in a Content Language | As a survey author, I want to choose one of the survey's content languages and have the designer board, the structure tree, the drawer panels, the element details and the editor dialogs show that language's text — falling back to the base text, visibly marked, where there is none — so that I can review a translation in the structure it belongs to rather than in a grid. | High | Proposed |
| FR-061 | Edit a Translation Where the Text Is Edited | As a survey author, I want each translatable field in the question, step, section, list, rule and survey editors to offer the selected content language's text beside the base text, validated for tokens and length, so that I can correct one string without leaving the element I am working on. | High | Proposed |
| FR-062 | Preview a Section in a Content Language | As a survey author, I want the section and field previews to render in the selected content language exactly as the Survey app would serve it — including its text direction, and showing the base text where a translation is missing or out of date — so that I can see length, layout and fallback problems before a respondent does. | High | Proposed |
| NFR-020 | Content-Language Read Cost | Rendering any authoring view in a content language must add at most one database query over the base-language render and must not query per displayed element; verified by a query-count test, and within NFR-001's 2-second budget for a 200-question survey. | Performance | Medium | Proposed |
| NFR-021 | Base Text Is Never Overwritten by a Translation | No code path may assign a translated string to a mapped element field; with a content language selected, an export's bytes, the validation panel, removal plans and the reporting section are identical to the base-language run. Verified by automated test. | Maintainability | High | Proposed |

### Constraints

| ID | Title | Description | Category | Priority | Status |
| --- | --- | --- | --- | --- | --- |
| C-026 | Author Offers Every Content Language | Author offers a survey's content languages regardless of which chrome translation files are mounted for Author itself. The rule that a site offers a content language only when its own chrome mount carries it is a Survey runtime rule for deployed sites; Author is the single writer for every site and must be able to author a language whose chrome it does not have. | Technical | High | Proposed |

### UC-047 Review and Edit a Translation in Context

```
# Use Case: Review and Edit a Translation in Context

## Overview

**Use Case ID:** UC-047  
**Use Case Name:** Review and Edit a Translation in Context  
**Primary Actor:** Survey Author  
**Goal:** See a survey's content in one of its translations while designing it, and correct a translation at the element where it is wrong, without leaving that element.  
**Status:** Proposed  
**Requirements:** [FR-060, FR-061, FR-062, NFR-020, NFR-021, C-024, C-026](../requirements.md)

## Preconditions

- The author is signed in with the `elicit_author` role (UC-002).
- The survey has at least one language in `content_languages` (UC-044).
- The author has opened the survey's editor or designer.

## Main Success Scenario

1. The author selects a content language from the selector in the survey editor
   or designer toolbar.
2. The system loads that survey's translations in that language and re-renders
   the current view: the board, the structure tree, the drawer panels and the
   element details show the translated text, marked where it falls back to the
   base text and where it is out of date.
3. The preview pane renders the selected section as the Survey app would serve
   it in that language, in that language's text direction, and reports how many
   strings fell back.
4. The author opens an element's editor and sees, under each translatable field,
   the field's text in the content language, with the base text beneath it and —
   for an out-of-date translation — the base wording it was translated from.
5. The author corrects the translation and saves.
6. The system validates the translation's tokens, placeholders and length,
   persists the element, then writes the translation with a source hash taken
   from the base text as saved, and re-renders.
7. The view shows the string as translated and the counts update.

## Alternative Flows

### A1: No translation exists for a string
2a. The base text is shown in the fallback style with a "not translated" marker,
    and the field's paired input is empty. Nothing is blank anywhere.

### A2: The base wording changed
4a. The paired field is marked out of date and shows the wording the translation
    was made from. The preview shows the base text for that string, because that
    is what a respondent would see (Survey §3.2).
6a. Saving a new translation clears the out-of-date state, because the hash is
    recomputed from the current base text.

### A3: Base text and translation changed in one save
6b. No out-of-date notification is raised for the current content language; it is
    raised for any other language that has a current translation of that field.

### A4: The translation is rejected
6c. Tokens that differ from the base text's, unbalanced braces or bars, or a
    length over the field's budget mark the paired field invalid with the reason;
    Save is blocked. The base field's own edit is not lost.

### A5: The author clears a translation
5a. Clearing a paired field asks for confirmation and then retires the
    translation row (closes `effective_to`); it never stores an empty value.
    Respondents in that language fall back to the base text.

### A6: The survey has no content language
1a. The selector is not shown and every view renders as it does today.

## Postconditions

### Success Postconditions
- The content language is remembered for this survey for the rest of the session.
- Corrected translations are stored as working copies (`version = 0`) with a
  source hash matching the current base text.
- No element's base-language field was changed by anything in this use case
  except an edit the author made to the base field itself.

### Failure Postconditions
- A rejected translation is not stored and the dialog stays open with the reason.
- If the element saved and its translation did not, the author is told which
  happened and pointed at the Translations view; no row is left inconsistent.

## Business Rules

### BR-001: The content language is a display mode
Selecting a content language changes what Author shows and where a second string
is stored. It never changes the language of a structural row, and it never makes
Author write a translated string into a base-language column.

### BR-002: Editors show stale translations, previews do not
An editing surface shows the stored translation whatever its state, marked. A
preview shows what the Survey app would serve, which is the base text when the
translation is missing or out of date.

### BR-003: The content language is independent of the tool's language
The chrome language (UC-040) and the content language are chosen separately and
neither constrains the other. Author offers every content language of the survey
whatever chrome files are mounted for Author (C-026).

### BR-004: Identity stays in the base language
Validation messages, removal and restore plans, export attribution, the reporting
section and any text naming an element to someone acting outside the respondent's
language remain in the base language whatever is selected.

### BR-005: One language at a time
Exactly one content language is displayed at a time, alongside the base text in
the editors. Comparing two translations is the Translations view's job (UC-043).
```

## 12. Phasing

This is Survey §10's phase 4 continued; it cannot start before that phase's
`TranslationService`, `Translation` entity and `TranslatableFields` exist, and it
is independent of phase 3 (the Survey runtime), so it can land while the runtime
is still being built.

1. **Selection and read model.** `ContentLanguageSelection`,
   `ContentLanguageSelector`, `ContentText`, `StructureNode.labelField` /
   `labelIn`, `ListService.optionTexts` carrying keys. No visible change yet
   beyond the selector.
2. **Read-only surfaces.** Board, banks, tree, survey editor heading, element
   details, status dots, counts. This alone is most of the value: an author can
   walk a survey in Spanish.
3. **Previews.** `SectionPreview` and the question preview under `asServed`, with
   `lang`, `dir` and the fallback caption.
4. **The write path.** `TranslatedField`, the five dialogs, `OptionDraft`'s
   translation, the sharpened stale notification (section 7).
5. **Docs.** The catalog entries above; `docs/entity_model.md` needs nothing new
   (the `TRANSLATION` entity arrives with the Survey change set); the author's
   manual (`docs/manual/elicit-author-manual.tex`) gains a section on working in
   a content language, with a screenshot of the paired field and one of an RTL
   preview; `docs/ai/AUTHORING_FOR_AI.md` is untouched by this proposal, since
   the file format does not change here (NFR-017's test stays green).

## 13. Open questions and risks

1. **Should the content language outlive the session?** It is proposed as session
   state, like the chrome choice. An author who works in Spanish every day will
   want it remembered; that needs a per-author preference store Author does not
   have. Worth deferring until someone asks.
2. **Progress badges on the board.** A per-step or per-section "12/14 translated"
   badge would make gaps visible without opening the grid, at the cost of more
   counting per render. Left out of the first cut deliberately.
3. **Changing a survey's base language after content exists** would mean
   rewriting every structural row from a translation and retiring the
   translations it came from. It is out of scope here and should be refused in
   `SurveyMetadataDialog` until someone designs it.
4. **Author's own chrome in a language it does not have.** Showing Japanese
   content in an English tool is the intended behavior (C-026), but the status
   words, captions and field labels around that Japanese text will be English.
   That is correct and should be said out loud in the manual, not fixed.
5. **The possessive wart.** `QuestionManager.replaceTokens`' English possessive
   handling (`QuestionManager.java:99-101`) will make a preview and a runtime
   agree on something wrong for other languages. This proposal reproduces the
   runtime faithfully; fixing the runtime is Survey's to schedule.
6. **Scope creep into the Translations view.** The paired field and the grid
   overlap, and the temptation will be to grow the paired field into a
   mini-grid ("show all languages here"). BR-005 exists to hold that line: one
   language at a time in context, all languages in the grid.
7. **Test drift.** Several existing view tests render authored text. If the content
   language defaults to anything but the base language for a fixture survey, they
   all move at once. The default must be the base language, and
   `ContentLanguageDoesNotLeakTest` is what keeps that honest.
