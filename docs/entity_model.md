# Entity Model

Source: `docs/requirements.md`, `docs/scd-contract.md`, and the shared
`survey` schema Author maps onto but does not migrate
(`Survey/src/main/resources/db/migration/V001__Create_Survey_Schema.sql`,
the greenfield track with Kimball Type 2 SCD already baked in). Entity and
attribute names below are the actual `survey.*` table/column names, since
Author's JPA/Panache entities map directly onto them — this document
describes the existing schema, it does not propose a new one.

Terminology, per `docs/scd-contract.md`: `id` is always the surrogate
primary key of one physical version row; `{entity}_id` is the durable key
shared by every version of that logical entity; `version` increments on
publish; "current" means `effective_to = '9999-12-31 23:59:59+00'` (there is
**no** `is_current` flag); `is_draft` marks an unpublished draft row. A
`_version` column that is a `CHECK`-pinned `0` (e.g. `select_group_version`)
is an FK-existence sentinel, not a real version pin — it is called out per
attribute below, not modeled as a live reference.

## Entity Relationship Diagram

```mermaid
erDiagram
    SURVEY ||--o{ STEP : "defines"
    SURVEY ||--o{ SECTION : "defines"
    SURVEY ||--o{ QUESTION : "defines"
    SURVEY ||--o{ SELECT_GROUP : "defines"
    SURVEY ||--o{ RELATIONSHIP : "defines"
    SURVEY ||--o{ ONTOLOGY : "defines"
    SURVEY ||--o{ STEPS_SECTION : "defines"
    SURVEY ||--o{ SECTIONS_QUESTION : "defines"
    SURVEY ||--o{ METADATA : "defines"
    QUESTION_TYPE ||--o{ QUESTION : "classifies"
    SELECT_GROUP ||--o{ SELECT_ITEM : "contains"
    SELECT_GROUP ||--o{ QUESTION : "constrains"
    STEP ||--o{ STEPS_SECTION : "assigned via"
    SECTION ||--o{ STEPS_SECTION : "assigned via"
    SECTION ||--o{ SECTIONS_QUESTION : "assigned via"
    QUESTION ||--o{ SECTIONS_QUESTION : "assigned via"
    OPERATOR_TYPE ||--o{ RELATIONSHIP : "evaluates"
    ACTION_TYPE ||--o{ RELATIONSHIP : "triggers"
    STEP ||--o{ RELATIONSHIP : "upstream step"
    STEP ||--o{ RELATIONSHIP : "downstream step"
    SECTIONS_QUESTION ||--o{ RELATIONSHIP : "upstream question"
    SECTIONS_QUESTION ||--o{ RELATIONSHIP : "downstream question"
    STEPS_SECTION ||--o{ RELATIONSHIP : "downstream section"
    DIMENSION ||--o{ ONTOLOGY : "categorizes"
    ONTOLOGY ||--o{ METADATA : "tags"
    QUESTION ||--o{ METADATA : "tagged via"
    SECTIONS_QUESTION ||--o{ METADATA : "tagged via"
    STEPS_SECTION ||--o{ METADATA : "tagged via"
```

### SURVEY

A single survey definition; the container for a survey's structure. Type 1 — in-place edits, never versioned.

| Attribute            | Description                                                                  | Data Type | Length/Precision | Validation Rules      |
|-----------------------|-------------------------------------------------------------------------------|-----------|-------------------|------------------------|
| id                    | Unique identifier                                                             | Integer   | 10                | Primary Key, Sequence  |
| name                  | Internal survey name                                                          | String    | 255               | Not Null, Unique       |
| display_order         | Order surveys are listed in                                                   | Integer   | 10                | Not Null, Unique       |
| title                 | Respondent-facing survey title                                                | String    | 255               | Not Null               |
| description           | Survey purpose/description                                                    | String    | 2000              | Optional               |
| initial_display_key   | Display key of the first step/section/question shown to a respondent          | String    | 255               | Optional               |
| post_survey_url       | URL to redirect to after survey completion                                    | String    | 255               | Optional               |
| published_by          | Identity of the user who last edited this survey's metadata                   | String    | Unlimited (text)  | Optional               |
| published_comment     | Comment attached to the last metadata edit                                    | String    | Unlimited (text)  | Optional               |

### QUESTION_TYPE

A supported kind of respondent input (e.g. TEXT, RADIO, CHECKBOX_GROUP); a fixed, platform-wide vocabulary, not survey-scoped.

