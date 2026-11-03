<!--
  One pull request per issue, from a branch named <type>/<issue>-<slug> off main.
  Title: what the change does, in the imperative, without the issue number.
  Tick every line that applies; strike one through (~~like this~~) with a reason when it does not.
  An unticked, unstruck line is a question the reviewer will ask.
-->

## Issue

Closes #

## What changed and why

<!-- Two to five sentences. Behavior first, implementation second. -->

## AIUP

<!-- docs/ is the source of truth; the docs commit comes before the code commit. -->

- [ ] `docs/requirements.md` updated (IDs: ) or no requirement changed
- [ ] `docs/use_cases.puml` and the use case spec(s) updated (IDs: ) or no behavior changed
- [ ] `docs/entity_model.md` updated or no entity changed
- [ ] Tests cite the UC and BR IDs they cover
- [ ] The author's manual under `docs/manual/` updated when a screen changed

## Checks

- [ ] A change to the `.elicit` format is mirrored in Survey's importer and in the umbrella's golden sample (`samples/census-household-survey.elicit`, via its generator), and the sample test still passes
- [ ] `scripts/sync-survey-schema.sh` run when Survey's migrations changed, so the schema mirror is fresh
- [ ] A new rule, control or token form was added to the golden census survey
- [ ] New user-facing text has its key in `translations.properties` and in the umbrella's `elicit-i18n/author/translations_es_419.properties` and `translations_ar.properties` (a branch of the same name there), a context note, and `i18n/TRANSLATION_REQUEST.md` regenerated
- [ ] Cross-module change: the tracking issue in `ElicitSoftware/Elicit` and the sibling PRs are linked above
- [ ] No AI co-author trailer in any commit message
