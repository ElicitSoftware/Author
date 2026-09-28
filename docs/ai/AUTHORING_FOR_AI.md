# Writing an Elicit survey definition

**Audience: an AI agent.** This document is self-contained. With it and a
description of the survey someone wants, you can write a complete `.elicit`
definition file that the Elicit **Author** application will import. You do not
need access to the Elicit source, a database, or a running server.

Your output is a **draft**. A person opens it in Author, fixes whatever Author's
validation reports, and exports the finished survey for deployment. So aim for a
structurally sound, complete first draft — not perfection.

---

## 1. What an Elicit survey is

An Elicit survey is a decision tree a respondent walks through. What they answer
decides what they see next, so designing a survey means designing that path.

**The containers.** Everything sits inside something else:

- A **survey** holds steps.
- A **step** is a page in the respondent's progress bar, grouping related
  sections.
- A **section** is a titled group of questions on that page.
- A **question** is one input the respondent answers.

Steps run in order, sections run in order within their step, and questions run in
order within their section. **Unless a rule says otherwise, that display order is
exactly what the respondent experiences.**

**Sections are mounted into steps, not owned by them.** One section can sit under
several steps and shares its questions between them. A survey that asks the same
block of questions about each of several subjects mounts one section under each
subject's step rather than duplicating it. The mounting is a separate record
(`steps_sections`), which is why a section's position is stored per mount rather
than on the section.

**Questions are assigned to sections** the same way, through
`sections_questions` — so one question can also appear in more than one section.

**The rules.** A rule reads the answer to one upstream question, compares it with a
reference value using an operator, and when the comparison holds performs an
action on one downstream target — a step, a section or a question. Every rule is
the same sentence:

> *question* — (*operator*) *value* → (*action*) *target*

There are three actions:

- **SHOW** — the target appears only when the rule holds. Until then the
  respondent never sees it.
- **REPEAT** — the target section or question is repeated as many times as the
  answer says, for example one "Child" section per child.
- **TEXT** — the answer is written into the target's text wherever a
  `{<TOKEN>|default}` placeholder appears, so a later question can say "your
  daughter" rather than "your child".

**There is no Hide rule.** Hiding is the same as not showing: give the element a
SHOW rule and it stays hidden until that rule holds. When several rules point at
the same target, **all** of them must hold.

**Tokens are slots, not questions.** `{<SUBJECT>|the subject}` in a question's text
is a placeholder. A TEXT rule fills it with an earlier answer, so a later question
can name what the respondent actually said. Without a filling rule the respondent
reads the default after the `|`.

**A token is named in angle brackets inside the placeholder, and stored without
them.** The text writes `{<SUBJECT>|the subject}`, or a whole phrase around the
reference — `{Has <SUBJECT> been|Were you}` — and the rule's `token` field holds the
name alone, upper case: `SUBJECT`, never `<SUBJECT>`. The runtime looks a value up
by that name, so a token written with its brackets in the `token` field fills
nothing, and a placeholder naming a token as a bare word (`{SUBJECT|the subject}`)
fills nothing either. Both are export errors. Markup belongs outside the braces:
`<b>{<SUBJECT>|the subject}</b>`, never inside them. A name is at most ten
characters, starts with a letter, and holds letters, digits and underscores only.

---

## 2. The file format

A `.elicit` file is UTF-8 plain text, at most 5 MB. It has a comment header, a
blank line, then one record per line:

```
# ELICIT_SURVEY_DRAFT_V1

surveys: 1||MySurvey|1|My Survey|A description.|0001-0001-0000-0001-0000-0000-0000||AI draft|
steps: 1||1|Welcome|welcome||0||||
```

Each record is `tablename: field|field|field|…`.

### The header

**Required.** The first line of the file must be exactly:

```
# ELICIT_SURVEY_DRAFT_V1
```

Any other `#` lines are ignored, so you may add comments freely. You do **not**
need to write per-table count lines; nothing checks them. (Files exported by
Author contain them, which is why you may see them in examples.)

### Escaping

Within a field: `\|` for a literal pipe, `\\` for a backslash, `\n` for a newline,
`\r` for a carriage return. **An empty field means null.** Trailing empty fields
still need their separators — a record must carry its full field count.

### Record order

Records must appear in this order, because each record's references are resolved
against records already read:

`surveys` → `select_groups` → `select_items` → `steps` → `sections` →
`steps_sections` → `questions` → `sections_questions` → `relationships` →
`reports` → `post_survey_actions` → `dimensions` → `ontology` → `metadata` →
`translations`

The `surveys` record must come first, and there must be **exactly one** of them —
one survey per file.

### The ids you write

