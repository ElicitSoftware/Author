# Use Case: Create Conditional Relationship

## Overview

**Use Case ID:** UC-019  
**Use Case Name:** Create Conditional Relationship  
**Primary Actor:** Survey Author  
**Goal:** Connect an upstream question's answer to a downstream step, section or question with an operator, a reference value and an action (Show, Repeat or Text), by drawing the rule on the designer board or from a question's menu.  
**Status:** Implemented  
**Requirements:** [FR-012, FR-019, FR-020, FR-022, FR-067, NFR-007, NFR-012](../requirements.md)

## Preconditions

- The author is signed in with the `elicit_author` role.
- The designer board is open with Show rules on, and the survey has at least one question assigned to a section.

## Main Success Scenario

1. The author drags a rule glyph — Show, Repeat or Text — from the palette and drops it on the question row whose answer the rule reads.
2. The system attaches a dangling arrow to that row, its action glyph at the midpoint, and follows the pointer.
3. The author drops the arrowhead on the target: a step lane header, a section card header or a question row.
4. The system opens the rule dialog with the upstream question, the action and the downstream target filled in; the card the row or header sits in tells the system which mount of a shared section is meant.
5. The author chooses the operator, enters the reference value, optionally names the token the rule fills — picked from the names the survey already uses or typed new; optional for Show and Repeat, required for Text (BR-005) — and an optional description, and saves.
6. The system creates the rule with a key minted once — stored question-only when the target sits in the upstream question's own section, otherwise with the step and section the target is shown in (BR-001) — draws the arrow labeled by its action and target (UC-020 BR-004), and refreshes validation.

## Alternative Flows

### A1: Keyboard path

**Trigger:** The author chooses Add rule… from a question row's menu (step 1)  
**Flow:**

1. The system opens the rule dialog with the upstream question filled in; the author picks the downstream kind and target from lists, in which a section mounted in several steps appears once per mount, labeled by its path. A question is labeled by its path and its type — `welcome › Demographics › married (radio)` — in both the upstream and the target list, since the type is what decides the operators and values the rule can use.
2. Use case continues at step 5.

### A2: Text action without a token, or a token that is not a name

**Trigger:** The action is Text and no token name is given; or a token is given that is not a name a
text could write between angle brackets, or whose name exceeds 10 characters (step 5)  
**Flow:**

1. The system stores a token as the name alone, upper case: a token typed `<S1>` is saved as `S1`,
   and one typed `s1` as `S1`. The brackets belong to the text that names the token, and the runtime
   keys a text's values on this name, so a token stored with them fills nothing (UC-007 BR-007).
   The name is what the length is measured against, so `<GRANDMAMA>` is nine characters, not eleven.
2. What no normalizing can repair — a name holding a character a reference cannot spell (`S 1`,
   `{S1}`, `Q#`), one that does not start with a letter, or one longer than ten characters — marks
   the token field and refuses to save.
3. Use case continues at step 5.

### A3: Repeat onto a step

**Trigger:** The action is Repeat and the target is a step (step 3)  
**Flow:**

1. The system refuses: the runtime repeats sections and questions, not steps.
2. Use case continues at step 3.

### A4: Author cancels or drops outside a target

**Trigger:** The arrowhead is released on empty board, or the dialog is closed (steps 3, 5)  
**Flow:**

1. The system removes the dangling arrow; nothing is created.
2. Use case ends.

### A5: Re-point an existing rule

**Trigger:** The author drags an arrow's tail or head to another row or header (step 1)  
**Flow:**

1. The system moves that end of the rule to the new element in the card it was dropped in, completes the path (BR-001), subject to A3, and redraws.
2. Use case ends.

### A6: Repeat that reads a multi-select

**Trigger:** The action is Repeat and the upstream question is a multi-select or a checkbox group (step 5)  
**Flow:**

1. The system says, under the operator, that the rule repeats its downstream once for each item the respondent selects and fills its token with that item's text (BR-007, BR-008), and on a new rule whose operator has not been chosen it sets the operator to Field exists.
2. The author optionally names the token and writes it in a placeholder in the downstream's texts — a section named `Michigan vs. {<GAME>|this game}`, for example.
3. An operator that can never hold for a selection — Boolean or Greater than — marks the operator field and refuses to save (BR-007).
4. Use case continues at step 6.

## Postconditions

### Success Postconditions

- The rule is current, reads from one question assignment, carries a downstream path whose innermost element is the step, section assignment or question assignment it acts on, and is drawn on the board at that element.

### Failure Postconditions

- Nothing is created or changed.

## Business Rules

### BR-001: The downstream target is a path

