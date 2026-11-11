# Author interface translations

The Author image carries **English only**. Every other language of the tool's own interface
strings (navigation, buttons, menus, grid headers, dialog labels, notifications and the built-in
guide; UC-040) is supplied by the deployment from a mounted directory. They are not survey content:
the words an author writes into a survey are translated on the Translations page, stored in
`survey.translations`, and travel inside the `.elicit` export (UC-043..UC-046).

| Where | What it is |
| --- | --- |
| `src/main/resources/vaadin-i18n/translations.properties` | English, authored here in Vaadin's standard location so the Copilot internationalization panel keeps editing it in place. It is the fallback for every key in every language. |
| `i18n/TRANSLATION_REQUEST.md` | Generated hand-off document: every English string with its context, length budget and flags, ready to give to a translator or an AI agent for one new language. |
| umbrella `elicit-i18n/author/translations_<tag>.properties` | The curated translations: Latin American Spanish (`es_419`) and Arabic (`ar`). |

## How a site gets a language

The tool reads `translations_<tag>.properties` from `<i18n.file.system.path>/author/`
(`/i18n/author/` by default). In docker compose, the umbrella's `elicit-i18n/` is mounted read-only
there; the dev and test profiles read the same directory at `../elicit-i18n`, which is why this
repository sits inside an umbrella checkout. Each file there:

- adds its language to the selector;
- overlays English key by key, so it may hold only the keys it changes, and a missing key falls back
  to English.

With nothing mounted the tool offers English only and shows no language selector.

To offer fewer languages than are mounted, list them:

```properties
i18n.offered.locales=en,es-419   # Spanish offered; a mounted Arabic file is ignored
```

A language left out is not offered, and `?lang=` for it is refused.

Per-language typography is layered:

1. `META-INF/i18n/i18n-config.json` on the classpath.
2. `i18n-config.json` at the mount root.
3. The `i18n.direction.<tag>` and `i18n.font-scale.<tag>` properties.

Each layer overrides the one before.

Not to be confused with `author.content.languages`, which is a different list for a different job:
the languages a **survey's content** may be published in (UC-044 BR-004). It is held equal to the
curated set by `checkLanguages.sh`, because a survey published in a language the applications lack
would reach no respondent.

## Adding a language

Hand `TRANSLATION_REQUEST.md` to a translator or an AI agent. Put the returned
`translations_<tag>.properties` in the umbrella's `elicit-i18n/author/`, add the tag to
`author.content.languages`, and run the i18n tests. `TranslationBundleConsistencyTest` reads
`../elicit-i18n` and fails if a curated file does not carry exactly the English key set with
matching placeholders.

All three applications must gain the language together — Survey, Admin and Author — or an author
could publish survey content in a language a respondent's Survey cannot render. The umbrella's
`checkLanguages.sh` holds `elicit-i18n/survey/`, `admin/` and `author/` to one set and
`author.content.languages` to the same, and fails the build otherwise; `buildDockerImage.sh` here
runs it before building.