| Attribute   | Description                                        | Data Type | Length/Precision | Validation Rules      |
|-------------|-----------------------------------------------------|-----------|-------------------|------------------------|
| id          | Unique identifier                                   | Integer   | 10                | Primary Key, Sequence  |
| name        | Question type name                                  | String    | 255               | Not Null, Unique       |
| data_type   | Underlying data type the question type stores       | String    | 255               | Optional               |
| description | Human-readable description of the question type     | String    | 255               | Optional               |

### OPERATOR_TYPE

A comparison operator usable in a relationship's condition (BOOLEAN, GREATER_THAN, EQUAL, NOT_EQUAL, FIELD_EXIST, CONTAINS); a fixed, platform-wide vocabulary.

| Attribute   | Description                                  | Data Type | Length/Precision | Validation Rules      |
|-------------|------------------------------------------------|-----------|-------------------|------------------------|
| id          | Unique identifier                             | Integer   | 10                | Primary Key, Sequence  |
| name        | Operator name                                 | String    | 255               | Not Null, Unique       |
| description | Human-readable description of the operator     | String    | 255               | Optional               |
| symbol      | Display symbol for the operator (e.g. =, >)    | String    | 10                | Optional               |

### ACTION_TYPE

An effect a relationship can trigger when its condition is met (SHOW, REPEAT, TEXT); a fixed, platform-wide vocabulary.

| Attribute   | Description                              | Data Type | Length/Precision | Validation Rules      |
|-------------|--------------------------------------------|-----------|-------------------|------------------------|
| id          | Unique identifier                         | Integer   | 10                | Primary Key, Sequence  |
| name        | Action name                               | String    | 255               | Not Null, Unique       |
| description | Human-readable description of the action   | String    | 255               | Optional               |

### SELECT_GROUP

A named collection of selectable options for choice-based questions. Type 2 SCD structural element (durable key: `select_group_id`).

| Attribute          | Description                                                                              | Data Type | Length/Precision | Validation Rules                                     |
|--------------------|--------------------------------------------------------------------------------------------|-----------|-------------------|--------------------------------------------------------|
| id                 | Surrogate primary key of this specific version row                                        | Integer   | 10                | Primary Key, Sequence                                  |
| survey_id          | Survey this select group belongs to                                                       | Integer   | 10                | Not Null, Foreign Key (surveys.id)                     |
| name               | Select group name                                                                          | String    | 255               | Not Null, Unique among current rows per survey         |
| description        | Select group description                                                                  | String    | 255               | Optional                                               |
| data_type          | Data type of the coded values                                                              | String    | 50                | Not Null, Default: Text                                |
| select_group_id    | Durable key shared by every version of this select group                                  | Integer   | 10                | Not Null                                               |
| version            | Version number, incremented each time a draft is published                                | Integer   | 10                | Not Null, Default: 0                                    |
| effective_from     | Start of this version's validity window                                                   | DateTime  | -                 | Not Null, Default: 1970-01-01T00:00:00Z                 |
| effective_to       | End of this version's validity window; the sentinel `9999-12-31T23:59:59Z` means current   | DateTime  | -                 | Not Null, Default: 9999-12-31T23:59:59Z                 |
| is_draft           | Whether this version row is an unpublished draft                                          | Boolean   | 1                 | Not Null, Default: false                                |
| published_by       | Identity of the user who published this version                                           | String    | Unlimited (text)  | Optional                                               |
| published_comment  | Comment supplied when this version was published                                          | String    | Unlimited (text)  | Optional                                               |

**Constraints:** At most one current row (`effective_to = 9999-12-31T23:59:59Z`) and at most one draft row may exist per `select_group_id`. `(survey_id, name)` is unique only among current rows — a retired version and its successor may legitimately share the old name.

### SELECT_ITEM

One selectable option (coded value + display text) belonging to a select group. Type 2 SCD structural element (durable key: `select_item_id`).