Use **small integers starting at 1**, numbered independently per table. They are
your own local ids; Author remaps them on import. They only have to be internally
consistent: when a `sections_questions` record says `question_id` 3, a `questions`
record with id 3 must already have appeared.

---

## 3. Record layouts

Field counts are exact minimums — a record with fewer fields is rejected.

The eight **versioned** tables (`select_groups`, `select_items`, `steps`,
`sections`, `steps_sections`, `questions`, `sections_questions`,
`relationships`) all end with the same five-field tail:

```
version | effective_from | effective_to | published_by | published_comment
```

Always write `0` for `version` and leave the other four empty: `0||||`

| Table | Fields | Layout |
| --- | --- | --- |
| `surveys` | 10 | `id \| survey_key \| name \| display_order \| title \| description \| initial_display_key \| post_survey_url \| published_by \| published_comment` |
| `select_groups` | 10 | `select_group_id \| select_group_key \| name \| description \| data_type \| ` + tail |
| `select_items` | 11 | `select_item_id \| select_item_key \| select_group_id \| display_text \| display_order \| coded_value \| ` + tail |
| `steps` | 11 | `step_id \| step_key \| display_order \| name \| dimension_name \| description \| ` + tail |
| `sections` | 11 | `section_id \| section_key \| display_order \| name \| dimension_name \| description \| ` + tail |
| `steps_sections` | 12 | `steps_sections_id \| steps_sections_key \| step_id \| step_display_order \| section_id \| section_display_order \| display_key \| ` + tail |
| `questions` | 20 | `question_id \| question_key \| type_id \| text \| short_text \| tool_tip \| required \| min_value \| max_value \| validation_text \| select_group_id \| mask \| placeholder \| default_value \| variant \| ` + tail |
| `sections_questions` | 10 | `sections_question_id \| sections_question_key \| question_id \| section_id \| display_order \| ` + tail |
| `relationships` | 19 | `relationship_id \| relationship_key \| upstream_step_id \| upstream_sq_id \| downstream_step_id \| downstream_ss_id \| downstream_sq_id \| operator_id \| action_id \| description \| token \| reference_value \| default_upstream_value \| override_upstream_value \| ` + tail |
| `reports` | 6 | `id \| report_key \| name \| description \| url \| display_order` |
| `post_survey_actions` | 6 | `id \| post_survey_action_key \| name \| description \| url \| execution_order` |
| `dimensions` | 3 | `id \| dimension_key \| name` |
| `ontology` | 5 | `id \| ontology_key \| name \| tag \| dimension` |
| `metadata` | 7 | `id \| metadata_key \| steps_sections_id \| question_id \| sections_question_id \| ontology_id \| value` |
| `translations` | 13 | `translation_id \| translation_key \| element_type \| element_key \| field \| language \| value \| source_hash \| ` + tail |

**Omit the last five tables.** `reports`, `post_survey_actions`, `dimensions`,
`ontology` and `metadata` are the reporting and integration layer. A person
configures those in Author, where the consequences are visible. They are listed
here only so you recognize them in an exported file.

Notes on individual fields:

- `display_order` — write `1, 2, 3…`. Author reassigns these locally, so their
  absolute values do not matter, only their relative order.
- `required` — `t` or `f`. Empty counts as `f`.
- `min_value` / `max_value` — integers, and only meaningful for numeric types.
- `coded_value` — what gets stored and what rules compare against, as opposed to
  `display_text`, which is what the respondent reads.
- `data_type` on `select_groups` — `Text` unless you have reason otherwise.
- `token` on `relationships` — the slot name alone, upper case, without the angle
  brackets the text uses to name it: `SUBJECT`, not `<SUBJECT>`. At most ten
  characters, starting with a letter.
- `dimension_name` on steps and sections — the reporting name for that step or
  section. Use `lower_snake_case`. If you leave it empty it defaults to the
  display name, which usually contains spaces and capitals and makes for poor
  reporting names, so write it explicitly.
- The file carries no `sample` field for questions.

### `display_key`

`steps_sections.display_key` is a 34-character address, and it is the one derived
value you must compute:

```
%04d-%04d-0000-%04d-0000-0000-0000
   |     |         |
   |     |         section's display order within this step
   |     step's display order
   survey id
```

So the first section of the first step of survey 1 is
`0001-0001-0000-0001-0000-0000-0000`. Author rewrites the survey component on
import; the rest must be right.

`surveys.initial_display_key` is the survey's entry point and **must equal the
`display_key` of one of your `steps_sections` records** — normally the first
section of the first step.

---

## 4. Leave these blank

This is the shortest section and the most useful.

