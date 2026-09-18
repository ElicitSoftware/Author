# Sample survey definitions

## `ElicitDesigner.elicit`

A meta-survey: it interviews a requester about the steps and sections of the survey they
want to build, and its completed responses are the skeleton for a new `.elicit` definition
file. It deliberately stops there — questions, answer lists, conditional rules and the
ontology/metadata layer are added afterwards by an author with the full picture.

Import it through **Apply Survey Definition** in the Admin app, or
`POST /secured/survey/apply`; open it for editing through Author's **Import definition**.

| | |
|---|---|
| Format | `ELICIT_SURVEY_EXPORT_V1`, post-Survey-V015 layout (no trailing `is_draft` field) |
| `survey_key` | `b3a7c2e4-5d18-4f92-8a60-1c9e3f75d2b4` |
| Records | 71 — 5 steps, 7 sections, 21 questions, 1 answer list (5 items), 3 rules |

The per-step block captures what the `steps`, `sections` and `steps_sections` records
need: the step name, its reporting name (`steps.dimension_name`), every section heading
in order as `Heading=ReportName` (`sections.name` and `sections.dimension_name`), and
which of those sections are re-mounted from an earlier step. That last one matters for
surveys like FHHS, where one "Cancers" section sits under eight different steps and
shares its questions between them.

The conditional logic uses the engine on itself: the step count `REPEAT`s the block that
describes each step, and the delivery questions `SHOW` based on which channels were
ticked. The count question deliberately sits in the section *before* the block it
repeats, because a section cannot repeat itself.

Re-uploading the file routes to update-in-place (UC-017) rather than a second install,
because `survey_key` is fixed. Its `element_key`s are UUIDv5-derived from stable names,
so regenerating produces identical keys and an update matches existing rows instead of
orphaning them.

## `fhhs_base.elicit`

What comes out of the designer when a researcher uses it to recreate the Family Health
History Survey. `fhhs_base.answers.md` is the researcher's answer sheet;
`generate_fhhs_base.py` transcribes it into this file. It is the FHHS *skeleton* only —
15 steps, 15 sections, 25 mounts, 3 reports and 1 post-survey action — with no questions,
answer lists, rules or metadata. Open it in Author's **Import definition** and build the
rest on top of it.

| | |
|---|---|
| `survey_key` | `7d4f2a9c-1e6b-5c83-9f47-2b8e6d1a4c55` |
| Records | 60 — 15 steps, 15 sections, 25 steps_sections, 3 reports, 1 post-survey action |

Where it knowingly differs from the deployed FHHS: the machine name is
`FamilyHistorySurvey` because the designer forbids spaces; the Thank-you step has a
section because the designer requires one (FHHS has none); the Proband Cancer step mounts
its only section at position 1 (FHHS has it at 2); and step and section descriptions are
blank because the designer does not ask for them.

```bash
python3 samples/generate_fhhs_base.py [survey_prefix] [output_path]
```

## `generate_elicit_designer.py`

Regenerates the file above. Field layouts are asserted against the importer's own
required-field counts, so a miscounted record fails at generation rather than on import —
worth having when `questions` records carry 20 fields and `relationships` 19.

```bash
python3 samples/generate_elicit_designer.py [survey_prefix] [output_path]
```

The `survey_prefix` argument only affects how the generated display keys read. It does
not need to match the target deployment: the Admin app's
`SurveyDefinitionImportService` rebases the leading component of every `display_key`
and `initial_display_key` onto the survey id that deployment actually allocates. See
`SurveyDefinitionFileFields#rebaseDisplayKey` and `SurveyDefinitionDisplayKeyRebaseTest`
in the Admin repo.