| Attribute              | Description                                                                                     | Data Type | Length/Precision | Validation Rules                                                   |
|------------------------|----------------------------------------------------------------------------------------------------|-----------|-------------------|----------------------------------------------------------------------|
| id                     | Surrogate primary key of this specific version row                                                | Integer   | 10                | Primary Key, Sequence                                                |
| survey_id              | Survey this select item belongs to                                                                | Integer   | 10                | Not Null, Foreign Key (surveys.id)                                   |
| select_group_id        | Durable key of the select group this item belongs to                                              | Integer   | 10                | Not Null, Foreign Key (select_groups.select_group_id, version)       |
| select_group_version   | FK-existence sentinel, always 0 — the select group's actual current version resolves via its own `effective_from`/`effective_to`, not this column | Integer   | 10                | Not Null, Values: 0                                                   |
| display_text           | Option text shown to the respondent                                                                | String    | 255               | Optional                                                             |
| display_order          | Order this option appears in within its select group                                              | Integer   | 10                | Not Null                                                             |
| coded_value            | Stored coded value for this option                                                                | String    | 255               | Optional                                                             |
| select_item_id         | Durable key shared by every version of this select item                                          | Integer   | 10                | Not Null                                                             |
| version                | Version number, incremented each time a draft is published                                        | Integer   | 10                | Not Null, Default: 0                                                  |
| effective_from         | Start of this version's validity window                                                           | DateTime  | -                 | Not Null, Default: 1970-01-01T00:00:00Z                               |
| effective_to           | End of this version's validity window; `9999-12-31T23:59:59Z` means current                        | DateTime  | -                 | Not Null, Default: 9999-12-31T23:59:59Z                               |
| is_draft               | Whether this version row is an unpublished draft                                                  | Boolean   | 1                 | Not Null, Default: false                                              |
| published_by           | Identity of the user who published this version                                                   | String    | Unlimited (text)  | Optional                                                             |
| published_comment      | Comment supplied when this version was published                                                  | String    | Unlimited (text)  | Optional                                                             |

**Constraints:** `(select_item_id, version)` is unique. `(select_group_id, display_text)` is unique among current rows. At most one current row and at most one draft row may exist per `select_item_id`.

### STEP

The top-level navigational unit of a survey. Type 2 SCD structural element (durable key: `step_id`).

| Attribute        | Description                                                                          | Data Type | Length/Precision | Validation Rules                          |
|-------------------|-----------------------------------------------------------------------------------------|-----------|-------------------|---------------------------------------------|
| id                | Surrogate primary key of this specific version row                                     | Integer   | 10                | Primary Key, Sequence                       |
| survey_id         | Survey this step belongs to                                                            | Integer   | 10                | Not Null, Foreign Key (surveys.id)          |
| display_order     | Order this step appears in; decimal-valued so a new step can be inserted between two existing ones without renumbering | Decimal   | 18,6              | Not Null, Unique among current rows per survey |
| name              | Step name                                                                               | String    | 255               | Not Null, Unique among current rows per survey |
| dimension_name    | Name of the reporting dimension this step's structure maps to                          | String    | 50                | Not Null                                    |
| description       | Step description                                                                       | String    | 255               | Optional                                    |
| step_id           | Durable key shared by every version of this step                                       | Integer   | 10                | Not Null                                    |
| version           | Version number, incremented each time a draft is published                            | Integer   | 10                | Not Null, Default: 0                         |
| effective_from    | Start of this version's validity window                                                | DateTime  | -                 | Not Null, Default: 1970-01-01T00:00:00Z      |
| effective_to      | End of this version's validity window; `9999-12-31T23:59:59Z` means current             | DateTime  | -                 | Not Null, Default: 9999-12-31T23:59:59Z      |
| is_draft          | Whether this version row is an unpublished draft                                       | Boolean   | 1                 | Not Null, Default: false                     |
| published_by      | Identity of the user who published this version                                        | String    | Unlimited (text)  | Optional                                    |
| published_comment | Comment supplied when this version was published                                       | String    | Unlimited (text)  | Optional                                    |

**Constraints:** `(step_id, version)` is unique. At most one current row and at most one draft row may exist per `step_id`. Renaming a step is a stakeholder-approved retroactive exception — it relabels historical reports as well as future ones.

### SECTION

A grouping of questions within a step. Type 2 SCD structural element (durable key: `section_id`).

