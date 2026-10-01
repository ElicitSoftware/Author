# Use Case: Delete Survey

## Overview

**Use Case ID:** UC-047  
**Use Case Name:** Delete Survey  
**Primary Actor:** Survey Author  
**Goal:** Take a whole survey out of Author for good, once the author has had every chance to keep a backup, without affecting any site that already runs it.  
**Status:** Draft  
**Requirements:** [FR-065, FR-003, FR-006, C-022](../requirements.md)

## Preconditions

- The author is signed in with the `elicit_author` role.
- The survey exists in Author.

## Main Success Scenario

1. The author chooses Delete on the survey's row in the surveys list.
2. The system shows a confirmation naming the survey and stating that the survey and everything in it — steps, sections, questions, choice lists, rules, reporting tags and translations, removed elements included — will be deleted permanently.
3. The system states whether the survey has been exported. For an exported survey it names who exported it last with the release note, and explains that Author holds the master copy, that sites which applied an export keep their copy and are not changed, and that those sites can only be updated again from the exported file.
4. The system urges the author to download a backup first, explaining that the backup is the only way to get the survey back, and offers Download backup.
5. The author chooses Download backup and the system hands over a backup file of the whole survey, without checking the definition first and without recording an export.
6. The author types the survey's name.
7. The system enables Delete once the typed name matches the survey's name exactly.
8. The author chooses Delete.
9. The system deletes the survey and every record belonging to it.
10. The system closes the confirmation, removes the survey from the list and reports that it was deleted.

## Alternative Flows

### A1: Author deletes without a backup

**Trigger:** The author does not choose Download backup (step 4)  
**Flow:**

1. The author ignores the advice; nothing prevents the delete.
2. Use case continues at step 6.

### A2: Survey has validation errors

**Trigger:** The definition has errors that would block an export (step 5)  
**Flow:**

1. The system produces the backup anyway; errors do not block it (BR-006).
2. Use case continues at step 6.

### A3: Typed name does not match

**Trigger:** The typed text is not the survey's name (step 7)  
**Flow:**

1. The system keeps Delete disabled.
2. Use case continues at step 6.

### A4: Survey has respondents in Author's database

**Trigger:** Author's database holds respondents for the survey (step 9)  
**Flow:**

1. The system refuses the delete, says how many respondents exist and that Author does not delete recorded answers, and keeps the confirmation open.
2. Use case ends.

### A5: Author cancels

**Trigger:** The author closes the confirmation instead of choosing Delete (step 8)  
**Flow:**

1. The system changes nothing; a backup downloaded at step 5 stays valid for as long as the survey is not edited.
2. Use case ends.

### A6: Survey already deleted elsewhere

**Trigger:** Another author deleted the survey while the confirmation was open (step 9)  
**Flow:**

1. The system reports that the survey no longer exists.
2. Use case ends.

## Postconditions

### Success Postconditions

- The survey and every record that belonged to it are gone from Author, current and retired alike.
- The survey key is free again, so importing the backup, or any export of the survey (UC-005), is accepted and restores it under the same survey and element keys.
- No deployed site is changed.

### Failure Postconditions

- The survey and all its contents are unchanged.

## Business Rules

### BR-001: Deletion is physical and total

Unlike removal (UC-009), which retires and keeps the row, deleting a survey removes every record it owns, retired ones included. Nothing in Author can restore it; only a backup or an exported file can.

### BR-002: The name is the confirmation

The delete is enabled only once the author has typed the survey's name exactly. A single click, or the name of a different survey, is never enough.

### BR-003: An exported survey is the master of copies Author cannot reach

Sites install a survey by applying an exported file, and match later updates by the survey and element keys. Deleting the survey in Author never reaches a site: its copy keeps running and keeps its answers. To change those sites later, the author imports the backup (or the last exported file) back into Author, edits it and exports again; the export is validated as usual (UC-008). A survey rebuilt from scratch has new keys and would install beside the old one rather than update it.

### BR-004: Recorded answers are never deleted from Author

Author's database holds survey definitions, not responses. A survey with respondents there is refused, because that database is acting as a site's.

### BR-005: Shared reporting tags survive

A reporting tag may be used by another survey that imported it under the same name. Such a tag passes to the survey still using it instead of being deleted; tags only this survey uses go with it.

### BR-006: The backup is taken as the survey stands

The backup holds every record the survey owns, retired ones included, and is taken without validation, so a survey with errors can still be kept. It is not an export: it records no release note and does not mark the survey exported. It carries the draft marker (C-022), so Admin refuses it and no site can install it; it can only be imported back into Author, which keeps every key.

### BR-007: Advice, not obligation

The backup is urged and is one click away, but it is not required: the author may delete without one (A1).