A rule's downstream is the path the Survey runtime walks from the outside in: the step, the section mounted in that step, the question assigned to that section. The innermost element named is what the rule acts on; the outer ones say where the runtime places it. The author names only the element the rule acts on; the system completes the path from where that element sits on the board. A question in the upstream question's own section is stored question-only: no downstream step, no downstream section and no upstream step, whichever card the author drew it in — the runtime shows it on the page being answered, whichever mount that is, so the rule fires from every mount of that section. (The runtime's login-time query hides every question of a section or step a rule names downstream; a same-section rule stored with a path would blank its own section for a fresh respondent.) A section, a step, or a question in another section names its step (and section), and the rule names the upstream step it fires from. A path that does not hold together — a section outside its step, a question outside its section, a question in another section with no mount named — is refused (FR-022).

### BR-002: Upstream is always a question assignment

A rule reads a question *in a section*; the same question assigned elsewhere is a different upstream.

### BR-003: Rules read as a sentence

Upstream question, operator, reference value, action, target — the order the runtime evaluates and the arrow draws (UC-029 BR-002).

### BR-004: Rules on one target are all required

The runtime applies a target's rules with AND; the board draws them as separate arrows converging on the target.

### BR-005: A rule reaches everything inside its target, and a token is a slot, not a question

The runtime builds a text's token values from the rules that target its step, then its section, then the question itself, the inner overriding the outer. A Text rule on a step therefore fills its token in every section and question of that step; one rule per step, not one per element. A token is the name of a slot in a text, declared by being used: the same token may be filled by several rules from different questions, each where the text is shown (the Family History Survey fills `<S1>` from fifteen places), and a rule of any action that carries a token fills it. The token is therefore offered for every action, not only Text: a Show or Repeat rule may name the slot the upstream answer fills in the target's texts (the Family History Survey's step-showing Show rules carry `S1` and `CS2`), and a Text rule must name one (A2). The token is stored as the name alone, upper case (`S1`); the angle brackets belong to the text that names it, and a token stored with them is asked for under a name it does not have and fills nothing (A2, UC-007 BR-007). The example answer a preview substitutes belongs to the question the rule reads (UC-011 BR-005), not to the rule, and it outranks the rule's own value wherever a real answer would — for a free-text, date or time source — because that is the choice the runtime makes between the two (UC-011 BR-004).

### BR-006: A token source is a question that collects a value

A token holds one of two things: the respondent's answer to the question the rule reads, or the rule's own default upstream value, which is a constant the author writes. A *display-only* question — `HTML`, which renders text inline, and `MODAL`, which renders it in a dialog — has no input and collects no answer, so a rule reading one can fill its token only from that constant, and a rule reading one with no constant fills nothing at all: the respondent reads the placeholder's default and nothing says why.

Which questions those are is read from `question_types.data_type`, which the platform leaves empty for exactly `HTML` and `MODAL` and sets for all fourteen types that collect a value. The flag rather than the two names, because the Survey runtime asks the same question of the same row when it decides what a token holds (Survey UC-002 BR-010), and a display-only type added later is then covered in both modules without either being edited.

A rule that carries a token and reads a display-only question is refused when it has no default upstream value, since it can never fill the slot. One that has a constant is accepted but warned about at export (UC-007): it works, but only by accident of the constant, and the slot no longer has anything to do with the question the rule reads. The Family History Survey drives the possessive on its Mother and Father steps this way — `FIELD_EXIST` on an `HTML` question, with the phrase in the constant — and those rules are what the warning exists to retire. The designer's preview follows the same rule: where a rule cannot fill its token, the preview shows the placeholder's default, which is what the respondent would read, rather than the source question's sample answer (UC-011 BR-005).

### BR-007: A Repeat reads a count or a selection

What a Repeat rule builds is decided by the question it reads (Survey UC-002 BR-012). A question answered with a number repeats the downstream that many times, instances 1 to N. A question answered by selecting several items from a list — `MULTI_SELECT` or `CHECKBOX_GROUP` — repeats it once for each item selected, and each instance belongs to its item: it is numbered by the item's position in the list, not by a running count, so a respondent who goes back and changes the selection keeps the answers already given about every item that stays selected, and gets them back for an item unselected and selected again. The instances appear in list order, whatever order the items were picked in.

The instances are built only while the rule's own condition holds, as for every rule, and the condition is evaluated against the selection as it is stored: the selected items' coded values. Field exists always holds, and is the operator this rule wants — nothing selected then builds nothing. Contains holds while one named item is among those selected, which makes the whole repeat conditional on that item; Equal and Not equal compare the whole selection with the reference value. Boolean and Greater than can never hold for a selection, so a Repeat reading a multi-select with either would build nothing for any respondent; it is refused when saved (A6) and reported as an error at export (UC-007 BR-014), since an imported or drafted definition never passed through this dialog.

The downstream is a section or a question (A3). Because an instance is tied to an item's position, reordering the items of a list that drives a repeat, or inserting one, in a later revision changes the order in which respondents who start after that revision see the instances, and nothing else: a repeated section's reporting row names the item it is about by the item's own key, not by its position (Survey UC-008 BR-012), so rows collected before and after the reorder report the same item. Respondents already answering are unaffected — they keep the list as it was when they started.

### BR-008: Inside a per-item instance, a token holds the item's text

Within an instance built from a selected item (BR-007), a token filled by a rule that reads the same multi-select question holds that item's display text (Survey UC-002 BR-013) — translated where the survey publishes the item's translation. That is the Repeat rule itself when it carries a token, which reaches every text in the repeated section or question (BR-005), and equally a Text rule from the same question onto the repeated section. The item's text outranks the rule's default upstream value, which BR-006 and UC-011 BR-004 would otherwise prefer for a coded answer: the constant is the same in every instance, and the item is the one thing that tells them apart. A default upstream value on such a rule is therefore only what a respondent reads if the item cannot be found.

The designer's previews show one instance, with the first item of the list in the slot (UC-011 BR-004). The item's text is treated as an answer is: escaped, and never re-read as a placeholder.