| Attribute        | Description                                                                          | Data Type | Length/Precision | Validation Rules                          |
|-------------------|-----------------------------------------------------------------------------------------|-----------|-------------------|---------------------------------------------|
| id                | Surrogate primary key of this specific version row                                     | Integer   | 10                | Primary Key, Sequence                       |
| survey_id         | Survey this section belongs to                                                         | Integer   | 10                | Not Null, Foreign Key (surveys.id)          |
| display_order     | Order this section appears in; decimal-valued for midpoint insertion without renumbering | Decimal   | 18,6              | Not Null, Unique among current rows per survey |
| name              | Section name                                                                            | String    | 255               | Optional                                    |
| dimension_name    | Name of the reporting dimension this section's structure maps to                       | String    | 50                | Not Null                                    |
| description       | Section description                                                                    | String    | 255               | Optional                                    |
| section_id        | Durable key shared by every version of this section                                    | Integer   | 10                | Not Null                                    |
| version           | Version number, incremented each time a draft is published                            | Integer   | 10                | Not Null, Default: 0                         |
| effective_from    | Start of this version's validity window                                                | DateTime  | -                 | Not Null, Default: 1970-01-01T00:00:00Z      |
| effective_to      | End of this version's validity window; `9999-12-31T23:59:59Z` means current             | DateTime  | -                 | Not Null, Default: 9999-12-31T23:59:59Z      |
| is_draft          | Whether this version row is an unpublished draft                                       | Boolean   | 1                 | Not Null, Default: false                     |
| published_by      | Identity of the user who published this version                                        | String    | Unlimited (text)  | Optional                                    |
| published_comment | Comment supplied when this version was published                                       | String    | Unlimited (text)  | Optional                                    |

**Constraints:** `(section_id, version)` is unique. At most one current row and at most one draft row may exist per `section_id`. Renaming a section is a stakeholder-approved retroactive exception — it relabels historical reports as well as future ones.

### QUESTION

A single respondent-facing question. Type 2 SCD structural element (durable key: `question_id`).

| Attribute             | Description                                                                        | Data Type | Length/Precision | Validation Rules                                              |
|------------------------|---------------------------------------------------------------------------------------|-----------|-------------------|------------------------------------------------------------------|
| id                    | Surrogate primary key of this specific version row                                    | Integer   | 10                | Primary Key, Sequence                                             |
| survey_id             | Survey this question belongs to                                                       | Integer   | 10                | Not Null, Foreign Key (surveys.id)                                |
| type_id               | The question's type                                                                   | Integer   | 10                | Not Null, Foreign Key (question_types.id)                        |
| text                  | Question text shown to the respondent, may contain token placeholders                | String    | 8000              | Not Null                                                          |
| short_text            | Abbreviated question text (e.g. for reports)                                         | String    | 100               | Optional                                                          |
| tool_tip              | Help text shown alongside the question                                                | String    | 255               | Optional                                                          |
| required              | Whether an answer is mandatory                                                        | Boolean   | 1                 | Not Null, Default: false                                          |
| min_value             | Minimum allowed numeric value                                                         | Integer   | 10                | Optional                                                          |
| max_value             | Maximum allowed numeric value                                                         | Integer   | 10                | Optional                                                          |
| validation_text       | Message shown when validation fails                                                   | String    | 255               | Optional                                                          |
| select_group_id       | Durable key of the select group backing this question's choices, if any               | Integer   | 10                | Optional, Foreign Key (select_groups.select_group_id, version)   |
| select_group_version  | FK-existence sentinel, always 0 — the select group's current version resolves via its own `effective_from`/`effective_to` | Integer   | 10                | Not Null, Values: 0                                               |
| mask                  | Input mask applied to the field                                                       | String    | 255               | Optional                                                          |
| placeholder           | Placeholder text shown in the empty input                                             | String    | 255               | Optional                                                          |
| default_value         | Pre-filled value                                                                      | String    | 255               | Optional                                                          |
| variant               | UI variant/style hint for rendering                                                   | String    | 255               | Optional                                                          |
| question_id           | Durable key shared by every version of this question                                 | Integer   | 10                | Not Null                                                          |
| version               | Version number, incremented each time a draft is published                           | Integer   | 10                | Not Null, Default: 0                                               |
| effective_from        | Start of this version's validity window                                               | DateTime  | -                 | Not Null, Default: 1970-01-01T00:00:00Z                           |
| effective_to          | End of this version's validity window; `9999-12-31T23:59:59Z` means current            | DateTime  | -                 | Not Null, Default: 9999-12-31T23:59:59Z                           |
| is_draft              | Whether this version row is an unpublished draft                                      | Boolean   | 1                 | Not Null, Default: false                                          |
| published_by          | Identity of the user who published this version                                       | String    | Unlimited (text)  | Optional                                                          |
| published_comment     | Comment supplied when this version was published                                      | String    | Unlimited (text)  | Optional                                                          |

