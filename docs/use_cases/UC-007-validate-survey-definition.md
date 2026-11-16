# Use Case: Validate Survey Definition

## Overview

**Use Case ID:** UC-007  
**Use Case Name:** Validate Survey Definition  
**Primary Actor:** Survey Author  
**Goal:** Learn, before exporting, which problems would make the definition fail or behave unexpectedly at a deployed site, and which edits will apply retroactively there.  
**Status:** Draft  
**Requirements:** [FR-005, FR-022, FR-018, FR-024, FR-061, FR-062, FR-064, FR-066, FR-067, C-011, C-012](../requirements.md)

## Preconditions

- The author is signed in with the `elicit_author` role.
- The survey exists in Author.

## Main Success Scenario

1. The author opens the survey in the editor (UC-006) or chooses Export (UC-008).
2. The system checks the current elements of the survey: every reference from one element to another resolves to a current element; every relationship has a downstream path that holds together (a step, a section mounted in it, a question assigned to that section; a question outside the upstream question's own section names its step and section) and a valid operator and action; every choice-type question refers to a select group that has at least one current item; every placeholder a step name, section name or question text uses is filled by a rule that reaches it, and every placeholder name a rule carries, whatever its action, is a name a text could write between angle brackets and is used by a text it reaches (BR-005..BR-007); every step has at least one section, every section is mounted in a step and holds at least one question, and every question is assigned to a section; every question outside the first step is reached by a Show or Repeat rule (BR-012); ordering values are unique within their parent; the survey's initial display key names a current section assignment; the survey's name and title are single-line (BR-015).
3. The system classifies each finding as an error (blocks export) or a warning (informational).
4. The system adds a retroactivity warning for each step or section whose name changed since the last export and for each question whose ontology assignment changed since the last export.
5. The system displays the findings in the validation panel, each worded for the author and in the author's language (BR-013), each naming where in the survey its element sits and, where an element can be opened, linked to it.
6. The author selects a finding and the system opens the element in the designer, selected on the board and in the structure tree, so that it can be corrected; with no errors present, the goal is achieved and export is allowed.

## Alternative Flows

### A1: No findings

**Trigger:** The checks produce neither errors nor warnings (step 3)  
**Flow:**

1. The system shows that the definition is ready to export.
2. Use case ends.

### A2: Errors present when exporting

**Trigger:** At least one error exists and the author is in the export flow (step 5)  
**Flow:**

1. The system disables Export and lists the errors in the export dialog, each with the same location line as the panel.
2. The author closes the dialog, corrects the elements and re-runs validation.
3. Use case continues at step 2.

### A3: No previous export to compare with

**Trigger:** The survey has never been exported (step 4)  
**Flow:**

1. The system omits the change-based retroactivity warnings and instead shows a general note that step and section names and ontology assignments apply retroactively at deployed sites.
2. Use case continues at step 5.

## Postconditions

### Success Postconditions

- The author has an up-to-date list of errors and warnings, each saying where its element sits and, where the element can be opened, linking to it.
- Export is enabled exactly when no errors exist.

### Failure Postconditions

- Validation could not run; the system reports why and export remains disabled.

## Business Rules

### BR-001: Errors block export, warnings do not

Anything a deployed site would reject or that would leave a respondent without a required option is an error. Structural emptiness and retroactivity are warnings.

### BR-002: References must point to current elements

A reference to a retired element is an error, because the deployed site would otherwise install a relationship or assignment to something respondents can no longer see.

### BR-003: Retroactive edits are called out every time

Renaming a step or section relabels historical reports at deployed sites; reassigning a question's ontology reclassifies historical answers. These two, and only these two, edits are flagged as retroactive, per the stakeholder sign-off of 2026-09-12.

### BR-004: Validation is repeatable and read-only

Running validation changes nothing in the survey.

### BR-005: Tokens are checked where they are shown

A rule reaches its target and everything inside it (UC-019 BR-005), so a token in a question's text is filled when a rule carrying that token targets the question assignment, its section mount, or its step — checked for every mount of the section. A placeholder is filled only when *every* `<NAME>` in its phrase is reached that way, because filling a phrase is all or nothing (BR-006): `{<G1> and <S1>|you both}` where a rule reaches `G1` and nothing reaches `S1` fills no more than one where nothing reaches either, and the respondent reads "you both". A placeholder with no default that nothing fills is an error, because the runtime would show the respondent the raw placeholder; one with a default is a warning. A rule of any action whose token appears in no text it reaches is a warning (any rule may carry a token; UC-019 BR-005). A placeholder that names a token as a bare word rather than as `<TOKEN>` is an error: the runtime fills only the bracketed form, so the placeholder fills nothing and the respondent always reads its default, with nothing about the text looking wrong. It is reported with the corrected placeholder, and checked in a step's name, a section's name, a question's text and a rule's own default upstream value, which is itself a placeholder. Only a name the survey actually uses is reported -- one some rule carries, or one another text brackets -- matched as a whole word, so prose is never mistaken for a token. A rule whose token is fed by a display-only question — one that collects no answer, so only the rule's own default upstream value can fill the slot (UC-019 BR-006) — is an error when the rule has no such value, because nothing can ever fill the token, and a warning when it has one, because the rule works only by way of that constant.

### BR-006: Every reference in a phrase must be a token some rule defines

Filling a phrase is all or nothing: the runtime abandons the whole placeholder the moment one
`<NAME>` in it has no value, and the respondent reads the default instead. So a reference no rule
defines does not merely fail on its own — it stops the placeholder filling at all, permanently and
invisibly, which is the same silent failure BR-005 exists for. Every `<NAME>` in a phrase is
therefore an error unless some rule in the survey carries that token.

Only the phrase is checked. The default after the bar is used verbatim and is never scanned for
references, so a `<…>` there is just text.

This is also how markup written inside a placeholder is caught. Markup belongs around a
placeholder, not within it: `<b>{<NAME>|name}</b>`, not `{<b><NAME></b>|<b>name</b>}`. A tag in the
phrase cannot be told from a token by its spelling — `<b>` is a well-formed token name, which is
exactly why position is the rule — but it can be told by the survey defining no token `b`. The
report names the undefined token and gives the fix, which is the same in both readings: move the
tag outside the braces, `<b>{`. Written inside, the tag is also duplicated into both branches of a
placeholder of which only one is ever chosen, so the pair can drift apart or straddle the bar and
leave the page unbalanced.

Checked in a step's name, a section's name, a question's text and a rule's own default upstream
value, which is itself a placeholder. Translators are given the same rule, so a phrase handed over
is prose and keys and never markup (UC-045 BR-003), and the hand-off's `html` flag means markup
outside the placeholders rather than merely an angle bracket somewhere in the string.

### BR-007: A rule's token is a name, not a reference

The runtime keys a text's token values on `relationships.token` exactly as the column holds it, and
asks for the name it parsed out of a reference. A token stored `<S1>` is therefore asked for as `S1`,
never matches, and every placeholder meant to use it falls back to its default forever — while the
rule reads correctly in the designer and in the arrow's tooltip, both of which show the token as
`<S1>` whichever way it is stored. A token holding a character a reference cannot spell (`S 1`,
`{S1}`, `Q#`) is unnameable the same way. Both are errors: nothing the rule reaches can ever fill.

A name that merely is not the canonical spelling — lower case, or not starting with a letter — still
fills, because every match ignores case, so it is a warning and not an error: a survey that runs
today is not blocked from export over spelling. What it costs is the survey's token list, which then
offers two names for one slot.

An unnameable token is reported once, at the rule. The checks that ask which texts use a token stay
quiet for it on purpose: BR-005 and BR-006 both read the name out of the brackets, so reading the
column strictly there instead would report one broken rule once for every text that names the token,
and every report would block export.

UC-019 A2 canonicalizes a token on the way in, so what this reports is what an import (UC-005) or a
definition drafted outside Author (UC-042) wrote into the column directly.

### BR-008: A finding says where its element sits

A finding names an element by name — a step, a section, a question, a rule — and a name is not a
place: the same section name can be mounted in two steps, and a rule's own name says nothing about
where in the survey it fires. Every finding therefore carries the path to what it concerns, written
the way the designer's breadcrumb writes it.

What the path holds depends on what the finding is about:

- a step: nothing, because a step is the outermost thing there is and the message already names it;
- a section or a question: the containers it sits in — the step for a section, the step and section
  for a question — since the message already names the element itself;
- a rule: the full path to the element the rule targets, because a rule has no place of its own. It
  is the one finding whose message says nothing about where to look.

A finding about the survey as a whole, about a list of options, or about the ontology carries no
path, because it is not about a place in the structure.

### BR-009: A finding opens the element it is about

Every finding whose element the designer can show is a link. Following it opens the designer with
that element selected — on the board, in the breadcrumb and in the structure tree — exactly as the
address selects an element (UC-006 BR-003). Three kinds of finding link elsewhere, because their
element does not live on the board:

- a rule opens the designer with the rule's *target* selected and the rule's own editor open over
  it, which is where the token, the operator and the action are corrected;
- a section in no step, or a question in no section, opens the designer and reveals the element in
  the drawer's Sections or Questions panel (UC-015 A7, UC-011 A7), because the board draws only
  what a step or a card holds;
- a finding about an ontology tag or one of its assignments opens the survey's Reporting page
  (UC-023);
- a finding about a missing or out-of-date translation opens the survey's Translations page
  (UC-043).

A finding with nothing to open — the survey has no steps, an option belongs to a removed list —
stays plain text. It is never a dead link: a finding is a link exactly when following it lands
somewhere, so an element the designer cannot currently show (a retired one, which the board hides
until Show removed is on) is reported without one.

### BR-010: An element out of the structure is reported as out of the structure

A section no step mounts, and a question no section assigns, are current elements of the survey:
they export, they count, and every rule and tag that names them stays with them. What they are not
is anywhere a respondent can arrive, and — before UC-015 A7 and UC-011 A7 — anywhere the author
could see, which is how one survives an import unnoticed.

Each is its own warning, named for what is actually wrong and what to do about it: *is not used.
Place it in a step, or remove it* for a section, *is not used. Place it in a section, or remove it*
for a question. A section that is in no step is not also reported as having no
questions, because the emptiness is downstream of the real defect and fixing the mount is what a
reader should be told to do.

They are warnings, not errors (BR-001): the definition installs cleanly at a site and nothing about
a deployed survey misbehaves. What it costs is a file carrying elements nothing shows.

### BR-011: An element is named by something, never by nothing

A finding names its element, so an element with no name — which an import can carry in — must still
be named by something a reader can match against what the tool shows. A step, a section or a
question whose name is blank is reported by its durable id (`Section '#1'`), the same fallback the
Questions panel and the structure tree already use for a question with no short text. A finding
never prints an empty name, and never prints `null`.

### BR-012: Only the first step may stand without a rule

A respondent's first sign-in builds every element no rule controls. The Survey runtime walks every
step, section and question of the survey and creates and saves an answer row for each one that is
not the downstream target of a rule, before the first page is shown. That is what keeps the first
page fast: everything else is *just in time*, built by the rule that calls for it at the moment an
answer calls for it. It also keeps the database free of rows no one will ever see: a respondent who
abandons the survey in the first step or two has had only those steps written for them. An
uncontrolled element in a later step defeats both — it is created and saved for every respondent at
sign-in, whether or not they ever reach it, and a survey made of them loads and saves the whole
survey before the first page.

So only the **first step** — the step with the lowest display order — may hold elements without a
rule. Every question in every later step must be *reached* by a Show or Repeat rule, where reached
means what it means for tokens (BR-005, UC-019 BR-005): a rule reaches its target and everything
inside it, so a question is reached by a rule on its step, on its section as mounted in that step,
or on the question itself (named through that mount, or — for a question in the upstream question's
own section — by itself). A section mounted in the first step and in a later step needs a rule for
the later mount only. Text rules do not count: the runtime builds nothing for a Text rule, which
only rewrites what another rule has built.

The usual rule is **Field exists → Show**: an upstream question the respondent has already passed —
often an HTML introduction on the previous step — with the Field exists operator holds as soon as
that question has been presented, so the target is built when the respondent reaches the question
rather than at sign-in. A step that should always follow is given one such rule on the step itself,
which reaches everything in it.

Each element nothing reaches is an **error** (BR-001), reported at the outermost level that is
wholly unreached so that one missing rule is one finding: the step when nothing in it is reached, the
section mount when the step has some rule but nothing reaches that mount or its questions, otherwise
each unreached question. A step or section with no questions builds nothing and is not reported here
(it is already a warning). The message names the remedy — add a Show rule, usually Field exists,
targeting the element — and links to it (BR-009).

### BR-013: A finding is written for the author, in the author's language

A finding is read by someone who builds surveys, not by someone who knows how a survey is stored.
Each one therefore says what is wrong and then what to do about it, in short sentences and in the
words the tool's own screens use: a section is *placed in* a step and a question in a section (never
an assignment or a mount); a rule has an *upstream* question and a *downstream* target — the
tool's words for the direction a survey flows, kept in every finding about a rule (stakeholder
decision of 2026-10-02); a rule *has* a token; a choice question uses a *list of options*; an element that is
gone *has been removed*. How the Survey runtime behaves is the reason a check exists, and belongs in
this specification and in the guide — not in the finding.

