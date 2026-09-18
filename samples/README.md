# Sample survey definitions

## `ElicitDesigner.elicit`

A meta-survey: it interviews an author about the survey they want to build, and its
completed responses are the specification for a new `.elicit` definition file.

Import it through **Apply Survey Definition** in the Admin app, or
`POST /secured/survey/apply`; open it for editing through Author's **Import definition**.

| | |
|---|---|
| Format | `ELICIT_SURVEY_EXPORT_V1`, post-Survey-V015 layout (no trailing `is_draft` field) |
| `survey_key` | `b3a7c2e4-5d18-4f92-8a60-1c9e3f75d2b4` |
| Records | 147 — 8 steps, 12 sections, 34 questions, 6 answer lists (32 items), 8 rules |

It is deliberately self-referential in one respect: the `AnswerType`, `RuleOperator`
and `RuleAction` lists do not store free text. Their `coded_value`s **are** the engine's
`type_id`, `operator_id` and `action_id`, so choosing "Single choice — dropdown" stores
`3` — exactly what the `questions` record needs. Responses transcribe into a definition
file rather than needing interpretation.

The conditional logic uses the engine on itself: step/question/list/rule counts `REPEAT`
the block that describes each one, and the delivery questions `SHOW` based on which
channels were ticked. Each count question deliberately sits in the section *before* the
block it repeats, because a section cannot repeat itself.

Re-uploading the file routes to update-in-place (UC-017) rather than a second install,
because `survey_key` is fixed. Its `element_key`s are UUIDv5-derived from stable names,
so regenerating produces identical keys and an update matches existing rows instead of
orphaning them.

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