**Constraints:** `(question_id, version)` is unique. At most one current row and at most one draft row may exist per `question_id`. This entity has no direct link to sections or ontology tags — those come through `SECTIONS_QUESTION` and `METADATA` respectively.

### STEPS_SECTION

The assignment of a section into a step, with per-step and per-section display order captured at assignment time. Type 2 SCD join element (durable key: `steps_sections_id`); `step_id`/`section_id` are durable references, not surrogate ids.

| Attribute              | Description                                                                                        | Data Type | Length/Precision | Validation Rules                                        |
|-------------------------|-------------------------------------------------------------------------------------------------------|-----------|-------------------|-------------------------------------------------------------|
| id                     | Surrogate primary key of this specific version row                                                    | Integer   | 10                | Primary Key, Sequence                                        |
| survey_id              | Survey this assignment belongs to                                                                     | Integer   | 10                | Not Null, Foreign Key (surveys.id)                           |
| step_id                | Durable key of the assigned step                                                                      | Integer   | 10                | Not Null, Foreign Key (steps.step_id, version)               |
| step_version           | FK-existence sentinel, always 0                                                                        | Integer   | 10                | Not Null, Values: 0                                          |
| step_display_order     | Step's display order captured at assignment time                                                      | Decimal   | 18,6              | Not Null                                                     |
| section_id             | Durable key of the assigned section                                                                   | Integer   | 10                | Not Null, Foreign Key (sections.section_id, version)         |
| section_version        | FK-existence sentinel, always 0                                                                        | Integer   | 10                | Not Null, Values: 0                                          |
| section_display_order  | Section's display order captured at assignment time                                                   | Decimal   | 18,6              | Not Null                                                     |
| display_key            | Hierarchical addressing key (`survey.step.step_instance.section.section_instance`)                    | String    | 34                | Not Null, Unique among current rows per survey               |
| steps_sections_id      | Durable key shared by every version of this assignment                                                | Integer   | 10                | Not Null                                                     |
| version                | Version number, incremented each time a draft is published                                           | Integer   | 10                | Not Null, Default: 0                                         |
| effective_from         | Start of this version's validity window                                                               | DateTime  | -                 | Not Null, Default: 1970-01-01T00:00:00Z                      |
| effective_to           | End of this version's validity window; `9999-12-31T23:59:59Z` means current                            | DateTime  | -                 | Not Null, Default: 9999-12-31T23:59:59Z                      |
| is_draft               | Whether this version row is an unpublished draft                                                      | Boolean   | 1                 | Not Null, Default: false                                     |
| published_by           | Identity of the user who published this version                                                       | String    | Unlimited (text)  | Optional                                                     |
| published_comment      | Comment supplied when this version was published                                                       | String    | Unlimited (text)  | Optional                                                     |

**Constraints:** `(steps_sections_id, version)` is unique. `(survey_id, display_key)` is unique among current rows. At most one current row and at most one draft row may exist per `steps_sections_id`. The Display Key's "instance" numbering addresses repeated occurrences within one published version and must not be conflated with `version`.

### SECTIONS_QUESTION

The assignment of a question into a section, with display order captured at assignment time. Type 2 SCD join element (durable key: `sections_question_id`); `question_id`/`section_id` are durable references, not surrogate ids.