- **Every `*_key` field** — the second field of every record. Leave it empty and
  Author mints a fresh identity on import. Do not invent UUIDs and do not write
  placeholder text; a non-empty value that is not a valid UUID is rejected.
- **`effective_from`, `effective_to`, `published_by`, `published_comment`** on the
  eight versioned tables. Write `version` as `0`.
- **Count lines in the header.**

On the `surveys` record only, two fields are worth filling:

- `published_by` — write `AI draft`, optionally naming the tool in parentheses. It
  shows in Author's survey list as "last edited by", so a person can see at a glance
  which surveys came from a model. Author overwrites it with the real author's name
  on the first export, so the marker clears itself.
- `published_comment` — **leave empty.** A value here means "this survey has
  already been exported" and changes what Author allows.

If you want to record provenance — the model, the date, the request — put a short
line in the survey's `description`, which a person can read and edit.

The `surveys` record has two further trailing fields, `base_language` and
`content_languages` (Survey V019), which you may leave off entirely: a record that
stops at `published_comment` is read as English content publishing no translations,
which is what a draft is. Do not write `translations` records. A translation belongs
to wording that has been reviewed, and it is authored in Author's Translations view
against the finished survey (UC-043).

---

## 5. Rules you must follow

Author validates a survey before it can be exported, and these are the checks it
runs. Importing a file does not run them, so a file can import and still be
blocked afterwards. Getting these right is the difference between a draft someone
can use and a draft they have to repair.

**Errors — these block the export:**

1. **A rule must point forward.** The target must come after the upstream question
   in display order. A rule that points backwards, or at its own container, is
   rejected. Practically: put the question that drives a rule *before* what the
   rule affects.
2. **A rule reads from a question assignment**, so `upstream_sq_id` is a
   `sections_questions` id — **not** a `questions` id. This is the single most
   common mistake. Also set `upstream_step_id` to the step the question is shown
   in.
3. **A rule writes to exactly one target.** Set exactly one of
   `downstream_step_id` (a `steps` id), `downstream_ss_id` (a `steps_sections`
   id) or `downstream_sq_id` (a `sections_questions` id), and leave the other two
   empty.
4. **Targeting a question in a different section needs the path.** If
   `downstream_sq_id` is a question outside the upstream question's own section,
   also set `downstream_step_id` and `downstream_ss_id` so the target is named as
   an outer→inner path. A question in the *same* section needs only
   `downstream_sq_id`.
5. **Every rule needs an operator and an action.**
6. **A choice question must have options.** If `type_id` is `RADIO`, `COMBOBOX`,
   `MULTI_SELECT` or `CHECKBOX_GROUP`, its `select_group_id` must point at a
   `select_groups` record that has at least one `select_items` record.
7. **A question must have text.**
8. **Every reference must resolve.** No assignment may point at a step, section or
   question that does not exist in the file.
9. **`initial_display_key` must match one of your `steps_sections` display keys.**
10. **A `{<TOKEN>}` with no default and no rule filling it is an error.** Either give
    every placeholder a default (`{<SUBJECT>|the subject}`) or make sure a TEXT
    rule reaches it.
11. **A rule's `token` must be the name alone.** Upper case, starting with a letter,
    letters, digits and underscores only, at most ten characters — `SUBJECT`, not
    `<SUBJECT>`, and not `Has SUBJECT been`.
12. **Every `<NAME>` in a placeholder must be a token some rule carries.** A phrase
    fills all or nothing, so one reference nothing defines stops the whole
    placeholder filling.

**Warnings — allowed, but a person will have to look at them:**

- A survey with no steps, a step with no sections, a section with no questions.
- Two elements sharing the same position.
- A `{<TOKEN>|default}` that no rule fills — respondents always read the default.
- A TEXT rule whose token no text it reaches actually uses.

**REPEAT has one more constraint worth knowing:** a section cannot repeat itself.
The question that says *how many* must sit in an **earlier** section than the block
it repeats. Put the count question in its own section before the repeating one.

---

## 6. The lookup ids

`type_id`, `operator_id` and `action_id` are written as raw integers and are
**not** remapped on import, so they must match the tables below.

### Question types (`questions.type_id`)

| id | name | notes |
| --- | --- | --- |
| 1 | `CHECKBOX` | single boolean checkbox |
| 2 | `DATE_PICKER` | |
| 3 | `COMBOBOX` | needs an answer list |
| 4 | `HTML` | informational, not an input |
| 5 | `INTEGER` | honors `min_value` / `max_value` |
| 6 | `DOUBLE` | |
| 7 | `RADIO` | needs an answer list |
| 8 | `TEXT` | single-line text |
| 9 | `TEXTAREA` | multi-line text |
| 10 | `MULTI_SELECT` | needs an answer list |
| 11 | `CHECKBOX_GROUP` | needs an answer list |
| 12 | `DATE_TIME_PICKER` | called `DATETIME` before Survey migration V018 |
| 13 | `EMAIL` | |
| 14 | `PASSWORD` | |
| 15 | `TIME_PICKER` | **V018 and later only** |
| 16 | `MODAL` | **V018 and later only** |

