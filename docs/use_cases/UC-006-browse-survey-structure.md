# Use Case: Browse Survey Structure

## Overview

**Use Case ID:** UC-006  
**Use Case Name:** Browse Survey Structure  
**Primary Actor:** Survey Author  
**Goal:** See the whole survey as a navigable tree of steps, sections and questions, always knowing which element is selected, so that large surveys stay orientable while editing.  
**Status:** Implemented  
**Requirements:** [FR-031, FR-038, NFR-001, NFR-013](../requirements.md)

## Preconditions

- The author is signed in with the `elicit_author` role.
- The survey exists in Author (created in UC-003 or imported in UC-005).

## Main Success Scenario

1. The author opens a survey from the Surveys list.
2. The system replaces the application navigation drawer with the survey's structure tree: steps in display order, each step's sections in order, each section's questions in order. Above the tree the drawer keeps a link back to the Surveys list.
3. The system shows the survey summary in the main area, with the validation panel (UC-007) and the Export action (UC-008).
4. The author expands a step and selects a question.
5. The system highlights the selected element in the tree and shows its details in the main area, including its type, order and a short form of its element key.
6. The author navigates to other elements, or uses the browser's back and forward, and the highlighted tree row always matches the element shown. On the designer, selection is mirrored both ways: selecting a box on the board selects the matching tree row and scrolls the tree to it, and selecting a tree row selects that element on the board.
7. The author leaves the survey by that link (or the title in the header), and the system restores the application navigation drawer.

## Alternative Flows

### A1: Survey not found

**Trigger:** The address names a survey that does not exist in Author (step 1)  
**Flow:**

1. The system shows a not-found page with a link back to the Surveys list.
2. Use case ends.

### A2: Element not found

**Trigger:** The address names an element that does not exist or belongs to another survey (step 5)  
**Flow:**

1. The system shows the survey summary and a message that the element could not be found.
2. Use case continues at step 3.

### A3: Show removed elements

**Trigger:** The author turns on Show removed in the tree (step 4)  
**Flow:**

1. The system includes retired elements in the tree, visibly marked, and offers Restore on them (UC-010).
2. Use case continues at step 4.

### A4: Empty survey

**Trigger:** The survey has no steps (step 2)  
**Flow:**

1. The system shows an empty tree with guidance to add a step or import a definition.
2. Use case continues at step 3.

## Postconditions

### Success Postconditions

- The author sees the survey's current structure and the selected element is highlighted.
- The address bar identifies the survey and, when one is selected, the element, so links can be shared.

### Failure Postconditions

- A not-found page or message is shown; nothing is changed.

## Business Rules

### BR-001: Tree shows current elements by default

Retired elements are hidden unless Show removed is on.

### BR-002: Order follows the survey's display order

Steps, sections and questions appear in the order respondents would meet them, using the same ordering values the definition file carries.

### BR-003: Selection is addressable

Every element the tree can show has its own address within the survey, so the highlighted row is derived from the address, not from click history.

The address names the element by kind and durable id — a step, a section, a question — and also
accepts the two forms a reference elsewhere in Author naturally has: an *assignment* (a section's
mount in a step, a question's place in a section), which selects that element in that container
rather than in whichever container happens to come first; and a *rule*, which selects the element
the rule targets and opens the rule's editor over it (UC-007 BR-009). An address naming something
the tree is not currently showing selects nothing and shows the survey as it would with no
selection (A2); it is never an error page.

### BR-003a: The board shows the structure; the panels show the survey

The board, the breadcrumb and the structure tree are the survey's *structure* — what a respondent
would walk through — so they draw an element where it is placed, and an element that is placed
nowhere appears in none of them. The drawer's Sections and Questions panels are the survey's
*contents*: every current section and question once, placed or not (UC-015 A7, UC-011 A7). Without
that, an unplaced element would exist in the database and in the export while being unreachable in
the tool, which is how one survives an import unnoticed.

### BR-004: On the designer the tree and board share a selection

While the designer is open, the drawer holds the tools palette above the structure tree and is resizable. Selecting an element on the board selects and scrolls to its tree row, and selecting a tree row selects that element on the board, so the two views always agree.

### BR-005: The selection is previewed as a respondent sees it

Beside the board the designer shows the selected section the way a respondent meets it: the section
name as the page title and each question rendered as its field, with the question's text, options,
placeholder, required indicator and tooltip, and any HTML in the text rendered rather than shown as
markup. A selected question is highlighted within the preview, and selecting a step previews its
pages. The preview is built by the same code path the question dialog's live preview uses, so the
two never disagree about how a text will render (FR-038).

### BR-006: The designer asks for a wide enough screen

The designer targets tablet-sized screens and larger. Below 768px it shows a full-screen notice
asking for a wider screen rather than reflowing the board, its lanes and its drag targets into a
phone layout that cannot be used (NFR-013).
