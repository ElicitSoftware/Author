# Author interface translations

The files here are the **Author tool's own interface strings** — navigation, buttons, menus, grid
headers, dialog labels, notifications and the built-in guide (UC-040). They are not survey content:
the words an author writes into a survey are translated on the Translations page, stored in
`survey.translations`, and travel inside the `.elicit` export (UC-043..UC-046).

| File | What it is |
| --- | --- |
| `TRANSLATION_REQUEST.md` | Generated hand-off document: every English string with its context, length budget and flags, ready to give to a translator or an AI agent for one new language. |
| `translations_es_419.properties` | Latin American Spanish. |
| `translations_ar.properties` | Arabic (right to left). |

## These are what the application ships

They are packaged onto the classpath at `vaadin-i18n/` by a `<resource>` block in `pom.xml`, next
to the English bundle, so a released image renders the translation it was built and tested with.
There is no mounted directory and no local override: a deployment can neither add a language nor
patch one, and the version of a translation travels with the version of the code.

This directory used to hold readable copies of files that were loaded from elsewhere. That is no
longer true — these are the files the build packages.

English is the exception, and it lives elsewhere on purpose: it is **authored** at
`src/main/resources/vaadin-i18n/translations.properties`, Vaadin's standard location, so the
Copilot internationalization panel keeps editing it in place. Everything here is **received** from
a translator, which is why it sits apart from `src/`.

The packaging include is `translations_*.properties`, not `translations*.properties`, so a stray
English copy in this directory could never shadow the authored one.

## What a site controls

One setting, `i18n.bundled.locales`. It both declares what the image carries — classpath resources
cannot be listed, so nothing can discover which bundles are in the jar — and lets a site offer
fewer than it carries:

```properties
i18n.bundled.locales=en,es-419   # this site offers English and Spanish; Arabic stays unreachable
```

Per-language typography is `META-INF/i18n/i18n-config.json` on the classpath, overridable per tag
by `i18n.direction.<tag>` and `i18n.font-scale.<tag>`.

Not to be confused with `author.content.languages`, which is a different list for a different job:
the languages a **survey's content** may be published in (UC-044 BR-004). The two are kept equal by
the umbrella's build gate, because a survey published in a language the applications lack would
reach no respondent.

## Adding a language

Languages are curated and arrive in a release. Hand `TRANSLATION_REQUEST.md` to a translator or an
AI agent, put the returned `translations_<tag>.properties` here, add the tag to
`i18n.bundled.locales` **and** to `author.content.languages`, and run the i18n tests.
`TranslationBundleConsistencyTest` fails if the new file does not carry exactly the English key set
with matching placeholders.

All three applications must gain the language together — Survey, Admin and Author — or an author
could publish survey content in a language a respondent's Survey cannot render.
