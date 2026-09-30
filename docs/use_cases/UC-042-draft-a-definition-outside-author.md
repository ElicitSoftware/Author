# Use Case: Draft a Definition Outside Author

## Overview

**Use Case ID:** UC-042  
**Use Case Name:** Draft a Definition Outside Author  
**Primary Actor:** Survey Author  
**Secondary Actor:** AI Agent (any vendor, in the author's own session)  
**Goal:** Describe a wanted survey to an AI agent, away from Author and without a running Elicit installation, and receive a `.elicit` draft that Author imports as an ordinary working copy, so that the author starts from a structured draft instead of an empty survey.  
**Status:** Implemented  
**Requirements:** [FR-048, FR-063, NFR-017, C-022](../requirements.md)

## Preconditions

- The author has access to an AI session. The agent needs no access to this source tree, to any database, or to a running Elicit installation.
- The author has the AI authoring specification, or can reach an Author that carries it: Author offers it on its *Draft with AI* page to anyone signed in with the `elicit_author` role, and the same document is `docs/ai/AUTHORING_FROM_AI.md` in this repository.
- To import the result afterwards, the author is signed in to Author with the `elicit_author` role.

## Main Success Scenario

1. The author chooses *Draft with AI*, offered beside *New survey* on the surveys list and in the navigation.
2. The system states the round trip in four steps, and offers the specification two ways: copied to the clipboard behind a short instruction addressed to the agent, or downloaded as a file to attach.
3. The author takes the specification to an AI session and describes the survey they want.
4. The agent reads the specification: the survey model (steps, sections, questions, the default path, the rules that connect them), the record layouts, the lookup id tables, and the rules a definition must obey.
5. The agent asks the author about anything that decides structure — the topics to cover, what repeats per person or item, what must be conditional.
6. The agent writes a `.elicit` file: one `surveys` record first, then the remaining records in dependency order, each with its full field count, and every `*_key` field left empty.
7. The agent marks the file `# ELICIT_SURVEY_DRAFT_V1` and writes `AI draft` into the survey's `published_by`, leaving `published_comment` empty.
8. The author returns to Author and chooses Import definition, which the *Draft with AI* page links to and which links back.
9. The system accepts the draft marker, mints a key for the survey and for every element, remaps the file's local ids, rebases the display keys, and creates an ordinary working copy.
10. The system reports that the file came from a draft and that its keys were minted here, and offers to open the survey in the editor.
11. The author opens the survey, reads the validation panel, and corrects what it reports.
12. When validation reports no errors, the author exports the survey (UC-008). The export carries the `ELICIT_SURVEY_EXPORT_V1` marker and real keys, and is the file a site can apply.

## Alternative Flows

### A1: The agent writes the export marker

**Trigger:** The file is marked `ELICIT_SURVEY_EXPORT_V1` rather than the draft marker (step 7)  
**Flow:**

1. The system imports it as an ordinary definition, keeping whatever keys the file carries.
2. If the file's `survey_key` is already open in Author, the system refuses it and says to open the survey from the Surveys list instead.

### A2: The file is malformed

**Trigger:** A record is short of its required field count, references an id that has not appeared, or the file carries no recognized marker (step 9)  
**Flow:**

1. The system imports nothing — the import is all-or-nothing — and reports the failure with the line number.
2. The author returns the message to the agent, which corrects the file, and the author imports again.

### A3: The draft imports but does not validate

**Trigger:** The definition is structurally readable but breaks an authoring rule — a rule that points backwards, a choice question with no options, a token nothing fills (step 11)  
**Flow:**

1. The system lists each problem in the validation panel and disables export while any error stands.
2. The author fixes them in the designer, or returns them to the agent and imports a corrected draft as a second survey.

### A4: Someone tries to install the draft at a site

**Trigger:** The draft file is given to the Admin app instead of to Author  
**Flow:**

1. Admin refuses it, because it gates on the `ELICIT_SURVEY_EXPORT_V1` marker and the draft does not carry it.
2. The author imports it into Author, reviews it, and exports a real definition to apply.

### A5: The build carries no specification

**Trigger:** The image was built without the specification on its classpath (step 1)  
**Flow:**

1. The system offers the path nowhere — no button on the surveys list, no navigation entry, and no link from the import page — rather than offering a page that has nothing to hand over.
2. The endpoint that serves the document answers 404 to a direct request.
3. Importing a draft still works: a specification the author already has is all the path needs from here on.

## Postconditions

### Success Postconditions

- A new survey exists in Author as a working copy, with a fresh `survey_key` and a fresh key for every element, and every row current at `version = 0`.
- The survey's `published_by` records that it came from an AI draft, until the first export replaces it with the author's own name.
- No deployed site has been touched.

### Failure Postconditions

- Nothing is written: a rejected import leaves the database exactly as it was.

## Business Rules

### BR-001: The specification is self-contained

The document must be usable by an agent with no access to Elicit's source, database or running services. Anything it needs — the model, the layouts, the id tables, the rules, a worked example — is in the document.

### BR-002: The specification cannot drift

Everything in the document that restates the implementation is verified against the code and the seeded lookup rows by automated test, and its worked example is imported and validated by that test. Its prose is read the same way a survey's texts are: every placeholder it writes must name its token in angle brackets, so the document cannot teach a form the export refuses, and the prose and the worked example cannot drift apart (NFR-017).

### BR-003: A draft is not a deployable definition

A draft carries `ELICIT_SURVEY_DRAFT_V1`, which only Author reads. Author writes only `ELICIT_SURVEY_EXPORT_V1`, and does so only when an author exports. A draft therefore reaches a site only after a person has reviewed it here (C-022).

### BR-004: A draft brings no identity

Keys left empty are minted on import. An agent never invents a `survey_key` or an element key, so a draft can be imported repeatedly, and identity is created at the moment the author adopts the draft.

### BR-005: There is no draft state in the database

An imported draft is an ordinary working copy. Its draft-ness is expressed by failing validation until it is fixed, not by a column — the schema has no draft flag and Author does not add one.

### BR-006: The tool offers the path, and does none of it

Author hands over the specification and states the round trip; the drafting happens in a session Author has no part in. Nothing is sent anywhere by the tool — the author carries the specification out and carries the draft back — so the path needs no network access, no vendor account and no configuration, and works with whatever agent the author already uses.

The specification is served role-gated and from the classpath, exactly as the manual is (UC-039 BR-004): it is authoring documentation, and `elicit_admin` does not imply `elicit_author` (UC-002 BR-001). What the image serves is the reviewed document byte for byte, so the drift gate on the document (BR-002) governs what an author actually receives.

The specification stays in English, the language it is written and gated in. The instruction copied in front of it is the tool's own chrome and is translated (UC-040), so an author addresses the agent in their language even though the document it reads is English.
