# Author interface translations

The files here are the **Author tool's own interface strings** — navigation, buttons, menus, grid
headers, dialog labels, notifications and the built-in guide (UC-040). They are not survey content:
the words an author writes into a survey are translated on the Translations page, stored in
`survey.translations`, and travel inside the `.elicit` export (UC-043..UC-046).

| File | What it is |
| --- | --- |
| `TRANSLATION_REQUEST.md` | Generated hand-off document: every English string with its context, length budget and flags, ready to give to a translator or an AI agent for one new language. |
| `translations.properties` | English — the default and the fallback for every other language. |
| `translations_es_419.properties` | Latin American Spanish. |
| `translations_ar.properties` | Arabic (right to left). |

## These are readable copies, not what the application loads

Author ships **English only**, on the classpath at
`src/main/resources/vaadin-i18n/translations.properties`. The other languages are supplied by the
deployment, from the umbrella repository's `elicit-i18n/author/`, mounted read-only at `/opt/i18n`
(`i18n.file.system.path`). The copies in this directory exist so that the published translations can
be read next to the hand-off document; nothing loads them, and they are not packaged into the image.

`i18n.local.path` is a real lookup tier, but it resolves to `i18n/<i18n.app.name>` — that is,
`i18n/author/` — so files sitting directly in `i18n/` are never read.

`TranslationBundleConsistencyTest` fails if any copy here goes missing or drifts from
`elicit-i18n/author/`. To refresh them after changing a translation:

```sh
cp ../elicit-i18n/author/translations*.properties i18n/
```

## Adding a language

Hand `TRANSLATION_REQUEST.md` to a translator, put the returned
`translations_<tag>.properties` in `elicit-i18n/author/`, copy it here, and run the i18n tests. The
switcher in the header appears as soon as a second language is mounted.