`Rule '…' points backwards` is kept word for word, because the designer's arrow label says the same
thing (UC-033) and an author should meet one phrase for one defect.

Every finding is resolved through the translation mechanism (UC-040, NFR-018), in the language of
the author looking at it: the sentence comes from the language file, and only what the author wrote
— a name, a placeholder, a token — is carried into it unchanged. Fields a finding points the author
to (the initial display key, a rule's default upstream value) are named by the label the form shows
in that language. A retroactivity warning (step 4) is worded when it is shown, not when the edit was
made, so it follows the reader's language rather than the editor's. Outside a browser session — a
test, a REST call — findings are in English.

### BR-014: A Repeat that reads a selection must be able to hold

A Repeat rule whose upstream question is a multi-select or a checkbox group repeats its downstream
once per selected item, and only while the rule's own condition holds (UC-019 BR-007). Boolean and
Greater than can never hold for a selection, so such a rule builds nothing for any respondent: the
repeated section or question simply never appears, and nothing says why. It is an **error**
(BR-001). The rule dialog refuses it when it is saved (UC-019 A6); it is checked again here because
a definition that was imported, or drafted outside the tool (UC-042), never passed through that
dialog. The message names the remedy: change the operator to Field exists.

### BR-015: Names are single-line

A survey name or title that holds a control character — a line break, a tab or any other — is an
**error** on the survey (BR-001). The survey details form refuses such a value when it is saved
(UC-003 BR-005, UC-004 BR-004); it is checked again here because a definition that was imported, or
drafted outside the tool (UC-042), never passed through that form. The message says which of the
two it is and that the author corrects it in the survey's details.