| Attribute              | Description                                                                     | Data Type | Length/Precision | Validation Rules                                          |
|-------------------------|------------------------------------------------------------------------------------|-----------|-------------------|---------------------------------------------------------------|
| id                     | Surrogate primary key of this specific version row                                | Integer   | 10                | Primary Key, Sequence                                          |
| survey_id              | Survey this assignment belongs to                                                 | Integer   | 10                | Not Null, Foreign Key (surveys.id)                             |
| question_id            | Durable key of the assigned question                                             | Integer   | 10                | Not Null, Foreign Key (questions.question_id, version)         |
| question_version       | FK-existence sentinel, always 0                                                   | Integer   | 10                | Not Null, Values: 0                                            |
| section_id             | Durable key of the assigned section                                              | Integer   | 10                | Not Null, Foreign Key (sections.section_id, version)           |
| section_version        | FK-existence sentinel, always 0                                                   | Integer   | 10                | Not Null, Values: 0                                            |
| display_order          | Order this question appears in within the section                                | Decimal   | 18,6              | Not Null                                                       |
| sections_question_id   | Durable key shared by every version of this assignment                          | Integer   | 10                | Not Null                                                       |
| version                | Version number, incremented each time a draft is published                      | Integer   | 10                | Not Null, Default: 0                                           |
| effective_from         | Start of this version's validity window                                          | DateTime  | -                 | Not Null, Default: 1970-01-01T00:00:00Z                        |
| effective_to           | End of this version's validity window; `9999-12-31T23:59:59Z` means current        | DateTime  | -                 | Not Null, Default: 9999-12-31T23:59:59Z                        |
| is_draft               | Whether this version row is an unpublished draft                                 | Boolean   | 1                 | Not Null, Default: false                                       |
| published_by           | Identity of the user who published this version                                  | String    | Unlimited (text)  | Optional                                                       |
| published_comment      | Comment supplied when this version was published                                  | String    | Unlimited (text)  | Optional                                                       |

**Constraints:** `(sections_question_id, version)` is unique. `(survey_id, question_id, section_id, display_order)` is unique among current rows. At most one current row and at most one draft row may exist per `sections_question_id`.

### RELATIONSHIP

A conditional-logic rule connecting an upstream question's answer to a downstream step, section assignment, or question assignment. Type 2 SCD structural element (durable key: `relationship_id`); all five cross-references are durable, not surrogate ids.

| Attribute                | Description                                                                              | Data Type | Length/Precision | Validation Rules                                                    |
|---------------------------|----------------------------------------------------------------------------------------------|-----------|-------------------|-------------------------------------------------------------------------|
| id                       | Surrogate primary key of this specific version row                                          | Integer   | 10                | Primary Key, Sequence                                                    |
| survey_id                | Survey this relationship belongs to                                                         | Integer   | 10                | Not Null, Foreign Key (surveys.id)                                       |
| upstream_step_id         | Durable key of the upstream step this relationship reads from, if step-scoped                | Integer   | 10                | Optional, Foreign Key (steps.step_id, version)                          |
| upstream_step_version    | FK-existence sentinel, always 0                                                              | Integer   | 10                | Not Null, Values: 0                                                      |
| upstream_sq_id           | Durable key of the upstream section-question assignment this relationship reads from        | Integer   | 10                | Not Null, Foreign Key (sections_questions.sections_question_id, version)|
| upstream_sq_version      | FK-existence sentinel, always 0                                                              | Integer   | 10                | Not Null, Values: 0                                                      |
| downstream_step_id       | Durable key of the downstream step this relationship affects, if any                        | Integer   | 10                | Optional, Foreign Key (steps.step_id, version)                          |
| downstream_step_version  | FK-existence sentinel, always 0                                                              | Integer   | 10                | Not Null, Values: 0                                                      |
| downstream_ss_id         | Durable key of the downstream step-section assignment this relationship affects, if any      | Integer   | 10                | Optional, Foreign Key (steps_sections.steps_sections_id, version)       |
| downstream_ss_version    | FK-existence sentinel, always 0                                                              | Integer   | 10                | Not Null, Values: 0                                                      |
| downstream_sq_id         | Durable key of the downstream section-question assignment this relationship affects, if any  | Integer   | 10                | Optional, Foreign Key (sections_questions.sections_question_id, version)|
| downstream_sq_version    | FK-existence sentinel, always 0                                                              | Integer   | 10                | Not Null, Values: 0                                                      |
| operator_id              | Comparison operator evaluated against the upstream answer                                   | Integer   | 10                | Not Null, Foreign Key (operator_types.id)                               |
| action_id                | Effect triggered when the condition is met                                                  | Integer   | 10                | Not Null, Foreign Key (action_types.id)                                 |
| description              | Human-readable description of the rule                                                      | String    | 255               | Optional                                                                 |
| token                    | Token name this relationship replaces, for TEXT actions                                     | String    | 10                | Optional                                                                 |
| reference_value          | Value the upstream answer is compared against                                                | String    | 255               | Optional                                                                 |
| default_upstream_value   | Default value used if the upstream question has not been answered                            | String    | 255               | Optional                                                                 |
| override_upstream_value  | Value that overrides the upstream answer for this evaluation                                 | String    | 255               | Optional                                                                 |
| relationship_id          | Durable key shared by every version of this relationship                                    | Integer   | 10                | Not Null                                                                 |
| version                  | Version number, incremented each time a draft is published                                  | Integer   | 10                | Not Null, Default: 0                                                     |
| effective_from           | Start of this version's validity window                                                     | DateTime  | -                 | Not Null, Default: 1970-01-01T00:00:00Z                                 |
| effective_to             | End of this version's validity window; `9999-12-31T23:59:59Z` means current                  | DateTime  | -                 | Not Null, Default: 9999-12-31T23:59:59Z                                 |
| is_draft                 | Whether this version row is an unpublished draft                                            | Boolean   | 1                 | Not Null, Default: false                                                 |
| published_by             | Identity of the user who published this version                                             | String    | Unlimited (text)  | Optional                                                                 |
| published_comment        | Comment supplied when this version was published                                             | String    | Unlimited (text)  | Optional                                                                 |