Ids 1–14 are stable. Ids 15 and 16 exist only where Survey migration V018 has been
applied; if you are unsure, stay within 1–14. The `id`, not the name, is what
travels in the file.

### Operators (`relationships.operator_id`)

| id | name | symbol |
| --- | --- | --- |
| 1 | `BOOLEAN` | yes/no, true/false, 0/1 |
| 2 | `GREATER THAN` | `>=` |
| 3 | `EQUAL` | `=` |
| 4 | `NOT_EQUAL` | `!=` |
| 5 | `FIELD_EXIST` | the field was presented |
| 6 | `CONTAINS` | the answer array contains the value |

### Actions (`relationships.action_id`)

| id | name |
| --- | --- |
| 1 | `SHOW` |
| 2 | `REPEAT` |
| 3 | `TEXT` |

---

## 7. A complete worked example

Two steps. The first asks whether the respondent completed a course; the second
holds the follow-up questions and appears only when the answer is yes.

```
# ELICIT_SURVEY_DRAFT_V1

surveys: 1||CourseFeedback|1|Course Feedback|A short feedback survey for a training course.|0001-0001-0000-0001-0000-0000-0000||AI draft|
select_groups: 1||YesNo|Yes or no|Text|0||||
select_items: 1||1|Yes|1|1|0||||
select_items: 2||1|No|2|0|0||||
steps: 1||1|Attendance|attendance||0||||
steps: 2||2|Feedback|feedback||0||||
sections: 1||1|Your attendance|your_attendance||0||||
sections: 2||2|Course feedback|course_feedback||0||||
steps_sections: 1||1|1|1|1|0001-0001-0000-0001-0000-0000-0000|0||||
steps_sections: 2||2|2|2|1|0001-0002-0000-0001-0000-0000-0000|0||||
questions: 1||7|Did you complete the course?|Completed||t||||1|||||0||||
questions: 2||5|How many sessions did you attend?|Sessions attended||f|0|20|Enter a number from 0 to 20||||||0||||
questions: 3||9|What would you change about the course?|Suggested changes||f|||||||||0||||
sections_questions: 1||1|1|1|0||||
sections_questions: 2||2|2|1|0||||
sections_questions: 3||3|2|2|0||||
relationships: 1||1|1||2||3|1|Show the feedback section when the course was completed||1|||0||||
```

Reading the rule, field by field: relationship `1`, no key, upstream step `1`,
upstream question assignment `1`, no downstream step, downstream section mount
`2`, no downstream question, operator `3` (`EQUAL`), action `1` (`SHOW`),
a description, no token, reference value `1` — the `coded_value` of "Yes" — then
the empty upstream-value fields and the `0||||` tail.

Note what makes it valid: the driving question is in step 1 and the target mount is
in step 2, so the rule points forward. The upstream reference is
`sections_questions` id 1, not `questions` id 1 — they happen to coincide here,
which is exactly why the distinction is easy to lose. The reference value matches
the option's `coded_value`, not its `display_text`. And question 1 is a `RADIO`
(type 7), so it carries `select_group_id` 1, which has two options.

---

## 8. How to work

1. Read what the person wants. Ask about anything that decides structure: how many
   topics, whether anything repeats per person or per item, what has to be
   conditional.
2. Sketch the containers first — steps, then sections, then questions — and get the
   order right, because order is the respondent's default path and rules can only
   point forward along it.
3. Write answer lists before the questions that use them.
4. Add rules last, once every element has an id.
5. Before you hand the file over, check it yourself:
   - one `surveys` record, first, and the header marker is
     `ELICIT_SURVEY_DRAFT_V1`;
   - every record has its exact field count, trailing separators included;
   - every id referenced exists and appears earlier in the file;
   - every rule points forward, names one target, and reads from a
     `sections_questions` id;
   - every choice question has a list with options;
   - `initial_display_key` matches a `steps_sections` display key;
   - every `{<TOKEN>}` names its token in angle brackets and has a default or a
     rule that fills it, and every rule's `token` is that name alone, upper case,
     without the brackets.
6. Say plainly what you left for the person to finish — reporting tags, tokens you
   were unsure about, anything you guessed.

Then tell them to open Author, choose **Import definition**, upload the file, and
work through whatever validation reports before exporting.
