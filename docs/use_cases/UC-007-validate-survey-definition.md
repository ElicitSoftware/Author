# Use Case: Validate Survey Definition

## Overview

**Use Case ID:** UC-007  
**Use Case Name:** Validate Survey Definition  
**Primary Actor:** Survey Author  
**Goal:** Learn, before exporting, which problems would make the definition fail or behave unexpectedly at a deployed site, and which edits will apply retroactively there.  
**Status:** Draft  
**Requirements:** [FR-005, FR-022, FR-018, FR-024, C-011, C-012](../requirements.md)

## Preconditions

- The author is signed in with the `elicit_author` role.
- The survey exists in Author.

## Main Success Scenario

1. The author opens the survey in the editor (UC-006) or chooses Export (UC-008).
2. The system checks the current elements of the survey: every reference from one element to another resolves to a current element; every relationship has a downstream path that holds together (a step, a section mounted in it, a question assigned to that section; a question outside the upstream question's own section names its step and section) and a valid operator and action; every choice-type question refers to a select group that has at least one current item; every `{<TOKEN>}` a step name, section name or question text uses is filled by a rule that reaches it, and every token a rule carries, whatever its action, is a name a text could write between angle brackets and is used by a text it reaches; every step has at least one section and every section at least one question; ordering values are unique within their parent; the survey's initial display key names a current section assignment.
3. The system classifies each finding as an error (blocks export) or a warning (informational).
4. The system adds a retroactivity warning for each step or section whose name changed since the last export and for each question whose ontology assignment changed since the last export.
5. The system displays the findings in the validation panel, each linked to the element concerned.
6. The author selects a finding and the system navigates to the element so that it can be corrected; with no errors present, the goal is achieved and export is allowed.

## Alternative Flows

### A1: No findings

**Trigger:** The checks produce neither errors nor warnings (step 3)  
**Flow:**

1. The system shows that the definition is ready to export.
2. Use case ends.

### A2: Errors present when exporting

**Trigger:** At least one error exists and the author is in the export flow (step 5)  
**Flow:**

1. The system disables Export and lists the errors in the export dialog.
2. The author corrects the elements and re-runs validation.
3. Use case continues at step 2.

### A3: No previous export to compare with

**Trigger:** The survey has never been exported (step 4)  
**Flow:**

1. The system omits the change-based retroactivity warnings and instead shows a general note that step and section names and ontology assignments apply retroactively at deployed sites.
2. Use case continues at step 5.

## Postconditions

### Success Postconditions

- The author has an up-to-date list of errors and warnings, each pointing to its element.
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