**Constraints:** `(relationship_id, version)` is unique. At most one current row and at most one draft row may exist per `relationship_id`. Exactly one of `downstream_step_id`, `downstream_sq_id`, `downstream_ss_id` must be set.

### DIMENSION

A named reporting dimension category (e.g. "Age", "Cancer Type") that ontology tags map onto; a platform-wide vocabulary table, not survey-scoped.

| Attribute | Description                    | Data Type | Length/Precision | Validation Rules      |
|-----------|----------------------------------|-----------|-------------------|-------------------------|
| id        | Unique identifier                | Integer   | 10                | Primary Key, Sequence   |
| name      | Dimension name                   | String    | 50                | Not Null, Unique        |

### ONTOLOGY

A named tag, scoped to a survey, that links survey elements to a reporting dimension.

| Attribute | Description                                          | Data Type | Length/Precision | Validation Rules                        |
|-----------|--------------------------------------------------------|-----------|-------------------|--------------------------------------------|
| id        | Unique identifier                                       | Integer   | 10                | Primary Key, Sequence                      |
| survey_id | Survey this ontology tag belongs to                     | Integer   | 10                | Not Null, Foreign Key (surveys.id)         |
| name      | Ontology entry name                                     | String    | 255               | Not Null, Unique with tag                  |
| tag       | Tag value applied to survey elements                    | String    | 255               | Not Null, Unique with name                 |
| dimension | Reporting dimension this tag maps to                    | Integer   | 10                | Optional, Foreign Key (dimensions.id)      |

### METADATA

A link between one structural element (a step/section assignment, a question, or a section/question assignment) and an ontology tag, driving dimensional reporting. Not itself versioned — its element references are durable keys, so one metadata row automatically covers every version of the referenced entity.

| Attribute             | Description                                                                                | Data Type | Length/Precision | Validation Rules                                                          |
|------------------------|-----------------------------------------------------------------------------------------------|-----------|-------------------|--------------------------------------------------------------------------|
| id                    | Unique identifier                                                                            | Integer   | 10                | Primary Key, Sequence                                                     |
| survey_id             | Survey this metadata assignment belongs to                                                   | Integer   | 10                | Not Null, Foreign Key (surveys.id)                                        |
| steps_sections_id     | Durable key of the tagged step/section assignment, if this metadata tags one                 | Integer   | 10                | Optional                                                                  |
| question_id           | Durable key of the tagged question, if this metadata tags one                                | Integer   | 10                | Optional                                                                  |
| sections_question_id  | Durable key of the tagged section/question assignment, if this metadata tags one              | Integer   | 10                | Optional                                                                  |
| ontology_id           | Ontology tag this metadata assigns                                                           | Integer   | 10                | Not Null, Foreign Key (ontology.id)                                      |
| value                 | Value recorded for this metadata assignment                                                  | String    | 255               | Optional                                                                  |

**Constraints:** `(steps_sections_id, question_id, sections_question_id, ontology_id, value)` is unique. Exactly one of `steps_sections_id`, `question_id`, `sections_question_id` must be set. Reassigning a question's ontology tag is a stakeholder-approved retroactive exception — it reclassifies historical answers as well as future ones.

## Excluded from This Model

`respondents`, `answers`, and `dependents` (respondent session and response data) and `reports`/`post_survey_actions`/`respondent_psa` (Admin-configured post-survey behavior) exist in the same `survey` schema but are out of Author's scope per `docs/vision.md` — Author neither creates nor edits them, and no requirement in `docs/requirements.md` reads or writes them.
